-- PROOF ONLY: runs the exact compiled setup window's handlers headlessly (no alerts) to prove the
-- AppleScriptObjC runtime path: Product process launch, JSON -> NSDictionary, NSNull handling, and
-- the language-first chrome lookup.
use framework "Foundation"
use scripting additions

on run argv
	set windowScript to item 1 of argv
	set launcherPath to item 2 of argv
	set homePath to item 3 of argv
	set sourceSha to item 4 of argv
	set w to load script (POSIX file windowScript)
	set setupLauncher of w to launcherPath
	set lines to {}

	set languageState to w's loadLanguage({"--setup-language", "--home", homePath, "--system-language", "ja-JP"})
	if languageState is missing value then return "FAIL loadLanguage returned missing value"
	set end of lines to "loadLanguage selected=" & (selectedLanguage of w) & " heading=" & (w's chromeText("setup.heading.choose-language")) & " home=" & (w's chromeText("setup.heading.choose-home"))

	set chosen to w's loadLanguage({"--choose-language", "ar", "--home", homePath})
	set end of lines to "chooseLanguage selected=" & (selectedLanguage of w) & " continue=" & (w's chromeText("setup.button.continue"))

	set inspected to w's runProductEntry({"--inspect-front-door", "--home", homePath, "--expected-source-sha", sourceSha, "--language", "hi"}, false)
	set projection to jsonValue of inspected
	if projection is missing value then return "FAIL inspect produced no JSON: " & (stderrText of inspected)
	set card to projection's objectForKey:"card"
	-- setupPlan and currentServiceSourceSha are JSON null here: the NSNull path that failed with -1728.
	set end of lines to "inspect state=" & (w's dictionaryText(projection, "semanticStateRef", "?")) & " setupPlan=" & (w's dictionaryText(projection, "setupPlan", "<null handled>")) & " serviceSha=" & (w's dictionaryText(projection, "currentServiceSourceSha", "<null handled>"))
	set end of lines to "card title=" & (w's dictionaryText(card, "title", "?")) & " language=" & (w's dictionaryText(card, "language", "?"))

	set oldDelimiters to AppleScript's text item delimiters
	set AppleScript's text item delimiters to linefeed
	set output to lines as text
	set AppleScript's text item delimiters to oldDelimiters
	return output
end run
