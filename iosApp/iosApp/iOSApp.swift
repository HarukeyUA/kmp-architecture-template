import ComposeApp
import SwiftUI

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self)
    var appDelegate: AppDelegate
    var body: some Scene {
        WindowGroup {
            ContentView(
                root: appDelegate.root,
                rootScreen: appDelegate.rootScreen,
                backDispatcher: appDelegate.backDispatcher
            )
        }
    }
}

class AppDelegate: NSObject, UIApplicationDelegate {
    private var stateKeeper = StateKeeperDispatcherKt.StateKeeperDispatcher(savedState: nil)
    var backDispatcher: BackDispatcher = BackDispatcherKt.BackDispatcher()

    private lazy var appGraph = IosAppGraphKt.createAppGraph()

    lazy var root: RootComponent = appGraph.rootComponentFactory.create(
        componentContext: DefaultComponentContext(
            lifecycle: ApplicationLifecycle(),
            stateKeeper: stateKeeper,
            instanceKeeper: nil,
            backHandler: backDispatcher
        )
    )

    lazy var rootScreen: RootScreen = appGraph.rootScreen
}
