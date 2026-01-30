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
    private static let savedVersionKey = "org.example.project.savedAppVersion"
    private static let savedBuildKey = "org.example.project.savedAppBuild"
    
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
    
    private var currentAppVersion: String {
        Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? ""
    }
    
    private var currentAppBuild: String {
        Bundle.main.infoDictionary?["CFBundleVersion"] as? String ?? ""
    }

    func application(_ application: UIApplication, shouldSaveSecureApplicationState coder: NSCoder) -> Bool {
        StateKeeperUtilsKt.save(coder: coder, state: stateKeeper.save())
        coder.encode(currentAppVersion, forKey: Self.savedVersionKey)
        coder.encode(currentAppBuild, forKey: Self.savedBuildKey)
        return true
    }

    func application(_ application: UIApplication, shouldRestoreSecureApplicationState coder: NSCoder) -> Bool {
        let savedVersion = coder.decodeObject(forKey: Self.savedVersionKey) as? String
        let savedBuild = coder.decodeObject(forKey: Self.savedBuildKey) as? String
        
        guard savedVersion == currentAppVersion && savedBuild == currentAppBuild else {
            return false
        }
        
        stateKeeper = StateKeeperDispatcherKt.StateKeeperDispatcher(savedState: StateKeeperUtilsKt.restore(coder: coder))
        return true
    }
}
