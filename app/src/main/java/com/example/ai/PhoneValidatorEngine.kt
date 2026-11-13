package com.example.ai

import com.example.model.PhoneValidationResult
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber
import java.util.Locale

data class CountryCallingOption(
    val regionCode: String,
    val countryName: String,
    val callingCode: String
)

object PhoneValidatorEngine {

    private val phoneUtil: PhoneNumberUtil by lazy { PhoneNumberUtil.getInstance() }

    val popularCountries: List<CountryCallingOption> = listOf(
        CountryCallingOption("IN", "India", "+91"),
        CountryCallingOption("US", "United States", "+1"),
        CountryCallingOption("GB", "United Kingdom", "+44"),
        CountryCallingOption("AE", "United Arab Emirates", "+971"),
        CountryCallingOption("SG", "Singapore", "+65"),
        CountryCallingOption("AU", "Australia", "+61"),
        CountryCallingOption("CA", "Canada", "+1"),
        CountryCallingOption("DE", "Germany", "+49"),
        CountryCallingOption("JP", "Japan", "+81"),
        CountryCallingOption("SA", "Saudi Arabia", "+966")
    )

    // Verified official Indian & international public emergency/regulatory helplines only
    private val verifiedOfficialHelplines: Map<String, String> = mapOf(
        "112" to "National Emergency Number (India)",
        "100" to "Police Emergency (India)",
        "101" to "Fire Emergency (India)",
        "102" to "Ambulance Service (India)",
        "108" to "Emergency Disaster & Medical (India)",
        "1091" to "Women Helpline (India)",
        "1098" to "Childline India Foundation",
        "1930" to "National Cyber Crime Helpline (I4C, India)",
        "198" to "Telecom Consumer Complaint Helpline (India)",
        "1915" to "National Consumer Helpline (India)",
        "14440" to "UIDAI Helpline (India)",
        "911" to "Emergency Services (US / Canada)",
        "999" to "Emergency Services (UK)"
    )

    fun validate(rawInput: String, defaultRegion: String = "IN"): PhoneValidationResult {
        val cleaned = rawInput.trim()
        val digitsOnly = cleaned.filter { it.isDigit() }

        val officialHelpline = verifiedOfficialHelplines[digitsOnly]
        if (officialHelpline != null) {
            return PhoneValidationResult(
                rawInput = cleaned,
                isValid = true,
                isPossible = true,
                e164Format = digitsOnly,
                internationalFormat = digitsOnly,
                nationalFormat = digitsOnly,
                countryCallingCode = if (defaultRegion == "IN") "+91" else "+1",
                regionCode = defaultRegion,
                countryName = Locale("", defaultRegion).getDisplayCountry(Locale.ENGLISH)
                    .ifBlank { defaultRegion },
                numberType = "OFFICIAL_HELPLINE",
                officialHelplineName = officialHelpline
            )
        }

        return try {
            val parsed: Phonenumber.PhoneNumber = phoneUtil.parse(cleaned, defaultRegion)
            val isValid = phoneUtil.isValidNumber(parsed)
            val isPossible = phoneUtil.isPossibleNumber(parsed)
            val region = phoneUtil.getRegionCodeForNumber(parsed)
                ?: if (parsed.hasCountryCode()) {
                    phoneUtil.getRegionCodeForCountryCode(parsed.countryCode)
                } else defaultRegion
            val normalizedRegion = if (region.isNullOrBlank() || region == "ZZ") defaultRegion else region
            val countryName = Locale("", normalizedRegion).getDisplayCountry(Locale.ENGLISH)
                .ifBlank { "International" }
            val numberType = phoneUtil.getNumberType(parsed).name
            val e164 = phoneUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164)
            val intl = phoneUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL)
            val natl = phoneUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.NATIONAL)

            PhoneValidationResult(
                rawInput = cleaned,
                isValid = isValid,
                isPossible = isPossible,
                e164Format = e164,
                internationalFormat = intl,
                nationalFormat = natl,
                countryCallingCode = "+${parsed.countryCode}",
                regionCode = normalizedRegion,
                countryName = countryName,
                numberType = numberType,
                officialHelplineName = null
            )
        } catch (_: Exception) {
            PhoneValidationResult(
                rawInput = cleaned,
                isValid = false,
                isPossible = false,
                e164Format = cleaned,
                internationalFormat = cleaned,
                nationalFormat = cleaned,
                countryCallingCode = "Unknown",
                regionCode = "--",
                countryName = "Unrecognized Format",
                numberType = "INVALID",
                officialHelplineName = null
            )
        }
    }

    fun extractPhoneNumbersFromText(text: String, defaultRegion: String = "IN"): List<String> {
        if (text.isBlank()) return emptyList()
        val found = linkedSetOf<String>()
        val matches = phoneUtil.findNumbers(text, defaultRegion)
        for (match in matches) {
            val num = match.number()
            if (phoneUtil.isValidNumber(num)) {
                found.add(phoneUtil.format(num, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL))
            }
        }
        // Also catch formatted toll-free or 10-digit Indian numbers if spaced unusually
        val regex = Regex("""(?:\+?\d{1,3}[\s.-]?)?(?:\(?\d{3,5}\)?[\s.-]?)?\d{3,5}[\s.-]?\d{4,5}""")
        regex.findAll(text).forEach { m ->
            val candidate = m.value.trim()
            val digits = candidate.count { it.isDigit() }
            if (digits in 10..15) {
                val res = validate(candidate, defaultRegion)
                if (res.isValid) {
                    found.add(res.internationalFormat)
                }
            }
        }
        return found.toList()
    }
}
