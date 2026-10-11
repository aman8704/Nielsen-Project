# Conductor Companion

A BMTC-style conductor ticketing demo app built with Kotlin and Android Jetpack.

## Features

- Day/night mode toggle
- BMTC-style bus ticket and stop selection UI
- GPS-based nearest stop detection for source selection
- Destination stop selection from a route list
- Passenger count and passenger type selection
- Fare calculation for selected route stops
- QR-like preview card and payment details panel
- Voice assistance toggle (UI + TTS support, no external backend required)
- Route info card with BMTC-inspired styling and stop badges

## Tech Stack

- Kotlin
- Android SDK 34
- Jetpack ViewModel + LiveData/Flow
- Room persistence
- Material 3 UI components
- Gson for route data parsing
- ZXing QR generation

## Project Structure

```text
.
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── assets/
│   │   │   │   └── bmtc_500d.json
│   │   │   ├── java/com/aman/conductorcompanion/
│   │   │   │   ├── data/
│   │   │   │   │   ├── local/
│   │   │   │   │   ├── model/
│   │   │   │   │   └── repository/
│   │   │   │   ├── ui/
│   │   │   │   │   ├── history/
│   │   │   │   │   ├── shift/
│   │   │   │   │   └── ticket/
│   │   │   │   └── MainActivity.kt
│   │   │   └── res/
│   │   │       ├── drawable/
│   │   │       ├── layout/
│   │   │       ├── navigation/
│   │   │       ├── values/
│   │   │       └── xml/
│   └── build.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
├── settings.gradle.kts
├── .gitignore
├── README.md
├── docs/
│   └── PROJECT_STRUCTURE.md
└── .github/
    └── workflows/
        └── android.yml
```

## Getting Started

### Prerequisites

- Android Studio latest stable
- JDK 17
- Android SDK with API 34

### Run locally

1. Clone the project
2. Open the project in Android Studio
3. Let Gradle sync complete
4. Select a device or emulator
5. Run the app

### Build from command line

```bash
./gradlew assembleDebug
```

## Notes

- The route stops are loaded from `app/src/main/assets/bmtc_500d.json`.
- GPS logic detects the nearest stop to estimate the boarding/source location.
- The app is a frontend/demo flow meant for UI simulation and conductor experience prototyping.

## License

This project is for educational and demo purposes.
