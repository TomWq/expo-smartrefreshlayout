# API 文档

## Props

### 基础配置

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `enableRefresh` | `boolean` | `true` | 是否启用下拉刷新功能 |
| `enableLoadMore` | `boolean` | `false` | 是否启用上拉加载功能（默认关闭，避免与 FlatList 的 onEndReached 冲突） |
| `enableAutoLoadMore` | `boolean` | `false` | 是否启用列表惯性滑动到底部时自动加载更多 |
| `enablePureScrollMode` | `boolean` | `false` | 是否启用纯滚动模式（Android 专属） |
| `renderHeader` | `(params: RenderHeaderParams) => React.ReactNode` | - | 自定义 Header 组件渲染函数，接收包含状态的参数对象。详见 [自定义 Header 文档](./CUSTOM_HEADER.md) |
| `renderFooter` | `() => React.ReactElement` | - | 自定义 Footer 组件渲染函数，提供后将自动使用自定义 Footer |

### 样式配置

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `headerType` | `'classics' \| 'material' \| string` | `'classics'` | Header 类型（classics: 经典样式，material: Material Design 样式） |
| `headerHeight` | `number` | `60` | Header 标准高度（dp/pt） |
| `footerHeight` | `number` | `60` | Footer 标准高度（dp/pt） |
| `headerInsetStart` | `number` | `0` | Header 起始位置偏移量（Android 专属） |
| `footerInsetStart` | `number` | `0` | Footer 起始位置偏移量（Android 专属） |

### 拖拽配置

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `dragRate` | `number` | `1.0` | Header 拖动比率（Android 专属） |
| `headerMaxDragRate` | `number` | `100` | Header 最大拖拽距离 / Header 标准高度（Android 专属） |
| `footerMaxDragRate` | `number` | `1.0` | Footer 最大拖拽距离 / Footer 标准高度（Android 专属） |
| `headerTriggerRate` | `number` | `1.0` | 刷新触发比率（Android 专属） |
| `footerTriggerRate` | `number` | `1.0` | 加载更多触发比率（Android 专属） |
| `reboundDuration` | `number` | `300` | 回弹动画时长（毫秒，Android 专属） |

### 滚动配置

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `enableScrollContentWhenLoaded` | `boolean` | `true` | 是否在加载完成时滚动列表显示新内容（Android 专属） |
| `enableScrollContentWhenRefreshed` | `boolean` | `true` | 是否在刷新完成时滚动列表显示新内容（Android 专属） |
| `enableOverScrollDrag` | `boolean` | `true` | 是否启用越界拖动（仿苹果效果） |
| `enableOverScrollBounce` | `boolean` | `true` | 是否启用越界回弹 |
| `enableNestedScroll` | `boolean` | `true` | 是否启用嵌套滚动（Android 专属） |
| `enableHapticFeedback` | `boolean` | `true` | 是否启用触觉反馈（震动提示） |

### 动画配置

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `enableHeaderTranslationContent` | `boolean` | `true` | 是否下拉 Header 时向下平移列表（Android 专属） |
| `enableFooterTranslationContent` | `boolean` | `true` | 是否上拉 Footer 时向上平移列表（Android 专属） |
| `enableLoadMoreWhenContentNotFull` | `boolean` | `false` | 是否在列表不满一页时开启上拉加载 |

### 经典样式配置

#### ClassicRefreshHeaderProps

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

#### ClassicLoadMoreFooterProps

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

### 事件回调

| 事件 | 参数 | 说明 |
|------|------|------|
| `onRefresh` | `() => void` | 下拉刷新回调 |
| `onLoadMore` | `() => void` | 上拉加载回调 |
| `onStateChanged` | `(state: RefreshState) => void` | 状态改变回调 |
| `onHeaderMoving` | `(params: onHeaderMoveProps) => void` | Header 移动回调（Android 专属） |
| `onFooterMoving` | `(params: onHeaderMoveProps) => void` | Footer 移动回调（Android 专属） |

## 类型定义

### RenderHeaderParams

`renderHeader` 回调函数的参数类型：

```typescript
interface RenderHeaderParams {
  /** 当前刷新状态 */
  state: RefreshState;
  
  /** Header 移动参数（实时更新） */
  moving: onHeaderMoveProps;
  
  /** 是否正在刷新 */
  refreshing: boolean;
}
```

详细说明请参考 [自定义 Header 文档](./CUSTOM_HEADER.md)。

### onHeaderMoveProps

```typescript
interface onHeaderMoveProps {
  isDragging: boolean;   // 是否正在拖拽
  percent: number;       // 拖拽进度（0-N，超过1表示超过触发高度）
  offset: number;        // 当前偏移量（dp/pt，已转换为逻辑像素）
  headerHeight: number;  // Header 高度（dp/pt，已转换为逻辑像素）
}
```

### RefreshState 枚举

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

## 方法

所有方法通过 `ExpoSmartrefreshlayoutModule` 调用：

```tsx
import { ExpoSmartrefreshlayoutModule } from 'expo-smartrefreshlayout';
```

### finishRefresh

完成刷新操作。

```tsx
ExpoSmartrefreshlayoutModule.finishRefresh(success?: boolean, delay?: number);
```

**参数：**
- `success` (boolean, 可选): 是否刷新成功，默认 `true`
- `delay` (number, 可选): 延迟时间（毫秒），默认 `0`

**示例：**

```tsx
// 刷新成功
ExpoSmartrefreshlayoutModule.finishRefresh(true, 300);

// 刷新失败
ExpoSmartrefreshlayoutModule.finishRefresh(false, 300);
```

### finishLoadMore

完成加载更多操作。

```tsx
ExpoSmartrefreshlayoutModule.finishLoadMore(success?: boolean, delay?: number, noMoreData?: boolean);
```

**参数：**
- `success` (boolean, 可选): 是否加载成功，默认 `true`
- `delay` (number, 可选): 延迟时间（毫秒），默认 `0`
- `noMoreData` (boolean, 可选): 是否没有更多数据，默认 `false`

**示例：**

```tsx
// 加载成功
ExpoSmartrefreshlayoutModule.finishLoadMore(true, 300);

// 加载失败
ExpoSmartrefreshlayoutModule.finishLoadMore(false, 300);

// 没有更多数据
ExpoSmartrefreshlayoutModule.finishLoadMore(true, 0, true);
```

### autoRefresh

触发自动刷新。

```tsx
ExpoSmartrefreshlayoutModule.autoRefresh(delay?: number);
```

**参数：**
- `delay` (number, 可选): 延迟时间（毫秒），默认 `0`

**示例：**

```tsx
// 立即刷新
ExpoSmartrefreshlayoutModule.autoRefresh();

// 延迟 500ms 后刷新
ExpoSmartrefreshlayoutModule.autoRefresh(500);
```

### autoLoadMore

触发自动加载更多。

```tsx
ExpoSmartrefreshlayoutModule.autoLoadMore(delay?: number);
```

**参数：**
- `delay` (number, 可选): 延迟时间（毫秒），默认 `0`

### setNoMoreData

设置没有更多数据状态。

```tsx
ExpoSmartrefreshlayoutModule.setNoMoreData(noMoreData: boolean);
```

**参数：**
- `noMoreData` (boolean): 是否没有更多数据

**示例：**

```tsx
// 设置没有更多数据
ExpoSmartrefreshlayoutModule.setNoMoreData(true);

// 重置状态
ExpoSmartrefreshlayoutModule.setNoMoreData(false);
```

## 重要提示

> ⚠️ **关于 finishRefresh/finishLoadMore**
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