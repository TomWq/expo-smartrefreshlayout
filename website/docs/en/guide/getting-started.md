---
title: Quick start
description: Install expo-smartrefreshlayout in a React Native New Architecture or Expo development-build project.
---

# Quick start

`expo-smartrefreshlayout` is a Fabric native component backed by SmartRefreshLayout on Android and a
vendored SmartRefreshControl implementation on iOS.

## Requirements

| Target | Minimum |
| --- | --- |
| React Native | 0.76 |
| React | 18.2 |
| Android | API 24 |
| iOS | 15.1 |
| Architecture | React Native New Architecture |

::: warning Expo Go is not supported
Use prebuild, a development build, or EAS Build so the native views are included.
:::

## Install

```bash
npm install expo-smartrefreshlayout@next
```

Install iOS pods, then rebuild the app:

```bash
cd ios
pod install
cd ..
```

## Minimal example

```tsx
import { FlatList, Text } from 'react-native';
import { SmartRefreshLayout } from 'expo-smartrefreshlayout';

export function Feed() {
  return (
    <SmartRefreshLayout
      style={{ flex: 1 }}
      onRefresh={() => reloadFirstPage()}
      onLoadMore={() => loadNextPage()}
    >
      <FlatList
        data={rows}
        keyExtractor={(row) => row.id}
        renderItem={({ item }) => <Text>{item.title}</Text>}
      />
    </SmartRefreshLayout>
  );
}
```

The component requires exactly one native scrolling child. In uncontrolled mode, a callback Promise
automatically finishes the matching native animation when it settles.

## Expo projects

```bash
npx expo prebuild
npx expo run:android
# or npx expo run:ios
```

The package does not require a config plugin. Native dependency changes still require a rebuilt development client.
