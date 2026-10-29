import AppkinShared
import SwiftUI

/// Every bundled example read by the shared Kotlin engine: its name and how many reusable views it has. The first
/// proof that the iOS app and Android run the same engine (launch argument `-screen engine`).
struct EngineCheck: View {
    private let examples: [(file: String, result: String)] = Bundle.main
        .urls(forResourcesWithExtension: "json", subdirectory: nil)?
        .sorted { $0.lastPathComponent < $1.lastPathComponent }
        .map { url in
            let file = url.deletingPathExtension().lastPathComponent
            guard let json = try? String(contentsOf: url, encoding: .utf8) else { return (file, "unreadable") }
            let app = AppParser(obj: json).parseApplication2()
            return (file, "\(app.name) · \(app.views.count) views")
        } ?? []

    var body: some View {
        NavigationStack {
            List(examples, id: \.file) { example in
                LabeledContent(example.file, value: example.result)
            }
            .navigationTitle("Shared engine")
        }
    }
}
