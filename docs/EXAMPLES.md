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
