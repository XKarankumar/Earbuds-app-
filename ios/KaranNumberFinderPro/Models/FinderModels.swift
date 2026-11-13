import Foundation

enum GlassThemeMode: String, CaseIterable, Identifiable {
    case dark3DGlass = "Dark 3D Glass"
    case whiteGlass = "White Glass"
    var id: String { rawValue }
}

enum DashboardModule: String, CaseIterable, Identifiable {
    case numberLookup = "Number Lookup"
    case photoScanner = "AI Photo Scanner"
    case spamDetector = "AI Spam Detector"
    case aiAssistant = "AI Assistant"
    case businessLookup = "Business Lookup"
    case searchHistory = "Search History"
    case securityCenter = "Security Center"
    case settings = "Settings"

    var id: String { rawValue }

    var systemIcon: String {
        switch self {
        case .numberLookup: return "phone.badge.checkmark"
        case .photoScanner: return "doc.text.viewfinder"
        case .spamDetector: return "shield.lefthalf.filled"
        case .aiAssistant: return "sparkles"
        case .businessLookup: return "building.2.crop.circle"
        case .searchHistory: return "clock.arrow.circlepath"
        case .securityCenter: return "lock.shield"
        case .settings: return "gearshape"
        }
    }
}

struct PhoneValidationInfo: Codable {
    let rawInput: String
    let isValid: Bool
    let e164Format: String
    let internationalFormat: String
    let countryCallingCode: String
    let regionCode: String
    let numberType: String
    let officialHelplineName: String?
}

struct NumberVerificationReport: Codable, Identifiable {
    var id: String { validation.e164Format + "_\(createdAt.timeIntervalSince1970)" }
    let validation: PhoneValidationInfo
    let publicCallerOrBusinessName: String
    let entityCategory: String
    let spamRiskScore: Int
    let riskLevel: String
    let carrierNetworkNote: String
    let confidenceLevel: String
    let sourceAttribution: String
    let summaryEn: String
    let summaryHi: String
    let createdAt: Date
}
