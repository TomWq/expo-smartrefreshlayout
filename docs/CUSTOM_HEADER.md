# 自定义 Header 文档

本文档介绍如何使用和自定义下拉刷新 Header。

## 目录

- [快速开始](#快速开始)
- [使用 DefaultRefreshHeader](#使用-defaultrefreshheader)
- [完全自定义 Header](#完全自定义-header)
- [RenderHeaderParams API](#renderheaderparams-api)
- [RefreshState 状态说明](#refreshstate-状态说明)
- [高级示例](#高级示例)

---

## 快速开始

`renderHeader` 是一个回调函数，它接收包含刷新状态的参数，让你可以根据状态自定义 Header UI。

```tsx
import { ExpoSmartrefreshlayoutView } from 'expo-smartrefreshlayout';

<ExpoSmartrefreshlayoutView
  onRefresh={handleRefresh}
  renderHeader={(params) => (
    <View>
      <Text>下拉进度: {params.moving.percent}</Text>
    </View>
  )}
>
  <FlatList data={data} ... />
</ExpoSmartrefreshlayoutView>
```

---

## 使用 DefaultRefreshHeader

我们提供了一个开箱即用的默认 Header 组件。

### 基础用法

```tsx
import { ExpoSmartrefreshlayoutView, DefaultRefreshHeader } from 'expo-smartrefreshlayout';

function MyScreen() {
  const handleRefresh = () => {
    // 刷新逻辑
  };

  return (
    <ExpoSmartrefreshlayoutView
      onRefresh={handleRefresh}
      renderHeader={(params) => (
        <DefaultRefreshHeader 
          params={params} 
          textColor="#333"
        />
      )}
    >
      <FlatList data={data} renderItem={...} />
    </ExpoSmartrefreshlayoutView>
  );
}
```

### DefaultRefreshHeader Props

| 属性 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `params` | `RenderHeaderParams` | ✅ | renderHeader 回调函数的参数 |
| `textColor` | `string` | ❌ | 文字颜色，默认 `#666666` |
| `style` | `ViewStyle` | ❌ | 自定义样式 |

### 功能特性

- ✅ 自动显示下拉/释放/刷新状态文字
- ✅ 箭头和加载指示器自动切换
- ✅ 显示上次更新时间
- ✅ 支持自定义文字颜色

---

## 完全自定义 Header

你可以完全自定义 Header 的 UI 和交互。

### 示例 1：简单的文字 Header

```tsx
<ExpoSmartrefreshlayoutView
  onRefresh={handleRefresh}
  renderHeader={({ state, moving, refreshing }) => (
    <View style={{ padding: 20, alignItems: 'center' }}>
      <Text>
        {refreshing ? '正在刷新...' : `下拉进度: ${(moving.percent * 100).toFixed(0)}%`}
      </Text>
    </View>
  )}
>
  <FlatList ... />
</ExpoSmartrefreshlayoutView>
```

### 示例 2：带动画的 Header

```tsx
import Animated, { useAnimatedStyle, interpolate } from 'react-native-reanimated';

function AnimatedHeader({ params }: { params: RenderHeaderParams }) {
  const { moving, refreshing } = params;
  
  // 根据下拉进度旋转箭头
  const animatedStyle = useAnimatedStyle(() => {
    const rotation = interpolate(
      moving.percent,
      [0, 1],
      [0, 180]
    );
    return {
      transform: [{ rotate: `${rotation}deg` }]
    };
  });

  return (
    <View style={{ padding: 20, alignItems: 'center' }}>
      {refreshing ? (
        <ActivityIndicator />
      ) : (
        <Animated.Text style={[{ fontSize: 24 }, animatedStyle]}>
          ↓
        </Animated.Text>
      )}
    </View>
  );
}

// 使用
<ExpoSmartrefreshlayoutView
  onRefresh={handleRefresh}
  renderHeader={(params) => <AnimatedHeader params={params} />}
>
  <FlatList ... />
</ExpoSmartrefreshlayoutView>
```

### 示例 3：根据状态显示不同内容

```tsx
import { RefreshState } from 'expo-smartrefreshlayout';

<ExpoSmartrefreshlayoutView
  onRefresh={handleRefresh}
  renderHeader={({ state, moving }) => {
    let text = '';
    let icon = '↓';
    
    switch (state) {
      case RefreshState.PullDownToRefresh:
        text = '下拉可以刷新';
        icon = '↓';
        break;
      case RefreshState.ReleaseToRefresh:
        text = '释放立即刷新';
        icon = '↑';
        break;
      case RefreshState.Refreshing:
        text = '正在刷新...';
        icon = '⟳';
        break;
      case RefreshState.RefreshFinish:
        text = '刷新完成';
        icon = '✓';
        break;
      default:
        text = '下拉刷新';
    }
    
    return (
      <View style={{ padding: 20, flexDirection: 'row', alignItems: 'center' }}>
        <Text style={{ fontSize: 24, marginRight: 10 }}>{icon}</Text>
        <View>
          <Text>{text}</Text>
          <Text style={{ fontSize: 11, color: '#999' }}>
            进度: {(moving.percent * 100).toFixed(0)}%
          </Text>
        </View>
      </View>
    );
  }}
>
  <FlatList ... />
</ExpoSmartrefreshlayoutView>
```

---

## RenderHeaderParams API

`renderHeader` 回调函数接收一个参数对象：

```typescript
interface RenderHeaderParams {
  /** 当前刷新状态 */
  state: RefreshState;
  
  /** Header 移动参数（实时更新） */
  moving: {
    /** 是否正在拖动 */
    isDragging: boolean;
    /** 下拉百分比（0-N，超过 1 表示超过触发阈值） */
    percent: number;
    /** 下拉偏移量（像素） */
    offset: number;
    /** Header 高度（像素） */
    headerHeight: number;
  };
  
  /** 是否正在刷新 */
  refreshing: boolean;
}
```

### 参数详解

#### `state: RefreshState`

当前的刷新状态，是一个枚举值：

- `RefreshState.None` (0): 无状态
- `RefreshState.PullDownToRefresh` (1): 下拉中
- `RefreshState.ReleaseToRefresh` (2): 可以释放刷新
- `RefreshState.Refreshing` (3): 正在刷新
- `RefreshState.RefreshFinish` (4): 刷新完成

#### `moving.percent: number`

下拉百分比，范围 `0 - N`：
- `0`: 未下拉
- `0.5`: 下拉了一半
- `1.0`: 达到触发阈值（通常是 Header 高度）
- `> 1.0`: 超过触发阈值

**用途**：
- 计算下拉进度
- 控制动画
- 判断是否可以触发刷新

```tsx
renderHeader={({ moving }) => (
  <Text>进度: {(moving.percent * 100).toFixed(0)}%</Text>
)}
```

#### `moving.offset: number`

下拉的实际偏移量（像素值）。

**用途**：
- 精确控制动画位移
- 自定义阻尼效果

```tsx
renderHeader={({ moving }) => (
  <View style={{ transform: [{ translateY: moving.offset * 0.5 }] }}>
    <Text>Header</Text>
  </View>
)}
```

#### `moving.isDragging: boolean`

用户是否正在拖动列表。

**用途**：
- 区分拖动中和回弹中
- 触发触觉反馈

```tsx
renderHeader={({ moving }) => {
  if (moving.isDragging && moving.percent >= 1.0) {
    // 触发震动反馈
  }
  return <Text>...</Text>;
}}
```

#### `refreshing: boolean`

是否正在刷新，等价于 `state === RefreshState.Refreshing`。

**用途**：
- 快速判断刷新状态
- 显示/隐藏加载指示器

```tsx
renderHeader={({ refreshing }) => (
  refreshing ? <ActivityIndicator /> : <Text>下拉刷新</Text>
)}
```

---

## RefreshState 状态说明

### 状态流转图

```
None (初始状态)
  ↓
PullDownToRefresh (开始下拉)
  ↓
ReleaseToRefresh (超过阈值，可以释放)
  ↓
Refreshing (松手，开始刷新)
  ↓
RefreshFinish (刷新完成)
  ↓
None (回到初始状态)
```

### 状态枚举值

```typescript
enum RefreshState {
  None = 0,                 // 无状态
  PullDownToRefresh = 1,    // 下拉刷新
  ReleaseToRefresh = 2,     // 释放立即刷新
  Refreshing = 3,           // 正在刷新
  RefreshFinish = 4,        // 刷新完成
  PullUpToLoad = 5,         // 上拉加载
  ReleaseToLoad = 6,        // 释放立即加载
  Loading = 7,              // 正在加载
  LoadFinish = 8,           // 加载完成
  NoMoreData = 9,           // 没有更多数据
}
```

---

## 高级示例

### 示例 1：Lottie 动画 Header

```tsx
import LottieView from 'lottie-react-native';

function LottieHeader({ params }: { params: RenderHeaderParams }) {
  const { moving, refreshing } = params;
  const animationRef = useRef<LottieView>(null);

  useEffect(() => {
    if (refreshing) {
      animationRef.current?.play();
    } else {
      // 根据下拉进度控制动画帧
      const frame = moving.percent * 60; // 假设动画有 60 帧
      animationRef.current?.play(frame, frame);
    }
  }, [moving.percent, refreshing]);

  return (
    <View style={{ height: 80, justifyContent: 'center', alignItems: 'center' }}>
      <LottieView
        ref={animationRef}
        source={require('./loading-animation.json')}
        style={{ width: 60, height: 60 }}
        loop={refreshing}
      />
    </View>
  );
}
```

### 示例 2：触觉反馈

```tsx
import * as Haptics from 'expo-haptics';

function HapticHeader({ params }: { params: RenderHeaderParams }) {
  const [hasTriggered, setHasTriggered] = useState(false);
  const { moving, state } = params;

  useEffect(() => {
    // 当下拉超过阈值时触发震动
    if (moving.isDragging && moving.percent >= 1.0 && !hasTriggered) {
      Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light);
      setHasTriggered(true);
    } else if (moving.percent < 1.0) {
      setHasTriggered(false);
    }
  }, [moving.percent, moving.isDragging]);

  return (
    <DefaultRefreshHeader params={params} textColor="#333" />
  );
}
```

### 示例 3：多语言支持

```tsx
import { useTranslation } from 'react-i18next';
import { RefreshState } from 'expo-smartrefreshlayout';

function I18nHeader({ params }: { params: RenderHeaderParams }) {
  const { t } = useTranslation();
  const { state, moving, refreshing } = params;

  const getText = () => {
    switch (state) {
      case RefreshState.PullDownToRefresh:
        return t('refresh.pullDown');
      case RefreshState.ReleaseToRefresh:
        return t('refresh.release');
      case RefreshState.Refreshing:
        return t('refresh.refreshing');
      case RefreshState.RefreshFinish:
        return t('refresh.complete');
      default:
        return t('refresh.idle');
    }
  };

  return (
    <View style={{ padding: 20, alignItems: 'center' }}>
      {refreshing ? <ActivityIndicator /> : <Text>↓</Text>}
      <Text>{getText()}</Text>
    </View>
  );
}
```

### 示例 4：渐变背景色

```tsx
import { interpolateColor } from 'react-native-reanimated';

function GradientHeader({ params }: { params: RenderHeaderParams }) {
  const { moving } = params;
  
  // 根据下拉进度改变背景色
  const backgroundColor = interpolateColor(
    moving.percent,
    [0, 0.5, 1],
    ['#ffffff', '#e3f2fd', '#2196f3']
  );

  return (
    <Animated.View style={{ padding: 20, backgroundColor }}>
      <Text style={{ textAlign: 'center' }}>
        {moving.percent >= 1 ? '释放刷新' : '下拉刷新'}
      </Text>
    </Animated.View>
  );
}
```

---

## 注意事项

1. **性能优化**：`moving` 参数会实时更新，避免在 `renderHeader` 中执行耗时操作
2. **状态管理**：如果需要在 Header 内部管理状态，使用 `useEffect` 监听 `params` 的变化
3. **高度设置**：自定义 Header 的高度应该合理，建议 60-100px
4. **指针事件**：Header 默认设置了 `pointerEvents="none"`，确保触摸事件能穿透到列表

---

## 完整示例

查看项目中的完整示例：
- [`example/DefaultHeaderExample.tsx`](../example/DefaultHeaderExample.tsx) - 使用 DefaultRefreshHeader
- 更多示例即将添加...

---

## 相关文档

- [API 文档](./API.md)
- [示例文档](./EXAMPLES.md)
- [主 README](../README.md)