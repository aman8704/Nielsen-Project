# Project Structure

## Application structure

This project is organized as a standard Android app with a single main app module.

### App module

- `app/src/main/java/com/aman/conductorcompanion`
  - Application entry point: `MainActivity.kt`
  - UI screens: `ui/history`, `ui/shift`, `ui/ticket`
  - Networking/database/repository layers under `data/`

### Data layer

- `data/local` : Room entities and DAOs
- `data/model` : route and ticket data models
- `data/repository` : route and ticket repository logic

### Resources

- `res/layout` : XML layouts for screens and list items
- `res/values` : colors, themes, and style configuration
- `res/navigation` : navigation graph for app flow
- `res/drawable` : reusable UI drawables

### Assets

- `assets/bmtc_500d.json` : sample BMTC-like route data used by the app

## Development flow

1. Route data is loaded from the JSON asset file.
2. ViewModel derives fare and stop state from the active route.
3. UI renders the selected destination and source/nearest stop info.
4. Ticket issuance is simulated through the app flow and can be extended for real APIs or backend integration.
