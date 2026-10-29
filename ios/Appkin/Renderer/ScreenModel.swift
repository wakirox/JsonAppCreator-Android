import AppkinShared
import SwiftUI

typealias JSONObject = Kotlinx_serialization_jsonJsonObject

/// One screen of the stack: the view named `viewId` (none: the app's main view) shown with a payload and a title.
struct ScreenRoute: Hashable {
    let id = UUID()
    var viewId: String?
    var payload: String?
    var title: String?
}

/// The screens pushed on the app's navigation stack.
@MainActor
final class Navigator: ObservableObject {
    @Published var path: [ScreenRoute] = []
}

/// A screen's state for SwiftUI: it watches the shared `ScreenViewModel`'s flows and carries out its effects.
@MainActor
final class ScreenModel: ObservableObject {
    let vm: ScreenViewModel
    private let engine: IosEngine
    private let alerts: AlertsViewModel
    private let navigator: Navigator
    private let route: ScreenRoute

    @Published private(set) var data: JSONObject?
    @Published private(set) var loading = false
    @Published private(set) var alert: AlertInfo?
    @Published private(set) var canRefresh = false

    nonisolated(unsafe) private var watchers: [Watching] = []

    init(engine: IosEngine, navigator: Navigator, route: ScreenRoute) {
        self.engine = engine
        self.navigator = navigator
        self.route = route
        let alerts = AlertsViewModel()
        self.alerts = alerts
        vm = engine.screen(viewId: route.viewId, payload: route.payload, title: route.title, alerts: alerts)
        data = vm.data.value as? JSONObject
        watchers = [
            Watch.shared.flow(flow: vm.data) { [weak self] value in self?.data = value as? JSONObject },
            Watch.shared.flow(flow: vm.loading) { [weak self] value in self?.loading = (value as? NSNumber)?.boolValue ?? false },
            Watch.shared.flow(flow: vm.lists.canRefresh) { [weak self] value in self?.canRefresh = (value as? NSNumber)?.boolValue ?? false },
            Watch.shared.flow(flow: RenderKit.shared.alert(alerts: alerts)) { [weak self] value in self?.alert = value as? AlertInfo },
            Watch.shared.flow(flow: RenderKit.shared.effects(screen: vm)) { [weak self] value in
                if let effect = value as? ScreenEffect { self?.handle(effect) }
            }
        ]
    }

    deinit {
        watchers.forEach { $0.cancel() }
        engine.dispose(screen: vm)
    }

    var title: String { vm.title ?? vm.app?.name ?? "" }

    func answerAlert(ok: Bool) {
        alert = nil
        alerts.answer(ok: ok)
    }

    func refresh() { vm.lists.refresh() }

    private func handle(_ effect: ScreenEffect) {
        switch effect.kind {
        case "openScreen":
            navigator.path.append(ScreenRoute(viewId: effect.view?.id, payload: effect.payload?.description, title: effect.title))
        case "close":
            // Only the screen on top goes back.
            if navigator.path.last?.id == route.id { navigator.path.removeLast() }
        case "openUrl":
            if let url = effect.url.flatMap(Self.systemURL) { UIApplication.shared.open(url) }
        case "copy":
            UIPasteboard.general.string = effect.text
        default:
            // Share, calendar, PDF and TextPack come in a later phase.
            break
        }
    }

    /// The address the system opens: a `geo:` link (Android's) becomes a search in Apple Maps.
    private static func systemURL(_ address: String) -> URL? {
        guard address.hasPrefix("geo:") else { return imageURL(address) }
        guard let query = address.split(separator: "?", maxSplits: 1).dropFirst().first,
              let place = query.split(separator: "&").first(where: { $0.hasPrefix("q=") })?.dropFirst(2) else { return nil }
        var components = URLComponents(string: "https://maps.apple.com/")
        components?.queryItems = [URLQueryItem(name: "q", value: String(place).removingPercentEncoding ?? String(place))]
        return components?.url
    }
}

/// The entries of one list: its content, as the shared `ListsState` loads it.
@MainActor
final class ListModel: ObservableObject {
    let entry: ListsStateEntry
    private let lists: ListsState
    @Published private(set) var content: ListsStateContent
    nonisolated(unsafe) private var watcher: Watching?

    init(screen: ScreenViewModel, view: ListView, data: JSONObject?) {
        lists = screen.lists
        entry = screen.lists.get(view: view, data: data)
        content = entry.content.value as! ListsStateContent
        watcher = Watch.shared.flow(flow: entry.content) { [weak self] value in
            if let content = value as? ListsStateContent { self?.content = content }
        }
    }

    deinit { watcher?.cancel() }

    func retry() { lists.retry(list: entry) }
    func loadMore() { lists.loadMore(list: entry) }
}
