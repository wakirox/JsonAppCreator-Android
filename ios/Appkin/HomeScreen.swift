import SwiftUI

struct HomeScreen: View {
    @State var showsAskAI = false
    /// The running app's name. On a wide screen (iPhone Duo open) it runs beside this list; on a narrow one it's pushed.
    @State var selection: String?
    @State private var yourApps = SampleData.yourApps

    var body: some View {
        NavigationSplitView {
            List(selection: $selection) {
                Section {
                    Button("New App", systemImage: "plus") {}
                    Button("Make an App with AI", systemImage: "sparkles") { showsAskAI = true }
                }

                Section {
                    DisclosureGroup("How It Works") {
                        Text("Appkin turns a JSON file into an app. Make one with your AI app, write it in the editor, or start from an example.")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }
                }

                Section("Your Apps") {
                    ForEach(yourApps) { app in
                        NavigationLink(value: app.name) { AppRow(app: app) }
                            .swipeActions {
                                Button("Delete", systemImage: "trash", role: .destructive) {
                                    yourApps.removeAll { $0.id == app.id }
                                }
                                Button("Share", systemImage: "square.and.arrow.up") {}
                                    .tint(.blue)
                            }
                            .contextMenu {
                                Button("Edit", systemImage: "curlybraces") {}
                                Button("Share", systemImage: "square.and.arrow.up") {}
                                Button("Delete", systemImage: "trash", role: .destructive) {}
                            }
                    }
                }

                Section {
                    ForEach(SampleData.examples) { app in
                        NavigationLink(value: app.name) { AppRow(app: app) }
                    }
                } header: {
                    Text("Examples")
                } footer: {
                    Text("Edit an example and your version is kept under Your Apps.")
                }
            }
            .navigationTitle("Appkin")
            .toolbar {
                Menu("More", systemImage: "ellipsis") {
                    Button("Paste Answer from AI", systemImage: "doc.on.clipboard") {}
                    Button("Open File…", systemImage: "folder") {}
                    Divider()
                    Button("About Appkin", systemImage: "info.circle") {}
                }
            }
        } detail: {
            if let selection {
                // The app gets half the screen beside the list: lay it out as on a narrow screen.
                NavigationStack { RunningApp(name: selection) }
                    .environment(\.horizontalSizeClass, .compact)
                    .id(selection)
            } else {
                ContentUnavailableView("No App Open", systemImage: "square.grid.2x2",
                                       description: Text("Choose an app to run it here."))
                    .clearOfFold()
            }
        }
        .sheet(isPresented: $showsAskAI) { AskAIScreen() }
    }
}

/// Stands in for the renderer: the screen a JSON app's main view would produce.
struct RunningApp: View {
    let name: String

    var body: some View {
        if name.hasPrefix("Summer Run") { SummerRunScreen() } else { MasterpiecesScreen() }
    }
}

private struct AppRow: View {
    let app: SavedApp

    var body: some View {
        HStack(spacing: 12) {
            AppIcon(app: app)
            VStack(alignment: .leading) {
                Text(app.name)
                Text(app.detail)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
        }
    }
}

#Preview { HomeScreen() }
