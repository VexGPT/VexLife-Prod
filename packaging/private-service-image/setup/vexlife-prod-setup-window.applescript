use framework "AppKit"
use framework "Foundation"
use scripting additions

property app : current application

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

on boundedLog(pathValue)
    set readError to reference
    set value to app's NSString's stringWithContentsOfFile:pathValue encoding:(app's NSUTF8StringEncoding) |error|:readError
    if value is missing value then return "No setup output was recorded."

    set maxCharacters to 3500
    set countValue to value's |length|()
    if countValue > maxCharacters then
        set value to value's substringFromIndex:(countValue - maxCharacters)
    end if
    return value as text
end boundedLog

on showFailure(messageText, detailsText)
    set alert to app's NSAlert's alloc()'s init()
    alert's setAlertStyle:(app's NSAlertStyleCritical)
    alert's setMessageText:messageText
    alert's setInformativeText:detailsText
    alert's addButtonWithTitle:"Close"
    alert's runModal()
end showFailure

on run
    set sourceSha to my bundleString("VexLifeProdExpectedSourceSha")
    set setupLauncher to my bundleString("VexLifeProdSetupLauncher")

    if not my isFullHex(sourceSha, 40) then
        my showFailure("VexLife could not start setup.", "The staged product source identity is invalid.")
        return
    end if

    if setupLauncher is missing value or not ((app's NSFileManager's defaultManager()'s isExecutableFileAtPath:setupLauncher) as boolean) then
        my showFailure("VexLife could not start setup.", "The exact staged VexLife setup program is unavailable.")
        return
    end if

    app's NSApplication's sharedApplication()
    app's NSApp's setActivationPolicy:(app's NSApplicationActivationPolicyRegular)
    app's NSApp's activateIgnoringOtherApps:true

    set consent to app's NSAlert's alloc()'s init()
    consent's setAlertStyle:(app's NSAlertStyleInformational)
    consent's setMessageText:"Continue with VexLife"
    consent's setInformativeText:"VexLife will verify the exact current product, prepare its private local runtime, and download verified model files if they are missing. The first setup can download about 15 GB."
    consent's addButtonWithTitle:"Continue"
    consent's addButtonWithTitle:"Cancel"

    set consentResult to consent's runModal()
    if consentResult is not (app's NSAlertFirstButtonReturn) then return

    set panel to app's NSWindow's alloc()'s initWithContentRect:{{0, 0}, {520, 160}} styleMask:(app's NSWindowStyleMaskTitled) backing:(app's NSBackingStoreBuffered) defer:false
    panel's setTitle:"VexLife"
    panel's center()

    set contentView to panel's contentView()
    set heading to app's NSTextField's labelWithString:"Setting up VexLife…"
    heading's setFont:(app's NSFont's boldSystemFontOfSize:18)
    heading's setFrame:{{64, 96}, {420, 28}}
    contentView's addSubview:heading

    set detail to app's NSTextField's wrappingLabelWithString:"VexLife is verifying the current product, local model, runtime, and service. This can take a while on first setup."
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
    task's setArguments:{"--expected-source-sha", sourceSha, "--allow-acquisition"}
    task's setStandardOutput:logHandle
    task's setStandardError:logHandle

    set launchError to reference
    set launched to task's launchAndReturnError:launchError
    if launched is not true then
        logHandle's closeFile()
        spinner's stopAnimation:me
        panel's orderOut:me
        my showFailure("VexLife setup could not start.", "The bundled setup process could not be launched.")
        return
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

    if statusValue is 0 and readyObserved then
        set successAlert to app's NSAlert's alloc()'s init()
        successAlert's setAlertStyle:(app's NSAlertStyleInformational)
        successAlert's setMessageText:"VexLife setup is complete."
        successAlert's setInformativeText:"The exact current VexLife local service is installed and running. The final VexLife product interface is a separate held edge in this candidate."
        successAlert's addButtonWithTitle:"Done"
        successAlert's runModal()
    else
        my showFailure("VexLife setup did not complete.", outputText)
    end if
end run

-- [VXG RealForever]
