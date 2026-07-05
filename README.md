# Detox — Reels & Shorts Blocker

An Android app that lets you keep using Instagram (for DMs/chat) and YouTube
(for regular videos) while automatically blocking you out of Reels and
Shorts — the two features designed to trigger endless scrolling.

## How it works

Android doesn't let one app "turn off" a feature inside another app. Instead,
this uses an **Accessibility Service**: a system feature (the same one screen
readers use) that lets an app read on-screen content in real time. This app
watches only Instagram and YouTube, and only for signs that you've opened
Reels or Shorts specifically — everything else (chats, feed, normal video
playback) is left alone. When it detects Reels/Shorts, it instantly triggers
Android's system "Back" action to bounce you out.

This is a legitimate, well-known technique — it's how most reputable
screen-time/focus apps work. It requires no root, no "hacking," and Google
Play requires you to explain accessibility usage clearly if you publish it.

## Features

- **Reels blocking** in Instagram (com.instagram.android)
- **Shorts blocking** in YouTube (com.google.android.youtube)
- **Daily stats**: how many times you were auto-blocked today + a 7-day bar chart
- **Focus schedule**: optionally only enforce blocking during chosen hours/days
  (e.g. 9 AM–6 PM, Mon–Fri) — outside that window Reels/Shorts work normally

## Project structure

```
DetoxApp/
  app/src/main/java/com/detoxapp/
    MainActivity.kt                     – status/stats/schedule UI
    service/ReelsShortsBlockerService.kt – the accessibility service (core logic)
    data/StatsRepository.kt             – per-day block counters
    data/ScheduleRepository.kt          – focus-schedule settings
    ui/WeeklyBarChartView.kt            – simple custom bar chart (no libraries)
  app/src/main/res/xml/accessibility_service_config.xml
  app/src/main/res/layout/activity_main.xml
```

## Building it

1. Open the `DetoxApp` folder in **Android Studio** (Hedgehog/Koala or newer).
2. Let it sync Gradle (it will generate the wrapper jar automatically, or
   go to *File > Sync Project with Gradle Files*).
3. Run on a device or emulator (minSdk 26 / Android 8.0+).
4. On first launch, tap **Enable blocker** → it opens
   *Settings > Accessibility* → find **Detox** → turn it on.
5. Set up your focus schedule if you want blocking limited to certain hours.

## Important: keeping detection working over time

Instagram and YouTube don't publish a stable way to detect "user is on
Reels/Shorts right now." This app looks for specific internal view IDs and
labels (see the `INSTAGRAM_REELS_MARKERS` / `YOUTUBE_SHORTS_MARKERS` lists at
the top of `ReelsShortsBlockerService.kt`). These are accurate as of early
2026, but **app updates to Instagram or YouTube can occasionally change
them**, which may cause blocking to silently stop working.

If that happens:
1. Install a tool like Google's "Accessibility Scanner" from the Play Store.
2. Open Reels/Shorts, run a scan, and note the new resource-id or label shown.
3. Add that string to the relevant marker list in the service file and rebuild.

## Permissions

Only one special permission is used: the **Accessibility Service** permission,
which you grant manually in Settings (Android requires this — no app can
request it silently). No internet permission, no contacts, no storage access.
Everything (stats, schedule) is stored locally on your device only.

## Notes / possible next steps

- Add a foreground-service notification reminding you it's active.
- Add per-app stats breakdown (Instagram vs YouTube separately) in the UI —
  the data is already tracked separately in `StatsRepository`, just not
  broken out visually yet.
- Add TikTok support the same way, if you use it (find its Shorts-equivalent
  container ID the same way described above).
