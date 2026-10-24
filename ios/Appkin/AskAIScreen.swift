import SwiftUI

struct AskAIScreen: View {
    @Environment(\.dismiss) private var dismiss
    @State private var request = "A sign-up form for a 10 km run in Rome"

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Text("Appkin has no AI inside. It gives instructions to an AI app you already use, such as ChatGPT, Claude or Gemini.")
                }
                Section("Describe Your App") {
                    TextField("What should it do?", text: $request, axis: .vertical)
                        .lineLimit(3...6)
                }
                Section {
                    Label("Share it to your AI app", systemImage: "1.circle")
                    Label("Chat there until you like it", systemImage: "2.circle")
                    Label("Open its file in Appkin, or paste the answer", systemImage: "3.circle")
                }
                Section {
                    Button("Copy Instructions", systemImage: "doc.on.doc") {}
                }
            }
            .navigationTitle("Make an App with AI")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel", systemImage: "xmark") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    ShareLink(item: request) { Label("Share", systemImage: "square.and.arrow.up") }
                }
            }
        }
        .presentationDetents([.large])
    }
}

#Preview { AskAIScreen() }
