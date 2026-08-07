# 示例

## 自动结束动画

最常用的写法只需要返回 Promise：

```tsx
import { useState } from 'react';
import { FlatList, Text } from 'react-native';
import { SmartRefreshLayout } from 'expo-smartrefreshlayout';

export function Feed() {
  const [rows, setRows] = useState<Row[]>([]);
  const [hasMore, setHasMore] = useState(true);

  return (
    <SmartRefreshLayout
      style={{ flex: 1 }}
      hasMore={hasMore}
      onRefresh={async () => {
        const result = await fetchFirstPage();
        setRows(result.rows);
        setHasMore(result.hasMore);
      }}
      onLoadMore={async () => {
        const result = await fetchNextPage();
        setRows((current) => [...current, ...result.rows]);
        setHasMore(result.hasMore);
        return { hasMore: result.hasMore };
      }}
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

## Android 官方 Header 配置

Classic 页可使用官方示例中的三种 Spinner 模式、最后更新时间和主题色：

```tsx
<SmartRefreshLayout
  headerStyle="classic"
  primaryColor="#fa8c16"
  indicatorColor="#ffffff"
  titleColor="#ffffff"
  classicSpinnerStyle="fixed-behind"
  classicEnableLastTime
  onRefresh={reload}
  onLoadMore={loadNextPage}
>
  <FlatList {...listProps} />
</SmartRefreshLayout>
```

Material 页对应官方的贝塞尔背景、内容偏移和进度圆颜色：

```tsx
<SmartRefreshLayout
  headerStyle="material"
  primaryColor="#f5222d"
  indicatorColor="#ffffff"
  materialShowBezierWave
  materialEnableHeaderTranslationContent={false}
  materialProgressBackgroundColor="#f5222d"
  onRefresh={reload}
>
  <FlatList {...listProps} />
</SmartRefreshLayout>
```

仓库中的 `example/App.tsx` 提供了独立的 Classic 与 Material 配置页，可直接切换
Spinner/波浪/内容偏移和主题色。

Classic 配置页初始使用官方 XML 的默认组合：透明 primary、深色 accent、显示最后更新时间，
并在每次配置变化后自动触发一次刷新预览；如果已有请求进行中，组件会拒绝新的主动请求以避免并发。

只传 `onRefresh` 时，加载更多默认关闭：

```tsx
<SmartRefreshLayout onRefresh={reload}>
  <ScrollView>{content}</ScrollView>
</SmartRefreshLayout>
```

## 错误处理

失败不会让动画一直停留。可以用错误回调显示提示：

```tsx
<SmartRefreshLayout
  onRefresh={reload}
  onRefreshError={(error) => {
    reportError(error);
    showToast('刷新失败，请重试');
  }}
  onLoadMore={loadNextPage}
  onLoadMoreError={() => showToast('加载失败')}
>
  <FlatList {...listProps} />
</SmartRefreshLayout>
```

## 受控刷新和加载

当页面已有请求状态时，可以直接把它们传给组件：

```tsx
const [refreshing, setRefreshing] = useState(false);
const [loadingMore, setLoadingMore] = useState(false);

<SmartRefreshLayout
  refreshing={refreshing}
  loadingMore={loadingMore}
  hasMore={hasMore}
  onRefresh={async () => {
    setRefreshing(true);
    try {
      await reload();
    } finally {
      setRefreshing(false);
    }
  }}
  onLoadMore={async () => {
    setLoadingMore(true);
    try {
      await loadNextPage();
    } finally {
      setLoadingMore(false);
    }
  }}
>
  <FlatList {...listProps} />
</SmartRefreshLayout>
```

## 页面进入后主动刷新

```tsx
import { useEffect, useRef } from 'react';
import type { SmartRefreshLayoutRef } from 'expo-smartrefreshlayout';

const refreshRef = useRef<SmartRefreshLayoutRef>(null);

useEffect(() => {
  refreshRef.current?.beginRefresh(150);
}, []);

<SmartRefreshLayout ref={refreshRef} onRefresh={reload}>
  <FlatList {...listProps} />
</SmartRefreshLayout>;
```

`beginRefresh` 会走与用户下拉相同的 `onRefresh` 回调，非受控模式下 Promise 完成后仍会自动结束。

## 回调式请求

某些旧接口通过回调通知完成，应先包装成 Promise，让组件准确等待请求结束：

```tsx
const refresh = () => new Promise<void>((resolve, reject) => {
  legacyReload((error) => {
    if (error) {
      reject(error);
    } else {
      resolve();
    }
  });
});

<SmartRefreshLayout onRefresh={refresh}>
  <FlatList {...listProps} />
</SmartRefreshLayout>;
```

如果使用受控 `refreshing`，也可以在回调完成时把它设为 `false`，由状态驱动结束动画。

## 自动加载更多

默认 footer 需要上拉手势触发。需要滚动到底部自动加载时：

```tsx
<SmartRefreshLayout
  loadMoreMode="auto"
  hasMore={hasMore}
  onLoadMore={loadNextPage}
>
  <FlatList {...listProps} />
</SmartRefreshLayout>
```

自动模式仍然是实验性能力：只有内容超过一屏并检测到用户向上滚动后才会解锁，完成一次请求后需要下一次向上滚动。不要同时给同一个列表启用它和 `FlatList.onEndReached`，否则可能发起重复请求。默认的 `loadMoreMode="pull"` 更适合分页边界不稳定的列表。

## 淘宝二楼（仅 Android）

二楼页面需要普通列表 `children` 和正式二楼内容 `secondFloor`。需要官方式揭露背景时，传入
`secondFloorBackground`；背景会在拖拽时显现，`secondFloor` 会在打开后淡入：

```tsx
const ref = useRef<SmartSecondFloorLayoutRef>(null);
const [state, setState] = useState<SecondFloorState>('idle');

<SmartSecondFloorLayout
  ref={ref}
  style={{ flex: 1 }}
  headerInset={56}
  floorRate={1.9}
  maxRate={2.5}
  refreshRate={1}
  floorDuration={1000}
  pullToCloseEnabled
  bottomPullUpToCloseRate={1 / 6}
  secondFloorBackground={<Image source={require('./moon.jpg')} style={{ flex: 1 }} />}
  secondFloor={
    <ScrollView nestedScrollEnabled>
      {floorRows.map((row) => <Text key={row.id}>{row.title}</Text>)}
    </ScrollView>
  }
  onRefresh={reload}
  onStateChange={setState}
>
  <FlatList data={rows} renderItem={({ item }) => <Text>{item.title}</Text>} />
</SmartSecondFloorLayout>

<Button title="打开二楼" onPress={() => ref.current?.openSecondFloor()} />
<Button title="关闭二楼" onPress={() => ref.current?.closeSecondFloor()} />
<Text>当前状态：{state}</Text>
```

`openSecondFloor()` 和 `closeSecondFloor()` 的返回值只表示命令是否被当前已挂载实例接受，
不代表动画已经结束。需要准确的生命周期时，监听 `onSecondFloorOpen`、`onSecondFloorClose`
和 `onStateChange`。该组件 Android-only，iOS 没有 TwoLevelHeader 等价实现，渲染时会抛出
明确错误；请在平台分支中不要挂载它。

当页面顶部有覆盖式 Toolbar 时，传入它的 dp 高度作为 `headerInset`。该值会成为原生 Header
总高度的一部分，因此普通刷新、进入二楼和最大拖拽阈值都会保持正确。

二楼内部可以滚动，但外层下拉关闭和内部列表在边界共享触摸事件。给内部 `ScrollView` 或
`FlatList` 设置 `nestedScrollEnabled`，并在滚动到顶部后再向下拖拽；横向分页或自定义边缘
手势可能仍与关闭手势竞争。二楼没有 load-more footer，分页需求应放在普通页面或自行实现。

示例页使用 SmartRefreshLayout 官方的淘宝普通页、月球背景和正式二楼图片，三者分别对应
普通内容、`secondFloorBackground` 和 `secondFloor`。
