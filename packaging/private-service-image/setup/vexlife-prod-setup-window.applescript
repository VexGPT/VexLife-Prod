use framework "AppKit"
use framework "Foundation"
use scripting additions

-- VexLife Setup Journey: native manifestation only.
--
-- This window renders one Product-owned card at a time and returns the chosen action ref.
-- It does not author State, decide which buttons exist, write card copy, or map a choice to an
-- Effect. Every card comes from `--inspect-front-door`; every Control goes back through
-- `--activate-front-door-control`, which re-observes current truth before acting and observes
-- again afterwards. The only things decided here are renderer-local navigation (choose another
-- Home location, check again, close) and how long-running work is shown (progress + Cancel
-- safely), ported from the predecessor's lived AppKit progress panel.

property NSAlertFirstButtonReturn : 1000
property NSModalResponseOK : 1
property NSUTF8StringEncoding : 4
property NSWindowStyleMaskTitled : 1
property NSWindowStyleMaskClosable : 2
property NSBackingStoreBuffered : 2
property fallbackProductOrigin : "http://127.0.0.1:18210/"
property progressPrefix : "[VexLife] "
property controlPrefix : "CONTROL:"
property selectedHomePath : missing value
property sourceSha : missing value
property setupLauncher : missing value

on run
	set sourceSha to my bundleString("VexLifeProdExpectedSourceSha")
	set setupLauncher to my bundleString("VexLifeProdSetupLauncher")
	if not my isFullHex(sourceSha, 40) then
		my showMessage("VexLife could not start.", "The staged VexLife version identity is invalid. Nothing was changed.", "Close")
		return
	end if
	if setupLauncher is missing value or not ((current application's NSFileManager's defaultManager()'s isExecutableFileAtPath:setupLauncher) as boolean) then
		my showMessage("VexLife could not start.", "The exact VexLife setup program is missing from this download. Nothing was changed.", "Close")
		return
	end if

	set defaultHome to (current application's NSHomeDirectory() as text) & "/.vexlife"
	set selectedHomePath to my chooseHome(defaultHome)
	if selectedHomePath is missing value then return

	repeat
		set nextAction to my showEntry()
		if nextAction is "CHOOSE_HOME" then
			set chosen to my chooseHome(selectedHomePath)
			if chosen is not missing value then set selectedHomePath to chosen
		else if nextAction is "RECHECK" then
			-- observe again
		else
			exit repeat
		end if
	end repeat
end run

-- ------------------------------------------------------------------ one journey step

on showEntry()
	set inspected to my runProductEntry({"--inspect-front-door", "--home", selectedHomePath, "--expected-source-sha", sourceSha}, false)
	set projection to jsonValue of inspected
	if projection is missing value then
		return my showProcessFailure("VexLife couldn't check this Mac.", inspected, false)
	end if

	set card to projection's objectForKey:"card"
	if card is missing value or card is (current application's NSNull's |null|()) then
		return my showProcessFailure("VexLife couldn't check this Mac.", inspected, false)
	end if

	set actionRef to my presentCard(card)
	if actionRef starts with controlPrefix then
		set controlRef to text ((length of controlPrefix) + 1) thru -1 of actionRef
		return my activateControl(controlRef)
	end if
	return actionRef
end showEntry

on presentCard(card)
	set titleText to my dictionaryText(card, "title", "VexLife")
	set railText to my dictionaryText(card, "rail", "")
	set bodyText to my dictionaryText(card, "body", "")
	set buttonList to card's objectForKey:"buttons"

	set alert to current application's NSAlert's alloc()'s init()
	alert's setMessageText:titleText
	alert's setInformativeText:railText
	-- The card body is several short sections; as an accessory view it keeps the wide, left-aligned
	-- alert layout (the same mechanism the Home chooser's text field already uses) instead of a
	-- narrow centered column.
	set bodyField to current application's NSTextField's wrappingLabelWithString:bodyText
	bodyField's setSelectable:true
	bodyField's setPreferredMaxLayoutWidth:520
	set fittedSize to bodyField's fittingSize()
	bodyField's setFrame:(current application's NSMakeRect(0, 0, 520, (height of fittedSize)))
	alert's setAccessoryView:bodyField

	set actionRefs to {}
	set buttonCount to (buttonList's |count|()) as integer
	repeat with buttonIndex from 0 to (buttonCount - 1)
		set buttonValue to (buttonList's objectAtIndex:buttonIndex)
		set labelText to my dictionaryText(buttonValue, "label", "")
		set refText to my dictionaryText(buttonValue, "actionRef", "")
		if labelText is not "" and refText is not "" then
			alert's addButtonWithTitle:labelText
			set end of actionRefs to refText
		end if
	end repeat
	if (count of actionRefs) is 0 then return "CLOSE"

	-- The Product puts Close last; let Escape choose it when it is not also the default button.
	if (count of actionRefs) > 1 then
		try
			((alert's |buttons|())'s lastObject())'s setKeyEquivalent:(character id 27)
		end try
	end if

	current application's NSApp's activateIgnoringOtherApps:true
	set response to (alert's runModal()) as integer
	set chosenIndex to response - NSAlertFirstButtonReturn + 1
	if chosenIndex < 1 or chosenIndex > (count of actionRefs) then return "CLOSE"
	return item chosenIndex of actionRefs
end presentCard

on activateControl(controlRef)
	set activated to my runProductEntry({"--activate-front-door-control", controlRef, "--home", selectedHomePath, "--expected-source-sha", sourceSha}, true)

	if cancelledByHuman of activated then
		my showMessage("Setup paused.", "VexLife stopped only the task you started. Anything already finished was kept, and an unfinished download resumes the next time you continue.", "Continue")
		return "RECHECK"
	end if

	set activation to jsonValue of activated
	if activation is missing value then
		return my showProcessFailure("VexLife could not read the result of that step.", activated, true)
	end if

	set failureValue to activation's objectForKey:"failure"
	if failureValue is not missing value and failureValue is not (current application's NSNull's |null|()) then
		set failureTitle to my dictionaryText(failureValue, "title", "VexLife stopped before finishing.")
		set failureDetail to my dictionaryText(failureValue, "detail", "")
		set technicalText to my dictionaryText(failureValue, "technicalDetail", "")
		if technicalText is not "" then set failureDetail to failureDetail & linefeed & linefeed & "Technical detail" & linefeed & technicalText
		set choice to my promptChoice(failureTitle, failureDetail, {"Continue", "Close"})
		if choice is 1 then return "RECHECK"
		return "CLOSE"
	end if

	set manifestationRef to my dictionaryText(activation, "manifestationRef", "")
	if manifestationRef is "OPEN_PRODUCT" then
		set originText to my dictionaryText(activation, "productOrigin", fallbackProductOrigin)
		open location originText
		set noticeValue to activation's objectForKey:"notice"
		if noticeValue is not missing value and noticeValue is not (current application's NSNull's |null|()) then
			my showMessage(my dictionaryText(noticeValue, "title", "VexLife is ready."), my dictionaryText(noticeValue, "detail", ""), "Done")
		end if
		return "DONE"
	end if
	if manifestationRef is "REFRESH_ENTRY" then return "RECHECK"
	if manifestationRef is "NO_ACTION" then return "CLOSE"
	return my showProcessFailure("VexLife returned an instruction this window does not know.", activated, true)
end activateControl

-- ------------------------------------------------------------------ Vex Home chooser

on chooseHome(suggestedHome)
	set candidate to suggestedHome
	repeat
		set alert to current application's NSAlert's alloc()'s init()
		alert's setMessageText:"Choose your Vex Home"
		alert's setInformativeText:("Your Vex Home is the one folder on this Mac where VexLife keeps your identity, conversations, memory and local model." & linefeed & linefeed & "~/.vexlife is only the default suggestion. You can type another location or choose a folder; VexLife looks at it first and changes nothing until you agree.")
		alert's addButtonWithTitle:"Continue"
		alert's addButtonWithTitle:"Choose Folder…"
		alert's addButtonWithTitle:"Cancel"
		set field to current application's NSTextField's alloc()'s initWithFrame:(current application's NSMakeRect(0, 0, 480, 26))
		field's setStringValue:candidate
		field's setPlaceholderString:"~/.vexlife"
		alert's setAccessoryView:field
		current application's NSApp's activateIgnoringOtherApps:true
		set response to (alert's runModal()) as integer

		if response is NSAlertFirstButtonReturn then
			set chosen to my normalizedHomePath(field's stringValue())
			if chosen is not missing value then return chosen
			my showMessage("Choose a full folder location.", "Type a location that starts with / or ~/, for example ~/.vexlife, or use Choose Folder….", "OK")
			set candidate to (field's stringValue()) as text
		else if response is (NSAlertFirstButtonReturn + 1) then
			set picked to my pickFolder(candidate)
			if picked is not missing value then set candidate to picked
		else
			return missing value
		end if
	end repeat
end chooseHome

on pickFolder(startPath)
	set panel to current application's NSOpenPanel's openPanel()
	panel's setCanChooseDirectories:true
	panel's setCanChooseFiles:false
	panel's setCanCreateDirectories:true
	panel's setAllowsMultipleSelection:false
	panel's setPrompt:"Use This Folder"
	panel's setMessage:"Choose an empty folder (use New Folder to make one) or an existing Vex Home."
	set startDirectory to my existingAncestor(startPath)
	if startDirectory is not missing value then
		panel's setDirectoryURL:(current application's NSURL's fileURLWithPath:startDirectory)
	end if
	current application's NSApp's activateIgnoringOtherApps:true
	set response to (panel's runModal()) as integer
	if response is not NSModalResponseOK then return missing value
	set chosenURL to (panel's URLs())'s firstObject()
	if chosenURL is missing value then return missing value
	return (chosenURL's |path|()) as text
end pickFolder

on existingAncestor(pathText)
	set candidatePath to my normalizedHomePath(pathText)
	if candidatePath is missing value then return (current application's NSHomeDirectory() as text)
	set fileManager to current application's NSFileManager's defaultManager()
	set cursor to current application's NSString's stringWithString:candidatePath
	repeat 64 times
		if (fileManager's fileExistsAtPath:cursor) as boolean then return cursor as text
		set parentPath to cursor's stringByDeletingLastPathComponent()
		if (parentPath as text) is (cursor as text) then exit repeat
		set cursor to parentPath
	end repeat
	return (current application's NSHomeDirectory() as text)
end existingAncestor

on normalizedHomePath(value)
	if value is missing value then return missing value
	set rawValue to value as text
	if rawValue is "" then return missing value
	set standardized to (current application's NSString's stringWithString:rawValue)'s stringByStandardizingPath()
	if standardized is missing value then return missing value
	set homePath to standardized as text
	if homePath does not start with "/" then return missing value
	return homePath
end normalizedHomePath

-- ------------------------------------------------------------------ process + progress

on runProductEntry(argumentsList, showProgress)
	set fileManager to current application's NSFileManager's defaultManager()
	set token to current application's NSUUID's UUID()'s UUIDString() as text
	set tempRoot to current application's NSTemporaryDirectory() as text
	set outPath to tempRoot & "vexlife-prod-entry-" & token & ".out"
	set errPath to tempRoot & "vexlife-prod-entry-" & token & ".err"
	fileManager's createFileAtPath:outPath |contents|:(missing value) attributes:(missing value)
	fileManager's createFileAtPath:errPath |contents|:(missing value) attributes:(missing value)
	set outHandle to current application's NSFileHandle's fileHandleForWritingAtPath:outPath
	set errHandle to current application's NSFileHandle's fileHandleForWritingAtPath:errPath

	set task to current application's NSTask's alloc()'s init()
	task's setExecutableURL:(current application's NSURL's fileURLWithPath:setupLauncher)
	task's setArguments:argumentsList
	task's setStandardOutput:outHandle
	task's setStandardError:errHandle

	set progressPanel to missing value
	set progressLabel to missing value
	set cancelledByHuman to false
	set launchErrorText to ""
	set taskStatus to 127

	if showProgress then
		set progressSurface to my beginProgress()
		set progressPanel to item 1 of progressSurface
		set progressLabel to item 2 of progressSurface
	end if

	set {launched, launchError} to task's launchAndReturnError:(reference)
	if launched is true then
		repeat while (task's isRunning()) as boolean
			current application's NSRunLoop's currentRunLoop()'s runUntilDate:(current application's NSDate's dateWithTimeIntervalSinceNow:0.2)
			if progressPanel is not missing value then
				if cancelledByHuman is false and ((progressPanel's isVisible()) as boolean) is false then
					set cancelledByHuman to true
					try
						task's terminate()
					end try
				end if
				set liveLine to my latestProgressLine(errPath)
				if liveLine is not missing value then progressLabel's setStringValue:liveLine
			end if
		end repeat
		set taskStatus to (task's terminationStatus()) as integer
	else
		set launchErrorText to "The VexLife setup program could not be launched."
		if launchError is not missing value then set launchErrorText to launchErrorText & " " & ((launchError's localizedDescription()) as text)
	end if

	if progressPanel is not missing value then
		try
			progressPanel's orderOut:(missing value)
		end try
	end if
	try
		outHandle's closeFile()
		errHandle's closeFile()
	end try

	set outData to current application's NSData's dataWithContentsOfFile:outPath
	set errData to current application's NSData's dataWithContentsOfFile:errPath
	set outText to ""
	set errText to launchErrorText
	if outData is not missing value then set outText to (current application's NSString's alloc()'s initWithData:outData encoding:NSUTF8StringEncoding) as text
	if errData is not missing value then set errText to errText & ((current application's NSString's alloc()'s initWithData:errData encoding:NSUTF8StringEncoding) as text)

	set parsed to missing value
	if outData is not missing value and (outData's |length|()) as integer > 0 then
		set {jsonObject, parseError} to current application's NSJSONSerialization's JSONObjectWithData:outData options:0 |error|:(reference)
		if jsonObject is not missing value then set parsed to jsonObject
	end if

	try
		fileManager's removeItemAtPath:outPath |error|:(missing value)
		fileManager's removeItemAtPath:errPath |error|:(missing value)
	end try

	return {exitCode:taskStatus, stdoutText:outText, stderrText:errText, cancelledByHuman:cancelledByHuman, jsonValue:parsed}
end runProductEntry

on beginProgress()
	set panelStyle to NSWindowStyleMaskTitled + NSWindowStyleMaskClosable
	set panel to current application's NSPanel's alloc()'s initWithContentRect:(current application's NSMakeRect(0, 0, 520, 168)) styleMask:panelStyle backing:NSBackingStoreBuffered defer:false
	panel's setTitle:"VexLife setup"
	panel's setReleasedWhenClosed:false
	set label to current application's NSTextField's labelWithString:"Working on it. Keep this window open."
	label's setFrame:(current application's NSMakeRect(24, 84, 472, 56))
	label's setLineBreakMode:0
	label's setUsesSingleLineMode:false
	label's setSelectable:true
	set hint to current application's NSTextField's labelWithString:"Cancel safely stops only this task. Finished work is kept and downloads resume next time."
	hint's setFrame:(current application's NSMakeRect(24, 56, 472, 18))
	hint's setSelectable:true
	set indicator to current application's NSProgressIndicator's alloc()'s initWithFrame:(current application's NSMakeRect(24, 25, 345, 18))
	indicator's setIndeterminate:true
	indicator's startAnimation:(missing value)
	set cancelButton to current application's NSButton's buttonWithTitle:"Cancel safely" target:panel action:"performClose:"
	cancelButton's setFrame:(current application's NSMakeRect(385, 14, 111, 32))
	panel's contentView()'s addSubview:label
	panel's contentView()'s addSubview:hint
	panel's contentView()'s addSubview:indicator
	panel's contentView()'s addSubview:cancelButton
	panel's |center|()
	panel's makeKeyAndOrderFront:(missing value)
	current application's NSApp's activateIgnoringOtherApps:true
	return {panel, label}
end beginProgress

on latestProgressLine(filePath)
	set progressData to current application's NSData's dataWithContentsOfFile:filePath
	if progressData is missing value then return missing value
	set progressString to current application's NSString's alloc()'s initWithData:progressData encoding:NSUTF8StringEncoding
	if progressString is missing value then return missing value
	set oldDelimiters to AppleScript's text item delimiters
	set AppleScript's text item delimiters to linefeed
	set sourceLines to text items of (progressString as text)
	set AppleScript's text item delimiters to oldDelimiters
	repeat with lineIndex from (count of sourceLines) to 1 by -1
		set lineText to item lineIndex of sourceLines as text
		if lineText starts with progressPrefix then
			if (length of lineText) > (length of progressPrefix) then return text ((length of progressPrefix) + 1) thru -1 of lineText
		end if
	end repeat
	return missing value
end latestProgressLine

-- ------------------------------------------------------------------ small helpers

-- effectMayHaveRun is false only for the no-effect inspection process. An activation whose result
-- is unreadable may already have created the Home, downloaded files or started VexLife, so the
-- window must not claim that nothing changed; it re-observes instead.
on showProcessFailure(titleText, resultRecord, effectMayHaveRun)
	set detailText to stderrText of resultRecord
	if detailText is "" then set detailText to stdoutText of resultRecord
	if detailText is "" then set detailText to "The setup program stopped without a readable explanation."
	if effectMayHaveRun then
		set leadText to "VexLife can't tell from this result what finished. Choose Check Again to see what is true on this Mac now; anything that finished was kept."
	else
		set leadText to "Checking doesn't change anything on this Mac, and nothing was changed."
	end if
	set choice to my promptChoice(titleText, leadText & linefeed & linefeed & (my boundedText(detailText)), {"Check Again", "Close"})
	if choice is 1 then return "RECHECK"
	return "CLOSE"
end showProcessFailure

on promptChoice(titleText, detailText, buttonTitles)
	set alert to current application's NSAlert's alloc()'s init()
	alert's setMessageText:titleText
	alert's setInformativeText:detailText
	repeat with buttonTitle in buttonTitles
		alert's addButtonWithTitle:(buttonTitle as text)
	end repeat
	current application's NSApp's activateIgnoringOtherApps:true
	return ((alert's runModal()) as integer) - NSAlertFirstButtonReturn + 1
end promptChoice

on showMessage(titleText, detailText, buttonTitle)
	my promptChoice(titleText, detailText, {buttonTitle})
end showMessage

on dictionaryText(dictionaryValue, keyName, fallbackValue)
	if dictionaryValue is missing value then return fallbackValue
	if dictionaryValue is (current application's NSNull's |null|()) then return fallbackValue
	set value to dictionaryValue's objectForKey:keyName
	if value is missing value then return fallbackValue
	if value is (current application's NSNull's |null|()) then return fallbackValue
	try
		set textValue to value as text
	on error
		return fallbackValue
	end try
	if textValue is "null" or textValue is "<null>" or textValue is "msng" then return fallbackValue
	return textValue
end dictionaryText

on boundedText(value)
	if value is missing value then return ""
	set sourceText to value as text
	if (count sourceText) > 3500 then set sourceText to text -3500 thru -1 of sourceText
	return sourceText
end boundedText

on bundleString(keyName)
	set value to current application's NSBundle's mainBundle()'s objectForInfoDictionaryKey:keyName
	if value is missing value then return missing value
	return value as text
end bundleString

on isFullHex(value, expectedLength)
	if value is missing value then return false
	set textValue to value as text
	if (count textValue) is not expectedLength then return false
	set {expression, expressionError} to current application's NSRegularExpression's regularExpressionWithPattern:("^[0-9a-f]{" & expectedLength & "}$") options:0 |error|:(reference)
	if expression is missing value then return false
	set matches to expression's numberOfMatchesInString:textValue options:0 range:(current application's NSMakeRange(0, expectedLength))
	return (matches as integer) is 1
end isFullHex

-- This AppKit projection is not a signed/public build and owns no Home, model, runtime,
-- service or lifecycle meaning.
-- [VXG RealForever]
