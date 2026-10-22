# Installing Film to Watch on a TV: a guide for AI agents

This file is for an AI coding agent (Claude Code, Codex, …) asked to install and set up
[Film to Watch](README.md) on someone's Android TV or Google TV. A person can follow it too.

**How to work through it**

- Do every step you can from the terminal yourself. Steps marked **👤 User** need the person: their TV remote, their
  browser, or a decision. Ask for one such step at a time, in plain words, and wait until they say it's done.
- After each step, run its **Check**. Don't move on until it passes.
- Never print, log, commit or paste anywhere an API key the user gives you, apart from writing it into
  `local.properties`.
- Once the TV is connected, you can see it: take a screenshot after anything that changes the screen
  (`adb -s "$TV" exec-out screencap -p > tv.png`) and look at it instead of asking the user what they see.

## Step 0: Ask two questions

1. "What TV or streaming stick is it?" (Any Android TV / Google TV with Android 8.0+ works.)
2. "Do you want *Where to Watch* (streaming services in your country, a Watch on Netflix button, full cast, reviews)?
   It needs a free TMDB account and means building the app on this computer, about 10 more minutes. Without it the
   app already works."

If the answer to 2 is no, steps 5 to 7 are skipped.

## Step 1: Check the computer

```bash
uname -s; command -v adb git gh curl; adb version | head -1
```

If `adb` is missing, install it:

| OS | Command |
| --- | --- |
| macOS | `brew install --cask android-platform-tools` |
| Debian / Ubuntu | `sudo apt install -y adb` |
| Fedora | `sudo dnf install -y android-tools` |
| Windows | `winget install Google.PlatformTools` (then open a new terminal) |

**Check:** `adb version` prints a version.

## Step 2: 👤 User: turn on developer mode on the TV

Tell the user, with the remote:

1. Open **Settings → Device Preferences → About**. On Google TV it's **Settings → System → About**.
2. Scroll to **Build** (on Google TV: **Android TV OS build**) and press OK on it **7 times**, until the TV says
   "You are now a developer". It may ask for the TV's PIN.
3. Go back to **Device Preferences** (Google TV: **System**) → **Developer options** and turn on **USB debugging**.
   If there's also **Network debugging** or **ADB debugging over network**, turn that on too.
4. Open **Settings → Network & Internet →** their Wi-Fi network, and read you the **IP address** (like `192.168.1.50`).
   The TV and this computer must be on the same network.

Explain why, in one sentence: this lets this computer install the app and press remote buttons for them, so they
don't have to; they approve this computer once on the TV and can switch developer mode off later.

Some Google TV devices (Android 12+) only offer **Wireless debugging** with pairing. Then: Developer options →
Wireless debugging → **Pair device with pairing code**, and run `adb pair <ip>:<pairing-port>` with the code shown,
then `adb connect <ip>:<port shown on the Wireless debugging screen>`.

## Step 3: Connect to the TV

```bash
TV=192.168.1.50:5555   # the address the user gave you
adb connect "$TV"
adb devices
```

The first time, the TV shows **"Allow USB debugging?"**. 👤 **User:** tick *Always allow from this computer* and
choose **Allow**. Then run `adb connect "$TV"` again.

**Check:** `adb devices` lists the TV as `device`. If it says:

- `unauthorized`: the prompt on the TV wasn't accepted. Ask the user to look at the TV, or run
  `adb disconnect; adb connect "$TV"` to show it again.
- `failed to connect` / `connection refused`: network debugging is off, the IP is wrong, or the TV is asleep. Ask
  the user to wake the TV and re-check the IP and the setting from step 2.

Also confirm the Android version (8.0 or newer):

```bash
adb -s "$TV" shell getprop ro.build.version.release
```

## Step 4: Install the app

Download the latest release APK and install it:

```bash
gh release download --repo 0xProbabillity/film-to-watch-tv --pattern '*.apk' --dir /tmp/film-to-watch-tv --clobber
# or, without the GitHub CLI:
# curl -L -o /tmp/film-to-watch-tv.apk "$(curl -s https://api.github.com/repos/0xProbabillity/film-to-watch-tv/releases/latest | grep -o 'https://[^"]*\.apk' | head -1)"
adb -s "$TV" install -r /tmp/film-to-watch-tv/*.apk
adb -s "$TV" shell am start -n com.filmtowatch.tv/.MainActivity
```

If the install fails with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, a build signed with a different key is already on
the TV. Tell the user that reinstalling clears their watchlist, and with their OK run
`adb -s "$TV" uninstall com.filmtowatch.tv` and install again.

**Check:** wait 8 seconds, take a screenshot and look at it. You should see *"What are you in the mood for?"* and
genre tiles with movie stills. Also:

```bash
adb -s "$TV" shell dumpsys activity activities | grep -m1 mResumedActivity   # shows com.filmtowatch.tv
```

If the user said no to TMDB, go to step 8.

## Step 5: 👤 User: create a TMDB API key

Guide the user through this in their browser (you can't sign up or accept terms for them):

1. Sign up at **https://www.themoviedb.org/signup** and confirm the email TMDB sends.
2. Open **https://www.themoviedb.org/settings/api** and choose **Create** (or *Request an API key*).
3. Type of use: **Personal** / non-commercial. Accept the terms of use.
4. Fill in the form. Application name: `Film to Watch`. Application URL: any page of theirs (a GitHub profile is
   fine). Summary, ready to paste:

   ```text
   Personal, non-commercial Android TV app I use at home to decide what to watch. It shows movie posters, details,
   cast, trailers, reviews and where a movie is available to stream in my country (watch providers), and keeps a
   local watchlist. Read-only public data, low request volume, TMDB and JustWatch are credited in the app. Not
   sold, monetized or published on any store.
   ```

   TMDB may also ask for a name, address and phone: that's between the user and TMDB, don't ask for those details
   yourself.
5. On the API page, copy the **API Read Access Token** (a long string starting with `eyJ`). The shorter **API Key**
   works too.

Ask the user to paste the token into the chat, or to put it into `local.properties` themselves in step 6 if they'd
rather not share it.

**Check** (don't echo the token):

```bash
curl -s -o /dev/null -w '%{http_code}\n' -H "Authorization: Bearer $TMDB_TOKEN" https://api.themoviedb.org/3/movie/550
# 200 = good. For a short API key use: "https://api.themoviedb.org/3/movie/550?api_key=$TMDB_KEY"
```

Optional: an **OMDb** key (https://www.omdbapi.com/apikey.aspx, free, sent by email) adds Rotten Tomatoes and
Metacritic scores.

## Step 6: Get the source and the build tools

```bash
git clone https://github.com/0xProbabillity/film-to-watch-tv.git && cd film-to-watch-tv
```

The build needs **JDK 17** and the **Android SDK**:

| | macOS | Debian / Ubuntu |
| --- | --- | --- |
| JDK 17 | `brew install openjdk@17` | `sudo apt install -y openjdk-17-jdk` |
| Android SDK | `brew install --cask android-commandlinetools` (SDK in `/opt/homebrew/share/android-commandlinetools`) | Download the [command-line tools](https://developer.android.com/studio#command-line-tools-only) into `~/Android/sdk/cmdline-tools/latest` |

If Android Studio is installed, its SDK (usually `~/Library/Android/sdk` or `~/Android/Sdk`) works as is.

Then install the SDK packages:

```bash
sdkmanager --sdk_root="$SDK" "platforms;android-35" "build-tools;34.0.0" "platform-tools"
```

`sdkmanager` shows Google's SDK license and waits for `y`. 👤 **User:** that license is theirs to accept. Show them
the prompt (or run `sdkmanager --sdk_root="$SDK" --licenses`) and let them answer; don't pipe `yes` into it for them.

Write `local.properties` (it's git-ignored):

```bash
cat > local.properties <<EOF
sdk.dir=$SDK
TMDB_API_KEY=<the token from step 5>
OMDB_API_KEY=
EOF
```

**Check:** `grep -c '^TMDB_API_KEY=.\+' local.properties` prints `1`, and `git status --short` doesn't list
`local.properties`.

## Step 7: Build and install

```bash
scripts/build.sh                       # finds JDK 17 itself, or set JAVA_HOME; first run downloads Gradle and libraries
scripts/install.sh "${TV%%:*}" app/build/outputs/apk/debug/app-debug.apk
```

The first build takes a few minutes. If the TV already has the release APK from step 4, the install fails with
`INSTALL_FAILED_UPDATE_INCOMPATIBLE` (different signing key): uninstall it as described in step 4 (the watchlist
is lost) and run the install again.

**Check:** open a movie's details and confirm the TMDB extras are there:

```bash
adb -s "$TV" shell input keyevent 19    # up: to "Start Swiping"
adb -s "$TV" shell input keyevent 23    # OK: start swiping
sleep 6
adb -s "$TV" shell input keyevent 20    # down: details
sleep 5
adb -s "$TV" exec-out screencap -p > tv.png
```

Look at the screenshot: there should be a **Watch on …** / **Rent on …** button before *Trailer* (for most well-known
movies), and pressing `↓` (keyevent 20) shows **Where to Watch**. Newer or obscure movies may have no streaming
offers; swipe to another one (`adb -s "$TV" shell input keyevent 4` goes back, keyevent 22 likes, 21 skips).

The region defaults to the TV's country if it's the US or in Europe, otherwise the US. The user can change it at the
bottom of the Discover tab (*Streaming region*).

## Step 8: Hand over

Tell the user:

- The app is in the TV's apps row as **Film to Watch** (they can move it to the favourites row: long-press OK on it).
- How to use it: pick genres → *Start Swiping* → `→` like, `←` skip, `↓` details, `↑` undo, `▶` trailer. Liked
  movies are in the *Watchlist* tab.
- Developer mode can stay on (it's needed for future updates from this computer), or they can turn **USB debugging**
  off in Developer options now. To update later: step 4 (or step 7) again.
- If they built with their own key: the APK contains it, so they shouldn't share it.

## Remote control reference

You can drive the app with `adb -s "$TV" shell input keyevent <code>`:

| Key | Code | | Key | Code |
| --- | --- | --- | --- | --- |
| Up | 19 | | OK | 23 |
| Down | 20 | | Back | 4 |
| Left | 21 | | Home | 3 |
| Right | 22 | | Play/Pause | 85 |

Start the app: `adb -s "$TV" shell am start -n com.filmtowatch.tv/.MainActivity` · restart it: add `-S` ·
uninstall: `adb -s "$TV" uninstall com.filmtowatch.tv`.

## Troubleshooting

| Symptom | Fix |
| --- | --- |
| Genre tiles stay black, "Couldn't reach the movie catalog" | The TV has no internet, or `v3-cinemeta.strem.io` is blocked on this network. Check with `adb -s "$TV" shell ping -c 2 v3-cinemeta.strem.io` |
| No *Where to Watch*, no *Watch on …* button | The app was built without a key (`grep TMDB_API_KEY local.properties`), the key is wrong (step 5 check), or the movie has no offers in the selected region |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | Different signing key than the installed copy: uninstall first (clears the watchlist) |
| `adb: device offline` | `adb kill-server && adb connect "$TV"`; wake the TV |
| The TV's IP changed | Re-read it in Settings → Network & Internet and connect again; a DHCP reservation on the router keeps it fixed |
| Build fails with "Unsupported class file" / wrong Java | Use JDK 17: `JAVA_HOME=$(/usr/libexec/java_home -v 17)` on macOS, or the `openjdk@17` path from Homebrew |
| *Watch on …* opens Google Play | That streaming app isn't installed on the TV; install it or pick another service under *Where to Watch* |
