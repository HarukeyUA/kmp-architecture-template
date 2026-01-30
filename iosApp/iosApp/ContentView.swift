import UIKit
import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    let root: RootComponent
    let rootScreen: RootScreen
    let backDispatcher: BackDispatcher

    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController(
            root: root,
            rootScreen: rootScreen,
            backDispatcher: backDispatcher
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    let root: RootComponent
    let rootScreen: RootScreen
    let backDispatcher: BackDispatcher

    var body: some View {
        ComposeView(root: root, rootScreen: rootScreen, backDispatcher: backDispatcher)
            .ignoresSafeArea()
    }
}
