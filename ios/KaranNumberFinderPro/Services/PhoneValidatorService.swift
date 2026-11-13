import Foundation
import PhoneNumberKit

final class PhoneValidatorService {
    private let phoneNumberUtility = PhoneNumberUtility()

    private let officialHelplines: [String: String] = [
        "112": "National Emergency Number (India)",
        "100": "Police Emergency (India)",
        "1930": "National Cyber Crime Helpline (India)",
        "1091": "Women Helpline (India)",
        "198": "Telecom Consumer Helpline (India)",
        "911": "Emergency Services (US/Canada)"
    ]

    func validate(_ input: String, defaultRegion: String = "IN") -> PhoneValidationInfo {
        let digits = input.filter(\.isNumber)
        if let official = officialHelplines[digits] {
            return PhoneValidationInfo(
                rawInput: input,
                isValid: true,
                e164Format: digits,
                internationalFormat: digits,
                countryCallingCode: defaultRegion == "IN" ? "+91" : "+1",
                regionCode: defaultRegion,
                numberType: "OFFICIAL_HELPLINE",
                officialHelplineName: official
            )
        }

        do {
            let parsed = try phoneNumberUtility.parse(input, withRegion: defaultRegion)
            return PhoneValidationInfo(
                rawInput: input,
                isValid: true,
                e164Format: phoneNumberUtility.format(parsed, toType: .e164),
                internationalFormat: phoneNumberUtility.format(parsed, toType: .international),
                countryCallingCode: "+\(parsed.countryCode)",
                regionCode: phoneNumberUtility.getRegionCode(of: parsed) ?? defaultRegion,
                numberType: "\(parsed.type)",
                officialHelplineName: nil
            )
        } catch {
            return PhoneValidationInfo(
                rawInput: input,
                isValid: false,
                e164Format: input,
                internationalFormat: input,
                countryCallingCode: "Unknown",
                regionCode: "--",
                numberType: "INVALID",
                officialHelplineName: nil
            )
        }
    }

    func extractNumbers(from text: String, defaultRegion: String = "IN") -> [String] {
        let matches = phoneNumberUtility.findNumbers(text, withRegion: defaultRegion)
        return Array(Set(matches.map { phoneNumberUtility.format($0, toType: .international) }))
    }
}
