---
title: Customization
description: Configure Classic and Material headers, colors, spinner behavior, messages, and haptics.
---

# Customization

## Classic

```tsx
<SmartRefreshLayout
  headerStyle="classic"
  primaryColor="#0f766e"
  indicatorColor="#ffffff"
  titleColor="#ffffff"
  classicSpinnerStyle="fixed-behind"
  classicEnableLastTime
  onRefresh={reload}
>
  <FlatList {...listProps} />
</SmartRefreshLayout>
```

`classicSpinnerStyle` accepts `scale`, `translate`, or `fixed-behind` on Android and iOS.

## Material

```tsx
<SmartRefreshLayout
  headerStyle="material"
  primaryColor="#e8573d"
  indicatorColor="#ffffff"
  materialShowBezierWave
  materialEnableHeaderTranslationContent={false}
  materialProgressBackgroundColor="#e8573d"
  onRefresh={reload}
>
  <FlatList {...listProps} />
</SmartRefreshLayout>
```

Material bezier and content-translation switches are Android-specific and ignored safely on iOS. Both Material
implementations support `indicatorColor` and `materialProgressBackgroundColor`; iOS keeps `primaryColor` for
Classic headers and footers.

## Custom Lottie header

`refreshHeader` mounts its content in the native Header slot and replaces the Classic or Material header. Its default
logical height is `80` and `refreshHeaderHeight` can change it. This example drives Lottie `progress` from pull
distance during `pulling` and `ready`. Do not filter the callback on `isDragging`: release spring-back must be able
to return the animation to `0`. Once `refreshing` begins, remove the controlled `progress` and use `play()` with
`loop`; when it returns to `idle`, call `pause()` and `reset()`.

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
    // Keep rebound updates so cancelling a pull returns the animation to frame 0.
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
      refreshHeaderHeight={80}
      refreshHeaderSpinnerStyle="translate"
      refreshHeaderTriggerRate={1}
      refreshHeaderMaxDragRate={2}
      refreshHeaderFinishDuration={250}
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

`offset`, `height`, and `maxDragHeight` in `onHeaderMoving` are logical pixels (Android dp / iOS pt), and
`percent >= 1` is the refresh threshold. `refreshHeaderSpinnerStyle` accepts `scale`, `translate`, or
`fixed-behind`; `refreshHeaderTriggerRate` is valid in `(0, 1]`, and `refreshHeaderMaxDragRate` is valid in
`[1, 9]`. The cross-platform upper limit of `9` prevents Android from treating `>= 10` as a physical-pixel height.
Use `refreshHeaderFinishDuration` (`0..60000` milliseconds) for a native completion dwell or finish animation; it
is different from `finishRefresh({ delay })`, which only delays sending the completion command.

Lifecycle events are available only for a custom Header. `onHeaderInitialized` runs after native dimensions are
initialized, `onHeaderReleased` when the user releases and native release/spring-back begins, and `onHeaderStart`
when the refresh animation actually starts. Each receives `{ height, maxDragHeight }`. `onHeaderFinish` runs when
the native completion state begins and receives `{ success }`; this result comes from the native completion callback,
not an inference from the `onRefresh` Promise.

## Messages and haptics

Pass a partial `messages` object to localize pull, release, loading, completion, and no-more-data states.
`hapticsEnabled` defaults to `true` and triggers once when the release threshold is reached.
