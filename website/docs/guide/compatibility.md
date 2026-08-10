---
title: 兼容性
description: 查看 expo-smartrefreshlayout 对 React Native、Expo、Android、iOS 和新架构的支持范围。
---

# 兼容性

## 支持矩阵

| 目标 | 支持范围 | 说明 |
| --- | --- | --- |
| React Native | 0.76+ | 仅支持 Fabric New Architecture |
| React | 18.2+ | React Native 版本仍决定实际 React 版本 |
| Expo | development build / prebuild / EAS Build | Expo Go 不包含本包原生代码 |
| Android | API 24+ | 使用官方 SmartRefreshLayout Header、Footer 与 TwoLevelHeader |
| iOS | 15.1+ | 使用 vendored SmartRefreshControl Classic/Material 实现 |

## 功能对比

| 能力 | Android | iOS |
| --- | --- | --- |
| 下拉刷新 | 支持 | 支持 |
| 上拉释放加载 | 支持 | 支持 |
| 自动加载更多 | 实验性 | 实验性 |
| Classic Header | 支持 | 支持 |
| Material Header | 支持 | 支持 |
| Classic Spinner 样式 | 支持 | 支持 |
| Material 贝塞尔背景与内容偏移 | 支持 | 安全忽略 |
| 淘宝二楼 | 支持 | 不支持，挂载会抛出明确错误 |

## 新架构要求

v2 使用 Fabric Native Component 和 Codegen Commands，不提供 Paper 旧架构实现。实例命令直接绑定到
具体原生视图，因此同一页面可以安全存在多个刷新容器。

React Native 0.76 到 0.81 项目需要显式确认新架构已启用；React Native 0.82+ 已只提供新架构。

## 滚动子组件约束

`SmartRefreshLayout` 的唯一 child 必须是能与原生容器协作的 React Native 滚动视图。空状态、浮层、
Toolbar 等附加 UI 应在刷新容器外部组合。Android 二楼的内部列表建议开启 `nestedScrollEnabled`。
