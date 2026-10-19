import SwiftUI

@main
struct AppkinApp: App {
    var body: some Scene {
        WindowGroup {
            SketchRoot(screen: UserDefaults.standard.string(forKey: "screen") ?? "home")
        }
    }
}

/// Opens one screen directly (launch argument `-screen <name>`), so each can be captured.
struct SketchRoot: View {
    let screen: String

    var body: some View {
        switch screen {
        case "app": NavigationStack { MasterpiecesScreen() }
        case "form": NavigationStack { SummerRunScreen() }
        case "editor": NavigationStack { EditorScreen() }
        case "ai": HomeScreen(showsAskAI: true)
        default: HomeScreen()
        }
    }
}
