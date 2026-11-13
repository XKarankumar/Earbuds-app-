package com.example.service

import android.annotation.SuppressLint
import android.app.Service
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import com.example.bluetooth.BrandRecognizer
import com.example.bluetooth.PermissionHelper
import com.example.data.StudioPreferencesRepository
import com.example.model.NotificationStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BluetoothMonitorService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var prefsRepo: StudioPreferencesRepository
    private var notificationStyle: NotificationStyle = NotificationStyle.DETAILED_TELEMETRY
    private var lastAlertedLowBatteryAddress: String? = null
    private var a2dpProxy: BluetoothA2dp? = null
    private var receiverRegistered = false

    companion object {
        private const val ACTION_BATTERY_LEVEL_CHANGED =
            "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED"
        private const val EXTRA_BATTERY_LEVEL =
            "android.bluetooth.device.extra.BATTERY_LEVEL"

        fun start(context: Context) {
            if (!PermissionHelper.hasConnectPermission(context)) return
            val intent = Intent(context, BluetoothMonitorService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, BluetoothMonitorService::class.java))
        }
    }

    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
            if (profile == BluetoothProfile.A2DP) {
                a2dpProxy = proxy as? BluetoothA2dp
                updateForegroundTelemetry()
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.A2DP) {
                a2dpProxy = null
            }
        }
    }

    private val btReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(ctx: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            if (!PermissionHelper.hasConnectPermission(this@BluetoothMonitorService)) {
                stopSelf()
                return
            }

            val device = IntentCompat.getParcelableExtra(
                intent,
                BluetoothDevice.EXTRA_DEVICE,
                BluetoothDevice::class.java,
            )

            when (action) {
                BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED,
                BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(
                        BluetoothProfile.EXTRA_STATE,
                        BluetoothProfile.STATE_DISCONNECTED,
                    )
                    if (device != null &&
                        (state == BluetoothProfile.STATE_CONNECTED || state == BluetoothProfile.STATE_DISCONNECTED)
                    ) {
                        val name = runCatching { device.name }.getOrNull() ?: "Bluetooth Audio"
                        val brand = BrandRecognizer.identify(name, address = device.address)
                        val battery = readBatteryReflection(device)
                        NotificationHelper.postConnectionAlert(
                            context = this@BluetoothMonitorService,
                            deviceName = name,
                            brand = brand,
                            connected = state == BluetoothProfile.STATE_CONNECTED,
                            batteryLevel = battery,
                            style = notificationStyle,
                        )
                    }
                    updateForegroundTelemetry()
                }
                ACTION_BATTERY_LEVEL_CHANGED -> {
                    val level = intent.getIntExtra(EXTRA_BATTERY_LEVEL, -1)
                    if (device != null && level in 0..20 && lastAlertedLowBatteryAddress != device.address) {
                        lastAlertedLowBatteryAddress = device.address
                        val name = runCatching { device.name }.getOrNull() ?: "Earbuds"
                        NotificationHelper.postLowBatteryAlert(
                            context = this@BluetoothMonitorService,
                            deviceName = name,
                            batteryLevel = level,
                            style = notificationStyle,
                        )
                    }
                    updateForegroundTelemetry()
                }
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    updateForegroundTelemetry()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        prefsRepo = StudioPreferencesRepository(applicationContext)
        scope.launch {
            notificationStyle = prefsRepo.prefsFlow.first().customization.notificationStyle
            prefsRepo.prefsFlow.collect {
                notificationStyle = it.customization.notificationStyle
                updateForegroundTelemetry()
            }
        }

        if (!PermissionHelper.hasConnectPermission(this)) {
            stopSelf()
            return
        }

        val notification = NotificationHelper.buildMonitorNotification(
            context = this,
            deviceName = null,
            brand = com.example.model.EarbudBrand.GENERIC,
            batteryLevel = null,
            style = notificationStyle,
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NotificationHelper.MONITOR_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE,
            )
        } else {
            startForeground(NotificationHelper.MONITOR_NOTIFICATION_ID, notification)
        }

        val filter = IntentFilter().apply {
            addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
            addAction(ACTION_BATTERY_LEVEL_CHANGED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        ContextCompat.registerReceiver(
            this,
            btReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED,
        )
        receiverRegistered = true

        val adapter = getSystemService(BluetoothManager::class.java)?.adapter
        runCatching {
            adapter?.getProfileProxy(this, profileListener, BluetoothProfile.A2DP)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        updateForegroundTelemetry()
        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun updateForegroundTelemetry() {
        if (!PermissionHelper.hasConnectPermission(this)) return
        val device = runCatching { a2dpProxy?.connectedDevices?.firstOrNull() }.getOrNull()
        val name = device?.let { runCatching { it.name }.getOrNull() }
        val brand = BrandRecognizer.identify(name, address = device?.address)
        val battery = device?.let { readBatteryReflection(it) }

        val updated = NotificationHelper.buildMonitorNotification(
            context = this,
            deviceName = name,
            brand = brand,
            batteryLevel = battery,
            style = notificationStyle,
        )
        val nm = getSystemService(android.app.NotificationManager::class.java)
        nm?.notify(NotificationHelper.MONITOR_NOTIFICATION_ID, updated)
    }

    private fun readBatteryReflection(device: BluetoothDevice): Int? {
        return try {
            val method = device.javaClass.getMethod("getBatteryLevel")
            (method.invoke(device) as? Int)?.takeIf { it in 0..100 }
        } catch (_: Throwable) {
            null
        }
    }

    override fun onDestroy() {
        if (receiverRegistered) {
            runCatching { unregisterReceiver(btReceiver) }
            receiverRegistered = false
        }
        val adapter = getSystemService(BluetoothManager::class.java)?.adapter
        a2dpProxy?.let { runCatching { adapter?.closeProfileProxy(BluetoothProfile.A2DP, it) } }
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
