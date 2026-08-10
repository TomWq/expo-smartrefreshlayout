---
title: 故障排查
description: 排查 Expo Go、新架构、滚动子节点、刷新动画、重复分页、iOS Pods 与 Android 二楼问题。
---

# 故障排查

## 原生组件找不到

确认应用启用了 React Native New Architecture，并在安装包后完整重建。Expo Go 不包含本包的
原生实现，需要 prebuild 或 development build。

```bash
# Expo
npx expo prebuild
npx expo run:android

# iOS CLI
cd ios && pod install && cd ..
```

## 刷新动画立即结束

非受控回调若同步返回，组件会认为请求已经结束。旧的回调式网络 API 应包装成 Promise，或使用
受控 `refreshing` / `loadingMore` 状态。

## 刷新动画一直不结束

传入 `refreshing` 或 `loadingMore` 后就是受控模式。确保所有成功与失败分支最终都把对应值恢复为
`false`，通常应放在 `finally` 中。

## 自动加载没有触发

自动模式要求内容超过一屏，并先检测到用户真实向上滚动。首次挂载、短列表或 footer 进入布局都不会
触发。每次请求完成后，还需要下一次向上滚动重新解锁。

## 重复分页请求

不要同时使用 `loadMoreMode="auto"` 与 `FlatList.onEndReached`。同一组件实例会阻止内部重复请求，
但外部列表回调仍是另一条请求入口。

## 内容或空状态不显示

刷新容器只接受一个滚动 child。不要把 `FlatList`、空状态和浮层并列放入容器；把空状态放进
`ListEmptyComponent`，浮层放在外部父布局。

## iOS 修改后没有生效

重新执行 `pod install` 并完整构建 App。Metro reload 不会重新编译 Objective-C++ 原生视图。

## Android 二楼手势冲突

给二楼内部 `ScrollView` 或 `FlatList` 设置 `nestedScrollEnabled`。内部列表到达顶部后再向下拖拽关闭，
并避免在边界区域叠加横向分页或其他手势识别器。

## iOS 挂载二楼报错

这是预期行为。`SmartSecondFloorLayout` 仅支持 Android，请使用 `Platform.OS` 或平台文件分支；
iOS 页面继续使用跨平台 `SmartRefreshLayout`。
