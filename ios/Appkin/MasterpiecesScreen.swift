import SwiftUI

/// What the renderer would produce for the Masterpieces example: a text view and a list.
/// On a wide screen (iPhone Duo open) the list and the opened painting sit side by side, off the fold.
struct MasterpiecesScreen: View {
    @Environment(\.horizontalSizeClass) private var widthClass
    @State var selection: SampleData.Painting.ID?

    var body: some View {
        Group {
            if #available(iOS 27.1, *), widthClass == .regular {
                ArrangementView {
                    list(wide: true)
                } secondary: {
                    if let painting = SampleData.paintings.first(where: { $0.id == selection }) {
                        PaintingDetail(painting: painting)
                    } else {
                        ContentUnavailableView("No Painting Selected", systemImage: "photo")
                    }
                }
                .arrangementViewStyle(.split)
            } else {
                list(wide: false)
            }
        }
        .navigationTitle("Masterpieces")
        .toolbar { AppMenu() }
    }

    private func list(wide: Bool) -> some View {
        // Narrow, rows push the painting: no row stays selected.
        List(selection: wide ? $selection : .constant(nil)) {
            Section {
                ForEach(SampleData.paintings) { painting in
                    if wide {
                        PaintingRow(painting: painting).tag(painting.id)
                    } else {
                        NavigationLink { PaintingScreen(painting: painting) } label: { PaintingRow(painting: painting) }
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
    }
}

private struct PaintingRow: View {
    let painting: SampleData.Painting

    var body: some View {
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

/// The painting on its own screen, pushed over the list on a narrow screen.
struct PaintingScreen: View {
    let painting: SampleData.Painting

    var body: some View {
        PaintingDetail(painting: painting)
            .navigationTitle(painting.title)
            .navigationBarTitleDisplayMode(.inline)
    }
}

struct PaintingDetail: View {
    let painting: SampleData.Painting

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 8) {
                AsyncImage(url: painting.image) { $0.resizable().scaledToFit() } placeholder: { ProgressView() }
                    .clipShape(.rect(cornerRadius: 12))
                Text(painting.title).font(.title2.bold())
                Text(painting.artist).font(.title3)
                Text(painting.year).foregroundStyle(.secondary)
            }
            .padding()
        }
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
