# Data safety declaration — Focus

Complete the store questionnaire against the final merged manifest and the current [Yandex Mobile Ads SDK Data Safety disclosure](https://ads.yandex.com/helpcenter/en/dev/android/app-privacy-android) before publishing. The values below describe the current source and should be rechecked if SDK settings or features change.

## App functionality
- The game creates no account and requests no personal profile details.
- Score, streak, sound preference, haptic preference and ad-personalization choice are stored locally in `SharedPreferences` and Android Auto Backup.
- No user-generated content, contacts, photos, messages, payment, health, precise location or microphone/camera access.
- The app does not separately initialize AppMetrica, send custom analytics events, or integrate a crash-reporting SDK. Yandex Mobile Ads 8.6.0 has AppMetrica-related transitive components; account-level SDK synchronization/automatic events are not configured by this app. Verify the Yandex dashboard before release.

## Yandex ads
- The only planned SDK is `com.yandex.android:mobileads:8.6.0`.
- The app explicitly calls `YandexAds.setLocationTracking(false)` and applies the user’s consent choice before SDK initialization.
- The SDK may process device or other identifiers and technical/ad interaction data to request and measure advertising. Verify the exact current Yandex disclosure in the SDK documentation and store SDK inventory at release time.
- Transport is protected by the SDK/network TLS. The user can choose contextual-only ads; the banner is omitted if no production unit ID is configured.

## Suggested Play / RuStore answers
- Data type: **Device or other IDs** — Yes if the advertising ID permission is present in the merged release manifest; verify the final APK/AAB manifest. Yandex’s current SDK Data Safety disclosure marks this for advertising/marketing, not analytics by default.
- Purpose: **Advertising or marketing** — Yes.
- Shared with third parties: **Yes**, Yandex as the ad provider.
- Encrypted in transit: **Yes** for SDK connections using HTTPS/TLS.
- Required or optional: **Optional for personalized advertising**; contextual ads may still be requested when SDK is configured. Ensure the exact store form matches the live implementation.
- Data deletion: local progress can be removed by clearing app storage or uninstalling; privacy/data requests: `kirillpetrov08@gmail.com`.
- All other categories: **No**, unless a future SDK/build manifest review identifies a new collection.

No ad unit ID is set by default. Release builds do not substitute demo or third-party IDs. Fill `focus.bannerUnitId` with the production banner ID from the Focus app in Yandex’s dashboard before enabling ads.
