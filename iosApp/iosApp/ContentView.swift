import SwiftUI
import SharedUI

struct ContentView: View {
    private let root: RootComponent

    init() {
        root = RootComponent(
            componentContext: DefaultComponentContext(
                lifecycle: ApplicationLifecycle()
            )
        )
    }

    var body: some View {
        ComposeUIViewController {
            App(root: root)
        }
        .ignoresSafeArea(.all)
    }
}