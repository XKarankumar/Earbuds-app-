import SwiftUI

struct DashboardView: View {
    @State private var themeMode: GlassThemeMode = .dark3DGlass
    @State private var phoneInput: String = ""
    @State private var selectedModule: DashboardModule?
    @StateObject private var aiService = FirebaseAiService()
    private let validator = PhoneValidatorService()
    private let vault = EncryptedVaultService()
    @State private var latestReport: NumberVerificationReport?
    @State private var errorMessage: String?
    @State private var isLoading = false

    private let columns = [
        GridItem(.flexible(), spacing: 12),
        GridItem(.flexible(), spacing: 12)
    ]

    var body: some View {
        NavigationStack {
            ZStack {
                LinearGradient(
                    colors: themeMode == .dark3DGlass
                        ? [Color(red: 0.02, green: 0.04, blue: 0.08), Color(red: 0.05, green: 0.10, blue: 0.20)]
                        : [Color(red: 0.95, green: 0.97, blue: 0.99), Color(red: 0.89, green: 0.93, blue: 0.97)],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
                .ignoresSafeArea()

                ScrollView {
                    VStack(spacing: 16) {
                        HStack {
                            VStack(alignment: .leading) {
                                Text("Karan Number Finder")
                                    .font(.title2.bold())
                                Text("Pro AI • Guest Mode Active")
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                            Spacer()
                            Button {
                                withAnimation(.spring) {
                                    themeMode = (themeMode == .dark3DGlass) ? .whiteGlass : .dark3DGlass
                                }
                            } label: {
                                Image(systemName: themeMode == .dark3DGlass ? "sun.max.fill" : "moon.stars.fill")
                                    .padding(10)
                                    .background(.ultraThinMaterial, in: Circle())
                            }
                        }

                        VStack(spacing: 12) {
                            HStack {
                                TextField("+91 98XXX XXXXX", text: $phoneInput)
                                    .keyboardType(.phonePad)
                                    .textFieldStyle(.roundedBorder)
                                Button("Scan") {
                                    Task { await runLookup() }
                                }
                                .buttonStyle(.borderedProminent)
                            }

                            if isLoading {
                                ProgressView("Running E.164 & AI verification...")
                            }
                            if let errorMessage {
                                Text(errorMessage).font(.caption).foregroundStyle(.red)
                            }
                            if let report = latestReport {
                                VStack(alignment: .leading, spacing: 6) {
                                    Text("\(report.validation.internationalFormat) • \(report.riskLevel) (\(report.spamRiskScore)/100)")
                                        .font(.headline)
                                    Text(report.publicCallerOrBusinessName).font(.subheadline)
                                    Text(report.summaryEn).font(.caption).foregroundStyle(.secondary)
                                }
                            }
                        }
                        .padding()
                        .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 22))

                        LazyVGrid(columns: columns, spacing: 12) {
                            ForEach(DashboardModule.allCases) { module in
                                Button {
                                    selectedModule = module
                                } label: {
                                    VStack(alignment: .leading, spacing: 10) {
                                        Image(systemName: module.systemIcon)
                                            .font(.title2)
                                            .foregroundStyle(.cyan)
                                        Text(module.rawValue)
                                            .font(.headline)
                                            .foregroundStyle(.primary)
                                    }
                                    .frame(maxWidth: .infinity, alignment: .leading)
                                    .padding()
                                    .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 20))
                                }
                            }
                        }

                        Text("© 2026 Hardik Kumar • Karan Number Finder Pro AI v1.0")
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                            .padding(.top, 8)
                    }
                    .padding()
                }
            }
            .preferredColorScheme(themeMode == .dark3DGlass ? .dark : .light)
        }
    }

    private func runLookup() async {
        guard !phoneInput.isEmpty else { return }
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }
        let validated = validator.validate(phoneInput)
        do {
            let report = try await aiService.analyzeNumber(validation: validated, isBusiness: false)
            latestReport = report
            try vault.saveReport(report)
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}
