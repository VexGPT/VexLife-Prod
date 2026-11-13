use framework "Foundation"
use scripting additions

property productOrigin : "http://127.0.0.1:18210/"

on isFullHex(value, expectedLength)
    if value is missing value then return false
    set textValue to value as text
    if (count textValue) is not expectedLength then return false

    set {expression, expressionError} to current application's NSRegularExpression's regularExpressionWithPattern:("^[0-9a-f]{" & expectedLength & "}$") options:0 |error|:(reference)
    if expression is missing value then return false

    set rangeValue to current application's NSMakeRange(0, expectedLength)
    set matches to expression's numberOfMatchesInString:textValue options:0 range:rangeValue
    return matches is 1
end isFullHex

on bundleString(keyName)
    set value to current application's NSBundle's mainBundle()'s objectForInfoDictionaryKey:keyName
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
    set outputPipe to current application's NSPipe's pipe()
    set task to current application's NSTask's alloc()'s init()
    task's setExecutableURL:(current application's NSURL's fileURLWithPath:setupLauncher)
    task's setArguments:argumentsList
    task's setStandardOutput:outputPipe
    task's setStandardError:outputPipe

    set {launched, launchError} to task's launchAndReturnError:(reference)
    if launched is not true then return {missing value, "The Product-entry process could not be launched."}

    repeat while (task's isRunning()) as boolean
        current application's NSRunLoop's currentRunLoop()'s runUntilDate:(current application's NSDate's dateWithTimeIntervalSinceNow:0.05)
    end repeat

    set dataValue to outputPipe's fileHandleForReading()'s readDataToEndOfFile()
    set textValue to current application's NSString's alloc()'s initWithData:dataValue encoding:(current application's NSUTF8StringEncoding)
    set statusValue to task's terminationStatus() as integer
    if statusValue is not 0 then return {missing value, textValue as text}

    set {jsonValue, parseError} to current application's NSJSONSerialization's JSONObjectWithData:dataValue options:0 |error|:(reference)
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

on formatGiB(value)
    try
        set gibValue to (value as real) / 1073741824
        return (gibValue as text) & " GiB"
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
    set summaryText to "Home: " & homePath & return & "Model: " & modelRef & return & "Storage available: " & (my formatGiB(storageValue)) & return & "Running Product source: " & serviceSource

    set setupPlan to projection's objectForKey:"setupPlan"
    try
        if setupPlan is not missing value and setupPlan is not (current application's NSNull's null()) then
            set setupModel to my dictionaryText(setupPlan, "modelRef", "Unknown")
            set modelBytes to setupPlan's objectForKey:"modelArtifactTotalBytes"
            set minimumMemory to setupPlan's objectForKey:"modelMinimumMemoryBytes"
            set currentMemory to setupPlan's objectForKey:"currentTotalMemoryBytes"
            set currentStorage to setupPlan's objectForKey:"currentFreeStorageBytes"
            set summaryText to summaryText & return & return & "Setup model: " & setupModel & return & "Model files: " & (my formatGiB(modelBytes)) & return & "Memory: " & (my formatGiB(currentMemory)) & " available hardware / " & (my formatGiB(minimumMemory)) & " minimum" & return & "Free storage before setup: " & (my formatGiB(currentStorage)) & return & "Setup may acquire missing model and local runtime prerequisites."
        end if
    end try

    return summaryText
end projectionSummary

on activateControl(setupLauncher, sourceSha, controlRef)
    set activated to my runJson(setupLauncher, {"--activate-front-door-control", controlRef, "--expected-source-sha", sourceSha})
    set activation to item 1 of activated
    if activation is missing value then
        my showFailure("VexLife could not complete the selected action.", item 2 of activated)
        return
    end if

    set manifestationRef to my dictionaryText(activation, "manifestationRef", "")
    if manifestationRef is "OPEN_PRODUCT" then
        my openProduct()
        return
    end if

    if manifestationRef is "REFRESH_ENTRY" then
        my showEntry()
        return
    end if

    if manifestationRef is "NO_ACTION" then return
    my showFailure("VexLife returned an unsupported presentation instruction.", manifestationRef)
end activateControl

on showEntry()
    set sourceSha to my bundleString("VexLifeProdExpectedSourceSha")
    set setupLauncher to my bundleString("VexLifeProdSetupLauncher")

    if not my isFullHex(sourceSha, 40) then
        my showFailure("VexLife could not start.", "The staged Product source identity is invalid.")
        return
    end if

    if setupLauncher is missing value or not ((current application's NSFileManager's defaultManager()'s isExecutableFileAtPath:setupLauncher) as boolean) then
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

    my activateControl(setupLauncher, sourceSha, controlRef)
end showEntry

on run
    my showEntry()
end run

-- [VXG RealForever]
