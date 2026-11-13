// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "KaranNumberFinderProAI",
    platforms: [
        .iOS(.v17)
    ],
    products: [
        .library(
            name: "KaranNumberFinderProKit",
            targets: ["KaranNumberFinderPro"]
        )
    ],
    dependencies: [
        .package(url: "https://github.com/firebase/firebase-ios-sdk.git", from: "11.6.0"),
        .package(url: "https://github.com/marmelroy/PhoneNumberKit.git", from: "4.0.0")
    ],
    targets: [
        .target(
            name: "KaranNumberFinderPro",
            dependencies: [
                .product(name: "FirebaseAI", package: "firebase-ios-sdk"),
                .product(name: "FirebaseAppCheck", package: "firebase-ios-sdk"),
                .product(name: "PhoneNumberKit", package: "PhoneNumberKit")
            ],
            path: "KaranNumberFinderPro"
        )
    ]
)
