import SwiftUI
import FirebaseCore
import FirebaseAppCheck

@main
struct KaranNumberFinderProApp: App {
    @State private var showSplash = true

    init() {
        #if DEBUG
        AppCheck.setAppCheckProviderFactory(AppCheckDebugProviderFactory())
        #else
        AppCheck.setAppCheckProviderFactory(AppAttestProviderFactory())
        #endif
        FirebaseApp.configure()
    }

    var body: some Scene {
        WindowGroup {
            ZStack {
                DashboardView()
                if showSplash {
                    SplashScreenView()
                        .transition(.opacity)
                        .onAppear {
                            DispatchQueue.main.asyncAfter(deadline: .now() + 1.1) {
                                withAnimation(.spring(duration: 0.45)) {
                                    showSplash = false
                                }
                            }
                        }
                }
            }
        }
    }
}

struct SplashScreenView: View {
    var body: some View {
        ZStack {
            LinearGradient(
                colors: [Color(red: 0.02, green: 0.04, blue: 0.08), Color(red: 0.04, green: 0.09, blue: 0.17)],
                startPoint: .top,
                endPoint: .bottom
            ).ignoresSafeArea()

            VStack(spacing: 16) {
                Image(systemName: "shield.lefthalf.filled.badge.checkmark")
                    .font(.system(size: 64))
                    .foregroundStyle(.cyan, .mint)
                Text("Karan Number Finder Pro AI")
                    .font(.title2.bold())
                    .foregroundStyle(.white)
                Text("v1.0 • Hardik Kumar • Guest Mode Ready")
                    .font(.caption)
                    .foregroundStyle(.white.opacity(0.75))
            }
        }
    }
}
