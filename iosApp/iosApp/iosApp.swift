import SwiftUI
import SharedUI

@main
struct ComposeApp: App {

    @Environment(\.scenePhase) private var scenePhase

    init() {
        // Debug-binary logging, then Koin — both before any UI or Supabase access.
        IosSyncBridgeKt.installIosLogging()
        DIKt.doInitKoin(platformModule: IosModuleKt.iosModule)
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .ignoresSafeArea(.all)
        }
        .onChange(of: scenePhase) { phase in
            if phase == .active {
                // Foreground bidirectional sync (Android's WorkManager equivalent until
                // BGTaskScheduler background refresh is wired). Cold-start and sign-in
                // pulls are triggered from common App.kt code.
                IosSyncBridgeKt.onForegroundSync()
            }
        }
    }
}
