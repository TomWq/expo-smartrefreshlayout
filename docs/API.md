# API

## SmartRefreshLayout

```tsx
import { SmartRefreshLayout } from 'expo-smartrefreshlayout';
```

组件必须包含且只包含一个 React Native 滚动组件，例如 `FlatList`、`SectionList` 或 `ScrollView`。

### Props

| 属性 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `children` | `ReactElement` | 必填 | 唯一的滚动子组件 |
| `refreshHeader` | `ReactElement` | - | 挂载到原生刷新 Header 内的自定义 React 内容 |
| `refreshHeaderHeight` | `number` | `80` | 自定义 Header 高度，单位为逻辑 dp/pt；有限且必须大于 0 |
| `refreshHeaderSpinnerStyle` | `'scale' \| 'translate' \| 'fixed-behind'` | `'translate'` | 仅作用于 `refreshHeader`，两端均支持 |
| `refreshHeaderTriggerRate` | `number` | `1` | 自定义 Header 触发阈值倍数；有效范围 `(0, 1]` |
| `refreshHeaderMaxDragRate` | `number` | `2` | 自定义 Header 最大拖动倍数；有效范围 `[1, 9]` |
| `refreshHeaderFinishDuration` | `number` | `0` | 原生完成状态停留/完成动画时长，单位毫秒 |
| `refreshEnabled` | `boolean` | 是否提供 `onRefresh` | 是否允许下拉刷新 |
| `loadMoreEnabled` | `boolean` | 是否提供 `onLoadMore` | 是否允许上拉加载 |
| `loadMoreMode` | `'pull' | 'auto'` | `'pull'` | `pull` 需要上拉释放；`auto` 需要先真实向上滚动且内容超过一屏 |
| `autoLoadMoreEnabled` | `boolean` | `false` | 兼容别名，等同于 `loadMoreMode="auto"`；新代码请使用 `loadMoreMode` |
| `refreshing` | `boolean` | 非受控 | 受控刷新状态 |
| `loadingMore` | `boolean` | 非受控 | 受控加载状态 |
| `hasMore` | `boolean` | `true` | `false` 时显示没有更多数据并禁止继续加载 |
| `hapticsEnabled` | `boolean` | `true` | 到达释放阈值时启用触觉反馈 |
| `headerStyle` | `'classic' \| 'material'` | `'classic'` | 刷新头样式 |
| `primaryColor` | `ColorValue` | 平台默认 | Android 专属；当前 Header 与 Classic footer 的主背景色 |
| `indicatorColor` | `ColorValue` | 平台默认 | 指示器颜色 |
| `titleColor` | `ColorValue` | 平台默认 | 状态文字颜色 |
| `classicSpinnerStyle` | `'scale' \| 'translate' \| 'fixed-behind'` | `'translate'` | Android 专属；对应 `ClassicsHeader.setSpinnerStyle` |
| `classicEnableLastTime` | `boolean` | `true` | Android 专属；显示或隐藏 Classic 的最后更新时间 |
| `materialShowBezierWave` | `boolean` | `false` | Android 专属；对应 `MaterialHeader.setShowBezierWave` |
| `materialEnableHeaderTranslationContent` | `boolean` | `false` | Android 专属；Material Header 下拉时内容是否同步偏移 |
| `materialProgressBackgroundColor` | `ColorValue` | Material 默认 | Android 专属；Material 进度圆背景色 |
| `messages` | `Partial<RefreshMessages>` | 英文默认文案 | 覆盖状态文案 |
| `onRefresh` | `(request) => void \| Promise<void>` | - | 下拉刷新回调，参数含 `requestId` 和 `source` |
| `onLoadMore` | `(request) => void \| { hasMore } \| Promise<...>` | - | 加载更多回调，可直接返回下一页是否还有数据 |
| `onRefreshError` | `(error: unknown) => void` | - | `onRefresh` 抛错后的通知 |
| `onLoadMoreError` | `(error: unknown) => void` | - | `onLoadMore` 抛错后的通知 |
| `onStateChange` | `(state: RefreshState) => void` | - | 原生刷新状态变化 |
| `onHeaderMoving` | `(event: HeaderMovingEvent) => void` | - | Header 下拉距离变化；适合驱动自定义 Header 动画 |
| `onHeaderInitialized` | `(event: HeaderLifecycleEvent) => void` | - | 原生 Header 完成尺寸初始化时触发 |
| `onHeaderReleased` | `(event: HeaderLifecycleEvent) => void` | - | 用户释放且原生开始回弹/释放动画时触发 |
| `onHeaderStart` | `(event: HeaderLifecycleEvent) => void` | - | 原生真正进入刷新动画时触发 |
| `onHeaderFinish` | `({ success }: HeaderFinishEvent) => void` | - | 原生 Header 进入完成状态时触发，结果来自原生 `onFinish` |

其余 `ViewProps` 会传给原生容器。

### Android Classic 与 Material 配置

这些 Props 对照官方 `ClassicsStyleActivity` 和 `MaterialStyleActivity`。它们只影响
Android，iOS 会忽略但仍可安全传入。

```tsx
<SmartRefreshLayout
  headerStyle="classic"
  primaryColor="#1677ff"
  indicatorColor="#ffffff"
  titleColor="#ffffff"
  classicSpinnerStyle="fixed-behind"
  classicEnableLastTime
  onRefresh={reload}
>
  <FlatList {...listProps} />
</SmartRefreshLayout>
```

```tsx
<SmartRefreshLayout
  headerStyle="material"
  primaryColor="#52c41a"
  indicatorColor="#ffffff"
  materialShowBezierWave
  materialEnableHeaderTranslationContent={false}
  materialProgressBackgroundColor="#52c41a"
  onRefresh={reload}
>
  <FlatList {...listProps} />
</SmartRefreshLayout>
```

`indicatorColor` 在 Material 模式中映射到官方 `setColorSchemeColors`；在 Classic
模式中用于箭头与加载指示器。`titleColor` 只影响 Classic Header/Footer 文案。

这些配置可以在组件运行期间动态更新。Classic Spinner 样式会强制重建 Header 以确保
`scale`、`translate` 和 `fixed-behind` 真正切换；如果当前正在刷新或分页，变更会等到
布局回到空闲状态后应用。

### Promise 行为

未传 `refreshing` 时，`onRefresh` 返回的 Promise settle 后会自动调用原生 `finishRefresh`。未传 `loadingMore` 时，`onLoadMore` 采用同样规则。

如果回调抛错，组件会以失败状态结束动画，并调用对应的 error handler。组件不会在没有 error handler 时制造未处理的 Promise rejection。

传入 `refreshing` 或 `loadingMore` 后即进入受控模式，调用方必须把对应值恢复为 `false` 才会结束动画。

一次刷新或分页请求从原生手势或实例命令开始，并带有只属于该视图实例的 `requestId`。同一时间只允许一个请求；重复手势、刷新与分页交叉触发，以及过期的延迟结束命令都会被忽略。

`onLoadMore` 可以返回 `{ hasMore: false }`，组件会在同一轮请求完成时锁定 footer，不必等待下一次 React 渲染。未返回该对象时，组件使用 `hasMore` Prop 的最新值。

自动模式不会在首次挂载、短列表或仅仅因为 footer 出现时触发。Android 和 iOS 都要求内容超过一屏，并先检测到用户向上滚动；请求完成后需要下一次向上滚动才会再次解锁自动加载。

### 自定义刷新 Header

`refreshHeader` 会把 React 内容挂载到两端原生刷新 Header 中，而不是作为列表内容的普通兄弟节点。默认高度为 `80`，可用 `refreshHeaderHeight` 覆盖；Android 使用 dp，iOS 使用 pt，事件统一以逻辑像素返回。`refreshHeaderSpinnerStyle` 的 `translate`、`scale`、`fixed-behind` 在两端分别映射到原生等价布局。`refreshHeaderTriggerRate` 的有效范围是 `(0, 1]`，`refreshHeaderMaxDragRate` 的有效范围是 `[1, 9]`；上限避开 Android 内核把 `>= 10` 解释为物理像素高度的特殊语义。`onHeaderMoving.maxDragHeight` 会反映该约束。

`refreshHeaderFinishDuration` 是原生 Header 进入成功/失败完成态后停留的时长（毫秒，iOS 内部换算为秒），由 Header 的原生完成回调驱动 `onHeaderFinish`。它不同于 `finishRefresh({ delay })`：后者只延迟向原生发起完成命令。

```tsx
const [headerProgress, setHeaderProgress] = useState(0);

<SmartRefreshLayout
  refreshHeader={<MyLottieHeader progress={headerProgress} />}
  onHeaderMoving={({ percent }) => {
    setHeaderProgress(Math.min(Math.max(percent, 0), 1));
  }}
  onRefresh={reload}
>
  <FlatList {...listProps} />
</SmartRefreshLayout>
```

自定义 Header 内的交互视图仍由 React 管理；需要让下拉手势优先通过时，可给纯展示内容设置 `pointerEvents="none"`。

### RefreshMessages

```ts
interface RefreshMessages {
  pullDown: string;
  releaseToRefresh: string;
  refreshing: string;
  refreshComplete: string;
  pullUp: string;
  releaseToLoadMore: string;
  loadingMore: string;
  noMoreData: string;
}
```

请求参数和结果类型：

```ts
interface RefreshRequest {
  requestId: number;
  source: 'gesture' | 'programmatic';
}

interface LoadMoreResult {
  hasMore: boolean;
}

interface HeaderMovingEvent {
  percent: number;
  offset: number;
  height: number;
  maxDragHeight: number;
  isDragging: boolean;
}

interface HeaderLifecycleEvent {
  height: number;
  maxDragHeight: number;
}

interface HeaderFinishEvent {
  success: boolean;
}
```

### RefreshState

```ts
type RefreshState =
  | 'idle'
  | 'pulling'
  | 'ready'
  | 'refreshing'
  | 'loading'
  | 'no-more-data';
```

## SmartRefreshLayoutRef

```tsx
import type { SmartRefreshLayoutRef } from 'expo-smartrefreshlayout';
```

### beginRefresh

```ts
beginRefresh(delay?: number): boolean;
```

主动开始刷新。返回 `true` 表示当前实例接受了请求，`false` 表示已有刷新或分页请求。`delay` 为非负毫秒数，非法值会归一化为 `0`。

### finishRefresh

```ts
finishRefresh(options?: {
  success?: boolean;
  delay?: number;
}): void;
```

结束当前刷新。默认 `success: true`、`delay: 0`。

### beginLoadMore

```ts
beginLoadMore(delay?: number): boolean;
```

主动开始加载更多。返回值与 `beginRefresh` 相同；`hasMore=false` 时返回 `false`。

### finishLoadMore

```ts
finishLoadMore(options?: {
  success?: boolean;
  hasMore?: boolean;
  delay?: number;
}): void;
```

结束当前加载。`hasMore: false` 会让原生 footer 进入没有更多数据状态。

### resetNoMoreData

```ts
resetNoMoreData(): void;
```

重置原生 footer 状态。若组件的 `hasMore` 仍为 `false`，下一次渲染会再次应用没有更多数据状态，因此受控场景应同时更新 `hasMore`。

## 兼容导出

v2 暂时保留旧组件名作为别名：

```ts
import { ExpoSmartrefreshlayoutView } from 'expo-smartrefreshlayout';
```

别名使用的仍是 v2 Props。旧的 `ExpoSmartrefreshlayoutModule`、旧 Props 和自定义 Header API 不再存在。

## SmartSecondFloorLayout（仅 Android）

```tsx
import { SmartSecondFloorLayout } from 'expo-smartrefreshlayout';
```

这是 Android-only 组件，底层使用 SmartRefreshLayout 的 `TwoLevelHeader`。`children` 必须是
普通页面的唯一滚动子组件，`secondFloor` 是覆盖式二楼内容。可选的
`secondFloorBackground` 用于在其后放置揭露式背景，并让正式二楼内容在进入后原生淡入。二楼组件不包含 footer，也不支持
`onLoadMore` 或自动加载更多。iOS 没有等价原生能力，渲染该组件会抛出明确错误：请在
平台分支中不要挂载它，或继续使用跨平台的 `SmartRefreshLayout`。

### Props

| 属性 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `children` | `ReactElement` | 必填 | 普通页面的唯一滚动子组件 |
| `secondFloor` | `ReactElement` | 必填 | 二楼的全屏内容；可放 `ScrollView`/`FlatList` |
| `secondFloorBackground` | `ReactElement` | - | `secondFloor` 后方的揭露背景；提供后正式内容会在打开时淡入 |
| `refreshEnabled` | `boolean` | 是否提供 `onRefresh` | 是否允许普通下拉刷新 |
| `refreshing` | `boolean` | 非受控 | 受控普通刷新状态 |
| `hapticsEnabled` | `boolean` | `true` | 到达刷新或二楼释放阈值时触觉反馈 |
| `secondFloorEnabled` | `boolean` | `true` | 是否允许进入二楼 |
| `headerInset` | `number` | `0` | Classic Header 顶部预留的逻辑高度。页面顶部有覆盖式 Toolbar 时传入其高度，使 Header 的可见位置与二楼阈值一起计算 |
| `maxRate` | `number` | `2.5` | 最大拖拽倍率，归一化到 `1.2..5` |
| `floorRate` | `number` | `1.9` | 二楼释放倍率，至少 `1.1`，且低于 `maxRate` |
| `refreshRate` | `number` | `1` | 普通刷新倍率，至少 `0.25`，且低于 `floorRate` |
| `floorDuration` | `number` | `1000` | 进入/停留二楼的动画时长（毫秒），归一化到 `0..10000` |
| `pullToCloseEnabled` | `boolean` | `true` | 是否允许在二楼向下拉关闭 |
| `bottomPullUpToCloseRate` | `number` | `1/6` | 二楼底部关闭拖拽倍率，归一化到 `0.01..0.5` |
| `primaryColor` | `ColorValue` | 平台默认 | Classic Header 背景色 |
| `indicatorColor` | `ColorValue` | 平台默认 | Classic 指示器颜色 |
| `titleColor` | `ColorValue` | 平台默认 | Classic 文案颜色 |
| `classicEnableLastTime` | `boolean` | `true` | 是否显示 Classic 最后更新时间 |
| `messages` | `Partial<SecondFloorMessages>` | 英文默认文案 | 覆盖普通刷新文案 |
| `onRefresh` | `(request) => void \| Promise<void>` | - | 普通下拉刷新回调 |
| `onRefreshError` | `(error: unknown) => void` | - | 刷新失败通知 |
| `onStateChange` | `(state: SecondFloorState) => void` | - | 普通刷新和二楼生命周期状态 |
| `onSecondFloorOpen` | `() => void` | - | 二楼展开动画完成 |
| `onSecondFloorClose` | `() => void` | - | 二楼关闭动画完成 |

`floorRate`、`maxRate` 和 `refreshRate` 会在 JS 与 Android 两端同时归一化。为了保持阈值
顺序，传入互相矛盾的值时，组件会把较低层级压到上限，而不是抛异常。

### SecondFloorMessages

```ts
interface SecondFloorMessages {
  pullDown: string;
  releaseToRefresh: string;
  refreshing: string;
  refreshComplete: string;
}
```

### SecondFloorState

```ts
type SecondFloorState =
  | 'idle'
  | 'pulling'
  | 'ready'
  | 'refreshing'
  | 'release-to-second-floor'
  | 'second-floor-opening'
  | 'second-floor'
  | 'second-floor-closing';
```

### SmartSecondFloorLayoutRef

```ts
interface SmartSecondFloorLayoutRef {
  beginRefresh(delay?: number): boolean;
  finishRefresh(options?: { success?: boolean; delay?: number }): void;
  openSecondFloor(): boolean;
  closeSecondFloor(): boolean;
}
```

`beginRefresh` 返回 `false` 表示已有刷新或二楼处于打开/动画状态；`openSecondFloor` 返回
`true` 表示命令已派发给空闲的已挂载实例，`closeSecondFloor` 返回 `true` 表示当前处于
可关闭的打开/展开状态。所有布尔返回值描述的是命令接受情况，不是动画完成情况。

二楼内容可以是嵌套 `ScrollView` 或 `FlatList`，但外层 `TwoLevelHeader` 会在边界拖拽时
接管触摸；务必给内部滚动组件配置 `nestedScrollEnabled`，并避免把横向分页手势放在同一
个边界区域。这个限制是原生手势竞争，不是 `onStateChange` 的状态缺失。
