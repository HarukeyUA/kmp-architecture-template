import ComposeApp
import SwiftUI

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self)
    var appDelegate: AppDelegate
    @Environment(\.scenePhase) private var scenePhase

    var body: some Scene {
        WindowGroup {
            ContentView(
                root: appDelegate.root,
                rootScreen: appDelegate.rootScreen,
                backDispatcher: appDelegate.backDispatcher
            )
        }
        // `initial: true` because onChange alone never fires for the phase a cold launch lands on.
        .onChange(of: scenePhase, initial: true) { _, newPhase in
            if newPhase == .active {
                appDelegate.markForegroundSessionOpen()
            }
        }
    }
}

class AppDelegate: NSObject, UIApplicationDelegate {
    private static let savedVersionKey = "org.example.project.savedAppVersion"
    private static let savedBuildKey = "org.example.project.savedAppBuild"
    // Whether the app has been in the foreground since UIKit last wrote its state archive. UIKit
    // writes the archive on backgrounding only and replays it after a crash, so a launch that
    // finds this set knows the archive predates whatever the user did last.
    private static let foregroundSessionOpenKey = "org.example.project.foregroundSessionOpen"
    
    private var stateKeeper = StateKeeperDispatcherKt.StateKeeperDispatcher(savedState: nil)
    var backDispatcher: BackDispatcher = BackDispatcherKt.BackDispatcher()

    // `DEV` is set in SWIFT_ACTIVE_COMPILATION_CONDITIONS by the Debug-dev/Release-dev build
    // configurations (scheme iosApp-dev), so the environment is decided by the scheme at compile
    // time. DevServerHost carries the build Mac's Bonjour name (dev plist only; expanded from
    // Generated.xcconfig at build time).
    private lazy var appGraph: IosAppGraph = {
        #if DEV
        let isDev = true
        #else
        let isDev = false
        #endif
        let devServerHost =
            Bundle.main.object(forInfoDictionaryKey: "DevServerHost") as? String
        return IosAppGraphKt.createAppGraph(isDev: isDev, devServerHost: devServerHost)
    }()

    lazy var root: RootComponent = appGraph.rootComponentFactory.create(
        componentContext: DefaultAppComponentContext(
            lifecycle: ApplicationLifecycle(),
            stateKeeper: stateKeeper,
            instanceKeeper: nil,
            backHandler: backDispatcher,
            snackbarHandler: SnackbarDispatcher()
        )
    )

    lazy var rootScreen: RootScreen = appGraph.rootScreen
    
    private var currentAppVersion: String {
        Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? ""
    }
    
    private var currentAppBuild: String {
        Bundle.main.infoDictionary?["CFBundleVersion"] as? String ?? ""
    }

    func markForegroundSessionOpen() {
        UserDefaults.standard.set(true, forKey: Self.foregroundSessionOpenKey)
    }

    func application(_ application: UIApplication, shouldSaveSecureApplicationState coder: NSCoder) -> Bool {
        StateKeeperUtilsKt.save(coder: coder, state: stateKeeper.save())
        coder.encode(currentAppVersion, forKey: Self.savedVersionKey)
        coder.encode(currentAppBuild, forKey: Self.savedBuildKey)
        UserDefaults.standard.set(false, forKey: Self.foregroundSessionOpenKey)
        return true
    }

    func application(_ application: UIApplication, shouldRestoreSecureApplicationState coder: NSCoder) -> Bool {
        let savedVersion = coder.decodeObject(of: NSString.self, forKey: Self.savedVersionKey) as? String
        let savedBuild = coder.decodeObject(of: NSString.self, forKey: Self.savedBuildKey) as? String
        
        guard savedVersion == currentAppVersion && savedBuild == currentAppBuild else {
            return false
        }
        guard !UserDefaults.standard.bool(forKey: Self.foregroundSessionOpenKey) else {
            return false
        }

        stateKeeper = StateKeeperDispatcherKt.StateKeeperDispatcher(savedState: StateKeeperUtilsKt.restore(coder: coder))
        return true
    }
}
