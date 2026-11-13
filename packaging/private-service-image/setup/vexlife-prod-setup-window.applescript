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
    set sourceText to value as text
    if (count sourceText) > 3500 then
        set sourceText to text -3500 thru -1 of sourceText
    end if
    return sourceText
end boundedText

on showFailure(messageText, detailsText)
    display alert messageText message (my boundedText(detailsText)) as critical buttons {"Close"} default button "Close"
end showFailure

on openProduct()
    open location productOrigin
end openProduct

on runJson(setupLauncher, argumentsList)
    set outputPipe to app's NSPipe's pipe()
    set task to app's NSTask's alloc()'s init()
    task's setExecutableURL:(app's NSURL's fileURLWithPath:setupLauncher)
    task's setArguments:argumentsList
    task's setStandardOutput:outputPipe
    task's setStandardError:outputPipe

    set launchError to reference
    set launched to task's launchAndReturnError:launchError
    if launched is not true then return {missing value, "The Product-entry process could not be launched."}

    repeat while (task's isRunning()) as boolean
        app's NSRunLoop's currentRunLoop()'s runUntilDate:(app's NSDate's dateWithTimeIntervalSinceNow:0.05)
    end repeat

    set dataValue to outputPipe's fileHandleForReading()'s readDataToEndOfFile()
    set textValue to app's NSString's alloc()'s initWithData:dataValue encoding:(app's NSUTF8StringEncoding)
    set statusValue to task's terminationStatus() as integer
    if statusValue is not 0 then return {missing value, textValue as text}

    set parseError to reference
    set jsonValue to app's NSJSONSerialization's JSONObjectWithData:dataValue options:0 |error|:parseError
    if jsonValue is missing value then return {missing value, textValue as text}
    return {jsonValue, ""}
end runJson

on dictionaryText(dictionaryValue, keyName, fallbackValue)
    if dictionaryValue is missing value then return fallbackValue
    set value to dictionaryValue's objectForKey:keyName
    if value is missing value then return fallbackValue
    try
        set textValue to value as text
    on error
        return fallbackValue
    end try
    if textValue is "null" or textValue is "<null>" then return fallbackValue
    return textValue
end dictionaryText

on dictionaryBoolean(dictionaryValue, keyName, fallbackValue)
    if dictionaryValue is missing value then return fallbackValue
    set value to dictionaryValue's objectForKey:keyName
    if value is missing value then return fallbackValue
    try
        return value as boolean
    on error
        return fallbackValue
    end try
end dictionaryBoolean

on formatGiB(value)
    try
        set gibValue to (value as real) / 1073741824
        return (gibValue as text) & " GB"
    on error
        return "Unknown"
    end try
end formatGiB

on projectionSummary(projection)
    set currentState to projection's objectForKey:"currentState"
    set homePath to my dictionaryText(currentState, "homePath", "Unknown")
    set modelRef to my dictionaryText(currentState, "activeModelRef", "No model selected")
    set storageValue to currentState's objectForKey:"freeStorageBytes"
    set serviceSource to my dictionaryText(projection, "currentServiceSourceSha", "None")
    return "Home: " & homePath & return & "Model: " & modelRef & return & "Storage available: " & (my formatGiB(storageValue)) & return & "Running Product source: " & serviceSource
end projectionSummary

on runSetup(setupLauncher, sourceSha, allowAcquisition)
    if allowAcquisition then
        set modeFlag to "--allow-acquisition"
    else
        set modeFlag to "--reuse-existing"
    end if

    set outputPipe to app's NSPipe's pipe()
    set task to app's NSTask's alloc()'s init()
    task's setExecutableURL:(app's NSURL's fileURLWithPath:setupLauncher)
    task's setArguments:{"--expected-source-sha", sourceSha, modeFlag}
    task's setStandardOutput:outputPipe
    task's setStandardError:outputPipe

    set launchError to reference
    set launched to task's launchAndReturnError:launchError
    if launched is not true then return {false, "The exact setup process could not be launched."}

    repeat while (task's isRunning()) as boolean
        app's NSRunLoop's currentRunLoop()'s runUntilDate:(app's NSDate's dateWithTimeIntervalSinceNow:0.1)
    end repeat

    set dataValue to outputPipe's fileHandleForReading()'s readDataToEndOfFile()
    set outputText to app's NSString's alloc()'s initWithData:dataValue encoding:(app's NSUTF8StringEncoding)
    set statusValue to task's terminationStatus() as integer
    set expectedReadyPrefix to "VEXLIFE_PROD_SETUP_READY" & tab & sourceSha & tab
    set outputString to outputText as text
    set readyObserved to outputString contains expectedReadyPrefix

    if statusValue is 0 and readyObserved then return {true, outputString}
    return {false, outputString}
end runSetup

on resolveAndRealize(setupLauncher, sourceSha, controlRef)
    set resolved to my runJson(setupLauncher, {"--resolve-front-door-control", controlRef, "--expected-source-sha", sourceSha})
    set effectProjection to item 1 of resolved
    if effectProjection is missing value then
        my showFailure("VexLife could not resolve the selected action.", item 2 of resolved)
        return
    end if

    set effectRef to my dictionaryText(effectProjection, "effectRef", "")
    if effectRef is "OPEN_PRESENTATION" then
        my openProduct()
        return
    end if

    if effectRef is "CONVERGE_PRODUCT" then
        set allowAcquisition to my dictionaryBoolean(effectProjection, "allowAcquisition", false)
        set setupResult to my runSetup(setupLauncher, sourceSha, allowAcquisition)
        if item 1 of setupResult is true then
            my openProduct()
        else
            my showFailure("VexLife did not complete the selected action.", item 2 of setupResult)
        end if
        return
    end if

    if effectRef is "" then return
    my showFailure("VexLife returned an unsupported Product-entry effect.", effectRef)
end resolveAndRealize

on run
    set sourceSha to my bundleString("VexLifeProdExpectedSourceSha")
    set setupLauncher to my bundleString("VexLifeProdSetupLauncher")

    if not my isFullHex(sourceSha, 40) then
        my showFailure("VexLife could not start.", "The staged Product source identity is invalid.")
        return
    end if

    if setupLauncher is missing value or not ((app's NSFileManager's defaultManager()'s isExecutableFileAtPath:setupLauncher) as boolean) then
        my showFailure("VexLife could not start.", "The exact staged VexLife setup program is unavailable.")
        return
    end if

    set inspected to my runJson(setupLauncher, {"--inspect-front-door", "--expected-source-sha", sourceSha})
    set projection to item 1 of inspected
    if projection is missing value then
        my showFailure("VexLife could not inspect the current Product state.", item 2 of inspected)
        return
    end if

    set displayState to projection's objectForKey:"displayState"
    set titleText to my dictionaryText(displayState, "title", "VexLife")
    set detailText to my dictionaryText(displayState, "detail", "")
    set primaryControlRef to my dictionaryText(displayState, "primaryControlRef", "")
    set primaryLabel to my dictionaryText(displayState, "primaryLabel", "")
    set secondaryControlRef to my dictionaryText(displayState, "secondaryControlRef", "")
    set secondaryLabel to my dictionaryText(displayState, "secondaryLabel", "")
    set cancelLabel to my dictionaryText(displayState, "cancelLabel", "Cancel")
    set summaryText to my projectionSummary(projection)
    set messageText to summaryText & return & return & detailText

    if primaryControlRef is "" or primaryLabel is "" then
        my showFailure(titleText, messageText)
        return
    end if

    if secondaryControlRef is not "" and secondaryLabel is not "" then
        set chosen to display alert titleText message messageText buttons {primaryLabel, secondaryLabel, cancelLabel} default button primaryLabel cancel button cancelLabel
        set chosenLabel to button returned of chosen
        if chosenLabel is primaryLabel then
            set controlRef to primaryControlRef
        else if chosenLabel is secondaryLabel then
            set controlRef to secondaryControlRef
        else
            return
        end if
    else
        set chosen to display alert titleText message messageText buttons {primaryLabel, cancelLabel} default button primaryLabel cancel button cancelLabel
        set chosenLabel to button returned of chosen
        if chosenLabel is primaryLabel then
            set controlRef to primaryControlRef
        else
            return
        end if
    end if

    my resolveAndRealize(setupLauncher, sourceSha, controlRef)
end run

-- [VXG RealForever]
