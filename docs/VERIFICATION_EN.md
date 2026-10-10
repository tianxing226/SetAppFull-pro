# Compatibility Notes

[简体中文](VERIFICATION.md) | [English](VERIFICATION_EN.md)

- Requires Android 11 or later and a modern LSPosed / libxposed-compatible framework (API 101 or 102).
- The framework controls scope authorization. A newly scoped app usually needs restarting; saving a rule does not mean the module is already loaded.
- Existing custom rules, explicit disables and resets take precedence over scoped defaults.
- Multi-window, picture-in-picture and floating windows suspend fullscreen policies. Navigation-bar control is released while the keyboard is visible.
- Bilibili playback optimization is limited to designated player pages in known versions and can be disabled independently. Version 9.14.0 may have startup issues in some environments and is not guaranteed compatible; unknown versions retain native behavior.
- Native aspect-ratio borders, encoded black bars, DRM and hardware protection may remain. Screenshot and VPN-detection compatibility options are off by default.

[Back to usage](../README_EN.md)
