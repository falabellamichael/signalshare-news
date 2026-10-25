# SignalShare: News branding

An original geometric S with a broadcast accent, designed for high contrast and small round watch displays.

- `signalshare-news-icon.svg`: square icon with the dark background.
- `signalshare-news-icon-512.png`: 512-by-512 PNG export for listing/marketing use.
- `signalshare-news-mark.svg`: transparent color mark.
- `signalshare-news-monochrome.svg`: transparent white mark for tinting.
- `signalshare-news-wordmark.svg` and `.png`: horizontal brand lockup.

Colors: ink `#071018`, white `#FFFFFF`, pale blue `#90DEFF`.

The Android launcher has independent foreground, background and monochrome layers. The important artwork fits within the central 66-by-66 region of a 108-by-108 canvas. The compact in-app and complication vectors use a tighter 72-by-72 viewport. The icon includes no text, so it remains recognizable at small sizes.

Native resources are in `app/src/main/res/drawable/ic_signalshare_*.xml`, `drawable/ic_news.xml` and `mipmap-anydpi-v26/ic_launcher.xml`. Keep their paths consistent with the SVG source when editing the mark.

These project-created assets are included under the repository's Apache-2.0 copyright license. As described by Apache-2.0, the license does not grant trademark rights or permission to imply that a fork is the official SignalShare distribution.
