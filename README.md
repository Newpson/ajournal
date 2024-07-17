![feather](https://github.com/user-attachments/assets/164f8bc4-904e-48ba-8dfe-6e40992b6264)

## About
It is a simple and lightweight drawing application that supports input from external devices such as graphics tablets.
Classic touch input with pressure emulation is also implemented (currently in separate `driverless` branch).

## System requirements
The application stack was selected in such a way that it works even on very old devices with Android 3.1 (the minimum version that supports both OpenGL ES 2.0 and Usb OTG connection).
Tested on Android 4.2, Android 5.0.1, Android 6.0, Android 12. The size of the final package (.apk) is currently 58 kilobytes (including icons).
The maximum graphics buffer size is about 1.5 megabytes.

## Features
- [ ] Clean project architucture
- [ ] Separate driver and drawing applications (implement IPC for them and make public API to use in another applications)
- [ ] Support more (than one) wired graphics tablets
- [x] Pressure affects line width
- [x] Lines can be of 10 colors (Tableau palette is currently hardcoded)
- [x] Surface clearing
- [x] (testing) Export surface to PNG
- [x] (testing) Drawing straight lines
- [ ] Multiple pages (adding new ones, deleting and navigating through existing ones)
- [ ] Batch PNG export (to PDF?)
- [ ] Export to vector formats (direct PDF or SVG) (compute shader is required, i.e. OpenGL ES 3.1, i.e. Android 5.0+)
- [ ] Line transformation (move, rotate, scale, delete), requires new *tricky* shaders to get ID of selected lines
- [ ] Undo/Redo

## Build (*nix)
You don't need an entire Android Studio to build an app. See `sdk.mk`. You need **JDK** (currently at least version 16 to use **commandline-tools**), **Android SDK** and **build-tools**.
You can download the last two using ![commandline-tools](https://developer.android.com/studio#command-line-tools-only).
In `sdk.mk` specify the path to the downloaded SDK, platform version and **build-tools** version. The build also requires a key store.

Then just
```sh
make
```
Install `ajournal.apk` via **adb** or just transfer package to your phone and install manually.
