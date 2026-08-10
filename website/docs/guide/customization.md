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
