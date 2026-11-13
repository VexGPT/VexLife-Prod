# Setup Journey screens — generated from Product source

`[VXG RealForever]`

Every card below was produced by `VexLifeProdMacFrontDoor.observeWithHooks(...)` →
`VexLifeProdMacSetupCard.form(...)` on this branch, using the same stubbed observations as
`VexLifeProdSetupJourneyTest`. The native window shows exactly this title / rail / body / buttons;
it authors none of it. Paths were rewritten to `/Users/vex` for readability.

Rail legend: `✓` complete · `●` you are here · `⊘` held (nothing can proceed until the cause changes) · `○` upcoming.

Button kinds: **Control** = a Family-authored Control sent back through `--activate-front-door-control`
(re-observed before acting) · *Navigation* = renderer-local (choose another Home, check again, close).

| # | Situation | Entry State |
|---|---|---|
| 1 | First run with the suggested ~/.vexlife | `HOME_FORMATION_REQUIRED` |
| 2 | Chose an empty folder | `HOME_FORMATION_REQUIRED` |
| 3 | Chose a folder that already has other files | `HOME_HELD` |
| 4 | Chose ~/Documents/Vex on a nearly full drive | `HOME_FORMATION_REQUIRED` |
| 5 | Unsupported Mac (Apple M2, 16 GB) | `HOST_HELD` |
| 6 | Home created; local model not set up yet | `SETUP_REQUIRED` |
| 7 | Download was interrupted at 9 GiB | `SETUP_REQUIRED` |
| 8 | Not enough free space for the model | `STORAGE_HELD` |
| 9 | A model file is damaged | `MODEL_FILES_HELD` |
| 10 | Returning after restarting the Mac | `START_REQUIRED` |
| 11 | Service registered but not responding | `REPAIR_REQUIRED` |
| 12 | VexLife from another Home is running | `HOLD_UNKNOWN` |
| 13 | A newer version is ready | `UPDATE_AVAILABLE` |
| 14 | VexLife is already running | `CURRENT` |
| 15 | After setup or start reaches CURRENT | `NOTICE` |
| 16 | Download fails (no internet) | `FAILURE` |
| 17 | Choice no longer lawful after re-check | `FAILURE` |

## 1. First run with the suggested ~/.vexlife

State: `HOME_FORMATION_REQUIRED`

```text
Create your Vex Home.

✓ This Mac   ● Vex Home   ○ Local model   ○ Start VexLife   ○ Ready

VexLife keeps your identity, conversations and memory together in one folder on this Mac: your Vex Home. Creating it does not download or start anything; you'll review the local model as a separate step.

What VexLife found
  Vex Home: /Users/vex/.vexlife — new; nothing is there yet
  This Mac: Apple M4 Pro, 48.0 GiB memory — meets the requirements
  Free space on that drive: 200.0 GiB

Up next (you'll confirm it separately)
  Local model: about 14.1 GiB download, about 17.1 GiB free space on this drive, and an internet connection.
```

Buttons (first = default, last = Escape): **[Create Vex Home]** `CONTROL:FORM_HOME` · *[Choose Another Location…]* `CHOOSE_HOME` · *[Cancel]* `CLOSE`

## 2. Chose an empty folder

State: `HOME_FORMATION_REQUIRED`

```text
Create your Vex Home.

✓ This Mac   ● Vex Home   ○ Local model   ○ Start VexLife   ○ Ready

VexLife keeps your identity, conversations and memory together in one folder on this Mac: your Vex Home. Creating it does not download or start anything; you'll review the local model as a separate step.

What VexLife found
  Vex Home: /Users/vex/VexHome — an empty folder VexLife can set up as your Home
  This Mac: Apple M4 Pro, 48.0 GiB memory — meets the requirements
  Free space on that drive: 200.0 GiB

Up next (you'll confirm it separately)
  Local model: about 14.1 GiB download, about 17.1 GiB free space on this drive, and an internet connection.
```

Buttons (first = default, last = Escape): **[Create Vex Home]** `CONTROL:FORM_HOME` · *[Choose Another Location…]* `CHOOSE_HOME` · *[Cancel]* `CLOSE`

## 3. Chose a folder that already has other files

State: `HOME_HELD`

```text
This folder is being preserved.

✓ This Mac   ⊘ Vex Home   ○ Local model   ○ Start VexLife   ○ Ready

The folder you chose already contains other files, so VexLife will not use it as a Vex Home. Choose an empty folder, or type a new folder name. Nothing in it was changed, moved or deleted.

What VexLife found
  Vex Home: /Users/vex/Projects — preserved (the selected folder already contains files that are not a Vex Home)
  This Mac: Apple M4 Pro, 48.0 GiB memory — meets the requirements
  Free space on that drive: 200.0 GiB
```

Buttons (first = default, last = Escape): *[Choose Another Location…]* `CHOOSE_HOME` · *[Check Again]* `RECHECK` · *[Close]* `CLOSE`

## 4. Chose ~/Documents/Vex on a nearly full drive

State: `HOME_FORMATION_REQUIRED`

```text
Create your Vex Home.

✓ This Mac   ● Vex Home   ○ Local model   ○ Start VexLife   ○ Ready

VexLife keeps your identity, conversations and memory together in one folder on this Mac: your Vex Home. Creating it does not download or start anything; you'll review the local model as a separate step.

What VexLife found
  Vex Home: /Users/vex/Documents/Vex — new; nothing is there yet
  This Mac: Apple M4 Pro, 48.0 GiB memory — meets the requirements
  Free space on that drive: 8.0 GiB

Up next (you'll confirm it separately)
  Local model: about 14.1 GiB download, about 17.1 GiB free space on this drive, and an internet connection.

Heads-up: this location is inside your Documents folder, which macOS protects. macOS may ask you to allow access, and VexLife's background service can be blocked from reading it later. A location such as ~/.vexlife avoids this.

Heads-up: the local model step will need about 17.1 GiB free on this drive, and it has 8.0 GiB. You can still create the Home now and free up space before the next step.
```

Buttons (first = default, last = Escape): **[Create Vex Home]** `CONTROL:FORM_HOME` · *[Choose Another Location…]* `CHOOSE_HOME` · *[Cancel]* `CLOSE`

## 5. Unsupported Mac (Apple M2, 16 GB)

State: `HOST_HELD`

```text
This Mac can't run VexLife's local model yet.

⊘ This Mac   ○ Vex Home   ○ Local model   ○ Start VexLife   ○ Ready

This Mac's chip is not yet certified for the local model. VexLife's local model currently runs on Macs with an Apple M4 Pro chip and at least 12.0 GiB of memory. This Mac: Apple M2, 16.0 GiB memory. Nothing was created or downloaded.

What VexLife found
  Vex Home: /Users/vex/.vexlife2 — new; nothing is there yet
  This Mac: Apple M2, 16.0 GiB memory — needs Apple M4 Pro and 12.0 GiB memory
  Free space on that drive: 200.0 GiB
```

Buttons (first = default, last = Escape): *[Close]* `CLOSE`

## 6. Home created; local model not set up yet

State: `SETUP_REQUIRED`

```text
Set up the local model.

✓ This Mac   ✓ Vex Home   ● Local model   ○ Start VexLife   ○ Ready

Your Vex Home is ready. Next, VexLife downloads its verified local model and runtime into your Home and starts VexLife. Review the size and requirements before you continue. If the download is interrupted, finished parts are kept and it resumes next time.

What VexLife found
  Vex Home: /Users/vex/.vexlife — your existing Vex Home
  This Mac: Apple M4 Pro, 48.0 GiB memory — meets the requirements
  Free space on that drive: 100.0 GiB
  Local model: not set up yet

What setup will do
  • Download about 14.1 GiB of verified model files (Heretic 27B (4-bit, MLX))
  • Prepare a private Python + MLX runtime inside your Vex Home
  • Start the local model and VexLife on this Mac only (127.0.0.1)
  Needs about 17.1 GiB free · 100.0 GiB available. Needs an internet connection; downloads resume if interrupted.
```

Buttons (first = default, last = Escape): **[Set Up VexLife]** `CONTROL:SET_UP` · *[Cancel]* `CLOSE`

## 7. Download was interrupted at 9 GiB

State: `SETUP_REQUIRED`

```text
Set up the local model.

✓ This Mac   ✓ Vex Home   ● Local model   ○ Start VexLife   ○ Ready

Your Vex Home is ready. Next, VexLife downloads its verified local model and runtime into your Home and starts VexLife. Review the size and requirements before you continue. If the download is interrupted, finished parts are kept and it resumes next time.

What VexLife found
  Vex Home: /Users/vex/.vexlife — your existing Vex Home
  This Mac: Apple M4 Pro, 48.0 GiB memory — meets the requirements
  Free space on that drive: 100.0 GiB
  Local model: 9.0 of 14.1 GiB downloaded

What setup will do
  • Download about 5.1 GiB of verified model files (Heretic 27B (4-bit, MLX))
  • Prepare a private Python + MLX runtime inside your Vex Home
  • Start the local model and VexLife on this Mac only (127.0.0.1)
  Needs about 8.1 GiB free · 100.0 GiB available. Needs an internet connection; downloads resume if interrupted.
```

Buttons (first = default, last = Escape): **[Set Up VexLife]** `CONTROL:SET_UP` · *[Cancel]* `CLOSE`

## 8. Not enough free space for the model

State: `STORAGE_HELD`

```text
Not enough free space for the local model.

✓ This Mac   ✓ Vex Home   ⊘ Local model   ○ Start VexLife   ○ Ready

VexLife needs about 17.1 GiB free on the drive that holds your Vex Home before it can download the local model. That drive has 5.0 GiB free. Free up space and choose Check Again, or choose a location on another drive. Nothing was downloaded.

What VexLife found
  Vex Home: /Users/vex/.vexlife — your existing Vex Home
  This Mac: Apple M4 Pro, 48.0 GiB memory — meets the requirements
  Free space on that drive: 5.0 GiB
  Local model: not downloaded yet

What setup will do
  • Download about 14.1 GiB of verified model files (Heretic 27B (4-bit, MLX))
  • Prepare a private Python + MLX runtime inside your Vex Home
  • Start the local model and VexLife on this Mac only (127.0.0.1)
  Needs about 17.1 GiB free · 5.0 GiB available. Needs an internet connection; downloads resume if interrupted.
```

Buttons (first = default, last = Escape): *[Choose Another Location…]* `CHOOSE_HOME` · *[Check Again]* `RECHECK` · *[Close]* `CLOSE`

## 9. A model file is damaged

State: `MODEL_FILES_HELD`

```text
A local model file needs attention.

✓ This Mac   ✓ Vex Home   ⊘ Local model   ○ Start VexLife   ○ Ready

Some model files in your Vex Home are not the size VexLife expects (2.0 GiB affected). VexLife keeps them and will not delete or replace them on its own. If you moved or edited files inside your Vex Home, put them back and choose Check Again. Guided model-file repair is not part of this setup yet.

What VexLife found
  Vex Home: /Users/vex/.vexlife — your existing Vex Home
  This Mac: Apple M4 Pro, 48.0 GiB memory — meets the requirements
  Free space on that drive: 100.0 GiB
  Local model: 2.0 GiB of files do not match their expected size
```

Buttons (first = default, last = Escape): *[Check Again]* `RECHECK` · *[Close]* `CLOSE`

## 10. Returning after restarting the Mac

State: `START_REQUIRED`

```text
Start VexLife.

✓ This Mac   ✓ Vex Home   ✓ Local model   ● Start VexLife   ○ Ready

Your Vex Home and local model are already on this Mac. Starting checks those files, then starts the local model and VexLife. Nothing will be downloaded.

What VexLife found
  Vex Home: /Users/vex/.vexlife — your existing Vex Home
  This Mac: Apple M4 Pro, 48.0 GiB memory — meets the requirements
  Free space on that drive: 100.0 GiB
  Local model: on this Mac (14.1 GiB)
  VexLife service: not running
```

Buttons (first = default, last = Escape): **[Start VexLife]** `CONTROL:START` · *[Cancel]* `CLOSE`

## 11. Service registered but not responding

State: `REPAIR_REQUIRED`

```text
VexLife isn't responding.

✓ This Mac   ✓ Vex Home   ✓ Local model   ● Start VexLife   ○ Ready

VexLife's background service is registered but not answering. Repair & Restart re-checks the local model files, then starts the local model and VexLife again. Your Home, conversations and memory stay as they are. Nothing will be downloaded.

What VexLife found
  Vex Home: /Users/vex/.vexlife — your existing Vex Home
  This Mac: Apple M4 Pro, 48.0 GiB memory — meets the requirements
  Free space on that drive: 100.0 GiB
  Local model: on this Mac (14.1 GiB)
  VexLife service: registered but not responding
```

Buttons (first = default, last = Escape): **[Repair & Restart]** `CONTROL:CONVERGE_DESIRED` · *[Cancel]* `CLOSE`

## 12. VexLife from another Home is running

State: `HOLD_UNKNOWN`

```text
VexLife will not replace the current process.

✓ This Mac   ✓ Vex Home   ○ Local model   ⊘ Start VexLife   ○ Ready

Something is already using VexLife's background service on this Mac, and VexLife cannot prove it belongs to the Home you chose. To keep your data safe it will not stop or replace it. If you already have a Vex Home somewhere else, choose that location. Otherwise quit the other VexLife and choose Check Again.

What VexLife found
  Vex Home: /Users/vex/.vexlife — your existing Vex Home
  This Mac: Apple M4 Pro, 48.0 GiB memory — meets the requirements
  Free space on that drive: 100.0 GiB
  Local model: on this Mac (14.1 GiB)
  VexLife service: in use by something VexLife cannot verify
```

Buttons (first = default, last = Escape): *[Choose Another Location…]* `CHOOSE_HOME` · *[Check Again]* `RECHECK` · *[Close]* `CLOSE`

## 13. A newer version is ready

State: `UPDATE_AVAILABLE`

```text
A newer version of VexLife is ready.

✓ This Mac   ✓ Vex Home   ✓ Local model   ● Start VexLife   ○ Ready

Your Home, conversations and memory stay as they are. Update & Restart stops only the exact VexLife service this Home owns and starts the newer one. Nothing will be downloaded.

What VexLife found
  Vex Home: /Users/vex/.vexlife — your existing Vex Home
  This Mac: Apple M4 Pro, 48.0 GiB memory — meets the requirements
  Free space on that drive: 100.0 GiB
  Local model: on this Mac (14.1 GiB)
  VexLife service: running an earlier version (bbbbbbb)
```

Buttons (first = default, last = Escape): **[Update & Restart]** `CONTROL:CONVERGE_DESIRED` · **[Open Current]** `CONTROL:OPEN_CURRENT` · *[Cancel]* `CLOSE`

## 14. VexLife is already running

State: `CURRENT`

```text
VexLife is running.

✓ This Mac   ✓ Vex Home   ✓ Local model   ✓ Start VexLife   ● Ready

Everything is current. Opening VexLife does not restart or replace anything.

What VexLife found
  Vex Home: /Users/vex/.vexlife — your existing Vex Home
  This Mac: Apple M4 Pro, 48.0 GiB memory — meets the requirements
  Free space on that drive: 100.0 GiB
  Local model: on this Mac (14.1 GiB)
  VexLife service: running this version
```

Buttons (first = default, last = Escape): **[Open VexLife]** `CONTROL:OPEN_CURRENT` · *[Cancel]* `CLOSE`

## 15. After setup or start reaches CURRENT

Shown once by the window after `OPEN_PRODUCT` follows an Effect.

```text
VexLife is ready.

VexLife is running on this Mac and opened in your browser at http://127.0.0.1:18210. You can close this window. To start VexLife again later (for example after restarting your Mac), run the same setup command: it checks first and offers only what is needed.
```

Buttons: *[Done]*

## 16. Download fails (no internet)

Failure ref: `NETWORK_UNAVAILABLE` — shown after the activation already re-observed; *Continue* re-inspects.

```text
VexLife couldn't download what it needed.

Check your internet connection and try again. Files that finished downloading were kept, and an unfinished download resumes where it stopped.

Technical detail
IllegalStateException: artifact delivery stopped
caused by ConnectException: Connection refused
```

Buttons: *[Continue]* · *[Close]*

## 17. Choice no longer lawful after re-check

Failure ref: `STATE_CHANGED_BEFORE_EFFECT` — shown after the activation already re-observed; *Continue* re-inspects.

```text
Something changed before VexLife acted.

VexLife looked again right before acting, and that choice no longer fits what is on this Mac now. Nothing was changed. Here is what is true now.
```

Buttons: *[Continue]* · *[Close]*

<!-- [VXG RealForever][VEXLIFE-PROD][SETUP-JOURNEY][SCREENS] -->
