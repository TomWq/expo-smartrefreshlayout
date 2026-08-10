---
title: Platforms
description: Android SmartRefreshLayout and iOS SmartRefreshControl implementation details and differences.
---

# Platforms

## Android

Android uses the official SmartRefreshLayout components: `ClassicsHeader`, `ClassicsFooter`, `MaterialHeader`,
and `TwoLevelHeader`. Android-specific Material options control the bezier background and content translation.

## iOS

iOS uses vendored and modernized Classic and Material SmartRefreshControl implementations. Both platforms share
the refresh, pagination, no-more-data, messages, Classic colors, spinner modes, last-refresh label, and
Promise-completion contract. Material headers on both platforms support `indicatorColor` and
`materialProgressBackgroundColor`; on iOS, `primaryColor` remains a Classic header/footer color.

The Android-only Material layout switches are ignored safely on iOS. The second-floor component throws instead
of silently degrading because no equivalent native interaction exists.

## Expo and multiple instances

The v2 package no longer depends on Expo Modules, despite its historical name. Expo autolinking works in a
development build. Commands use Fabric view refs rather than a global native service, so multiple refresh containers
keep independent locks, request IDs, and completion commands.
