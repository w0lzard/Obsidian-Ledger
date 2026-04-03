import SwiftUI
import BackgroundTasks
import SharedUI

@main
struct ComposeApp: App {

    @Environment(\.scenePhase) private var scenePhase

    init() {
        // Koin initialisation — runs before any UI
        DIKt.doInitKoin(platformModule: IosModuleKt.iosModule)

        // BGTaskScheduler — must register before app finishes launching
        BGTaskScheduler.shared.register(
            forTaskWithIdentifier: "com.ryuken.obsidianledger.sync",
            using: nil
        ) { task in
            guard let refreshTask = task as? BGAppRefreshTask else { return }
            SyncScheduler().schedule()
            refreshTask.setTaskCompleted(success: true)
        }
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .ignoresSafeArea(.all)
        }
        .onChange(of: scenePhase) { phase in
            if phase == .active {
                // foreground sync — wire AppLifecycleObserver here later
            }
        }
    }
}
