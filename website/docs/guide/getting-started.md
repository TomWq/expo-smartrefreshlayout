---
title: 快速开始
description: 安装 expo-smartrefreshlayout，在 React Native 新架构或 Expo development build 中接入原生刷新容器。
---

# 快速开始

`expo-smartrefreshlayout` 是一个 React Native Fabric 原生组件。Android 底层使用
[SmartRefreshLayout](https://github.com/scwang90/SmartRefreshLayout)，iOS 使用仓库内维护的
SmartRefreshControl Classic/Material 实现。

## 环境要求

| 目标 | 最低要求 |
| --- | --- |
| React Native | 0.76 |
| React | 18.2 |
| Android | API 24 |
| iOS | 15.1 |
| 架构 | React Native New Architecture |

::: warning Expo Go 不支持
本包包含双端原生视图。Expo 项目需要使用 prebuild、development build 或 EAS Build。
:::

## 安装

当前 v2 仍通过 next tag 发布：

```bash
npm install expo-smartrefreshlayout@next
```

iOS 需要重新安装 Pods：

```bash
cd ios
pod install
cd ..
```

然后完整重建 App。React Native CLI 与 Expo prebuild 都通过 autolinking 接入，不需要手动注册原生包。

## 最小用法

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

组件必须包含且只包含一个原生滚动子组件，例如 `FlatList`、`SectionList` 或 `ScrollView`。
回调返回 Promise 时，非受控模式会等待 Promise 完成后自动收起动画。

## Expo 项目

安装依赖后创建 development build：

```bash
npx expo prebuild
npx expo run:android
# 或 npx expo run:ios
```

本包不需要 config plugin，但原生依赖变化后仅刷新 Metro 不会生效，必须重建 development client。

## 下一步

- 了解请求如何开始与结束：[刷新与分页](./refresh-and-load-more)。
- 配置 Header、颜色与文案：[样式与文案](./customization)。
- 使用 Android TwoLevelHeader：[Android 二楼](./second-floor)。
- 从旧版升级：[从 v1 迁移](./migration)。
