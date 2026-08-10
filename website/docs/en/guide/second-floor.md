---
title: Android second floor
description: Use SmartSecondFloorLayout, TwoLevelHeader, reveal backdrops, formal content, and open/close commands.
---

# Android second floor

`SmartSecondFloorLayout` wraps Android SmartRefreshLayout's `TwoLevelHeader`. It is a separate component and
does not include a load-more footer.

::: danger Android only
Do not mount this component on iOS. iOS has no equivalent native interaction, so mounting throws an explicit error.
:::

![Formal Android second-floor content](/image-second-floor-content.jpg)

```tsx
<SmartSecondFloorLayout
  ref={floorRef}
  style={{ flex: 1 }}
  headerInset={56}
  refreshRate={1}
  floorRate={1.9}
  maxRate={2.5}
  floorDuration={1000}
  secondFloorBackground={<Image source={moon} style={{ flex: 1 }} />}
  secondFloor={<ScrollView nestedScrollEnabled>{floorContent}</ScrollView>}
  onRefresh={reload}
  onSecondFloorOpen={handleOpen}
  onSecondFloorClose={handleClose}
>
  <FlatList data={rows} renderItem={renderItem} />
</SmartSecondFloorLayout>
```

`children` is the normal scrolling page, `secondFloorBackground` is the optional revealed layer, and
`secondFloor` is the formal full-screen content. Thresholds are normalized to preserve
`refreshRate < floorRate < maxRate`.

Use `headerInset` for an overlay toolbar. Enable `nestedScrollEnabled` inside the second floor and avoid adding
competing horizontal or edge gestures to the same boundary.

`openSecondFloor()` and `closeSecondFloor()` report command acceptance. Observe `onSecondFloorOpen`,
`onSecondFloorClose`, or `onStateChange` for the actual lifecycle.
