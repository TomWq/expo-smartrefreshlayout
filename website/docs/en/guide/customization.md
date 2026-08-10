---
title: Customization
description: Configure Classic and Material headers, colors, spinner behavior, messages, and haptics.
---

# Customization

## Classic

```tsx
<SmartRefreshLayout
  headerStyle="classic"
  primaryColor="#0f766e"
  indicatorColor="#ffffff"
  titleColor="#ffffff"
  classicSpinnerStyle="fixed-behind"
  classicEnableLastTime
  onRefresh={reload}
>
  <FlatList {...listProps} />
</SmartRefreshLayout>
```

`classicSpinnerStyle` accepts `scale`, `translate`, or `fixed-behind` on Android and iOS.

## Material

```tsx
<SmartRefreshLayout
  headerStyle="material"
  primaryColor="#e8573d"
  indicatorColor="#ffffff"
  materialShowBezierWave
  materialEnableHeaderTranslationContent={false}
  materialProgressBackgroundColor="#e8573d"
  onRefresh={reload}
>
  <FlatList {...listProps} />
</SmartRefreshLayout>
```

Material bezier and content-translation switches are Android-specific and ignored safely on iOS. Both Material
implementations support `indicatorColor` and `materialProgressBackgroundColor`; iOS keeps `primaryColor` for
Classic headers and footers.

## Messages and haptics

Pass a partial `messages` object to localize pull, release, loading, completion, and no-more-data states.
`hapticsEnabled` defaults to `true` and triggers once when the release threshold is reached.
