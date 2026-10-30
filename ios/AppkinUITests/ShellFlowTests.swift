import XCTest

/// Drives the shell by touch: Home and the examples, Ask your AI app, pasting an answer into the editor, opening it,
/// the problem bar, About and the API key sheet. Screenshots go to /tmp/appkin-shots when that folder exists.
@MainActor
final class ShellFlowTests: XCTestCase {
    let app = XCUIApplication()

    override func setUp() {
        continueAfterFailure = false
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

    /// The system's Paste button reads the clipboard only when tapped, with no question asked.
    private func tapPaste() {
        let paste = app.buttons["Paste"].firstMatch
        XCTAssertTrue(paste.waitForExistence(timeout: 5), "Paste button")
        paste.tap()
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
        app.buttons["Done"].firstMatch.tap()
        app.buttons["Copy"].tap()
        XCTAssertTrue(contains("Copied").waitForExistence(timeout: 5))
        shot("shell-ask-ai")
        app.buttons["See what's sent"].tap()
        shot("shell-ask-preview")
        XCTAssertTrue(contains("characters, about").waitForExistence(timeout: 10))
        app.buttons["Done"].tap()

        // Paste an AI answer: it opens in the editor with no problems, and Open keeps it in Your apps.
        UIPasteboard.general.string = "Here is your app:\n```json\n" + (try raw("v3_race_signup")) + "\n```"
        app.buttons["Ask your AI app"].firstMatch.tap()
        app.swipeUp()
        XCTAssertTrue(contains("Got your AI app's answer").waitForExistence(timeout: 5))
        shot("shell-paste")
        tapPaste()
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
        app.buttons["Ask your AI app"].firstMatch.tap()
        app.swipeUp()
        tapPaste()
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
        // A file that is a valid app says what it is first; "Look at it in the editor" opens it as an opened file.
        app.buttons["Look at it in the editor"].tap()
        XCTAssertTrue(app.navigationBars["Opened file"].waitForExistence(timeout: 10))
        XCTAssertTrue(contains("Opened from a file").exists)
        shot("shell-opened-file")
    }

    func testMoviesUsesTheBundledKey() throws {
        app.launch()
        text("Movies").tap()
        XCTAssertFalse(app.navigationBars["API keys"].waitForExistence(timeout: 3), "the build's key is bundled")
        XCTAssertTrue(app.navigationBars["Movies"].waitForExistence(timeout: 10))
        // Films from TMDB (network needed): a poster title in the popular list.
        XCTAssertTrue(contains("Popular movies").waitForExistence(timeout: 20))
        sleep(6)
        shot("shell-movies")
        XCTAssertGreaterThan(app.images.count + app.staticTexts.count, 5)
    }

    private static let keyTestApp = """
    {"schemaVersion":3,"name":"Key Test","icon":"🔑","version":"1","author":"Test","mainView":{"id":"l0","type":"layout","views":[
    {"type":"text","label":"Key test app"},
    {"type":"form","buttonTitle":"Go","formElements":[{"type":"text","mapping":"q","title":"Q"}],
    "action":{"id":"a1","type":"click","event":{"type":"rest","url":"https://httpbin.org/get?key=__TEST_API_KEY__","method":"get"}}}]}}
    """

    /// A typed API key is kept in the Keychain: after the app is relaunched it isn't asked for again.
    func testApiKeyIsAskedOnceAndRemembered() throws {
        let file = NSTemporaryDirectory() + "key-test-\(UUID().uuidString).json"
        try Self.keyTestApp.write(toFile: file, atomically: true, encoding: .utf8)
        app.launchArguments = ["-openFile", file]
        app.launch()
        app.buttons["Open"].tap()
        XCTAssertTrue(app.navigationBars["API keys"].waitForExistence(timeout: 10))
        XCTAssertTrue(text("TEST_API_KEY").exists)
        let field = app.secureTextFields.firstMatch
        field.tap()
        field.typeText("fake-test-value")
        shot("shell-api-key")
        app.buttons["Load app"].tap()
        XCTAssertTrue(app.navigationBars["Key Test"].waitForExistence(timeout: 10))
        menu("Home")
        XCTAssertTrue(app.navigationBars["Appkin"].waitForExistence(timeout: 5))
        app.terminate()

        app.launchArguments = []
        app.launch()
        XCTAssertTrue(app.navigationBars["Appkin"].waitForExistence(timeout: 10))
        text("Key Test").tap()
        XCTAssertFalse(app.navigationBars["API keys"].waitForExistence(timeout: 3), "read from the Keychain")
        XCTAssertTrue(app.navigationBars["Key Test"].waitForExistence(timeout: 10))
    }
}
