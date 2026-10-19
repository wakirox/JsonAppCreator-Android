import SwiftUI

struct SavedApp: Identifiable {
    let id = UUID()
    let name: String
    let detail: String
    let color: Color
}

enum SampleData {
    static let yourApps = [
        SavedApp(name: "Book Club", detail: "Opened just now", color: .red),
        SavedApp(name: "Summer Run 2026", detail: "Opened yesterday", color: .green),
    ]

    static let examples = [
        SavedApp(name: "Masterpieces", detail: "Famous paintings, with a screen for each", color: .indigo),
        SavedApp(name: "Hello", detail: "The smallest app: one screen of text", color: .teal),
        SavedApp(name: "Summer Run", detail: "A race sign-up form with required fields", color: .orange),
        SavedApp(name: "The Movie DB", detail: "Movies from an online API", color: .pink),
    ]

    struct Painting: Identifiable {
        let id = UUID()
        let title: String
        let artist: String
        let year: String
        let file: String

        var thumbnail: URL {
            URL(string: "https://commons.wikimedia.org/wiki/Special:FilePath/\(file)?width=330")!
        }
    }

    static let paintings = [
        Painting(title: "The Great Wave off Kanagawa", artist: "Katsushika Hokusai", year: "c. 1831",
                 file: "Tsunami_by_hokusai_19th_century.jpg"),
        Painting(title: "Girl with a Pearl Earring", artist: "Johannes Vermeer", year: "c. 1665",
                 file: "1665_Girl_with_a_Pearl_Earring.jpg"),
        Painting(title: "Paris Street; Rainy Day", artist: "Gustave Caillebotte", year: "1877",
                 file: "Gustave_Caillebotte_-_Paris_Street;_Rainy_Day_-_Google_Art_Project.jpg"),
        Painting(title: "The Kiss", artist: "Gustav Klimt", year: "1907–1908",
                 file: "The_Kiss_-_Gustav_Klimt_-_Google_Cultural_Institute.jpg"),
        Painting(title: "The Bedroom", artist: "Vincent van Gogh", year: "1889",
                 file: "Vincent_van_Gogh_-_The_Bedroom_-_Google_Art_Project.jpg"),
        Painting(title: "A Sunday on La Grande Jatte", artist: "Georges Seurat", year: "1884–1886",
                 file: "Georges_Seurat_-_A_Sunday_on_La_Grande_Jatte_--_1884_-_Google_Art_Project.jpg"),
    ]
}

/// The app's letter on a rounded square, like a small app icon.
struct AppIcon: View {
    let app: SavedApp

    var body: some View {
        Text(app.name.prefix(1))
            .font(.headline)
            .foregroundStyle(.white)
            .frame(width: 34, height: 34)
            .background(app.color.gradient, in: .rect(cornerRadius: 8))
    }
}
