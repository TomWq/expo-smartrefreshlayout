---
title: 样式与文案
description: 配置 Classic、Material Header、颜色、Spinner 行为、状态文案和触觉反馈。
---

# 样式与文案

## Classic Header

```tsx
<SmartRefreshLayout
  headerStyle="classic"
  primaryColor="#0f766e"
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

`classicSpinnerStyle` 支持 `scale`、`translate` 与 `fixed-behind`。Android 与 iOS 都支持这些
核心样式。Android 上运行时切换 Spinner 会重建 Header；若请求正在进行，会等待空闲后应用。

## Material Header

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

`materialShowBezierWave` 与 `materialEnableHeaderTranslationContent` 是 Android 官方 Header 的
细节开关，iOS 会安全忽略。两端的 Material Header 都支持 `indicatorColor` 与
`materialProgressBackgroundColor`；iOS 的 `primaryColor` 仅用于 Classic Header/Footer。

## 自定义 Lottie Header

`refreshHeader` 会将内容挂载进原生 Header 槽位，并覆盖 Classic/Material Header。下例让 Lottie 在
`pulling` 和 `ready` 阶段由 `progress` 跟随下拉距离；不要用 `isDragging` 过滤回调，否则松手取消时的
回弹不会把动画带回 `0`。进入 `refreshing` 后移除受控 `progress`，改用 `play()` 和 `loop`；完成后在
`idle` 阶段 `pause()`、`reset()`。

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
    // 保留回弹事件，让取消下拉时动画回到首帧。
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
              ? '正在刷新...'
              : headerProgress >= 1
                ? '松开刷新'
                : `下拉 ${headerOffset} px`}
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

`onHeaderMoving` 的 `offset`、`height`、`maxDragHeight` 都是逻辑像素（Android dp / iOS pt），
`percent >= 1` 表示达到刷新阈值。自定义 Header 当前固定高度为 `80` 逻辑像素。

## 自定义文案

```tsx
<SmartRefreshLayout
  messages={{
    pullDown: '下拉刷新',
    releaseToRefresh: '松开刷新',
    refreshing: '正在刷新...',
    refreshComplete: '刷新完成',
    pullUp: '上拉加载更多',
    releaseToLoadMore: '松开加载',
    loadingMore: '正在加载...',
    noMoreData: '没有更多数据',
  }}
  onRefresh={reload}
>
  <FlatList {...listProps} />
</SmartRefreshLayout>
```

`messages` 接受部分字段，未传项继续使用内置英文默认值。建议产品在应用层集中提供本地化文案。

## 触觉反馈

`hapticsEnabled` 默认开启，在拖拽达到释放阈值时触发一次反馈。若应用已经在相同手势节点提供反馈，
可传 `false` 避免重复。
