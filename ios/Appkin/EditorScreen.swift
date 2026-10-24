import SwiftUI

struct EditorScreen: View {
    @State private var json = JSONHighlighter.highlight(SampleJSON.summerRun)
    @State private var showsProblems = false

    var body: some View {
        TextEditor(text: $json)
            .font(.system(.footnote, design: .monospaced))
            .autocorrectionDisabled()
            .textInputAutocapitalization(.never)
            .contentMargins(.horizontal, 12, for: .scrollContent)
            .safeAreaInset(edge: .bottom) {
                Button { showsProblems = true } label: {
                    Label("No Problems: Ready to Run", systemImage: "checkmark.circle.fill")
                        .padding(.horizontal, 6)
                }
                .buttonStyle(.glass)
                .tint(.green)
                .padding(.bottom, 8)
            }
            // Partly folded, the text and its status move to one side of the fold together.
            .clearOfFold()
            .navigationTitle("Edit This App")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel", systemImage: "xmark") {} }
                ToolbarItemGroup(placement: .primaryAction) {
                    Menu("Insert", systemImage: "plus") {
                        Button("Text", systemImage: "textformat") {}
                        Button("Button", systemImage: "button.horizontal") {}
                        Button("Image", systemImage: "photo") {}
                        Button("List", systemImage: "list.bullet") {}
                        Button("Form", systemImage: "list.bullet.rectangle") {}
                    }
                    Button("Run", systemImage: "play.fill") {}
                }
            }
    }
}

enum SampleJSON {
    static let summerRun = """
    {
      "schemaVersion": 3,
      "name": "Summer Run 2026",
      "version": "1.0.0",
      "mainView": {
        "type": "layout",
        "views": [
          {
            "type": "text",
            "label": "Summer Run · 10 km",
            "textStyle": "bold",
            "textSize": 20
          },
          {
            "type": "form",
            "buttonTitle": "Sign me up",
            "formElements": [
              {
                "type": "text",
                "mapping": "name",
                "title": "Full name",
                "mandatory": true
              }
            ]
          }
        ]
      }
    }
    """
}

/// Colours keys, strings, numbers and literals. A sketch: the real editor will use the
/// shared engine's tokens.
enum JSONHighlighter {
    static func highlight(_ text: String) -> AttributedString {
        var result = AttributedString(text)
        let patterns: [(String, Color)] = [
            (#""[^"]*"(?=\s*:)"#, .blue),
            (#":\s*("[^"]*")"#, .green),
            (#"\b-?\d+(\.\d+)?\b"#, .orange),
            (#"\b(true|false|null)\b"#, .purple),
        ]
        for (pattern, color) in patterns {
            let regex = try! NSRegularExpression(pattern: pattern)
            let ns = text as NSString
            for match in regex.matches(in: text, range: NSRange(location: 0, length: ns.length)) {
                let range = match.numberOfRanges > 1 && match.range(at: 1).location != NSNotFound
                    ? match.range(at: 1) : match.range
                if let swiftRange = Range(range, in: text), let attrRange = Range(swiftRange, in: result) {
                    result[attrRange].foregroundColor = color
                }
            }
        }
        return result
    }
}

#Preview { NavigationStack { EditorScreen() } }
