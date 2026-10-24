import SwiftUI

/// What the renderer would produce for the Summer Run example: a heading and a form.
struct SummerRunScreen: View {
    @State private var name = "Giulia Rossi"
    @State private var email = "giulia@example.com"
    @State private var distance = "10 km"
    @State private var size = "M"
    @State private var birthday = Calendar.current.date(from: DateComponents(year: 1998, month: 3, day: 12))!
    @State private var acceptsRules = true
    @State private var showsConfirmation = false

    var body: some View {
        Form {
            Section {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Summer Run · 10 km").font(.title2.bold())
                    Text("Saturday 13 June, 8:00 · Villa Borghese, Rome").foregroundStyle(.secondary)
                }
            }

            Section {
                LabeledContent("Full Name") {
                    TextField("Your name", text: $name)
                        .textContentType(.name)
                        .multilineTextAlignment(.trailing)
                }
                LabeledContent("Email") {
                    TextField("you@example.com", text: $email)
                        .textContentType(.emailAddress)
                        .keyboardType(.emailAddress)
                        .textInputAutocapitalization(.never)
                        .multilineTextAlignment(.trailing)
                }
            } footer: {
                Text("We send your race number here.")
            }

            Section("Distance") {
                Picker("Distance", selection: $distance) {
                    ForEach(["5 km", "10 km", "Half Marathon"], id: \.self, content: Text.init)
                }
                .pickerStyle(.segmented)
                .listRowBackground(Color.clear)
                .listRowInsets(EdgeInsets())
            }

            Section {
                Picker("T-Shirt Size", selection: $size) {
                    ForEach(["S", "M", "L", "XL"], id: \.self, content: Text.init)
                }
                DatePicker("Date of Birth", selection: $birthday, displayedComponents: .date)
                Toggle("I accept the race rules", isOn: $acceptsRules)
            } footer: {
                Text("Runners must be 16 or older.")
            }

            Section {
                Button("Sign Me Up") { showsConfirmation = true }
                    .frame(maxWidth: .infinity)
                    .fontWeight(.semibold)
            }
        }
        .clearOfFold()
        .navigationTitle("Summer Run 2026")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar { AppMenu() }
        .alert("You're In", isPresented: $showsConfirmation) {
            Button("OK") {}
        } message: {
            Text("See you at the start line, Giulia.")
        }
    }
}

#Preview { NavigationStack { SummerRunScreen() } }
