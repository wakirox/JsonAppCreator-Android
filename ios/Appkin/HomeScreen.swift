import SwiftUI

struct HomeScreen: View {
    @State var showsAskAI = false
    @State private var yourApps = SampleData.yourApps

    var body: some View {
        NavigationStack {
            List {
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
                        NavigationLink { MasterpiecesScreen() } label: { AppRow(app: app) }
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
                        NavigationLink { MasterpiecesScreen() } label: { AppRow(app: app) }
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
            .sheet(isPresented: $showsAskAI) { AskAIScreen() }
        }
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
