---
title: 从 v1 迁移到 v2
description: 将旧 Expo Modules API、全局命令和旧 Props 迁移到 Fabric 视图实例与 v2 API。
---

# 从 v1 迁移到 v2

v2 是一次新架构重写。包名仍是 `expo-smartrefreshlayout`，但不再依赖 Expo Modules API，也不兼容旧架构。

## 1. 更新运行环境

- React Native 升级到 0.76 或更高版本。
- 启用 New Architecture。
- iOS deployment target 至少为 15.1。
- Android minSdk 至少为 24。
- 重新安装 Pods 并完整重建应用。

Expo 应用可以继续使用该库，但需要 development build 或本地原生构建，Expo Go 不包含此原生组件。

## 2. 替换组件 API

推荐改用新名称：

```diff
- import { ExpoSmartrefreshlayoutView } from 'expo-smartrefreshlayout';
+ import { SmartRefreshLayout } from 'expo-smartrefreshlayout';
```

`ExpoSmartrefreshlayoutView` 暂时仍是别名，但只接受 v2 Props。

常用 Props 映射：

| v1 | v2 |
| --- | --- |
| `enableRefresh` | `refreshEnabled` |
| `enableLoadMore` | `loadMoreEnabled` |
| `enableAutoLoadMore` | `loadMoreMode="auto"`（旧别名 `autoLoadMoreEnabled` 仍可用） |
| `enableHapticFeedback` | `hapticsEnabled` |
| `headerType="classics"` | `headerStyle="classic"` |
| `headerType="material"` | `headerStyle="material"` |
| `onStateChanged` | `onStateChange` |
| `renderHeader` | `refreshHeader` |
| `onHeaderMoving` | `onHeaderMoving`，事件字段改为 `percent`、`offset`、`height`、`maxDragHeight`、`isDragging` |
| Classic Header/Footer 文字对象 | `messages` |
| Header/Footer 强调色 | `indicatorColor`、`titleColor` |
| Classic `setSpinnerStyle` | `classicSpinnerStyle` |
| Classic `setEnableLastTime` | `classicEnableLastTime` |
| Material `setShowBezierWave` | `materialShowBezierWave` |
| Material `setEnableHeaderTranslationContent` | `materialEnableHeaderTranslationContent` |

`onRefresh` 和 `onLoadMore` 现在可以返回 Promise，非受控模式会自动结束动画：

```diff
 const refresh = async () => {
   await reload();
-  ExpoSmartrefreshlayoutModule.finishRefresh(true, 300);
 };
```

`refreshHeader` 会把 React 内容挂载到两端原生 Header 槽位，而不是放入列表内容；提供后会覆盖
Classic/Material Header。自定义 Header 当前固定为 `80` 逻辑像素高；`onHeaderMoving` 的
`offset`、`height`、`maxDragHeight` 使用 dp/pt 逻辑像素，`percent >= 1` 表示到达刷新阈值。

## 3. 删除全局 Module 调用

v1 的全局 module 无法区分页面中的多个刷新容器。v2 把命令绑定到 Fabric 视图实例：

```diff
- ExpoSmartrefreshlayoutModule.autoRefresh(200);
+ refreshRef.current?.beginRefresh(200);

- ExpoSmartrefreshlayoutModule.finishRefresh(true, 300);
+ refreshRef.current?.finishRefresh({ success: true, delay: 300 });

- ExpoSmartrefreshlayoutModule.autoLoadMore();
+ refreshRef.current?.beginLoadMore();

- ExpoSmartrefreshlayoutModule.finishLoadMore(true, 0, true);
+ refreshRef.current?.finishLoadMore({ success: true, hasMore: false });

- ExpoSmartrefreshlayoutModule.setNoMoreData(false);
+ refreshRef.current?.resetNoMoreData();
```

ref 的声明方式：

```tsx
const refreshRef = useRef<SmartRefreshLayoutRef>(null);

<SmartRefreshLayout ref={refreshRef} onRefresh={refresh}>
  <FlatList {...listProps} />
</SmartRefreshLayout>;
```

## 4. 移除的 API

为了让 Android 和 iOS 行为一致，以下 v1 API 仍未迁移：

- `renderFooter` 和 `DefaultRefreshHeader`
- `onFooterMoving`
- Header/Footer 高度、拖拽倍率、回弹时间等 Android 细粒度参数
- 旧 `classicRefreshHeaderProps`、`classicLoadMoreFooterProps` 对象形式；Classic/Material
  官方样式配置请改用 `classicSpinnerStyle`、`classicEnableLastTime`、
  `materialShowBezierWave`、`materialEnableHeaderTranslationContent` 和
  `materialProgressBackgroundColor`
- Paper 旧架构支持

自动加载的触发语义也已收紧：不会因为初始内容测量、短列表或 footer 回弹直接发起请求，必须先发生真实的向上滚动。分页回调可以返回 `{ hasMore }`，避免依赖异步的 `hasMore` render 时序。

不要把上述仍未迁移的旧 Props 原样留在代码中；TypeScript 会将其报告为错误。后续扩展应优先设计为两端一致的 Fabric Props 或 Commands。

## 5. 受控状态

如果传入 `refreshing` 或 `loadingMore`，对应状态由调用方负责归零：

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

未传受控状态时，不需要手动调用 finish 命令。

## 6. Android 二楼能力

v2 新增的 `SmartSecondFloorLayout` 是独立的 Android-only 组件，不是旧
`SmartRefreshLayout` 的一个 Header 配置。普通内容仍是唯一的 `children`，二楼内容通过
`secondFloor` 槽位传入；可选 `secondFloorBackground` 会放在其后并在手势中显现。它没有 `onLoadMore` 或 footer。`floorRate`、`maxRate`、
`refreshRate`、`floorDuration`、`pullToCloseEnabled` 和 `bottomPullUpToCloseRate` 对应
SmartRefreshLayout `TwoLevelHeader` 的参数。

```tsx
<SmartSecondFloorLayout
  ref={floorRef}
  secondFloor={<ScrollView nestedScrollEnabled>{floorContent}</ScrollView>}
  onRefresh={reload}
>
  <FlatList data={rows} renderItem={renderRow} />
</SmartSecondFloorLayout>
```

`openSecondFloor()`、`closeSecondFloor()` 的布尔返回值表示命令是否被已挂载实例接受，
不是动画完成信号；用 `onSecondFloorOpen`、`onSecondFloorClose` 和 `onStateChange` 监听
生命周期。iOS 没有 TwoLevelHeader 等价实现，不能在 iOS 渲染该组件；请在平台分支中继续
使用 `SmartRefreshLayout`。
