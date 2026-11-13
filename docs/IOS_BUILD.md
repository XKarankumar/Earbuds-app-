# iOS Build Instructions (Xcode / SwiftPM)

The editable SwiftUI iOS project is located under `/ios/`.

## Prerequisites
- macOS with **Xcode 15.4+** and **iOS 17+ SDK**
- Apple Developer account (for device signing & App Attest)
- Firebase iOS configuration file (`GoogleService-Info.plist`) downloaded from Firebase project `gen-lang-client-0047373257`

## Setup & Build Steps
1. Open `/ios/Package.swift` in Xcode (or create an Xcode App target importing the `KaranNumberFinderProKit` Swift Package).
2. Add your iOS app bundle in Firebase Console and place `GoogleService-Info.plist` into the app target resources.
3. Verify `Info.plist` includes `NSCameraUsageDescription`, `NSPhotoLibraryUsageDescription`, and `NSFaceIDUsageDescription`.
4. Select an iOS Simulator or physical iPhone target and press **Cmd + R** (Build & Run), or **Product > Archive** to export an `.ipa`.
