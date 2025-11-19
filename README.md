# expo-smartrefreshlayout

一个功能强大的 React Native 下拉刷新和上拉加载组件，**基于 Expo Modules 开发**，使用原生库实现：
- Android: [SmartRefreshLayout](https://github.com/scwang90/SmartRefreshLayout)
- iOS: [MJRefresh](https://github.com/CoderMJLee/MJRefresh)

> 💡 本组件使用 [Expo Modules API](https://docs.expo.dev/modules/overview/) 构建，提供了类型安全的原生模块接口和优秀的开发体验。

## ✨ 特性

- ✅ 支持下拉刷新和上拉加载
- ✅ 支持自定义刷新头和加载尾样式
- ✅ 支持经典（Classic）和 Material Design 两种样式
- ✅ 支持 Lottie 动画集成，可实现精美的自定义动画效果
- ✅ 丰富的配置选项和事件回调
- ✅ 完整的 TypeScript 类型定义
- ✅ 支持自动加载更多
- ✅ 支持嵌套滚动
- ✅ 流畅的动画效果
- ✅ 跨平台支持（Android & iOS）
- ✅ 支持自定义 Header 组件（Footer 自定义功能待实现）
- ✅ 完整的状态追踪和实时回调
- ✅ 同时支持 React Native 新旧架构（Paper & Fabric）

## 📦 安装

```bash
npm install expo-smartrefreshlayout
# 或
yarn add expo-smartrefreshlayout
# 或
pnpm add expo-smartrefreshlayout
```

### Expo 项目

如果你使用的是 Expo 管理的项目，安装后需要重新构建原生代码：

```bash
# 使用 EAS Build
eas build --platform all

# 或使用本地构建
npx expo prebuild
npx expo run:android
npx expo run:ios
```

### 纯 React Native 项目

对于纯 React Native 项目，确保已安装 `expo` 包作为依赖：

```bash
npm install expo
# 然后重新构建应用
npx react-native run-android
npx react-native run-ios
```

## 🚀 快速开始

### 基础用法

```tsx
import { ExpoSmartrefreshlayoutView, ExpoSmartrefreshlayoutModule } from 'expo-smartrefreshlayout';
import { FlatList, View, Text } from 'react-native';
import { useState } from 'react';

function App() {
  const [data, setData] = useState([1, 2, 3, 4, 5]);

  const handleRefresh = async () => {
    // 执行刷新逻辑
    await fetchData();
    // 完成刷新
    ExpoSmartrefreshlayoutModule.finishRefresh(true, 300);
  };

  return (
    <ExpoSmartrefreshlayoutView
      style={{ flex: 1 }}
      onRefresh={handleRefresh}
    >
      <FlatList
        data={data}
        renderItem={({ item }) => (
          <View style={{ padding: 20, borderBottomWidth: 1 }}>
            <Text>Item {item}</Text>
          </View>
        )}
        keyExtractor={(item) => item.toString()}
      />
    </ExpoSmartrefreshlayoutView>
  );
}
```

### 启用加载更多

```tsx
<ExpoSmartrefreshlayoutView
  enableLoadMore={true}
  onRefresh={handleRefresh}
  onLoadMore={handleLoadMore}
>
  <FlatList data={data} {...listProps} />
</ExpoSmartrefreshlayoutView>
```

### 自定义样式

```tsx
<ExpoSmartrefreshlayoutView
  headerType="material"
  classicRefreshHeaderProps={{
    headerAccentColor: '#007AFF',
    REFRESH_HEADER_PULLING: '下拉刷新',
    REFRESH_HEADER_RELEASE: '释放刷新',
  }}
  onRefresh={handleRefresh}
>
  <FlatList data={data} {...listProps} />
</ExpoSmartrefreshlayoutView>
```

## 📖 文档

- **[API 文档](./docs/API.md)** - 完整的 Props、方法和类型定义
- **[自定义 Header 文档](./docs/CUSTOM_HEADER.md)** - 自定义下拉刷新 Header 的完整指南
- **[示例代码](./docs/EXAMPLES.md)** - 丰富的使用示例和最佳实践

## 🏗️ 架构支持

本组件基于 Expo Modules API 构建，**自动支持 React Native 的新旧架构**：

- ✅ **旧架构（Paper）**：React Native < 0.74
- ✅ **新架构（Fabric）**：React Native >= 0.68
- ✅ **零配置切换**：组件会根据项目架构自动适配

## 🎯 核心 API

### 主要 Props

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `enableRefresh` | `boolean` | `true` | 是否启用下拉刷新 |
| `enableLoadMore` | `boolean` | `false` | 是否启用上拉加载 |
| `headerType` | `'classics' \| 'material'` | `'classics'` | Header 类型 |
| `renderHeader` | `(params: RenderHeaderParams) => React.ReactNode` | - | 自定义 Header 组件，接收状态参数 |
| `onRefresh` | `() => void` | - | 下拉刷新回调 |
| `onLoadMore` | `() => void` | - | 上拉加载回调 |

### 主要方法

```tsx
import { ExpoSmartrefreshlayoutModule } from 'expo-smartrefreshlayout';

// 完成刷新
ExpoSmartrefreshlayoutModule.finishRefresh(success?: boolean, delay?: number);

// 完成加载更多
ExpoSmartrefreshlayoutModule.finishLoadMore(success?: boolean, delay?: number, noMoreData?: boolean);

// 自动刷新
ExpoSmartrefreshlayoutModule.autoRefresh(delay?: number);

// 设置没有更多数据
ExpoSmartrefreshlayoutModule.setNoMoreData(noMoreData: boolean);
```

> ⚠️ **重要提示**：虽然组件内部有 3 秒自动结束机制，但**强烈建议手动调用** `finishRefresh/finishLoadMore` 方法以获得最佳用户体验。

## 💡 常见场景

### 场景 1：只需要下拉刷新（推荐）

使用 FlatList 自带的 `onEndReached` 处理加载更多：

```tsx
<ExpoSmartrefreshlayoutView onRefresh={handleRefresh}>
  <FlatList
    data={data}
    onEndReached={handleEndReached}
    onEndReachedThreshold={0.1}
    renderItem={({ item }) => <Item data={item} />}
  />
</ExpoSmartrefreshlayoutView>
```

### 场景 2：需要统一的刷新和加载更多 UI

显式启用 `enableLoadMore`：

```tsx
<ExpoSmartrefreshlayoutView
  enableLoadMore={true}
  onRefresh={handleRefresh}
  onLoadMore={handleLoadMore}
>
  <FlatList data={data} renderItem={({ item }) => <Item data={item} />} />
</ExpoSmartrefreshlayoutView>
```


## 📄 许可证

MIT

## 🔗 相关链接

- [API 文档](./docs/API.md)
- [自定义 Header 文档](./docs/CUSTOM_HEADER.md)
- [示例代码](./docs/EXAMPLES.md)
- [SmartRefreshLayout (Android)](https://github.com/scwang90/SmartRefreshLayout)
- [MJRefresh (iOS)](https://github.com/CoderMJLee/MJRefresh)


## 📮 反馈与支持

如果你在使用过程中遇到问题或有任何建议，欢迎：

- 📝 提交 [GitHub Issue](https://github.com/TomWq/expo-smartrefreshlayout/issues)
- 💬 参与 [Discussions](https://github.com/TomWq/expo-smartrefreshlayout/discussions)
- ⭐ 给项目点个 Star 支持一下
- 💬 加入 QQ 群：952241387 
