# Google Play release preparation

The repository can remain public and Apache-2.0 licensed while the official app is offered on Google Play for free or for a price. Apache-2.0 permits others to reuse and redistribute the application code under its terms; a paid listing does not make the source exclusive.

This document is release planning, not a claim that the app has passed Play review or that the current development APK is a production release.

Before the first submission:

1. Confirm the final application ID. The current `com.nocturne.news` ID preserves the existing watchface integration; Google Play package names are permanent. Do not rename it casually or break the assigned complication component.
2. Configure a private production upload key and Play App Signing. Keep keys, passwords and service-account credentials out of this repository. Produce a signed release Android App Bundle; do not submit the current debug-signed APK as a production release.
3. Complete the Wear OS listing, support/contact details, content rating, Data safety form and a public privacy policy reflecting the app and all SDKs. Verify phone handoff and the full release build on supported watches.
4. Complete the News and Magazines declaration. Keep publisher attribution, regularly updated content and accessible app contact information. Check content-source terms and rights separately from the application code license.
5. Complete required Play testing and review. New personal developer accounts may require a closed test with at least 12 testers continuously opted in for 14 days before applying for production access. Use Play Console's account-specific requirements at submission time.

Relevant official guidance:

- [Create and set up an app, including free/paid and app bundles](https://support.google.com/googleplay/android-developer/answer/9859152?hl=en)
- [News and magazines policy](https://support.google.com/googleplay/android-developer/answer/9935326?hl=en)
- [User Data and privacy policy requirements](https://support.google.com/googleplay/android-developer/answer/10144311?hl=en)
- [Testing requirements for new personal accounts](https://support.google.com/googleplay/android-developer/answer/14151465?hl=en)
- [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)

Policies can change. Recheck the current requirements when preparing the Play submission.
