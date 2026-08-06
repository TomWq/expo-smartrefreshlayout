# expo-smartrefreshlayout

React Native 新架构下的原生下拉刷新和上拉加载容器。

- Android: [SmartRefreshLayout](https://github.com/scwang90/SmartRefreshLayout)
- iOS: [MJRefresh](https://github.com/CoderMJLee/MJRefresh)
- React Native: Fabric Native Component + Codegen Commands

v2 不再依赖 Expo Modules API，也不要求应用安装 `expo`。库内没有全局原生服务，因此实例操作直接使用 Fabric Commands；这里不额外放置一个没有职责的 TurboModule。

## 要求

| 环境 | 最低版本 |
| --- | --- |
| React Native | 0.76 |
| React | 18.2 |
| iOS | 15.1 |
| Android | API 24 |

应用必须启用 React Native New Architecture。Expo 项目可以使用 development build，但不能在 Expo Go 中运行。

## 安装

```bash
npm install expo-smartrefreshlayout@next
```

iOS 安装原生依赖：

```bash
cd ios && pod install
```

然后重新构建应用。React Native CLI 和 Expo prebuild 项目都会通过 autolinking 接入，无需手动注册原生包。

## 基础用法

`onRefresh` 和 `onLoadMore` 可以直接返回 Promise。默认非受控模式下，Promise 结束后组件会自动收起原生动画；失败时也会正确结束动画。

```tsx
import { useCallback, useState } from 'react';
import { FlatList, Text } from 'react-native';
import { SmartRefreshLayout } from 'expo-smartrefreshlayout';

export function MessageList() {
  const [items, setItems] = useState<string[]>([]);
  const [hasMore, setHasMore] = useState(true);

  const refresh = useCallback(async () => {
    const firstPage = await api.list({ page: 1 });
    setItems(firstPage.items);
    setHasMore(firstPage.hasMore);
  }, []);

  const loadMore = useCallback(async () => {
    const nextPage = await api.list({ offset: items.length });
    setItems((current) => [...current, ...nextPage.items]);
    setHasMore(nextPage.hasMore);
    return { hasMore: nextPage.hasMore };
  }, [items.length]);

  return (
    <SmartRefreshLayout
      style={{ flex: 1 }}
      hasMore={hasMore}
      onRefresh={refresh}
      onLoadMore={loadMore}
    >
      <FlatList
        data={items}
        keyExtractor={(item) => item}
        renderItem={({ item }) => <Text>{item}</Text>}
      />
    </SmartRefreshLayout>
  );
}
```

`SmartRefreshLayout` 只接受一个原生滚动子组件。需要组合空状态或浮层时，请在外部布局中组合，不要在刷新容器里并列放多个子节点。

默认分页模式是 `loadMoreMode="pull"`，必须上拉并释放才触发。`loadMoreMode="auto"` 只有在内容超过一屏且用户真实向上滚动后才会解锁，不会在首次挂载或短列表时自行触发。一次请求完成后，下一次自动加载仍需要新的向上滚动。

## 定制显示

```tsx
<SmartRefreshLayout
  headerStyle="material"
  indicatorColor="#1677ff"
  titleColor="#333333"
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
  onRefresh={refresh}
>
  <FlatList {...listProps} />
</SmartRefreshLayout>
```

## 受控模式

传入 `refreshing` 或 `loadingMore` 后，对应动画由调用方控制。回调完成不会自动修改受控值。

```tsx
const [refreshing, setRefreshing] = useState(false);

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
</SmartRefreshLayout>;
```

## 实例命令

命令属于具体视图实例，通过 ref 调用，不再使用全局 module：

```tsx
import { useRef } from 'react';
import type { SmartRefreshLayoutRef } from 'expo-smartrefreshlayout';

const refreshRef = useRef<SmartRefreshLayoutRef>(null);

refreshRef.current?.beginRefresh();
refreshRef.current?.finishRefresh({ success: true, delay: 200 });
refreshRef.current?.beginLoadMore();
refreshRef.current?.finishLoadMore({ success: true, hasMore: false });
refreshRef.current?.resetNoMoreData();

<SmartRefreshLayout ref={refreshRef} onRefresh={refresh}>
  <FlatList {...listProps} />
</SmartRefreshLayout>;
```

通常不需要手动结束动画。主动触发刷新、受控状态机或需要覆盖默认成功/失败展示时才使用实例命令；非受控回调若不是 Promise，会在同步返回后立即结束，旧的回调式请求应先包装成 Promise。

`beginRefresh` 和 `beginLoadMore` 在当前实例已有请求时返回 `false`；成功接受时返回 `true`。刷新和分页共享同一把实例锁，过期请求的完成命令不会结束较新的动画。

## 文档

- [完整 API](./docs/API.md)
- [常用示例](./docs/EXAMPLES.md)
- [v1 到 v2 迁移](./docs/MIGRATION.md)

## 架构说明

`SmartRefreshLayout` 是一个 Fabric Native Component。Props 和事件由 RN Codegen 生成类型安全的原生接口，`beginRefresh`、`finishRefresh` 等实例动作由 Fabric Commands 分发。

TurboModule 适合不属于某个视图实例的原生能力。这个库的状态全部属于刷新容器实例，如果使用全局 TurboModule，多个列表同时存在时反而无法可靠定位目标视图。因此 v2 有意不提供全局原生 module。

## 本地开发

```bash
npm install
npm run typecheck
npm run codegen
npm run build
npm test -- --runInBand
```

JS 产物使用 React Native Builder Bob 构建；仓库本身不会用 `create-react-native-library` 重新生成。原生 Fabric 组件、Codegen 契约和现有包名仍由本仓库维护。

示例工程位于 `example/`，启用了 New Architecture。

运行本地真机示例：

```bash
cd example
npm install
npx expo start --clear
```

另开终端构建并安装原生 Development Build：

```bash
npx expo run:android --device --no-bundler
# 或
npx expo run:ios --device --no-bundler
```

示例通过 Expo Autolinking 的 `searchPaths` 直接使用仓库根目录源码，不要把仓库配置成 `file:..` 依赖；后者会把整个示例目录递归复制进 `node_modules`。首次运行必须使用 Development Build，不能使用 Expo Go。

## License

MIT
