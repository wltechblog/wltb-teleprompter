# WLTechBlog Teleprompter

An Android teleprompter app for reading scripts and presentations. Load a `.txt` file, set your pace, and record yourself presenting — all from one screen.

## Features

### Teleprompter
- Load scripts from `.txt` files
- Auto-scroll with a smooth, configurable speed
- Manual scroll supported at any time, including during playback
- Play / Pause / Reset controls
- Portrait and landscape support (sensor rotation)

### Camera & Recording
- Front camera preview shown as a picture-in-picture overlay while you read
- Record yourself presenting directly in the app — saved as MP4 to `Movies/Teleprompter/`
- Switch between front and back cameras with one tap
- Recording stops cleanly before a camera switch

### Auto-hide UI
- Controls and the action bar hide automatically when playback starts, giving you a full-screen reading view
- Tap anywhere to bring controls back for 5 seconds
- Controls return immediately when you pause or reset

### Settings
- **Font size** — adjust from 12 sp to 60 sp with a live preview
- **Default scroll speed** — set the starting speed the slider uses each session
- Settings are saved and applied automatically on every launch

## Building the Project

### Prerequisites

- Java Development Kit (JDK) 17 or higher
- Android SDK (API level 33)
- Gradle 8.4 or higher

### Build Instructions

1. Clone the repository
2. Set up your `local.properties` file with the path to your Android SDK:
   ```
   sdk.dir=/path/to/your/android-sdk
   ```
3. Build the debug APK:
   ```bash
   ./gradlew assembleDebug
   ```
4. The APK will be generated at:
   ```
   app/build/outputs/apk/debug/app-debug.apk
   ```

### Installing on Device

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

Or copy the APK to your device and install it manually.

## Permissions

The app requests the following permissions at runtime:

| Permission | Purpose |
|---|---|
| `CAMERA` | Camera preview and recording |
| `RECORD_AUDIO` | Audio track in recorded videos |
| `WRITE_EXTERNAL_STORAGE` | Saving recordings on Android 9 and older |

The app functions as a plain teleprompter if camera permissions are denied.

## Usage

1. Tap **Load Script** to select a `.txt` file
2. Tap **Play** to start scrolling — the controls will fade out automatically
3. Adjust scroll speed with the slider before or during playback
4. Tap the screen during playback to briefly reveal the controls
5. Tap **⏺ Record** to start recording; tap **⏹ Stop** to finish
6. Tap **⇄ Camera** to switch between front and back cameras
7. Open the **⋮ menu → Settings** to change font size or default speed
8. Tap **Reset** to stop and return to the top

## License

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.

## Copyright

Copyright (C) 2026 Josh at WLTechBlog
