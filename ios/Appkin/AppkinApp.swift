import SwiftUI

@main
struct AppkinApp: App {
    var body: some Scene {
        WindowGroup {
            SketchRoot(screen: UserDefaults.standard.string(forKey: "screen") ?? "home",
                       open: UserDefaults.standard.string(forKey: "open"))
        }
    }
}

/// Opens one screen directly (launch argument `-screen <name>`), so each can be captured. `-open <app name>` runs
/// that app from the home screen, and on the app screen picks the first painting.
struct SketchRoot: View {
    let screen: String
    let open: String?

    var body: some View {
        switch screen {
        case "app": NavigationStack { MasterpiecesScreen(selection: open == nil ? nil : SampleData.paintings[0].id) }
        case "form": NavigationStack { SummerRunScreen() }
        case "editor": NavigationStack { EditorScreen() }
        case "ai": HomeScreen(showsAskAI: true)
        default: HomeScreen(selection: open)
        }
    }
}
