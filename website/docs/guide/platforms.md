---
title: 平台说明
description: 理解 Android SmartRefreshLayout 与 iOS SmartRefreshControl 的实现差异和安全降级行为。
---

# 平台说明

## Android

Android 使用 [SmartRefreshLayout](https://github.com/scwang90/SmartRefreshLayout) 原生组件：

- `ClassicsHeader` 与 `ClassicsFooter` 提供经典样式和最后更新时间。
- `MaterialHeader` 提供进度圆、贝塞尔背景和内容偏移选项。
- `TwoLevelHeader` 提供 Android 淘宝二楼。
- `primaryColor` 会作用于当前 Header 与 Classic Footer。

## iOS

iOS 使用仓库内 vendored 并现代化维护的 SmartRefreshControl Classic/Material 组件。双端共享：

- 刷新、分页与没有更多数据的状态契约。
- `headerStyle`、主要颜色、指示器颜色和文案。
- `classicSpinnerStyle`、`classicEnableLastTime` 与 Promise 自动结束行为。

Android 专属的 Material 贝塞尔波浪、Header 内容偏移和二楼能力在 iOS 没有等价行为。
前两个 Material 布局 Props 会安全忽略；iOS Material 的进度圆背景使用
`materialProgressBackgroundColor`，而 `primaryColor` 继续用于 Classic Header/Footer。
`SmartSecondFloorLayout` 则会明确报错，避免悄悄降级成不同交互。

## Expo

包名包含 `expo` 是历史延续，v2 不再依赖 Expo Modules API，也不要求宿主安装 `expo`。
Expo 项目仍可以通过 autolinking 使用本包，但必须重新构建原生 development client。

## 多实例

所有命令都通过 Fabric Commands 绑定到视图 ref。这个设计没有全局原生 service，因此多个列表可以同时
存在，各自的刷新锁、`requestId` 和完成命令不会互相干扰。
