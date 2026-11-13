package com.example.bluetooth

import android.bluetooth.le.ScanRecord
import android.util.SparseArray
import com.example.model.EarbudBrand

object BrandRecognizer {

    // Bluetooth SIG Company Identifiers (assigned numbers)
    private const val COMPANY_APPLE = 0x004C
    private const val COMPANY_HARMAN_JBL = 0x0057
    private const val COMPANY_SAMSUNG = 0x0075
    private const val COMPANY_SONY = 0x012D
    private const val COMPANY_HARMAN_ALT = 0x039F
    private const val COMPANY_OPLUS_ONEPLUS = 0x049A
    private const val COMPANY_VIVO = 0x0641
    private const val COMPANY_OPPO = 0x079A

    fun identify(
        deviceName: String?,
        gattManufacturerName: String? = null,
        scanRecord: ScanRecord? = null,
        address: String? = null,
    ): EarbudBrand {
        val combined = listOfNotNull(deviceName, gattManufacturerName)
            .joinToString(" ")
            .lowercase()

        if (combined.isNotBlank()) {
            when {
                combined.containsAny("vivo", "iqoo", "tws 3", "tws 4", "tws air", "tws neo") ->
                    return EarbudBrand.VIVO
                combined.containsAny("oneplus", "nord buds", "bullets wireless") ->
                    return EarbudBrand.ONEPLUS
                combined.containsAny("galaxy buds", "samsung", "gear iconx") ->
                    return EarbudBrand.SAMSUNG
                combined.containsAny("jbl", "harman", "tour pro", "tune flex", "tune beam", "live pro") ->
                    return EarbudBrand.JBL
                combined.containsAny("realme", "buds air", "buds t", "buds q", "techlife") ->
                    return EarbudBrand.REALME
                combined.containsAny("oppo", "enco") ->
                    return EarbudBrand.OPPO
                combined.containsAny("boat", "airdopes", "nirvana", "rockerz", "immortal") ->
                    return EarbudBrand.BOAT
                combined.containsAny("airpods", "beats", "apple") ->
                    return EarbudBrand.APPLE
                combined.containsAny("sony", "wf-1000", "wh-1000", "linkbuds", "wf-c") ->
                    return EarbudBrand.SONY
                combined.containsAny("nothing", "ear (1)", "ear (2)", "ear (a)", "cmf buds") ->
                    return EarbudBrand.NOTHING
            }
        }

        val mfgData: SparseArray<ByteArray>? = scanRecord?.manufacturerSpecificData
        if (mfgData != null && mfgData.size() > 0) {
            for (i in 0 until mfgData.size()) {
                when (mfgData.keyAt(i)) {
                    COMPANY_VIVO -> return EarbudBrand.VIVO
                    COMPANY_SAMSUNG -> return EarbudBrand.SAMSUNG
                    COMPANY_HARMAN_JBL, COMPANY_HARMAN_ALT -> return EarbudBrand.JBL
                    COMPANY_OPPO -> return EarbudBrand.OPPO
                    COMPANY_OPLUS_ONEPLUS -> return EarbudBrand.ONEPLUS
                    COMPANY_APPLE -> return EarbudBrand.APPLE
                    COMPANY_SONY -> return EarbudBrand.SONY
                }
            }
        }

        // Common public OUI checks where deterministic
        val normalizedMac = address?.uppercase()?.replace("-", ":") ?: ""
        if (normalizedMac.startsWith("AC:12:03") || normalizedMac.startsWith("DC:56:E7")) {
            return EarbudBrand.VIVO
        }

        return EarbudBrand.GENERIC
    }

    fun isLikelyAudioName(name: String): Boolean {
        return listOf(
            "buds", "tws", "airpods", "airdopes", "enco", "nord", "oneplus",
            "samsung", "galaxy", "jbl", "realme", "oppo", "vivo", "iqoo",
            "boat", "nirvana", "rockerz", "sony", "wf-", "wh-", "nothing",
            "cmf", "ear", "headset", "headphone", "audio", "pods", "sound",
        ).any { name.contains(it, ignoreCase = true) }
    }

    private fun String.containsAny(vararg needles: String): Boolean {
        return needles.any { this.contains(it) }
    }
}
