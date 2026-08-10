---
title: Refresh and load more
description: Promise completion, controlled state, request locking, requestId, pull-release, and automatic loading.
---

# Refresh and load more

## Promise completion

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

In uncontrolled mode, a fulfilled or rejected Promise finishes the native animation. Rejections also call
`onRefreshError` or `onLoadMoreError` when provided.

## Controlled state

Passing `refreshing` or `loadingMore` makes that state controlled. Reset the value in every completion path:

```tsx
onRefresh={async () => {
  setRefreshing(true);
  try {
    await reload();
  } finally {
    setRefreshing(false);
  }
}}
```

## One request per instance

Refresh and pagination share an instance lock. The callback receives a `{ requestId, source }` object, where
`source` is `gesture` or `programmatic`. Duplicate gestures, crossed refresh/load requests, and stale delayed
finish commands are ignored.

## Load-more modes

`pull` is the default and requires a pull-release gesture. `auto` only unlocks after content exceeds the viewport
and the user performs a real upward scroll. Do not combine automatic mode with `FlatList.onEndReached`.

## Instance commands

```ts
ref.current?.beginRefresh(150);
ref.current?.finishRefresh({ success: true, delay: 200 });
ref.current?.beginLoadMore();
ref.current?.finishLoadMore({ success: true, hasMore: false });
ref.current?.resetNoMoreData();
```

The boolean begin result reports command acceptance, not request completion.
