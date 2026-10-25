# Version 1.2.0 validation

Verified on 2026-10-25.

- Galaxy Watch8 Classic SM-L505F, API 36, with the app installed in place over the existing provider.
- API 36 round Wear OS emulator.
- Thirteen JVM tests passed: RSS parsing, DTD/size/item limits, deduplication, coverage retention, rotation, link validation and date formatting.
- Android lint completed without errors. Warnings covered dependency versions, English UI text construction and unused legacy refresh strings.
- Actual JobScheduler work fetched 20 Google News RSS items without opening an Activity for the fetch.
- Native reader checks passed on the emulator and watch: bulletin tap, saved snapshot, saved list, reading-size control, system Back, three distinct complication PendingIntent deliveries, real related coverage and refresh preserving list scroll.
- Compact font sizes were installed and inspected on the watch after feedback: list 15.5sp, standard bulletin heading 17sp, coverage 16sp, action labels 13sp.
- Screenshots in this repository are from the physical watch.

Not verified: paired-phone browser delivery, physical rotation of the hardware bezel, long-duration battery usage, natural timed five-minute rollover and 15-minute refresh intervals. The complication selection logic was exercised with controlled timestamps; the actual RSS fetch ran over HTTPS.

Build output and device-specific debugging credentials are not stored in this repository.
