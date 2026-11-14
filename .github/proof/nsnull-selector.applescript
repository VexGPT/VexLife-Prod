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
	return report
end run
