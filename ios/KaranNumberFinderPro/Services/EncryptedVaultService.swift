import Foundation
import CryptoKit
import PDFKit
import UIKit

final class EncryptedVaultService {
    private let keychainAccount = "com.hardikkumar.karanfinder.aeskey.v1"
    private let storageKey = "encrypted_search_history_v1"

    func saveReport(_ report: NumberVerificationReport) throws {
        var existing = loadReports()
        existing.insert(report, at: 0)
        let data = try JSONEncoder().encode(existing)
        let sealed = try AES.GCM.seal(data, using: getOrCreateSymmetricKey())
        if let combined = sealed.combined {
            UserDefaults.standard.set(combined, forKey: storageKey)
        }
    }

    func loadReports() -> [NumberVerificationReport] {
        guard let combined = UserDefaults.standard.data(forKey: storageKey),
              let box = try? AES.GCM.SealedBox(combined: combined),
              let decrypted = try? AES.GCM.open(box, using: getOrCreateSymmetricKey()),
              let decoded = try? JSONDecoder().decode([NumberVerificationReport].self, from: decrypted) else {
            return []
        }
        return decoded
    }

    func deleteReport(id: String) throws {
        let filtered = loadReports().filter { $0.id != id }
        let data = try JSONEncoder().encode(filtered)
        let sealed = try AES.GCM.seal(data, using: getOrCreateSymmetricKey())
        if let combined = sealed.combined {
            UserDefaults.standard.set(combined, forKey: storageKey)
        }
    }

    func exportPdf(reports: [NumberVerificationReport]) -> URL {
        let url = FileManager.default.temporaryDirectory
            .appendingPathComponent("KaranFinder_Report_\(Int(Date().timeIntervalSince1970)).pdf")
        let renderer = UIGraphicsPDFRenderer(bounds: CGRect(x: 0, y: 0, width: 595, height: 842))
        try? renderer.writePDF(to: url) { ctx in
            ctx.beginPage()
            var y: CGFloat = 40
            let title = "Karan Number Finder Pro AI — Security Report (Hardik Kumar, v1.0)"
            title.draw(at: CGPoint(x: 40, y: y), withAttributes: [
                .font: UIFont.boldSystemFont(ofSize: 16)
            ])
            y += 30
            for item in reports {
                let block = "\(item.validation.internationalFormat) • \(item.publicCallerOrBusinessName) • Risk: \(item.riskLevel) (\(item.spamRiskScore)/100)\n\(item.summaryEn)\n"
                block.draw(in: CGRect(x: 40, y: y, width: 515, height: 110), withAttributes: [
                    .font: UIFont.systemFont(ofSize: 11)
                ])
                y += 115
                if y > 740 {
                    ctx.beginPage()
                    y = 40
                }
            }
        }
        return url
    }

    private func getOrCreateSymmetricKey() -> SymmetricKey {
        if let existingData = readKeychainData() {
            return SymmetricKey(data: existingData)
        }
        let newKey = SymmetricKey(size: .bits256)
        let raw = newKey.withUnsafeBytes { Data($0) }
        saveKeychainData(raw)
        return newKey
    }

    private func readKeychainData() -> Data? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrAccount as String: keychainAccount,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]
        var result: AnyObject?
        SecItemCopyMatching(query as CFDictionary, &result)
        return result as? Data
    }

    private func saveKeychainData(_ data: Data) {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrAccount as String: keychainAccount,
            kSecValueData as String: data
        ]
        SecItemAdd(query as CFDictionary, nil)
    }
}
