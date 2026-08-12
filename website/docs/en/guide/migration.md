---
title: Migrate from v1
description: Move from the legacy Expo Modules API and global commands to Fabric view refs and the v2 API.
---

# Migrate from v1

v2 is a New Architecture rewrite. The package name remains `expo-smartrefreshlayout`, but Expo Modules and
the Paper architecture are no longer supported.

## Update the environment

- Upgrade React Native to 0.76 or later and enable New Architecture.
- Use iOS 15.1+ and Android API 24+.
- Reinstall pods and rebuild the native app.
- Use an Expo development build instead of Expo Go.

## Rename the component

```diff
- import { ExpoSmartrefreshlayoutView } from 'expo-smartrefreshlayout';
+ import { SmartRefreshLayout } from 'expo-smartrefreshlayout';
```

The old component name remains as a temporary alias but accepts only v2 props.

## Replace global commands

```diff
- ExpoSmartrefreshlayoutModule.autoRefresh(200);
+ refreshRef.current?.beginRefresh(200);

- ExpoSmartrefreshlayoutModule.finishRefresh(true, 300);
+ refreshRef.current?.finishRefresh({ success: true, delay: 300 });

- ExpoSmartrefreshlayoutModule.autoLoadMore();
+ refreshRef.current?.beginLoadMore();
```

Return a Promise from callbacks in uncontrolled mode instead of manually finishing ordinary requests.

## Important prop changes

| v1 | v2 |
| --- | --- |
| `enableRefresh` | `refreshEnabled` |
| `enableLoadMore` | `loadMoreEnabled` |
| `enableAutoLoadMore` | `loadMoreMode="auto"` |
| `enableHapticFeedback` | `hapticsEnabled` |
| `headerType="classics"` | `headerStyle="classic"` |
| `onStateChanged` | `onStateChange` |
| `renderHeader` | `refreshHeader` |
| `onHeaderMoving` | `onHeaderMoving` with `percent`, `offset`, `height`, `maxDragHeight`, and `isDragging` |

`refreshHeader` mounts React content into the native Header slot on both platforms instead of into list content, and
replaces the Classic or Material header. The custom Header has a fixed logical height of `80`.
`onHeaderMoving` reports `offset`, `height`, and `maxDragHeight` in dp/pt logical pixels; `percent >= 1` is the
refresh threshold.

`renderFooter`, `DefaultRefreshHeader`, `onFooterMoving`, Paper support, and the old global module are not part of
v2. Legacy Header/Footer height, drag-rate, and rebound-time tuning parameters also remain unsupported.
