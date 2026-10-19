import SwiftUI

/// What the renderer would produce for the Masterpieces example: a text view and a list.
struct MasterpiecesScreen: View {
    var body: some View {
        List {
            Section {
                ForEach(SampleData.paintings) { painting in
                    NavigationLink { PaintingScreen(painting: painting) } label: {
                        HStack(spacing: 14) {
                            AsyncImage(url: painting.thumbnail) { image in
                                image.resizable().scaledToFill()
                            } placeholder: {
                                Color.secondary.opacity(0.15)
                            }
                            .frame(width: 64, height: 64)
                            .clipShape(.rect(cornerRadius: 8))

                            VStack(alignment: .leading, spacing: 2) {
                                Text(painting.title).font(.headline)
                                Text(painting.artist).foregroundStyle(.secondary)
                                Text(painting.year).font(.subheadline).foregroundStyle(.secondary)
                            }
                        }
                    }
                }
            } header: {
                Text("Eight paintings everyone knows. Tap one to see it up close.")
                    .font(.body)
                    .foregroundStyle(.primary)
                    .textCase(nil)
                    .padding(.bottom, 4)
            } footer: {
                Text("Public-domain works · images from Wikimedia Commons")
            }
        }
        .navigationTitle("Masterpieces")
        .toolbar { AppMenu() }
    }
}

struct PaintingScreen: View {
    let painting: SampleData.Painting

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 8) {
                AsyncImage(url: painting.thumbnail) { $0.resizable().scaledToFit() } placeholder: { ProgressView() }
                Text(painting.artist).font(.title3)
                Text(painting.year).foregroundStyle(.secondary)
            }
            .padding()
        }
        .navigationTitle(painting.title)
        .navigationBarTitleDisplayMode(.inline)
    }
}

/// The menu every JSON app gets (Android's three-dot menu).
struct AppMenu: View {
    var body: some View {
        Menu("More", systemImage: "ellipsis") {
            Button("Edit This App", systemImage: "curlybraces") {}
            Button("Share App", systemImage: "square.and.arrow.up") {}
            Divider()
            Button("About This App", systemImage: "info.circle") {}
        }
    }
}

#Preview { NavigationStack { MasterpiecesScreen() } }
