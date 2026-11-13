use framework "AppKit"
use framework "Foundation"
use scripting additions

property app : current application
property productOrigin : "http://127.0.0.1:18210/"

on isFullHex(value, expectedLength)
    if value is missing value then return false
    set textValue to value as text
    if (count textValue) is not expectedLength then return false

    set expressionError to reference
    set expression to app's NSRegularExpression's regularExpressionWithPattern:("^[0-9a-f]{" & expectedLength & "}$") options:0 |error|:expressionError
    if expression is missing value then return false

    set rangeValue to app's NSMakeRange(0, expectedLength)
    set matches to expression's numberOfMatchesInString:textValue options:0 range:rangeValue
    return matches is 1
end isFullHex

on bundleString(keyName)
    set value to app's NSBundle's mainBundle()'s objectForInfoDictionaryKey:keyName
    if value is missing value then return missing value
    return value as text
end bundleString

on boundedText(value)
    if value is missing value then return ""
    set source to app's NSString's stringWithString:(value as text)
    set maxCharacters to 3500
    set countValue to source's |length|()
    if countValue > maxCharacters then
        set source to source's substringFromIndex:(countValue - maxCharacters)
    end if
    return source as text
end boundedText

on showFailure(messageText, detailsText)
    set alert to app's NSAlert's alloc()'s init()
    alert's setAlertStyle:(app's NSAlertStyleCritical)
    alert's setMessageText:messageText
    alert's setInformativeText:(my boundedText(detailsText))
    alert's addButtonWithTitle:"Close"
    alert's runModal()
end showFailure

on openProduct()
    app's NSWorkspace's sharedWorkspace()'s openURL:(app's NSURL's URLWithString:productOrigin)
end openProduct

on runInspection(setupLauncher, sourceSha)
    set outputPipe to app's NSPipe's pipe()
    set task to app's NSTask's alloc()'s init()
    task's setExecutableURL:(app's NSURL's fileURLWithPath:setupLauncher)
    task's setArguments:{"--inspect-front-door", "--expected-source-sha", sourceSha}
    task's setStandardOutput:outputPipe
    task's setStandardError:outputPipe

    set launchError to reference
    set launched to task's launchAndReturnError:launchError
    if launched is not true then return {missing value, "The front-door observation process could not be launched."}

    repeat while (task's isRunning()) as boolean
        app's NSRunLoop's currentRunLoop()'s runUntilDate:(app's NSDate's dateWithTimeIntervalSinceNow:0.05)
    end repeat

    set dataValue to outputPipe's fileHandleForReading()'s readDataToEndOfFile()
    set textValue to app's NSString's alloc()'s initWithData:dataValue encoding:(app's NSUTF8StringEncoding)
    if (task's terminationStatus() as integer) is not 0 then return {missing value, textValue as text}

    set parseError to reference
    set projection to app's NSJSONSerialization's JSONObjectWithData:dataValue options:0 |error|:parseError
    if projection is missing value then return {missing value, textValue as text}
    return {projection, ""}
end runInspection

on dictionaryText(dictionaryValue, keyName, fallbackValue)
    if dictionaryValue is missing value then return fallbackValue
    set value to dictionaryValue's objectForKey:keyName
    if value is missing value or value is (app's NSNull's null()) then return fallbackValue
    return value as text
end dictionaryText

on formatGiB(value)
    if value is missing value or value is (app's NSNull's null()) then return "Unknown"
    set gib to (value as real) / 1073741824
    set tenth to (round (gib * 10)) / 10
    return (tenth as text) & " GB"
end formatGiB

on projectionSummary(projection)
    set currentState to projection's objectForKey:"currentState"
    set homePath to my dictionaryText(currentState, "homePath", "Unknown")
    set modelRef to my dictionaryText(currentState, "activeModelRef", "No model selected")
    set storageValue to currentState's objectForKey:"freeStorageBytes"
    set serviceSource to my dictionaryText(projection, "currentServiceSourceSha", "None")
    return "Home: " & homePath & return & "Model: " & modelRef & return & "Storage available: " & (my formatGiB(storageValue)) & return & "Running Product source: " & serviceSource
end projectionSummary

on performSetup(setupLauncher, sourceSha, modeFlag, headingText)
    set panel to app's NSWindow's alloc()'s initWithContentRect:{{0, 0}, {520, 160}} styleMask:(app's NSWindowStyleMaskTitled) backing:(app's NSBackingStoreBuffered) defer:false
    panel's setTitle:"VexLife"
    panel's center()

    set contentView to panel's contentView()
    set heading to app's NSTextField's labelWithString:headingText
    heading's setFont:(app's NSFont's boldSystemFontOfSize:18)
    heading's setFrame:{{64, 96}, {420, 28}}
    contentView's addSubview:heading

    set detail to app's NSTextField's wrappingLabelWithString:"VexLife is preserving your Home and continuity while the exact Product lifecycle owner verifies and converges the local service."
    detail's setFrame:{{64, 42}, {420, 50}}
    contentView's addSubview:detail

    set spinner to app's NSProgressIndicator's alloc()'s initWithFrame:{{24, 90}, {24, 24}}
    spinner's setStyle:(app's NSProgressIndicatorStyleSpinning)
    spinner's startAnimation:me
    contentView's addSubview:spinner

    panel's makeKeyAndOrderFront:me
    app's NSApp's activateIgnoringOtherApps:true

    set logPath to ((app's NSTemporaryDirectory()) as text) & "vexlife-prod-setup-" & sourceSha & ".log"
    app's NSFileManager's defaultManager()'s removeItemAtPath:logPath |error|:(missing value)
    app's NSFileManager's defaultManager()'s createFileAtPath:logPath contents:(missing value) attributes:(missing value)
    set logHandle to app's NSFileHandle's fileHandleForWritingAtPath:logPath

    set task to app's NSTask's alloc()'s init()
    task's setExecutableURL:(app's NSURL's fileURLWithPath:setupLauncher)
    task's setArguments:{"--expected-source-sha", sourceSha, modeFlag}
    task's setStandardOutput:logHandle
    task's setStandardError:logHandle

    set launchError to reference
    set launched to task's launchAndReturnError:launchError
    if launched is not true then
        logHandle's closeFile()
        spinner's stopAnimation:me
        panel's orderOut:me
        my showFailure("VexLife could not start the requested lifecycle action.", "The exact setup process could not be launched.")
        return false
    end if

    repeat while (task's isRunning()) as boolean
        app's NSRunLoop's currentRunLoop()'s runUntilDate:(app's NSDate's dateWithTimeIntervalSinceNow:0.1)
    end repeat

    logHandle's closeFile()
    spinner's stopAnimation:me
    panel's orderOut:me

    set statusValue to task's terminationStatus() as integer
    set outputText to my boundedLog(logPath)
    set expectedReadyPrefix to "VEXLIFE_PROD_SETUP_READY" & tab & sourceSha & tab
    set readyRange to (app's NSString's stringWithString:outputText)'s rangeOfString:expectedReadyPrefix
    set readyObserved to (readyRange's location) is not (app's NSNotFound)

    if statusValue is 0 and readyObserved then return true
    my showFailure("VexLife did not complete the requested lifecycle action.", outputText)
    return false
end performSetup

on boundedLog(pathValue)
    set readError to reference
    set value to app's NSString's stringWithContentsOfFile:pathValue encoding:(app's NSUTF8StringEncoding) |error|:readError
    if value is missing value then return "No setup output was recorded."
    return my boundedText(value as text)
end boundedLog

on run
    set sourceSha to my bundleString("VexLifeProdExpectedSourceSha")
    set setupLauncher to my bundleString("VexLifeProdSetupLauncher")

    if not my isFullHex(sourceSha, 40) then
        my showFailure("VexLife could not start.", "The staged product source identity is invalid.")
        return
    end if

    if setupLauncher is missing value or not ((app's NSFileManager's defaultManager()'s isExecutableFileAtPath:setupLauncher) as boolean) then
        my showFailure("VexLife could not start.", "The exact staged VexLife setup program is unavailable.")
        return
    end if

    app's NSApplication's sharedApplication()
    app's NSApp's setActivationPolicy:(app's NSApplicationActivationPolicyRegular)
    app's NSApp's activateIgnoringOtherApps:true

    set inspection to my runInspection(setupLauncher, sourceSha)
    set projection to item 1 of inspection
    if projection is missing value then
        my showFailure("VexLife could not inspect the current Product state.", item 2 of inspection)
        return
    end if

    set actionName to my dictionaryText(projection, "action", "HOLD_UNKNOWN")
    set summaryText to my projectionSummary(projection)

    if actionName is "HOLD_UNKNOWN" then
        set detailText to my dictionaryText(projection, "serviceDetail", "Current Product ownership is not exact.")
        my showFailure("VexLife will not replace the current process.", summaryText & return & return & detailText)
        return
    end if

    if actionName is "OPEN_CURRENT" then
        set alert to app's NSAlert's alloc()'s init()
        alert's setMessageText:"VexLife is current."
        alert's setInformativeText:summaryText
        alert's addButtonWithTitle:"Open VexLife"
        alert's addButtonWithTitle:"Cancel"
        if (alert's runModal()) is (app's NSAlertFirstButtonReturn) then my openProduct()
        return
    end if

    if actionName is "UPDATE_AVAILABLE" then
        set alert to app's NSAlert's alloc()'s init()
        alert's setMessageText:"A newer VexLife Product source is available."
        alert's setInformativeText:(summaryText & return & return & "Your Home, Conversation, Memory, and model continuity will be preserved. Update & Restart retires only the exact owned prior Product service.")
        alert's addButtonWithTitle:"Update & Restart"
        alert's addButtonWithTitle:"Open Current"
        alert's addButtonWithTitle:"Cancel"
        set answer to alert's runModal()
        if answer is (app's NSAlertFirstButtonReturn) then
            if my performSetup(setupLauncher, sourceSha, "--reuse-existing", "Updating VexLife…") then my openProduct()
        else if answer is (app's NSAlertSecondButtonReturn) then
            my openProduct()
        end if
        return
    end if

    if actionName is "REPAIR_REQUIRED" or actionName is "UPDATE_REPAIR_REQUIRED" then
        set alert to app's NSAlert's alloc()'s init()
        if actionName is "REPAIR_REQUIRED" then
            alert's setMessageText:"VexLife needs repair."
            alert's addButtonWithTitle:"Repair & Restart"
        else
            alert's setMessageText:"VexLife can update and repair the owned Product service."
            alert's addButtonWithTitle:"Update & Restart"
        end if
        alert's setInformativeText:(summaryText & return & return & "The exact owned Product service will be retired and restarted. Your Home and Vex continuity remain preserved.")
        alert's addButtonWithTitle:"Cancel"
        if (alert's runModal()) is (app's NSAlertFirstButtonReturn) then
            if my performSetup(setupLauncher, sourceSha, "--reuse-existing", "Repairing VexLife…") then my openProduct()
        end if
        return
    end if

    if actionName is "SETUP_REQUIRED" then
        set alert to app's NSAlert's alloc()'s init()
        alert's setMessageText:"Set up VexLife"
        alert's setInformativeText:(summaryText & return & return & "VexLife will use the canonical Home and may download verified model/runtime prerequisites only because you choose Set Up.")
        alert's addButtonWithTitle:"Set Up VexLife"
        alert's addButtonWithTitle:"Cancel"
        if (alert's runModal()) is (app's NSAlertFirstButtonReturn) then
            if my performSetup(setupLauncher, sourceSha, "--allow-acquisition", "Setting up VexLife…") then my openProduct()
        end if
        return
    end if

    my showFailure("VexLife front-door state is unsupported.", actionName)
end run

-- [VXG RealForever]
