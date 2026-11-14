use framework "Foundation"
use scripting additions
on run
	set report to ""
	try
		set n to current application's NSNull's |null|()
		set report to report & "escaped |null|(): OK"
	on error message number code
		set report to report & "escaped |null|(): ERROR " & code
	end try
	set parsed to (current application's NSJSONSerialization's JSONObjectWithData:((current application's NSString's stringWithString:"{\"a\":null}")'s dataUsingEncoding:4) options:0 |error|:(missing value))
	set value to parsed's objectForKey:"a"
	if value is (current application's NSNull's |null|()) then
		set report to report & "; JSON null compares equal to escaped NSNull: OK"
	else
		set report to report & "; JSON null comparison: MISMATCH"
	end if
	return report
end run
