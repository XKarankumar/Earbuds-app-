import Foundation
import FirebaseAI
import UIKit
import Vision

@MainActor
final class FirebaseAiService: ObservableObject {
    private lazy var model = FirebaseAI.firebaseAI(backend: .googleAI()).generativeModel(
        modelName: "gemini-3.8-flash",
        generationConfig: GenerationConfig(temperature: 0.2)
    )

    func analyzeNumber(validation: PhoneValidationInfo, isBusiness: Bool) async throws -> NumberVerificationReport {
        let prompt = """
        Analyze this phone number for Karan Number Finder Pro AI (Hardik Kumar, v1.0).
        STRICT PRIVACY: Never invent private names, home addresses, Aadhaar, bank info, or live location.
        If not a verified public entity or helpline, respond with "Unavailable (Unlisted / Private Number)".
        Input: \(validation.internationalFormat), Country: \(validation.regionCode) (\(validation.countryCallingCode)), Valid: \(validation.isValid), Type: \(validation.numberType), Mode: \(isBusiness ? "Business" : "Number").
        Respond as JSON with keys: publicCallerOrBusinessName, entityCategory, spamRiskScore (0-100), riskLevel, carrierNetworkNote, confidenceLevel, summaryEn, summaryHi.
        """
        let response = try await model.generateContent(prompt)
        let text = response.text ?? "{}"
        let data = text.data(using: .utf8) ?? Data()
        let json = (try? JSONSerialization.jsonObject(with: data) as? [String: Any]) ?? [:]

        return NumberVerificationReport(
            validation: validation,
            publicCallerOrBusinessName: json["publicCallerOrBusinessName"] as? String ?? "Unavailable (Unlisted / Private Number)",
            entityCategory: json["entityCategory"] as? String ?? "Unlisted / Private",
            spamRiskScore: json["spamRiskScore"] as? Int ?? 20,
            riskLevel: json["riskLevel"] as? String ?? "Low",
            carrierNetworkNote: json["carrierNetworkNote"] as? String ?? "ITU E.164 verified. Live carrier HLR requires authorized paid telecom API.",
            confidenceLevel: json["confidenceLevel"] as? String ?? "Medium",
            sourceAttribution: "PhoneNumberKit + Firebase AI Logic (Gemini 3.8 Flash)",
            summaryEn: json["summaryEn"] as? String ?? text,
            summaryHi: json["summaryHi"] as? String ?? "",
            createdAt: Date()
        )
    }

    func extractTextWithVision(image: UIImage) async throws -> String {
        guard let cgImage = image.cgImage else { return "" }
        return try await withCheckedThrowingContinuation { continuation in
            let request = VNRecognizeTextRequest { req, err in
                if let err { continuation.resume(throwing: err); return }
                let lines = (req.results as? [VNRecognizedTextObservation])?
                    .compactMap { $0.topCandidates(1).first?.string } ?? []
                continuation.resume(returning: lines.joined(separator: "\n"))
            }
            request.recognitionLevel = .accurate
            let handler = VNImageRequestHandler(cgImage: cgImage, options: [:])
            try? handler.perform([request])
        }
    }

    func streamAssistantReply(prompt: String, language: String) -> AsyncThrowingStream<String, Error> {
        AsyncThrowingStream { continuation in
            Task {
                do {
                    let stream = try model.generateContentStream("[\(language)] \(prompt)")
                    var full = ""
                    for try await chunk in stream {
                        if let part = chunk.text {
                            full += part
                            continuation.yield(full)
                        }
                    }
                    continuation.finish()
                } catch {
                    continuation.finish(throwing: error)
                }
            }
        }
    }
}
