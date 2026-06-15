# 🔦 Simple Flashlight

Eine einfache, schnelle und stabile Taschenlampen-App für Android.

## Funktionen

- **LED-Taschenlampe** – Ein-/Ausschalten mit großem Power-Button
- **SOS-Modus** – Blinkt das internationale SOS-Signal (···−−−···)
- **Auto-Off Timer** – 1, 5 oder 10 Minuten
- **Bildschirmlicht-Fallback** – Falls keine LED verfügbar ist
- **Vibrationsfeedback** – Haptisches Feedback bei jeder Aktion
- **Akkuanzeige** – Zeigt aktuellen Batteriestand und Ladezustand
- **Dunkles Design** – Minimalistisch, gut bedienbar
- **Homescreen-Widget** – Taschenlampe direkt vom Startbildschirm schalten

## Build

```bash
cd /path/to/SimpleFlashlight
./gradlew assembleDebug
```

APK liegt dann unter: `app/build/outputs/apk/debug/app-debug.apk`

## Berechtigungen

| Berechtigung | Zweck |
|---|---|
| `CAMERA` | LED-Flash über CameraManager steuern |
| `FLASHLIGHT` | Zugriff auf die Taschenlampen-LED |
| `VIBRATE` | Haptisches Feedback |

**Keine Internetberechtigung. Keine Tracker. Keine Werbung.**

## Technische Details

- **Sprache:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Min SDK:** 26 (Android 8.0)
- **Target SDK:** 34 (Android 14)
- **Architektur:** MVVM mit AndroidViewModel
- **LED-Steuerung:** CameraManager.setTorchMode()
- **SOS:** Handler-basiertes Blink-Muster
- **Timer:** CountDownTimer mit Auto-Off
- **Bildschirmlicht:** WindowManager.LayoutParams.screenBrightness = 1.0f
- **Akku:** BroadcastReceiver für ACTION_BATTERY_CHANGED

## Kontakt
`Flashlight@seblabs.unbox.at`
