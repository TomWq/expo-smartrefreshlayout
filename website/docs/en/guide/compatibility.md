---
title: Compatibility
description: Supported React Native, Expo, Android, iOS, and New Architecture versions and features.
---

# Compatibility

## Support matrix

| Target | Support | Notes |
| --- | --- | --- |
| React Native | 0.76+ | Fabric New Architecture only |
| React | 18.2+ | The RN version selects the actual React version |
| Expo | development build, prebuild, EAS Build | Expo Go does not include this native code |
| Android | API 24+ | Official SmartRefreshLayout headers, footer, and TwoLevelHeader |
| iOS | 15.1+ | Vendored Classic and Material SmartRefreshControl implementations |

## Feature matrix

| Feature | Android | iOS |
| --- | --- | --- |
| Pull to refresh | Yes | Yes |
| Pull-release load more | Yes | Yes |
| Guarded automatic load more | Experimental | Experimental |
| Classic and Material headers | Yes | Yes |
| Classic spinner modes | Yes | Yes |
| Material bezier and content translation | Yes | Ignored safely |
| Second floor | Yes | Unsupported with an explicit error |

`SmartRefreshLayout` must wrap exactly one React Native scrolling view. Compose overlays and other siblings
outside it, and enable `nestedScrollEnabled` for scrollable Android second-floor content.
