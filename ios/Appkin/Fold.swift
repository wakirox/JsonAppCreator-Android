import SwiftUI

extension View {
    /// Keeps the view off the fold while an iPhone Duo is partly folded: iOS moves it to one side, as it does its
    /// own sheets and alerts. Open flat or on other iPhones, nothing changes.
    func clearOfFold() -> some View {
        modifier(ClearOfFold())
    }
}

private struct ClearOfFold: ViewModifier {
    func body(content: Content) -> some View {
        if #available(iOS 27.1, *) {
            ArrangementView { content } secondary: { Color.clear }
                .arrangementViewStyle(.overlay)
        } else {
            content
        }
    }
}
