
import SwiftUI
import SharedUI

struct ComposeUIViewController: UIViewControllerRepresentable {
    let content: () -> UIViewController

    init(_ content: @escaping () -> UIViewController) {
        self.content = content
    }

    func makeUIViewController(context: Context) -> UIViewController {
        content()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}