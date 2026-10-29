import XCTest

/// Drives the shell by touch: Home and the examples, Ask your AI app, pasting an answer into the editor, opening it,
/// the problem bar, About and the API key sheet. Screenshots go to /tmp/appkin-shots when that folder exists.
@MainActor
final class ShellFlowTests: XCTestCase {
    let app = XCUIApplication()

    override func setUp() {
        continueAfterFailure = false
        addUIInterruptionMonitor(withDescription: "Paste") { alert in
            for label in ["Allow Paste", "Allow"] where alert.buttons[label].exists {
                alert.buttons[label].tap()
                return true
            }
            return false
        }
    }

    private func text(_ label: String) -> XCUIElement {
        app.descendants(matching: .any).matching(NSPredicate(format: "label == %@", label)).firstMatch
    }

    private func contains(_ part: String) -> XCUIElement {
        app.descendants(matching: .any).matching(NSPredicate(format: "label CONTAINS %@", part)).firstMatch
    }

    private func shot(_ name: String) {
        try? XCUIScreen.main.screenshot().pngRepresentation.write(to: URL(fileURLWithPath: "/tmp/appkin-shots/\(name).png"))
    }

    private func raw(_ name: String) throws -> String {
        // .../ios/AppkinUITests/ShellFlowTests.swift -> the repository's raw resources
        let root = URL(fileURLWithPath: #filePath).deletingLastPathComponent().deletingLastPathComponent().deletingLastPathComponent()
        return try String(contentsOf: root.appending(path: "app/src/main/res/raw/\(name).json"), encoding: .utf8)
    }

    private func menu(_ item: String) {
        app.buttons["Menu"].firstMatch.tap()
        let entry = app.buttons[item].firstMatch
        XCTAssertTrue(entry.waitForExistence(timeout: 5), item)
        entry.tap()
    }

    func testHomeExamplesAskPasteEditorAbout() throws {
        app.launch()
        XCTAssertTrue(app.navigationBars["Appkin"].waitForExistence(timeout: 10))
        shot("shell-home")

        // Examples open in the renderer.
        text("Masterpieces").tap()
        XCTAssertTrue(app.navigationBars["Masterpieces"].waitForExistence(timeout: 10))
        shot("shell-masterpieces")
        menu("Home")
        XCTAssertTrue(app.navigationBars["Appkin"].waitForExistence(timeout: 5))
        text("App ideas").tap()
        XCTAssertTrue(app.navigationBars["App ideas"].waitForExistence(timeout: 10))
        menu("Home")

        // Ask your AI app: describe, copy.
        app.buttons["Ask your AI app"].firstMatch.tap()
        let request = app.textFields["ask-request"]
        XCTAssertTrue(request.waitForExistence(timeout: 5))
        request.tap()
        request.typeText("A list of my favourite books")
        app.navigationBars.firstMatch.tap()
        app.buttons["Copy"].tap()
        XCTAssertTrue(contains("Copied").waitForExistence(timeout: 5))
        shot("shell-ask-ai")
        app.buttons["See what's sent"].tap()
        XCTAssertTrue(app.navigationBars["What's sent"].waitForExistence(timeout: 10))
        shot("shell-ask-preview")
        app.buttons["Done"].tap()

        // Paste an AI answer: it opens in the editor with no problems, and Open keeps it in Your apps.
        UIPasteboard.general.string = "Here is your app:\n```json\n" + (try raw("v3_race_signup")) + "\n```"
        app.buttons["Paste answer from your AI app"].tap()
        XCTAssertTrue(app.navigationBars["From your AI app"].waitForExistence(timeout: 10))
        XCTAssertTrue(text("No problems: ready to open").waitForExistence(timeout: 15))
        shot("shell-editor-ok")
        app.buttons["Open"].tap()
        XCTAssertTrue(app.navigationBars.element(boundBy: 0).waitForExistence(timeout: 10))
        menu("Home")
        XCTAssertTrue(app.navigationBars["Appkin"].waitForExistence(timeout: 5))
        XCTAssertTrue(contains("Opened just now").waitForExistence(timeout: 5))

        // About.
        menu("About Appkin")
        XCTAssertTrue(app.navigationBars["About Appkin"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.buttons["Report a problem"].exists)
        shot("shell-about")
        app.buttons["Terms of use"].tap()
        XCTAssertTrue(contains("Appkin shows apps described in JSON files").waitForExistence(timeout: 5))
    }

    func testBrokenAnswerShowsProblems() throws {
        let file = "/tmp/appkin-test/bucharest.json"
        try XCTSkipUnless(FileManager.default.fileExists(atPath: file))
        app.launch()
        UIPasteboard.general.string = try String(contentsOfFile: file, encoding: .utf8)
        menu("Paste answer from your AI app")
        XCTAssertTrue(app.navigationBars["From your AI app"].waitForExistence(timeout: 10))
        XCTAssertTrue(text("5 problems").waitForExistence(timeout: 40))
        XCTAssertTrue(app.buttons["Send problems to your AI app…"].exists)
        shot("shell-editor-problems")
        app.buttons["problems-toggle"].tap()
        shot("shell-editor-problems-open")
    }

    func testOpenedFileGoesToEditor() throws {
        let file = "/tmp/appkin-test/bucharest.json"
        try XCTSkipUnless(FileManager.default.fileExists(atPath: file))
        app.launchArguments = ["-openFile", file]
        app.launch()
        _ = app.navigationBars["Opened file"].waitForExistence(timeout: 10)
        shot("shell-opened-file")
        XCTAssertTrue(app.navigationBars["Opened file"].exists)
        XCTAssertTrue(contains("Opened from a file").exists)
        shot("shell-opened-file")
    }

    func testMoviesAsksForKey() throws {
        app.launch()
        text("Movies").tap()
        XCTAssertTrue(app.navigationBars["API keys"].waitForExistence(timeout: 10))
        shot("shell-api-key")
    }
}
