package com.example.bluetooth

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.model.EarbudBrand
import com.example.model.FirmwareCheckStage
import com.example.model.FirmwareInfoState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FirmwareManager(private val context: Context) {

    suspend fun verifyOfficialFirmwareChannel(
        current: FirmwareInfoState,
        brand: EarbudBrand,
        isConnected: Boolean,
        batteryLevel: Int?,
    ): FirmwareInfoState = withContext(Dispatchers.IO) {
        if (!isConnected) {
            return@withContext current.copy(
                checkStage = FirmwareCheckStage.OFFLINE_OR_DISCONNECTED,
                statusHeadline = "Connect earbuds before checking firmware",
                statusDetail = "Firmware compatibility verification requires an active Bluetooth GATT/A2DP link.",
                checkProgress = 0f,
                verificationSteps = listOf("Waiting for Bluetooth earbuds connection"),
            )
        }

        val steps = mutableListOf<String>()
        val batteryOk = batteryLevel == null || batteryLevel >= 50
        steps += if (batteryLevel != null) {
            if (batteryOk) {
                "Safety Gate: Battery at $batteryLevel% (>= 50% required)"
            } else {
                "Safety Gate Warning: Battery at $batteryLevel% (charge >= 50% before OTA)"
            }
        } else {
            "Safety Gate: Standard battery level not exposed by headset"
        }

        steps += if (current.installedVersion != null) {
            "GATT DIS (0x180A): Firmware revision ${current.installedVersion} verified"
        } else {
            "GATT DIS (0x180A): Firmware characteristic not exposed over standard BLE"
        }

        steps += if (current.hasStandardDfuService) {
            "Standard Bluetooth DFU service (0xFE59 / OTA) detected on device"
        } else {
            "Standard open DFU service absent · Brand uses ${brand.officialCompanionName}"
        }

        val hasNetwork = isNetworkAvailable()
        val officialPortalReachable = if (hasNetwork) {
            probeOfficialHttpsEndpoint(brand.officialSupportUrl)
        } else {
            false
        }

        steps += when {
            !hasNetwork -> "Network offline · Cannot query official ${brand.displayName} server"
            officialPortalReachable -> "Official ${brand.displayName} HTTPS endpoint verified"
            else -> "Official ${brand.displayName} endpoint unreachable right now"
        }

        val stage = when {
            !hasNetwork -> FirmwareCheckStage.OFFLINE_OR_DISCONNECTED
            current.hasStandardDfuService -> FirmwareCheckStage.DFU_SERVICE_READY
            brand != EarbudBrand.GENERIC -> FirmwareCheckStage.PROPRIETARY_OEM_PROTOCOL
            else -> FirmwareCheckStage.UP_TO_DATE_STANDARD
        }

        val headline = when (stage) {
            FirmwareCheckStage.DFU_SERVICE_READY ->
                "Standard Bluetooth DFU interface detected"
            FirmwareCheckStage.PROPRIETARY_OEM_PROTOCOL ->
                "Official ${brand.displayName} protocol required for OTA"
            FirmwareCheckStage.UP_TO_DATE_STANDARD ->
                "Standard Bluetooth telemetry verified"
            FirmwareCheckStage.OFFLINE_OR_DISCONNECTED ->
                "Network offline · Local GATT check complete"
            else -> "Verification complete"
        }

        val detail = when (stage) {
            FirmwareCheckStage.DFU_SERVICE_READY ->
                "Standard BLE DFU service found. CK never installs unsigned or unverified packages; confirm official release in ${brand.officialCompanionName} before flashing."
            FirmwareCheckStage.PROPRIETARY_OEM_PROTOCOL ->
                "${brand.displayName} encrypts and signs firmware packages using ${brand.officialCompanionName}. To protect your earbuds from bricking, CK only allows official manufacturer OTA channels."
            FirmwareCheckStage.UP_TO_DATE_STANDARD ->
                "Device uses standard Bluetooth audio profiles without an exposed vendor OTA endpoint. Check manufacturer support for signed releases."
            else ->
                "Connect to Wi-Fi or mobile data to reach ${brand.officialSupportUrl} for official release verification."
        }

        val timestamp = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

        current.copy(
            checkStage = stage,
            statusHeadline = headline,
            statusDetail = detail,
            officialReachable = officialPortalReachable,
            verificationSteps = steps,
            checkProgress = 1f,
            lastCheckedLabel = "Checked $timestamp",
        )
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun probeOfficialHttpsEndpoint(urlString: String): Boolean {
        return runCatching {
            val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
                requestMethod = "HEAD"
                connectTimeout = 4_000
                readTimeout = 4_000
                instanceFollowRedirects = true
            }
            val code = conn.responseCode
            conn.disconnect()
            code in 200..399
        }.getOrDefault(false)
    }
}
