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
| `refreshEnabled` | `boolean` | 是否提供 `onRefresh` | 是否允许下拉刷新 |
| `loadMoreEnabled` | `boolean` | 是否提供 `onLoadMore` | 是否允许上拉加载 |
| `loadMoreMode` | `'pull' | 'auto'` | `'pull'` | `pull` 需要上拉释放；`auto` 需要先真实向上滚动且内容超过一屏 |
| `autoLoadMoreEnabled` | `boolean` | `false` | 兼容别名，等同于 `loadMoreMode="auto"`；新代码请使用 `loadMoreMode` |
| `refreshing` | `boolean` | 非受控 | 受控刷新状态 |
| `loadingMore` | `boolean` | 非受控 | 受控加载状态 |
| `hasMore` | `boolean` | `true` | `false` 时显示没有更多数据并禁止继续加载 |
| `hapticsEnabled` | `boolean` | `true` | 到达释放阈值时启用触觉反馈 |
| `headerStyle` | `'classic' \| 'material'` | `'classic'` | 刷新头样式 |
| `indicatorColor` | `ColorValue` | 平台默认 | 指示器颜色 |
| `titleColor` | `ColorValue` | 平台默认 | 状态文字颜色 |
| `messages` | `Partial<RefreshMessages>` | 英文默认文案 | 覆盖状态文案 |
| `onRefresh` | `(request) => void \| Promise<void>` | - | 下拉刷新回调，参数含 `requestId` 和 `source` |
| `onLoadMore` | `(request) => void \| { hasMore } \| Promise<...>` | - | 加载更多回调，可直接返回下一页是否还有数据 |
| `onRefreshError` | `(error: unknown) => void` | - | `onRefresh` 抛错后的通知 |
| `onLoadMoreError` | `(error: unknown) => void` | - | `onLoadMore` 抛错后的通知 |
| `onStateChange` | `(state: RefreshState) => void` | - | 原生刷新状态变化 |

其余 `ViewProps` 会传给原生容器。

### Promise 行为

未传 `refreshing` 时，`onRefresh` 返回的 Promise settle 后会自动调用原生 `finishRefresh`。未传 `loadingMore` 时，`onLoadMore` 采用同样规则。

如果回调抛错，组件会以失败状态结束动画，并调用对应的 error handler。组件不会在没有 error handler 时制造未处理的 Promise rejection。

传入 `refreshing` 或 `loadingMore` 后即进入受控模式，调用方必须把对应值恢复为 `false` 才会结束动画。

一次刷新或分页请求从原生手势或实例命令开始，并带有只属于该视图实例的 `requestId`。同一时间只允许一个请求；重复手势、刷新与分页交叉触发，以及过期的延迟结束命令都会被忽略。

`onLoadMore` 可以返回 `{ hasMore: false }`，组件会在同一轮请求完成时锁定 footer，不必等待下一次 React 渲染。未返回该对象时，组件使用 `hasMore` Prop 的最新值。

自动模式不会在首次挂载、短列表或仅仅因为 footer 出现时触发。Android 和 iOS 都要求内容超过一屏，并先检测到用户向上滚动；请求完成后需要下一次向上滚动才会再次解锁自动加载。

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
