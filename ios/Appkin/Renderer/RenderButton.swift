import AppkinShared
import SwiftUI

/// A button: its text, an icon, or both; a tonal button across its space, or a round one for an icon alone.
struct RenderButton: View {
    @EnvironmentObject private var model: ScreenModel
    let view: ButtonView
    let data: JSONObject?

    var body: some View {
        let title = RenderKit.shared.text(view: view, data: data)
        let symbol = RenderKit.shared.iconName(view: view).map(symbolName)
        let longClick = RenderKit.shared.isLongClick(screen: model.vm, view: view)
        let run = { if !longClick { model.vm.onAction(action: view.action, data: data) } }
        Group {
            if let symbol, RenderKit.shared.isIconOnly(view: view) {
                let spoken = RenderKit.shared.fill(text: view.label, data: data)
                Button(action: run) { Image(systemName: symbol) }
                    .buttonStyle(.bordered)
                    .buttonBorderShape(.circle)
                    .accessibilityLabel(spoken.isEmpty ? (RenderKit.shared.iconName(view: view) ?? "") : spoken)
            } else {
                Button(action: run) {
                    Group {
                        if let symbol { Label(title, systemImage: symbol) } else { Text(title) }
                    }
                    .frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)
                .controlSize(.large)
            }
        }
        .simultaneousGesture(
            LongPressGesture().onEnded { _ in
                if longClick { model.vm.onAction(action: view.action, data: data) }
            }
        )
    }
}

/// The SF Symbol for a JSON button icon.
func symbolName(_ icon: String) -> String {
    switch icon {
    case "add": "plus"
    case "check": "checkmark"
    case "close": "xmark"
    case "copy": "doc.on.doc"
    case "delete": "trash"
    case "edit": "pencil"
    case "email": "envelope"
    case "favorite": "heart"
    case "info": "info.circle"
    case "open": "arrow.up.right.square"
    case "phone": "phone"
    case "place": "mappin.and.ellipse"
    case "refresh": "arrow.clockwise"
    case "search": "magnifyingglass"
    case "settings": "gearshape"
    case "share": "square.and.arrow.up"
    case "star": "star"
    default: "questionmark.circle"
    }
}
