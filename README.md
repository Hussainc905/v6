# ELYAS FUN V4 REALME 3D — source project

**Android / Java 17 / OpenGL ES 2.0, entirely offline.**

This project is an **experimental fully native 3D-geometry prototype**, not a pre-rendered cinematic movie or a finished motion-capture animation. The hinged door, child body/limbs, facial plane, portable folding ladder and cloth exist in a depth-tested OpenGL scene. A locally stored **cartoon likeness texture** based on the supplied child photo is attached to the head of the 3D model. It is a hybrid 3D character, not a detailed rigged 3D scan.

**V4 story (~52 sec):** Open door, child emerges mildly grumpy, walks with portable ladder, opens it, climbs/wipes a first area, descends and moves ladder, climbs/wipes a second area, descends, looks at viewer and waves goodbye, returns to door. **Only optional door sound**. No music, speech, video, analytics, network or ads.

### Changes designed for Realme 12 Pro+ / realme UI 7.0 / Android 16

- Active foreground service registers screen-on, screen-off and ACTION_USER_PRESENT broadcasts.
- After screen turns on it checks whether keyguard is dismissed for up to 20 seconds, and uses debouncing so animation does not start twice on one unlock.
- The home screen shows **actual in-process service state**, last event, last animation timestamp/reason, overlay permission and last recorded error. A saved user preference is *not* misrepresented as active service.
- Native real 3D renderer is transparent while used as an overlay (on compatible devices) and passes taps through. Preview shows a scene background.
- Intent-filtered boot receiver attempts service restoration if enabled, but Android/OEM may reject it. `START_STICKY` recovery is best-effort.
- `specialUse` foreground service follows API 34+ type rules. Google Play may review this category separately.
- **No Accessibility Service, no root, no bypass of Android restrictions.**

### Install and test

1. Create a **private** GitHub repository (the built-in child likeness texture is personal).
2. Upload contents of this directory to the repository root. A workflow exists at **`.github/workflows/android-apk.yml`**. Do not use a root-level `workflows` folder.
3. Visit **Actions** → `ELYAS FUN V4 3D - Build APK` → Run workflow. Download the `ELYAS_FUN_V4_REALME_3D_APK` artifact when successful and install `app-debug.apk`.
4. Open the app, approve Notifications and **Display over other apps**, and use **Test overlay**. If the overlay works, choose **Enable after unlock**. Lock, then unlock the phone to test.
5. Realme UI 7.0: Settings > Battery > Power saving settings > App battery management > ELYAS FUN > Allow background activity. Lock the app from Recents > three dots > Lock. Keep Power Saving off while testing. These settings may be translated differently on your device.
6. Return to the app for the last event and service diagnostics. If the service is not running after reboot, manually open app and enable it again.

**Important constraints:** Modern Android may prevent apps from starting foreground services from the background. Merely granting SYSTEM_ALERT_WINDOW does *not* make background service startup universally legal on Android 15/16. A working persistent service is generally needed to observe unlock events; some OEM builds may terminate it. Unlock automation is therefore **not guaranteed**. If Android shows a restricted-settings warning, only grant access to a sideloaded app you personally trust.

### Files

- `app/src/main/java/com/elyastech/elyasfun/ThreeDSceneView.java`: GLES2 3D mesh renderer, timeline and camera.
- `FunOverlayService.java`: foreground listener and transparent overlay.
- `MainActivity.java`: Arabic controls and service diagnostics.
- `BootReceiver.java`: best-effort reboot restoration.
- `SoundFX.java`: door-only optional WAV.
- `app/src/main/assets/child_face_texture.png`: stylized likeness texture, **private**.
- `.github/workflows/android-apk.yml`: Android APK CI workflow.

### Verification performed

Only static file/layout checks and ZIP-integrity checks can be performed in the current environment. There is **no Android SDK or Android emulator here**, so the project has not been compiled into an APK or tested on a Realme phone. GitHub Actions is the next compilation test.


## V4.0.1 Java compiler fix
Corrected six float-to-int compile errors in ThreeDSceneView.java by changing skin/shirt/pants colors to int. Updated version code to 9 for installing over V4.
