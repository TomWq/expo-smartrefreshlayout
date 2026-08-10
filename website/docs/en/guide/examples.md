---
title: Examples
description: Promise completion, controlled requests, commands, automatic loading, and Android second-floor examples.
---

# Examples

## Automatic Promise completion

```tsx
<SmartRefreshLayout
  hasMore={hasMore}
  onRefresh={reload}
  onLoadMore={async () => {
    const result = await loadNextPage();
    setHasMore(result.hasMore);
    return { hasMore: result.hasMore };
  }}
>
  <FlatList {...listProps} />
</SmartRefreshLayout>
```

## Error handling

```tsx
<SmartRefreshLayout
  onRefresh={reload}
  onRefreshError={(error) => reportError(error)}
  onLoadMore={loadNextPage}
  onLoadMoreError={() => showToast('Unable to load more')}
>
  <FlatList {...listProps} />
</SmartRefreshLayout>
```

## Programmatic refresh

```tsx
const refreshRef = useRef<SmartRefreshLayoutRef>(null);

useEffect(() => {
  refreshRef.current?.beginRefresh(150);
}, []);

<SmartRefreshLayout ref={refreshRef} onRefresh={reload}>
  <FlatList {...listProps} />
</SmartRefreshLayout>;
```

## Automatic loading

```tsx
<SmartRefreshLayout
  loadMoreMode="auto"
  hasMore={hasMore}
  onLoadMore={loadNextPage}
>
  <FlatList {...listProps} />
</SmartRefreshLayout>
```

Automatic mode requires overflowing content and a real upward scroll. Keep `FlatList.onEndReached` disabled.

For a complete Android second-floor example, see [Android second floor](./second-floor).
