# Letterboxd Android Widgets

Android port of [LetterboxdWidgets](https://github.com/original-repo) — originally built for iOS Scriptable. This version runs as native Android home screen widgets.

## Widgets

### Recents
Displays your 4 most recently watched films with star ratings. Updates every 30 minutes.

### Favourites
Displays your 4 favourite films from your Letterboxd profile. Updates once per day.

## Setup

1. Open the project in **Android Studio Hedgehog (2023.1.1)** or newer.
2. Connect your device or start an emulator (API 26+).
3. Run **▶ Run 'app'** to install.
4. On your home screen, long-press → **Widgets**.
5. Find **Letterboxd Recents** or **Letterboxd Favourites** and drag to your home screen.
6. A configuration screen will appear — enter your Letterboxd username and tap **Add Widget**.
7. The widget loads your posters automatically.

## Project Structure

```
app/src/main/
├── AndroidManifest.xml
├── java/com/letterboxd/widget/
│   ├── Film.kt                        # Data model
│   ├── LetterboxdScraper.kt           # RSS + HTML scraping logic
│   ├── WidgetPrefs.kt                 # SharedPreferences helper
│   ├── WidgetUpdater.kt               # Downloads bitmaps, builds RemoteViews
│   ├── RecentsWidgetProvider.kt       # AppWidgetProvider for Recents
│   ├── FavouritesWidgetProvider.kt    # AppWidgetProvider for Favourites
│   ├── RecentsWidgetConfigActivity.kt # Username setup screen (Recents)
│   ├── FavouritesWidgetConfigActivity.kt
│   └── WidgetUpdateService.kt         # Background refresh service
└── res/
    ├── layout/
    │   ├── widget_recents.xml
    │   ├── widget_favourites.xml
    │   ├── widget_loading.xml
    │   └── activity_config.xml
    ├── xml/
    │   ├── recents_widget_info.xml
    │   └── favourites_widget_info.xml
    └── drawable/
        ├── widget_background.xml      # Dark gradient (#202831 → #15191E)
        └── poster_placeholder.xml
```

## How It Works

| Feature | iOS (Scriptable) | Android |
|---|---|---|
| Data source (Recents) | RSS feed | RSS feed (XmlPullParser) |
| Data source (Favourites) | HTML scrape | HTML scrape (Regex) |
| Image loading | `Request.loadImage()` | `BitmapFactory.decodeStream` |
| Rounded corners | `posterPhoto.cornerRadius = 10` | Canvas + PorterDuff mask |
| Widget layout | `ListWidget` + stacks | `RemoteViews` XML layout |
| Caching | `FileManager` local files | `SharedPreferences` (slugs) + disk bitmap cache |
| Tap actions | Universal links | `PendingIntent` → `ACTION_VIEW` |

## Requirements

- Android 8.0 (API 26) or higher
- Internet permission (declared in manifest)

## Notes

- Tapping a poster opens the film page in your browser / Letterboxd app.
- Tapping the title opens your Letterboxd profile.
- If Letterboxd changes their HTML structure, `LetterboxdScraper.kt` is the only file that needs updating.
