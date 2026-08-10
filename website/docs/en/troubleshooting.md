---
title: Troubleshooting
description: Diagnose New Architecture, Expo Go, child layout, stuck animation, duplicate pagination, pods, and second-floor issues.
---

# Troubleshooting

## Native component not found

Enable New Architecture and rebuild the app after installation. Expo Go does not contain this native component;
use prebuild or a development build. On iOS, rerun `pod install`.

## Animation finishes immediately

A synchronous callback completes immediately in uncontrolled mode. Wrap callback-based networking in a Promise,
or use controlled `refreshing` / `loadingMore` props.

## Animation never finishes

Controlled mode requires the app to restore the matching prop to `false`. Put the reset in `finally` so both success
and failure paths complete.

## Automatic loading does not run

The content must exceed the viewport and the user must scroll upward. Mounting, short content, and footer visibility
do not unlock automatic loading.

## Duplicate pagination

Do not combine `loadMoreMode="auto"` with `FlatList.onEndReached`.

## Missing content or empty state

The refresh container requires exactly one scrolling child. Use `ListEmptyComponent` for an empty list and compose
overlays outside the refresh container.

## Android second-floor gesture conflicts

Enable `nestedScrollEnabled` for inner scrolling content and avoid competing horizontal or edge gestures. Do not
mount `SmartSecondFloorLayout` on iOS.
