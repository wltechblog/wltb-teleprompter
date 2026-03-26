# WLTechBlog Teleprompter

A simple Android teleprompter app for reading scripts and presentations. Load your .txt files and control the scrolling with customizable speed.

## Features

- Load scripts from .txt files
- Auto-scroll with configurable speed
- Manual scroll support
- Play/Pause/Reset controls
- Screen rotation support (sensor mode)
- Clean, readable interface

## Building the Project

### Prerequisites

- Java Development Kit (JDK) 8 or higher
- Android SDK (API level 33)
- Gradle 8.4 or higher

### Build Instructions

1. Clone the repository
2. Set up your local.properties file with the path to your Android SDK:
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

Install the generated APK on your Android device using:
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

Or copy the APK file to your device and install it manually.

## Usage

1. Tap "Load Script" to select a .txt file from your device
2. Use the Play/Pause button to control auto-scrolling
3. Adjust scroll speed with the slider at the bottom
4. Use Reset to return to the top of the script
5. Manually scroll up/down at any time
6. Rotate your device to view in landscape or portrait mode

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
