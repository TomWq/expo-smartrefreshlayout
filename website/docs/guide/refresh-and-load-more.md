---
title: 刷新与分页
description: 理解 Promise 自动结束、受控状态、实例锁、requestId、上拉释放和自动加载模式。
---

# 刷新与分页

## Promise 自动结束

默认是非受控模式。只需返回请求 Promise：

```tsx
<SmartRefreshLayout
  hasMore={hasMore}
  onRefresh={async () => {
    const page = await api.firstPage();
    setRows(page.rows);
    setHasMore(page.hasMore);
  }}
  onLoadMore={async () => {
    const page = await api.nextPage();
    setRows((rows) => [...rows, ...page.rows]);
    setHasMore(page.hasMore);
    return { hasMore: page.hasMore };
  }}
>
  <FlatList {...listProps} />
</SmartRefreshLayout>
```

Promise 成功或失败后都会结束动画。失败时还会调用 `onRefreshError` 或 `onLoadMoreError`。

## 受控状态

传入 `refreshing` 或 `loadingMore` 后，对应请求进入受控模式。此时回调完成不会自动收起动画，
调用方必须把值恢复为 `false`：

```tsx
<SmartRefreshLayout
  refreshing={refreshing}
  onRefresh={async () => {
    setRefreshing(true);
    try {
      await reload();
    } finally {
      setRefreshing(false);
    }
  }}
>
  <FlatList {...listProps} />
</SmartRefreshLayout>
```

## 同一实例只允许一个请求

刷新与分页共享一个实例锁。请求开始时回调会收到：

```ts
interface RefreshRequest {
  requestId: number;
  source: 'gesture' | 'programmatic';
}
```

请求进行期间的重复手势、刷新与分页交叉触发都会被拒绝。延迟结束命令也只会结束匹配的请求，
不会把更新的请求提前收起。

## 两种加载模式

`loadMoreMode="pull"` 是默认值，用户必须上拉并释放才触发分页。

`loadMoreMode="auto"` 只有在内容超过一屏且检测到真实向上滚动后才会解锁。首次挂载、短列表或
footer 仅仅出现都不会触发。一次请求完成后，需要下一次向上滚动才会再次解锁。

::: warning 避免重复分页
不要同时启用自动加载和 `FlatList.onEndReached`，否则两个入口可能发起重复请求。
:::

## 实例命令

```tsx
const ref = useRef<SmartRefreshLayoutRef>(null);

ref.current?.beginRefresh(150);
ref.current?.finishRefresh({ success: true, delay: 200 });
ref.current?.beginLoadMore();
ref.current?.finishLoadMore({ success: true, hasMore: false });
ref.current?.resetNoMoreData();
```

`beginRefresh` 与 `beginLoadMore` 返回的是命令是否被接受，不代表网络请求已经完成。
