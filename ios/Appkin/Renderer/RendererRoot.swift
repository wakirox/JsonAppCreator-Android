import AppkinShared
import SwiftUI

/// Runs a JSON app: its main view in a navigation stack, every opened screen pushed on it.
struct RendererRoot: View {
    private let engine: IosEngine
    @StateObject private var navigator = Navigator()
    private let root = ScreenRoute(viewId: nil, payload: nil, title: nil)

    init(json: String) {
        let version = Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "0"
        engine = IosEngine(json: json, versionName: version, platform: "iOS \(UIDevice.current.systemVersion)", photoData: { PhotoStore.data($0) })
        ImageLoader.userAgent = engine.userAgent
    }

    var body: some View {
        NavigationStack(path: $navigator.path) {
            ScreenHost(engine: engine, navigator: navigator, route: root, isRoot: true)
                .navigationDestination(for: ScreenRoute.self) { route in
                    ScreenHost(engine: engine, navigator: navigator, route: route, isRoot: false)
                }
        }
    }
}

/// Opens a bundled example by its resource name (`v3_masterpieces`).
struct BundledExample: View {
    let name: String

    var body: some View {
        if let url = Bundle.main.url(forResource: name, withExtension: "json"),
           let json = try? String(contentsOf: url, encoding: .utf8) {
            RendererRoot(json: json)
        } else {
            ContentUnavailableView("No example named \(name)", systemImage: "questionmark.folder")
        }
    }
}

/// One screen on the stack, with its title, alert and wait.
private struct ScreenHost: View {
    @StateObject private var model: ScreenModel
    let isRoot: Bool

    init(engine: IosEngine, navigator: Navigator, route: ScreenRoute, isRoot: Bool) {
        _model = StateObject(wrappedValue: ScreenModel(engine: engine, navigator: navigator, route: route))
        self.isRoot = isRoot
    }

    var body: some View {
        RenderScreen()
            .environmentObject(model)
            .navigationTitle(model.title)
            .navigationBarTitleDisplayMode(isRoot ? .large : .inline)
            .overlay {
                if model.loading {
                    ProgressView()
                        .padding(24)
                        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 20))
                }
            }
            .alert(
                model.title,
                isPresented: Binding(get: { model.alert != nil }, set: { _ in }),
                presenting: model.alert
            ) { alert in
                Button("OK") { model.answerAlert(ok: true) }
                if alert.hasCancel { Button("Cancel", role: .cancel) { model.answerAlert(ok: false) } }
            } message: { alert in
                Text(alert.text)
            }
    }
}
