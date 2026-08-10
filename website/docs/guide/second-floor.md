---
title: Android 淘宝二楼
description: 使用 SmartSecondFloorLayout、TwoLevelHeader、揭露背景、正式内容和打开关闭实例命令。
---

# Android 淘宝二楼

`SmartSecondFloorLayout` 基于 Android SmartRefreshLayout 的 `TwoLevelHeader`。它是独立组件，
不属于普通 `SmartRefreshLayout` 的 Header 样式，也不提供上拉加载 Footer。

::: danger 仅支持 Android
iOS 没有等价原生能力。不要在 iOS 平台分支中挂载该组件；挂载时会抛出明确错误。
:::

## 组成

![普通页面、揭露背景与正式二楼内容](/image-second-floor-content.jpg)

- `children`：普通页面唯一的滚动子组件。
- `secondFloorBackground`：拖拽过程中逐渐揭露的背景，可选。
- `secondFloor`：二楼打开后显示的正式全屏内容。

## 基本用法

```tsx
const floorRef = useRef<SmartSecondFloorLayoutRef>(null);

<SmartSecondFloorLayout
  ref={floorRef}
  style={{ flex: 1 }}
  headerInset={56}
  floorRate={1.9}
  maxRate={2.5}
  refreshRate={1}
  floorDuration={1000}
  pullToCloseEnabled
  secondFloorBackground={
    <Image source={require('./moon.jpg')} style={{ flex: 1 }} />
  }
  secondFloor={
    <ScrollView nestedScrollEnabled>{floorContent}</ScrollView>
  }
  onRefresh={reload}
  onSecondFloorOpen={() => track('floor_opened')}
  onSecondFloorClose={() => track('floor_closed')}
>
  <FlatList data={rows} renderItem={renderItem} />
</SmartSecondFloorLayout>
```

## 阈值

| Prop | 默认值 | 含义 |
| --- | --- | --- |
| `refreshRate` | `1` | 普通下拉刷新的释放倍率 |
| `floorRate` | `1.9` | 进入二楼的释放倍率 |
| `maxRate` | `2.5` | 最大拖拽倍率 |
| `floorDuration` | `1000` | 展开与停留动画时长，毫秒 |
| `bottomPullUpToCloseRate` | `1/6` | 从二楼底部上拉关闭的倍率 |

矛盾的阈值会在 JavaScript 与 Android 两端归一化，保证 `refreshRate < floorRate < maxRate`。

## Toolbar 与内部滚动

页面顶部有覆盖式 Toolbar 时，把其逻辑高度传入 `headerInset`。它会计入 Header 总高度，使 Classic
Header 的可见位置与二楼阈值保持一致。

二楼内容可以是 `ScrollView` 或 `FlatList`，但会与外层下拉关闭共享边界手势。请开启
`nestedScrollEnabled`，并避免在同一边界区域叠加横向分页或自定义拖拽手势。

## 打开和关闭

```ts
floorRef.current?.openSecondFloor();
floorRef.current?.closeSecondFloor();
```

返回值表示命令是否被已挂载实例接受，不代表动画已经结束。准确生命周期请监听
`onSecondFloorOpen`、`onSecondFloorClose` 与 `onStateChange`。
