import SwiftUI
import ComposeApp

@main
struct RunningHubIosApp: App {
    init() {
        IosRuntimeModuleKt.startRunningHubKoin()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .ignoresSafeArea(.keyboard)
        }
    }
}
