# SetAppFull pro 2.0.5 verification

Version: 2.0.5 / 205; Android 11+; libxposed API 101 / 102.

- Passed: 22 unit tests, release build, release lint, APK metadata checks, and release signature verification.
- Checked: Bilibili portrait video detail page top layout and return to Home.
- Unverified: the general FusionApp rule, VPN compatibility at runtime, and persistence across restarts. Instrumentation tests and a full app regression suite were not run for this release.

VPN compatibility handles common VPN status queries only in selected apps and is off by default. It does not change routing or handle proxy detection, TLS validation, or certificate pinning. Screenshots remain subject to DRM and hardware protection.

[Back to usage](../README_EN.md)
