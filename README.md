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

如果你使用的是 Expo 管理的项目（使用 `expo prebuild` 或开发构建），安装后需要重新构建原生代码：

```bash
# 使用 EAS Build
eas build --platform all

# 或使用本地构建
npx expo prebuild
npx expo run:android
npx expo run:ios
```

### 纯 React Native 项目

对于纯 React Native 项目（通过 `react-native init` 创建），确保已安装 `expo` 包作为依赖：

```bash
npm install expo
# 然后重新构建应用
npx react-native run-android
npx react-native run-ios
```

## 🏗️ 架构支持

本组件基于 Expo Modules API 构建，**自动支持 React Native 的新旧架构**：

- ✅ **旧架构（Paper）**：React Native < 0.74，使用传统 Bridge 通信
- ✅ **新架构（Fabric）**：React Native >= 0.68，自动启用新架构特性
- ✅ **零配置切换**：组件会根据项目架构自动适配，无需任何额外配置


## 🚀 快速开始

### 基础用法

```tsx
import { ExpoSmartrefreshlayoutView } from 'expo-smartrefreshlayout';
import { FlatList, View, Text } from 'react-native';

function App() {
  const [data, setData] = useState([1, 2, 3, 4, 5]);
  const [refreshing, setRefreshing] = useState(false);

  const handleRefresh = () => {
    console.log('开始刷新');
    // 模拟网络请求
    setTimeout(() => {
      setData([...Array(5)].map((_, i) => i + 1));
      // 刷新完成后调用 finishRefresh（推荐）
      ExpoSmartrefreshlayoutModule.finishRefresh(true, 300);
    }, 2000);
  };

  const handleLoadMore = () => {
    console.log('开始加载更多');
    // 模拟加载更多
    setTimeout(() => {
      setData([...data, ...Array(5)].map((_, i) => data.length + i + 1)]);
      // 加载完成后调用 finishLoadMore（推荐）
      ExpoSmartrefreshlayoutModule.finishLoadMore(true, 300);
    }, 2000);
  };

  return (
    <ExpoSmartrefreshlayoutView
      style={{ flex: 1 }}
      onRefresh={handleRefresh}
      onLoadMore={handleLoadMore}
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

### 调用方法

方法通过 Module 暴露，直接导入使用：

```tsx
import { ExpoSmartrefreshlayoutView, ExpoSmartrefreshlayoutModule } from 'expo-smartrefreshlayout';

function App() {
  const handleRefresh = async () => {
    try {
      // 执行刷新逻辑
      await fetchData();
      // 刷新成功
      ExpoSmartrefreshlayoutModule.finishRefresh(true, 300);
    } catch (error) {
      // 刷新失败
      ExpoSmartrefreshlayoutModule.finishRefresh(false, 300);
    }
  };

  const handleLoadMore = async () => {
    try {
      const newData = await loadMoreData();
      if (newData.length === 0) {
        // 没有更多数据
        ExpoSmartrefreshlayoutModule.finishLoadMore(true, 0, true);
      } else {
        // 加载成功
        ExpoSmartrefreshlayoutModule.finishLoadMore(true, 300);
      }
    } catch (error) {
      // 加载失败
      ExpoSmartrefreshlayoutModule.finishLoadMore(false, 300);
    }
  };

  return (
    <ExpoSmartrefreshlayoutView
      onRefresh={handleRefresh}
      onLoadMore={handleLoadMore}
    >
      {/* 你的内容 */}
    </ExpoSmartrefreshlayoutView>
  );
}
```

## 📖 API 文档

### Props

#### 基础配置

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `enableRefresh` | `boolean` | `true` | 是否启用下拉刷新功能 |
| `enableLoadMore` | `boolean` | `false` | 是否启用上拉加载功能（默认关闭，避免与 FlatList 的 onEndReached 冲突） |
| `enableAutoLoadMore` | `boolean` | `false` | 是否启用列表惯性滑动到底部时自动加载更多 |
| `enablePureScrollMode` | `boolean` | `false` | 是否启用纯滚动模式（Android 专属） |
| `renderHeader` | `() => React.ReactElement` | - | 自定义 Header 组件渲染函数，提供后将自动使用自定义 Header |
| `renderFooter` | `() => React.ReactElement` | - | 自定义 Footer 组件渲染函数，提供后将自动使用自定义 Footer |

#### 样式配置

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `headerType` | `'classics' \| 'material' \| string` | `'classics'` | Header 类型（classics: 经典样式，material: Material Design 样式） |
| `headerHeight` | `number` | `60` | Header 标准高度（dp/pt） |
| `footerHeight` | `number` | `60` | Footer 标准高度（dp/pt） |
| `headerInsetStart` | `number` | `0` | Header 起始位置偏移量（Android 专属） |
| `footerInsetStart` | `number` | `0` | Footer 起始位置偏移量（Android 专属） |

#### 拖拽配置

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `dragRate` | `number` | `1.0` | Header 拖动比率（Android 专属） |
| `headerMaxDragRate` | `number` | `100` | Header 最大拖拽距离 / Header 标准高度（Android 专属） |
| `footerMaxDragRate` | `number` | `1.0` | Footer 最大拖拽距离 / Footer 标准高度（Android 专属） |
| `headerTriggerRate` | `number` | `1.0` | 刷新触发比率（Android 专属） |
| `footerTriggerRate` | `number` | `1.0` | 加载更多触发比率（Android 专属） |
| `reboundDuration` | `number` | `300` | 回弹动画时长（毫秒，Android 专属） |

#### 滚动配置

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `enableScrollContentWhenLoaded` | `boolean` | `true` | 是否在加载完成时滚动列表显示新内容（Android 专属） |
| `enableScrollContentWhenRefreshed` | `boolean` | `true` | 是否在刷新完成时滚动列表显示新内容（Android 专属） |
| `enableOverScrollDrag` | `boolean` | `true` | 是否启用越界拖动（仿苹果效果） |
| `enableOverScrollBounce` | `boolean` | `true` | 是否启用越界回弹 |
| `enableNestedScroll` | `boolean` | `true` | 是否启用嵌套滚动（Android 专属） |
| `enableHapticFeedback` | `boolean` | `true` | 是否启用触觉反馈（震动提示） |

#### 动画配置

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `enableHeaderTranslationContent` | `boolean` | `true` | 是否下拉 Header 时向下平移列表（Android 专属） |
| `enableFooterTranslationContent` | `boolean` | `true` | 是否上拉 Footer 时向上平移列表（Android 专属） |
| `enableLoadMoreWhenContentNotFull` | `boolean` | `false` | 是否在列表不满一页时开启上拉加载 |

#### 经典样式配置

##### ClassicRefreshHeaderProps

```tsx
classicRefreshHeaderProps={{
  // 颜色配置
  headerAccentColor: '#007AFF',        // 强调颜色
  headerPrimaryColor: '#FFFFFF',       // 主题颜色（背景色，Android 专属）
  
  // 文字大小配置
  headerTitleTextSize: 16,             // 标题文字大小（sp）
  headerTimeTextSize: 12,              // 时间文字大小（sp）
  headerTimePaddingTop: 0,             // 时间标签上边距（dp，Android 专属）
  
  // 显示配置
  headerShowTime: true,                // 是否显示时间
  headerFinishDuration: 500,           // 刷新完成停留时间（毫秒，Android 专属）
  
  // 图标配置
  headerDrawableSize: 20,              // 同时设置箭头和图片大小（dp，Android 专属）
  headerDrawableArrowSize: 20,         // 箭头大小（dp，Android 专属）
  headerDrawableProgressSize: 20,      // 进度图标大小（dp，Android 专属）
  headerDrawableMarginRight: 10,       // 图标与文字间距（dp，Android 专属）
  headerDrawableArrow: '',             // 自定义箭头图片（Android 专属）
  headerDrawableProgress: '',          // 自定义进度图片（Android 专属）
  
  // 时间配置
  headerTimeFormat: 'M-d HH:mm',       // 时间格式化（Android 专属）
  headerLastUpdateText: '上次更新时间', // 手动设置时间文字（不会自动更新）
  
  // 状态文字
  REFRESH_HEADER_PULLING: '下拉刷新',
  REFRESH_HEADER_RELEASE: '释放刷新',
  REFRESH_HEADER_REFRESHING: '正在刷新...',
  REFRESH_HEADER_LOADING: '正在加载...',      // Android 专属
  REFRESH_HEADER_FINISH: '刷新完成',
  REFRESH_HEADER_FAILED: '刷新失败',          // Android 专属
  REFRESH_HEADER_SECONDARY: '释放进入二楼',    // Android 专属（二楼功能）
  REFRESH_HEADER_UPDATE: '上次更新 M-d HH:mm', // Android 专属
}}
```

##### ClassicLoadMoreFooterProps

```tsx
classicLoadMoreFooterProps={{
  // 颜色配置
  footerAccentColor: '#007AFF',        // 强调颜色
  footerPrimaryColor: '#FFFFFF',       // 主题颜色（背景色，Android 专属）
  
  // 文字大小配置
  footerTitleTextSize: 14,             // 标题文字大小（sp）
  
  // 图标配置
  footerDrawableSize: 20,              // 同时设置箭头和图片大小（dp，Android 专属）
  footerDrawableArrowSize: 20,         // 箭头大小（dp，Android 专属）
  footerDrawableProgressSize: 20,      // 进度图标大小（dp，Android 专属）
  footerDrawableMarginRight: 10,       // 图标与文字间距（dp，Android 专属）
  footerDrawableArrow: '',             // 自定义箭头图片（Android 专属）
  
  // 显示配置
  footerFinishDuration: 1000,          // 加载完成停留时间（毫秒，默认 1000，Android 专属）
  
  // 状态文字
  REFRESH_FOOTER_PULLING: '上拉加载更多',
  REFRESH_FOOTER_RELEASE: '释放加载',        // Android 专属
  REFRESH_FOOTER_LOADING: '正在加载...',
  REFRESH_FOOTER_REFRESHING: '正在加载...',  // Android 专属
  REFRESH_FOOTER_FINISH: '加载完成',         // Android 专属
  REFRESH_FOOTER_FAILED: '加载失败',
  REFRESH_FOOTER_NOTHING: '没有更多数据',
}}
```

#### 事件回调

| 事件 | 参数 | 说明 |
|------|------|------|
| `onRefresh` | `() => void` | 下拉刷新回调 |
| `onLoadMore` | `() => void` | 上拉加载回调 |
| `onStateChanged` | `(event: {state: RefreshState}) => void` | 状态改变回调 |
| `onHeaderMoving` | `(event: HeaderMovingEvent) => void` | Header 移动回调 |
| `onFooterMoving` | `(event: FooterMovingEvent) => void` | Footer 移动回调 |

##### HeaderMovingEvent

```typescript
interface HeaderMovingEvent {
  isDragging: boolean;   // 是否正在拖拽
  percent: number;       // 拖拽进度（0-N，超过1表示超过触发高度）
  offset: number;        // 当前偏移量（dp/pt，已转换为逻辑像素）
  headerHeight: number;  // Header 高度（dp/pt，已转换为逻辑像素）
}
```

##### FooterMovingEvent

```typescript
interface FooterMovingEvent {
  isDragging: boolean;   // 是否正在拖拽
  percent: number;       // 拖拽进度（0-N）
  offset: number;        // 当前偏移量（dp/pt）
  footerHeight: number;  // Footer 高度（dp/pt）
}
```

##### RefreshState 枚举

```typescript
enum RefreshState {
  None = 0,              // 无状态
  PullDownToRefresh = 1, // 下拉刷新
  ReleaseToRefresh = 2,  // 释放刷新
  Refreshing = 3,        // 正在刷新
  RefreshFinish = 4,     // 刷新完成
  PullUpToLoad = 5,      // 上拉加载
  ReleaseToLoad = 6,     // 释放加载
  Loading = 7,           // 正在加载
  LoadFinish = 8,        // 加载完成
  NoMoreData = 9,        // 没有更多数据
}
```

### 方法（通过 Module 调用）

所有方法通过 `ExpoSmartrefreshlayoutModule` 调用：

```tsx
import { ExpoSmartrefreshlayoutModule } from 'expo-smartrefreshlayout';

// 完成刷新操作
ExpoSmartrefreshlayoutModule.finishRefresh(success?: boolean, delay?: number);

// 完成加载更多操作
ExpoSmartrefreshlayoutModule.finishLoadMore(success?: boolean, delay?: number, noMoreData?: boolean);

// 自动刷新
ExpoSmartrefreshlayoutModule.autoRefresh(delay?: number);

// 自动加载更多
ExpoSmartrefreshlayoutModule.autoLoadMore(delay?: number);

// 设置是否没有更多数据
ExpoSmartrefreshlayoutModule.setNoMoreData(noMoreData: boolean);
```

> ⚠️ **重要提示：关于 finishRefresh/finishLoadMore**
> 
> 虽然组件内部实现了 **3 秒自动结束机制**（防止卡住），但**强烈建议手动调用这些方法**：
> 
> - ✅ **即时反馈**：数据加载完立即结束刷新动画，无需等待 3 秒
> - ✅ **精确控制**：可以设置延迟时间，提供更好的视觉反馈
> - ✅ **状态展示**：可以区分成功/失败状态，提升用户体验
> - ✅ **性能优化**：避免不必要的等待时间
> 
> **不手动调用的后果：**
> - 无论成功失败，都会在 3 秒后自动结束
> - 无法向用户展示刷新失败的状态
> - 快速刷新时仍需等待完整的 3 秒

#### finishRefresh

完成刷新操作。

```tsx
import { ExpoSmartrefreshlayoutModule } from 'expo-smartrefreshlayout';

// 刷新成功
ExpoSmartrefreshlayoutModule.finishRefresh(true, 300);

// 刷新失败
ExpoSmartrefreshlayoutModule.finishRefresh(false, 300);
```

**参数：**
- `success` (boolean, 可选): 是否刷新成功，默认 `true`
- `delay` (number, 可选): 延迟时间（毫秒），默认 `0`

#### finishLoadMore

完成加载更多操作。

```tsx
import { ExpoSmartrefreshlayoutModule } from 'expo-smartrefreshlayout';

// 加载成功
ExpoSmartrefreshlayoutModule.finishLoadMore(true, 300);

// 加载失败
ExpoSmartrefreshlayoutModule.finishLoadMore(false, 300);

// 没有更多数据
ExpoSmartrefreshlayoutModule.finishLoadMore(true, 0, true);
```

**参数：**
- `success` (boolean, 可选): 是否加载成功，默认 `true`
- `delay` (number, 可选): 延迟时间（毫秒），默认 `0`
- `noMoreData` (boolean, 可选): 是否没有更多数据，默认 `false`

#### autoRefresh

触发自动刷新。

```tsx
import { ExpoSmartrefreshlayoutModule } from 'expo-smartrefreshlayout';

// 立即刷新
ExpoSmartrefreshlayoutModule.autoRefresh();

// 延迟 500ms 后刷新
ExpoSmartrefreshlayoutModule.autoRefresh(500);
```

**参数：**
- `delay` (number, 可选): 延迟时间（毫秒），默认 `0`

#### autoLoadMore

触发自动加载更多。

```tsx
import { ExpoSmartrefreshlayoutModule } from 'expo-smartrefreshlayout';

ExpoSmartrefreshlayoutModule.autoLoadMore();
```

**参数：**
- `delay` (number, 可选): 延迟时间（毫秒），默认 `0`

#### setNoMoreData

设置没有更多数据状态。

```tsx
import { ExpoSmartrefreshlayoutModule } from 'expo-smartrefreshlayout';

// 设置没有更多数据
ExpoSmartrefreshlayoutModule.setNoMoreData(true);

// 重置状态
ExpoSmartrefreshlayoutModule.setNoMoreData(false);
```

**参数：**
- `noMoreData` (boolean): 是否没有更多数据

## 🎨 高级用法

### 加载更多功能说明

#### 为什么默认关闭 enableLoadMore？

在 React Native 中，`FlatList` 组件自带 `onEndReached` 属性用于处理加载更多场景，这是最常用且轻量的方案。为了避免功能冲突和给开发者更好的灵活性，`enableLoadMore` 默认设置为 `false`。

#### 使用场景选择

**场景 1：只需要下拉刷新（推荐，最常见）**

使用 FlatList 自带的 `onEndReached` 处理加载更多：

```tsx
import { ExpoSmartrefreshlayoutView, ExpoSmartrefreshlayoutModule } from 'expo-smartrefreshlayout';
import { FlatList } from 'react-native';

function App() {
  const [data, setData] = useState([1, 2, 3, 4, 5]);

  const handleRefresh = async () => {
    await fetchData();
    ExpoSmartrefreshlayoutModule.finishRefresh(true, 300);
  };

  const handleEndReached = () => {
    // 使用 FlatList 自带的加载更多
    loadMoreData();
  };

  return (
    <ExpoSmartrefreshlayoutView onRefresh={handleRefresh}>
      <FlatList
        data={data}
        onEndReached={handleEndReached}
        onEndReachedThreshold={0.1}
        renderItem={({ item }) => <Item data={item} />}
      />
    </ExpoSmartrefreshlayoutView>
  );
}
```

**场景 2：需要统一的刷新和加载更多 UI**

显式启用 `enableLoadMore`，使用组件提供的加载更多功能：

```tsx
import { ExpoSmartrefreshlayoutView, ExpoSmartrefreshlayoutModule } from 'expo-smartrefreshlayout';
import { FlatList } from 'react-native';

function App() {
  const [data, setData] = useState([1, 2, 3, 4, 5]);

  const handleRefresh = async () => {
    await fetchData();
    ExpoSmartrefreshlayoutModule.finishRefresh(true, 300);
  };

  const handleLoadMore = async () => {
    const newData = await loadMoreData();
    
    if (newData.length === 0) {
      // 没有更多数据
      ExpoSmartrefreshlayoutModule.finishLoadMore(true, 0, true);
    } else {
      setData([...data, ...newData]);
      ExpoSmartrefreshlayoutModule.finishLoadMore(true, 300);
    }
  };

  return (
    <ExpoSmartrefreshlayoutView
      enableLoadMore={true}  // 显式启用
      onRefresh={handleRefresh}
      onLoadMore={handleLoadMore}
      classicLoadMoreFooterProps={{
        footerAccentColor: '#007AFF',
        REFRESH_FOOTER_PULLING: '上拉加载更多',
        REFRESH_FOOTER_LOADING: '正在加载...',
        REFRESH_FOOTER_NOTHING: '没有更多了',
      }}
    >
      <FlatList
        data={data}
        renderItem={({ item }) => <Item data={item} />}
      />
    </ExpoSmartrefreshlayoutView>
  );
}
```

#### 优缺点对比

| 方案 | 优点 | 缺点 |
|------|------|------|
| **FlatList onEndReached** | • 轻量级，性能好<br>• RN 原生支持<br>• 开发者熟悉 | • 无加载动画<br>• 需要自行实现 loading 状态 |
| **组件 enableLoadMore** | • 统一的 UI 风格<br>• 内置加载动画<br>• 丰富的自定义选项 | • 略微增加复杂度<br>• 需要显式启用 |

#### 推荐做法

- ✅ **大多数情况**：使用 FlatList 的 `onEndReached`，简单高效
- ✅ **需要统一 UI**：启用 `enableLoadMore={true}`，获得一致的用户体验
- ✅ **避免同时使用**：不要同时使用 `onEndReached` 和 `enableLoadMore`，会导致重复触发

### 自定义 Header

你可以完全自定义 Header 的外观和动画效果，通过 `renderHeader` 属性提供自定义组件。

#### 基础用法

```tsx
import { ExpoSmartrefreshlayoutView, ExpoSmartrefreshlayoutModule } from 'expo-smartrefreshlayout';
import { View, Text, FlatList } from 'react-native';
import { useState } from 'react';

function CustomHeaderExample() {
  const [data, setData] = useState([1, 2, 3, 4, 5]);
  const [offset, setOffset] = useState(0);
  const [percent, setPercent] = useState(0);
  
  const handleRefresh = async () => {
    // 执行刷新逻辑
    await fetchData();
    ExpoSmartrefreshlayoutModule.finishRefresh(true, 300);
  };
  
  return (
    <ExpoSmartrefreshlayoutView
      headerHeight={80}  // 设置 Header 高度
      renderHeader={() => (
        // 自定义 Header 组件
        <View style={{ 
          height: 80, 
          justifyContent: 'center', 
          alignItems: 'center',
          backgroundColor: '#f0f0f0'
        }}>
          <Text>下拉距离: {offset}dp</Text>
          <Text>下拉进度: {(percent * 100).toFixed(0)}%</Text>
          <Text>{percent >= 1 ? '释放刷新' : '继续下拉'}</Text>
        </View>
      )}
      onHeaderMoving={(event) => {
        setOffset(event.offset);
        setPercent(event.percent);
      }}
      onRefresh={handleRefresh}
    >
      {/* 内容列表 */}
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

#### 带动画的自定义 Header

```tsx
import { Animated, ActivityIndicator } from 'react-native';
import { useRef } from 'react';
import { RefreshState } from 'expo-smartrefreshlayout';

function AnimatedCustomHeader() {
  const [data, setData] = useState([1, 2, 3, 4, 5]);
  const [refreshState, setRefreshState] = useState(RefreshState.None);
  const rotateAnim = useRef(new Animated.Value(0)).current;
  const scaleAnim = useRef(new Animated.Value(0)).current;

  const handleStateChanged = (state: RefreshState) => {
    console.log('刷新状态改变:', state);
    setRefreshState(state);
  };

  const handleHeaderMoving = (event) => {
    // 根据下拉进度更新动画
    const { percent } = event;
    
    // 旋转动画：0-360度
    rotateAnim.setValue(percent * 360);
    
    // 缩放动画：0-1
    scaleAnim.setValue(Math.min(percent, 1));
  };
  
  const handleRefresh = async () => {
    // 执行刷新逻辑
    await new Promise(resolve => setTimeout(resolve, 2000));
    setData([...Array(10)].map((_, i) => i + 1));
    
    ExpoSmartrefreshlayoutModule.finishRefresh(true, 300);
  };
  
  const rotate = rotateAnim.interpolate({
    inputRange: [0, 360],
    outputRange: ['0deg', '360deg'],
  });
  
  // 判断是否正在刷新
  const isRefreshing = refreshState === RefreshState.Refreshing;
  
  // 根据状态显示不同文字
  const getStateText = () => {
    switch (refreshState) {
      case RefreshState.PullDownToRefresh:
        return '下拉刷新';
      case RefreshState.ReleaseToRefresh:
        return '释放刷新';
      case RefreshState.Refreshing:
        return '正在刷新...';
      case RefreshState.RefreshFinish:
        return '刷新完成';
      default:
        return '下拉刷新';
    }
  };
  
  return (
    <ExpoSmartrefreshlayoutView
      renderHeader={() => (
        // 自定义动画 Header
        <View style={{ 
          height: 80, 
          justifyContent: 'center', 
          alignItems: 'center',
          backgroundColor: '#fff'
        }}>
          {isRefreshing ? (
            <ActivityIndicator size="large" color="#007AFF" />
          ) : (
            <Animated.View
              style={{
                transform: [
                  { rotate },
                  { scale: scaleAnim }
                ]
              }}
            >
              <Text style={{ fontSize: 40 }}>↓</Text>
            </Animated.View>
          )}
          <Text style={{ marginTop: 8, color: '#666' }}>
            {getStateText()}
          </Text>
        </View>
      )}
      onStateChanged={handleStateChanged}
      onHeaderMoving={handleHeaderMoving}
      onRefresh={handleRefresh}
    >
      {/* 内容列表 */}
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

#### 使用 LottieView 实现复杂动画

如果你需要更复杂的动画效果，可以结合 [lottie-react-native](https://github.com/lottie-react-native/lottie-react-native) 来实现：

```bash
# 安装 lottie-react-native
npm install lottie-react-native
# 或
yarn add lottie-react-native
```

**实现下拉进度控制的 Lottie 动画：**

```tsx
import { RefreshState, ExpoSmartrefreshlayoutView, onHeaderMoveProps } from 'expo-smartrefreshlayout';
import ExpoSmartrefreshlayoutModule from 'expo-smartrefreshlayout/ExpoSmartrefreshlayoutModule';
import { useState, useRef } from 'react';
import { FlatList, Text, View } from 'react-native';
import LottieView from 'lottie-react-native';

export default function LottieCustomHeader() {
  const lottieRef = useRef<LottieView>(null);
  const [data, setData] = useState([1, 2, 3, 4, 5]);
  const [refreshState, setRefreshState] = useState(RefreshState.None);
  const [animationProgress, setAnimationProgress] = useState(0);

  const handleRefresh = async () => {
    // 执行刷新逻辑
    await new Promise(resolve => setTimeout(resolve, 2000));
    setData([...Array(10)].map((_, i) => i + 1));
    
    ExpoSmartrefreshlayoutModule.finishRefresh(true, 300);
  };
 
  const handleHeaderMoving = (event: onHeaderMoveProps) => {
    const { percent, isDragging } = event;
    
    // 根据下拉百分比控制动画进度
    // percent 范围是 0-1+，当达到触发刷新的阈值时会超过1
    // 我们将其限制在 0-1 之间来控制动画
    const progress = Math.min(percent, 1);
    
    if (isDragging) {
      // 在拖动时更新动画进度
      setAnimationProgress(progress);
    }
  };

  const handleStateChanged = (state: RefreshState) => {
    setRefreshState(state);
    
    // 当开始刷新时，播放完整动画
    if (state === RefreshState.Refreshing) {
      lottieRef.current?.play();
    }
  };

  return (
    <View style={{ flex: 1 }}>
      <ExpoSmartrefreshlayoutView
        style={{ flex: 1, backgroundColor: '#f5f5f5' }}
        headerHeight={80}
        renderHeader={() => (
          // 自定义 Lottie 动画 Header
          <View style={{ 
            height: 80, 
            justifyContent: 'center', 
            alignItems: 'center',
            backgroundColor: '#fff'
          }}>
            <LottieView
              ref={lottieRef}
              source={require('./assets/refresh-animation.json')}
              style={{ width: 100, height: 100 }}
              loop={refreshState === RefreshState.Refreshing}
              autoPlay={refreshState === RefreshState.Refreshing}
              progress={refreshState === RefreshState.Refreshing ? undefined : animationProgress}
            />
          </View>
        )}
        onHeaderMoving={handleHeaderMoving}
        onStateChanged={handleStateChanged}
        onRefresh={handleRefresh}
      >
        {/* 内容列表 */}
        <FlatList
          data={data}
          style={{ flex: 1 }}
          renderItem={({ item }) => (
            <View style={{ padding: 20, borderBottomWidth: 1 }}>
              <Text>Item {item}</Text>
            </View>
          )}
          keyExtractor={(item) => item.toString()}
        />
      </ExpoSmartrefreshlayoutView>
    </View>
  );
}
```

**关键点说明：**

1. **动画进度控制**：
   - 使用 `useState` 创建 `animationProgress` 状态来存储动画进度
   - 在 `onHeaderMoving` 回调中根据 `percent`（下拉百分比）更新进度
   - 将进度限制在 0-1 之间：`Math.min(percent, 1)`

2. **LottieView 配置**：
   - `ref={lottieRef}`：用于在刷新时调用 `play()` 方法
   - `progress={animationProgress}`：下拉时根据进度显示对应动画帧
   - `autoPlay={refreshState === RefreshState.Refreshing}`：刷新时自动播放
   - `loop={refreshState === RefreshState.Refreshing}`：刷新时循环播放
   - 刷新时将 `progress` 设为 `undefined`，让动画自动播放

3. **状态区分**：
   - **下拉时**：通过 `progress` 属性控制动画帧，跟随下拉距离
   - **刷新时**：`progress` 为 `undefined`，启用 `autoPlay` 和 `loop` 自动循环播放
   - **完成后**：动画停止在最后一帧

4. **动画文件**：
   - 从 [LottieFiles](https://lottiefiles.com/) 下载 JSON 格式的动画文件
   - 放在项目的 `assets` 目录下
   - 使用 `require()` 引入

**效果：**
- ✅ 下拉过程中，Lottie 动画会随着下拉距离逐帧变化
- ✅ 释放刷新后，动画自动循环播放
- ✅ 刷新完成后，动画停止
- ✅ 提供更流畅、更精美的用户体验

#### 重要说明

1. **renderHeader 属性**：通过 `renderHeader` 属性提供自定义 Header 组件，系统会自动识别并使用它

2. **Header 高度**：建议设置 `headerHeight` 属性来指定 Header 的标准高度，这会影响触发刷新的时机

3. **状态管理**：使用 `onStateChanged` 回调监听刷新状态变化，根据 `RefreshState` 枚举值控制 UI 显示
   - `RefreshState.PullDownToRefresh`: 下拉中
   - `RefreshState.ReleaseToRefresh`: 可以释放刷新
   - `RefreshState.Refreshing`: 正在刷新
   - `RefreshState.RefreshFinish`: 刷新完成

4. **事件监听**：通过 `onHeaderMoving` 实时获取下拉状态（offset、percent 等），用于更新自定义 Header 的动画效果

5. **刷新控制**：在 `onRefresh` 中处理刷新逻辑，完成后通过 `ExpoSmartrefreshlayoutModule.finishRefresh()` 结束刷新

6. **跨平台兼容**：自定义 Header 在 Android 和 iOS 上都完全支持

### 监听状态变化

```tsx
function StateExample() {
  const handleStateChanged = (event: { state: RefreshState }) => {
    switch (event.state) {
      case RefreshState.PullDownToRefresh:
        console.log('下拉刷新');
        break;
      case RefreshState.ReleaseToRefresh:
        console.log('释放刷新');
        break;
      case RefreshState.Refreshing:
        console.log('正在刷新');
        break;
      case RefreshState.RefreshFinish:
        console.log('刷新完成');
        break;
    }
  };
  
  return (
    <ExpoSmartrefreshlayoutView
      onStateChanged={handleStateChanged}
    >
      {/* 内容 */}
    </ExpoSmartrefreshlayoutView>
  );
}
```

### Material Design 样式

```tsx
<ExpoSmartrefreshlayoutView
  headerType="material"
  classicRefreshHeaderProps={{
    headerAccentColor: '#FF5722',
  }}
>
  {/* 内容 */}
</ExpoSmartrefreshlayoutView>
```

### 触觉反馈（震动提示）

组件默认启用触觉反馈功能，当你下拉或上拉到可以释放刷新的临界点时，手机会震动提示用户：

```tsx
<ExpoSmartrefreshlayoutView
  enableHapticFeedback={true}  // 默认为 true，可以省略
  onRefresh={handleRefresh}
  onLoadMore={handleLoadMore}
>
  {/* 内容 */}
</ExpoSmartrefreshlayoutView>
```

**如何工作：**
- ✅ **下拉刷新**：当 `percent >= 1.0`（即下拉距离超过触发阈值）时触发震动
- ✅ **上拉加载**：当 `percent >= 1.0`（即上拉距离超过触发阈值）时触发震动
- ✅ **智能防抖**：同一次拖拽只会触发一次震动，避免连续震动
- ✅ **跨平台支持**：Android 和 iOS 均完美支持

**禁用触觉反馈：**

```tsx
<ExpoSmartrefreshlayoutView
  enableHapticFeedback={false}  // 关闭震动反馈
  onRefresh={handleRefresh}
>
  {/* 内容 */}
</ExpoSmartrefreshlayoutView>
```

**实现细节：**
- **Android**：使用 `HapticFeedbackConstants.CONTEXT_CLICK` 提供轻微震动
- **iOS**：使用 `UIImpactFeedbackGenerator(style: .light)` 提供轻触感反馈
- **性能优化**：震动仅在拖拽过程中触发一次，释放后重置状态


### 完整示例

```tsx
import React, { useState } from 'react';
import { View, Text, FlatList, StyleSheet } from 'react-native';
import { 
  ExpoSmartrefreshlayoutView, 
  ExpoSmartrefreshlayoutModule,
  RefreshState 
} from 'expo-smartrefreshlayout';

export default function CompleteExample() {
  const [data, setData] = useState([1, 2, 3, 4, 5, 6, 7, 8, 9, 10]);
  const [refreshState, setRefreshState] = useState(RefreshState.None);

  const handleStateChanged = (state: RefreshState) => {
    console.log('状态改变:', state);
    setRefreshState(state);
  };

  const handleRefresh = async () => {
    try {
      // 模拟网络请求
      await new Promise(resolve => setTimeout(resolve, 2000));
      
      // 更新数据
      const newData = Array.from({ length: 10 }, (_, i) => i + 1);
      setData(newData);
      
      // 完成刷新
      ExpoSmartrefreshlayoutModule.finishRefresh(true, 300);
    } catch (error) {
      ExpoSmartrefreshlayoutModule.finishRefresh(false, 300);
    }
  };

  const handleLoadMore = async () => {
    try {
      // 模拟加载更多
      await new Promise(resolve => setTimeout(resolve, 2000));
      
      // 模拟没有更多数据的情况
      if (data.length >= 50) {
        ExpoSmartrefreshlayoutModule.finishLoadMore(true, 0, true);
        return;
      }
      
      // 添加更多数据
      const moreData = Array.from(
        { length: 10 }, 
        (_, i) => data.length + i + 1
      );
      setData([...data, ...moreData]);
      
      // 完成加载
      ExpoSmartrefreshlayoutModule.finishLoadMore(true, 300);
    } catch (error) {
      ExpoSmartrefreshlayoutModule.finishLoadMore(false, 300);
    }
  };

  return (
    <View style={styles.container}>
      <ExpoSmartrefreshlayoutView
        style={styles.refresh}
        headerType="classics"
        enableRefresh={true}
        enableLoadMore={true}
        enableAutoLoadMore={false}
        onRefresh={handleRefresh}
        onLoadMore={handleLoadMore}
        onStateChanged={handleStateChanged}
        classicRefreshHeaderProps={{
          headerAccentColor: '#007AFF',
          headerPrimaryColor: '#FFFFFF',
          REFRESH_HEADER_PULLING: '下拉可以刷新',
          REFRESH_HEADER_RELEASE: '释放立即刷新',
          REFRESH_HEADER_REFRESHING: '正在刷新数据...',
          REFRESH_HEADER_FINISH: '刷新完成',
        }}
        classicLoadMoreFooterProps={{
          footerAccentColor: '#007AFF',
          footerPrimaryColor: '#FFFFFF',
          REFRESH_FOOTER_PULLING: '上拉加载更多',
          REFRESH_FOOTER_RELEASE: '释放立即加载',
          REFRESH_FOOTER_LOADING: '正在加载...',
          REFRESH_FOOTER_FINISH: '加载完成',
          REFRESH_FOOTER_NOTHING: '已经到底了',
        }}
      >
        <FlatList
          data={data}
          renderItem={({ item }) => (
            <View style={styles.item}>
              <Text style={styles.itemText}>Item {item}</Text>
            </View>
          )}
          keyExtractor={(item) => item.toString()}
        />
      </ExpoSmartrefreshlayoutView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  refresh: {
    flex: 1,
  },
  item: {
    padding: 20,
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: '#e0e0e0',
  },
  itemText: {
    fontSize: 16,
    color: '#333',
  },
});
```


## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📄 许可证

MIT

## 🔗 相关链接

- [SmartRefreshLayout (Android)](https://github.com/scwang90/SmartRefreshLayout)
- [MJRefresh (iOS)](https://github.com/CoderMJLee/MJRefresh)
