---
title: Examples
description: Promise completion, Lottie custom headers, controlled requests, commands, automatic loading, and Android second-floor examples.
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

## Lottie custom header

The complete Expo Router example is
[example/app/lottie.tsx](https://github.com/TomWq/expo-smartrefreshlayout/blob/main/example/app/lottie.tsx).
This standalone component covers the full lifecycle: `progress` follows native pull and spring-back in
`pulling`/`ready`; refreshing removes controlled `progress` and uses `play()` + `loop`; completion pauses and
resets the animation. Do not update progress only when `isDragging` is true, or a cancelled pull cannot return to
its first frame.

```tsx
import { useCallback, useEffect, useRef, useState } from 'react';
import { FlatList, Text, View } from 'react-native';
import LottieView from 'lottie-react-native';
import {
  SmartRefreshLayout,
  type HeaderMovingEvent,
} from 'expo-smartrefreshlayout';

const rows = ['Inbox', 'Mentions', 'Saved'];
const wait = (milliseconds: number) =>
  new Promise<void>((resolve) => setTimeout(resolve, milliseconds));

export function LottieRefreshList() {
  const lottieRef = useRef<LottieView>(null);
  const refreshingRef = useRef(false);
  const [refreshing, setRefreshing] = useState(false);
  const [headerProgress, setHeaderProgress] = useState(0);
  const [headerOffset, setHeaderOffset] = useState(0);

  refreshingRef.current = refreshing;

  useEffect(() => {
    const animation = lottieRef.current;
    if (refreshing) {
      animation?.play();
      return;
    }

    animation?.pause();
    animation?.reset();
  }, [refreshing]);

  const handleHeaderMoving = useCallback(({ percent, offset }: HeaderMovingEvent) => {
    if (refreshingRef.current) {
      return;
    }

    setHeaderProgress(Math.min(Math.max(percent, 0), 1));
    setHeaderOffset(Math.max(0, Math.round(offset)));
  }, []);

  const refresh = useCallback(async () => {
    refreshingRef.current = true;
    setRefreshing(true);

    try {
      await wait(900);
    } finally {
      refreshingRef.current = false;
      setRefreshing(false);
      setHeaderProgress(0);
      setHeaderOffset(0);
    }
  }, []);

  const handleAnimationLoaded = useCallback(() => {
    if (refreshingRef.current) {
      lottieRef.current?.play();
    }
  }, []);

  return (
    <SmartRefreshLayout
      style={{ flex: 1 }}
      loadMoreEnabled={false}
      refreshHeader={
        <View
          pointerEvents="none"
          style={{ alignItems: 'center', height: 80, justifyContent: 'center' }}
        >
          <LottieView
            ref={lottieRef}
            source={require('../assets/load.json')}
            progress={refreshing ? undefined : headerProgress}
            loop
            style={{ height: 48, width: 48 }}
            onAnimationLoaded={handleAnimationLoaded}
          />
          <Text>
            {refreshing
              ? 'Refreshing...'
              : headerProgress >= 1
                ? 'Release to refresh'
                : `Pull ${headerOffset} px`}
          </Text>
        </View>
      }
      onHeaderMoving={handleHeaderMoving}
      onRefresh={refresh}
    >
      <FlatList
        data={rows}
        keyExtractor={(item) => item}
        renderItem={({ item }) => <Text style={{ padding: 24 }}>{item}</Text>}
      />
    </SmartRefreshLayout>
  );
}
```

A custom Header is a fixed `80` logical-pixel native slot. `offset`, `height`, and `maxDragHeight` use Android dp /
iOS pt; `percent >= 1` is the refresh threshold.

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
