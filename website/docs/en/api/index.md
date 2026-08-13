---
title: API reference
description: SmartRefreshLayout and SmartSecondFloorLayout props, states, callback contracts, and instance commands.
---

# API reference

## SmartRefreshLayout

```tsx
import { SmartRefreshLayout } from 'expo-smartrefreshlayout';
```

The component requires exactly one `FlatList`, `SectionList`, `ScrollView`, or compatible native scrolling child.

### Props

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `children` | `ReactElement` | required | The single scrolling child |
| `refreshHeader` | `ReactElement` | - | React content mounted in the Android/iOS native refresh-header slot; replaces the Classic or Material header selected by `headerStyle` |
| `refreshHeaderHeight` | `number` | `80` | Custom Header logical height (Android dp / iOS pt); finite positive values are capped at `10000` and rounded, while invalid or non-positive rounded values fall back to `80` |
| `refreshHeaderSpinnerStyle` | `'scale' \| 'translate' \| 'fixed-behind'` | `'translate'` | Native motion mode for `refreshHeader` on both platforms |
| `refreshHeaderTriggerRate` | `number` | `1` | Custom Header refresh-trigger height multiplier; valid range `(0, 1]`, otherwise falls back to `1` |
| `refreshHeaderMaxDragRate` | `number` | `2` | Custom Header maximum-pull-height multiplier; valid range `[1, 9]`, otherwise falls back to `2` |
| `refreshHeaderFinishDuration` | `number` | `0` | Custom Header native completion-state dwell/finish-animation duration in milliseconds, normalized to `0..60000` |
| `refreshEnabled` | `boolean` | has `onRefresh` | Enables pull to refresh |
| `loadMoreEnabled` | `boolean` | has `onLoadMore` | Enables load more |
| `loadMoreMode` | `'pull' \| 'auto'` | `'pull'` | `pull` requires a pull-release; `auto` waits for overflowing content and a real upward scroll |
| `autoLoadMoreEnabled` | `boolean` | `false` | Deprecated compatibility alias for `loadMoreMode="auto"`; ignored when `loadMoreMode` is set |
| `refreshing` | `boolean` | uncontrolled | Controlled refresh state |
| `loadingMore` | `boolean` | uncontrolled | Controlled pagination state |
| `hasMore` | `boolean` | `true` | Shows no-more-data and prevents another load when false |
| `hapticsEnabled` | `boolean` | `true` | Haptic feedback at the release threshold |
| `headerStyle` | `'classic' \| 'material'` | `'classic'` | Refresh header implementation |
| `primaryColor` | `ColorValue` | platform default | Classic header/footer primary background; Android Material also uses it as its primary color |
| `indicatorColor` | `ColorValue` | platform default | Indicator color |
| `titleColor` | `ColorValue` | platform default | Classic state-label color |
| `classicSpinnerStyle` | `'scale' \| 'translate' \| 'fixed-behind'` | `'translate'` | Classic header motion on Android and iOS |
| `classicEnableLastTime` | `boolean` | `true` | Shows the Classic last-refresh label on Android and iOS |
| `materialShowBezierWave` | `boolean` | `false` | Android-only Material bezier background |
| `materialEnableHeaderTranslationContent` | `boolean` | `false` | Android-only Material content translation |
| `materialProgressBackgroundColor` | `ColorValue` | Material default | Material progress-circle background on Android and iOS |
| `messages` | `Partial<RefreshMessages>` | English defaults | State-label overrides |
| `onRefresh` | `(request) => void \| Promise<void>` | - | Refresh callback |
| `onLoadMore` | `(request) => void \| LoadMoreResult \| Promise<void \| LoadMoreResult>` | - | Pagination callback; may return the next `hasMore` value |
| `onRefreshError` | `(error: unknown) => void` | - | Refresh failure notification |
| `onLoadMoreError` | `(error: unknown) => void` | - | Pagination failure notification |
| `onStateChange` | `(state: RefreshState) => void` | - | Native state change |
| `onHeaderMoving` | `(event: HeaderMovingEvent) => void` | - | Custom-header pull-distance updates, including spring-back after release |
| `onHeaderInitialized` | `(event: HeaderLifecycleEvent) => void` | - | Custom Header only: native dimensions initialized |
| `onHeaderReleased` | `(event: HeaderLifecycleEvent) => void` | - | Custom Header only: user released and native release/spring-back begins |
| `onHeaderStart` | `(event: HeaderLifecycleEvent) => void` | - | Custom Header only: native refresh animation actually begins |
| `onHeaderFinish` | `(event: HeaderFinishEvent) => void` | - | Custom Header only: native completion state begins with success or failure |

Other `ViewProps` pass through to the native container.

### Custom native header

`refreshHeader` is mounted in a real Android/iOS native refresh-header slot, rather than as an ordinary child of the
list. When supplied, it replaces the Classic or Material header chosen by `headerStyle`. Its default logical height
is `80` and `refreshHeaderHeight` can change it; keep the content layout aligned with that height. For display-only
content, use `pointerEvents="none"` so it cannot intercept the list's pull gesture.

`refreshHeaderSpinnerStyle` accepts `scale`, `translate`, or `fixed-behind`. `refreshHeaderTriggerRate` controls
the refresh threshold relative to Header height, while `refreshHeaderMaxDragRate` controls the real maximum pull
height. The maximum rate is capped at `9` so Android never treats `>= 10` as a physical-pixel height;
`onHeaderMoving.maxDragHeight` reports the resulting limit. `refreshHeaderFinishDuration` controls the native
success/failure completion dwell or finish animation. It is distinct from `finishRefresh({ delay })`, which delays
sending the completion command.

`onHeaderMoving` fires while dragging and while the header springs back after release. `offset`, `height`, and
`maxDragHeight` are platform-independent logical pixels (Android dp / iOS pt); `percent >= 1` means the refresh
threshold has been reached. For a custom Header, `onHeaderInitialized`, `onHeaderReleased`, and `onHeaderStart`
receive `{ height, maxDragHeight }`. `onHeaderFinish` receives `{ success }` from the native completion callback,
rather than a result inferred from the JavaScript Promise.

### Header colors and behavior

Classic headers and footers support `primaryColor`, `indicatorColor`, and `titleColor` on both platforms. The
Classic header additionally supports `classicSpinnerStyle` and `classicEnableLastTime`. The Material header supports
`indicatorColor` and `materialProgressBackgroundColor` on both platforms. Android additionally uses
`primaryColor` for Material and exposes its bezier and content-translation switches. iOS safely ignores those
two Android-only layout switches and keeps `primaryColor` for Classic headers and footers.

```tsx
<SmartRefreshLayout
  headerStyle="classic"
  primaryColor="#1677ff"
  indicatorColor="#ffffff"
  titleColor="#ffffff"
  classicSpinnerStyle="fixed-behind"
  classicEnableLastTime
  onRefresh={reload}
>
  <FlatList {...listProps} />
</SmartRefreshLayout>
```

```tsx
<SmartRefreshLayout
  headerStyle="material"
  primaryColor="#52c41a"
  indicatorColor="#ffffff"
  materialShowBezierWave
  materialEnableHeaderTranslationContent={false}
  materialProgressBackgroundColor="#52c41a"
  onRefresh={reload}
>
  <FlatList {...listProps} />
</SmartRefreshLayout>
```

Header configuration can change while the component is mounted. Android rebuilds its Classic header when the
spinner mode changes; if a request is active, the rebuild waits until the native layout is idle.

### Completion behavior

Without `refreshing`, a settled Promise from `onRefresh` automatically finishes the native refresh. Without
`loadingMore`, `onLoadMore` follows the same rule. A synchronous callback therefore completes immediately.

Thrown errors finish the matching uncontrolled animation with failure and call `onRefreshError` or
`onLoadMoreError` when provided. The component does not create an unhandled Promise rejection when an error
handler is absent.

Passing `refreshing` or `loadingMore` makes that request controlled. The caller must restore the matching prop to
`false` to finish the native animation.

Refresh and pagination share one lock per view instance. Each accepted request has a `requestId`; duplicate
gestures, crossed refresh/load requests, and stale delayed finish commands are ignored.

In automatic mode, the footer does not request data on mount, for short content, or merely because it appears.
The user must scroll upward after content exceeds the viewport. A completed automatic request needs another upward
scroll before it can unlock again. Do not combine it with `FlatList.onEndReached`.

### Callback and state types

```ts
interface RefreshMessages {
  pullDown: string;
  releaseToRefresh: string;
  refreshing: string;
  refreshComplete: string;
  pullUp: string;
  releaseToLoadMore: string;
  loadingMore: string;
  noMoreData: string;
}

interface RefreshRequest {
  requestId: number;
  source: 'gesture' | 'programmatic';
}

interface LoadMoreResult {
  hasMore: boolean;
}

interface HeaderMovingEvent {
  /** Pull distance relative to the refresh trigger threshold. */
  percent: number;
  /** Current header pull distance in logical pixels (dp/pt). */
  offset: number;
  /** Native header height in logical pixels (dp/pt). */
  height: number;
  /** Maximum pull distance in logical pixels (dp/pt). */
  maxDragHeight: number;
  /** Whether the user is actively dragging the scroll view. */
  isDragging: boolean;
}

interface HeaderLifecycleEvent {
  /** Native Header height in logical pixels (dp/pt). */
  height: number;
  /** Maximum pull distance in logical pixels (dp/pt). */
  maxDragHeight: number;
}

interface HeaderFinishEvent {
  /** Result reported by the native Header completion state. */
  success: boolean;
}

type RefreshState =
  | 'idle'
  | 'pulling'
  | 'ready'
  | 'refreshing'
  | 'loading'
  | 'no-more-data';
```

`messages` accepts a partial object; omitted labels retain the built-in English defaults. Returning
`{ hasMore: false }` from `onLoadMore` locks the footer in that same request, without waiting for a new render.
Otherwise the latest `hasMore` prop is used.

### SmartRefreshLayoutRef

```tsx
import type { SmartRefreshLayoutRef } from 'expo-smartrefreshlayout';
```

```ts
interface SmartRefreshLayoutRef {
  beginRefresh(delay?: number): boolean;
  finishRefresh(options?: { success?: boolean; delay?: number }): void;
  beginLoadMore(delay?: number): boolean;
  finishLoadMore(options?: {
    success?: boolean;
    hasMore?: boolean;
    delay?: number;
  }): void;
  resetNoMoreData(): void;
}
```

`beginRefresh` and `beginLoadMore` return `true` when the mounted instance accepted the command. They return
`false` for an active request; `beginLoadMore` also returns `false` when `hasMore` is false. A non-finite or
negative delay is normalized to `0` milliseconds.

`finishRefresh` and `finishLoadMore` complete only the matching active operation. Their defaults are
`success: true` and `delay: 0`; `finishLoadMore({ hasMore: false })` puts the native footer into no-more-data.
`resetNoMoreData()` clears that native footer state. In a controlled pagination flow, also restore `hasMore` to
`true`, otherwise the next render applies no-more-data again.

## Compatibility export

v2 temporarily keeps the old component name as an alias:

```ts
import { ExpoSmartrefreshlayoutView } from 'expo-smartrefreshlayout';
```

It accepts v2 props. The old `ExpoSmartrefreshlayoutModule` and legacy v1 props are not present.

## SmartSecondFloorLayout (Android only)

```tsx
import { SmartSecondFloorLayout } from 'expo-smartrefreshlayout';
```

This component wraps SmartRefreshLayout's Android `TwoLevelHeader`. `children` is the ordinary page's single
scrolling child; `secondFloor` is the full-screen formal content. `secondFloorBackground` is an optional layer
behind it that appears during the reveal while formal content fades in. There is no footer or `onLoadMore` API.

Mounting this component on iOS throws an explicit error because there is no equivalent native interaction. Branch
on the platform before rendering it.

### Props

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `children` | `ReactElement` | required | Normal page's single scrolling child |
| `secondFloor` | `ReactElement` | required | Full-screen second-floor content, such as a `ScrollView` or `FlatList` |
| `secondFloorBackground` | `ReactElement` | - | Optional backdrop behind formal content |
| `refreshEnabled` | `boolean` | has `onRefresh` | Enables ordinary pull to refresh |
| `refreshing` | `boolean` | uncontrolled | Controlled ordinary refresh state |
| `hapticsEnabled` | `boolean` | `true` | Feedback at refresh or second-floor release thresholds |
| `secondFloorEnabled` | `boolean` | `true` | Enables the second-floor gesture and open command |
| `headerInset` | `number` | `0` | Logical height reserved for an overlay toolbar |
| `maxRate` | `number` | `2.5` | Maximum pull multiplier, normalized to `1.2..5` |
| `floorRate` | `number` | `1.9` | Second-floor release multiplier, at least `1.1` and below `maxRate` |
| `refreshRate` | `number` | `1` | Ordinary refresh multiplier, at least `0.25` and below `floorRate` |
| `floorDuration` | `number` | `1000` | Second-floor open/stay duration in milliseconds, normalized to `0..10000` |
| `pullToCloseEnabled` | `boolean` | `true` | Enables the downward close gesture |
| `bottomPullUpToCloseRate` | `number` | `1/6` | Bottom upward-close rate, normalized to `0.01..0.5` |
| `primaryColor` | `ColorValue` | platform default | Classic header primary background |
| `indicatorColor` | `ColorValue` | platform default | Classic indicator color |
| `titleColor` | `ColorValue` | platform default | Classic state-label color |
| `classicEnableLastTime` | `boolean` | `true` | Shows the Classic last-refresh label |
| `messages` | `Partial<SecondFloorMessages>` | English defaults | Ordinary-refresh label overrides |
| `onRefresh` | `(request) => void \| Promise<void>` | - | Ordinary refresh callback |
| `onRefreshError` | `(error: unknown) => void` | - | Refresh failure notification |
| `onStateChange` | `(state: SecondFloorState) => void` | - | Refresh and second-floor lifecycle state |
| `onSecondFloorOpen` | `() => void` | - | Open animation completed |
| `onSecondFloorClose` | `() => void` | - | Close animation completed |

Conflicting `refreshRate`, `floorRate`, and `maxRate` values are normalized in JavaScript and Android to preserve
their threshold ordering rather than throwing. Pass `headerInset` when a toolbar overlays the page: it becomes part
of the native header height, so the visible header and pull thresholds agree.

```ts
interface SecondFloorMessages {
  pullDown: string;
  releaseToRefresh: string;
  refreshing: string;
  refreshComplete: string;
}

type SecondFloorState =
  | 'idle'
  | 'pulling'
  | 'ready'
  | 'refreshing'
  | 'release-to-second-floor'
  | 'second-floor-opening'
  | 'second-floor'
  | 'second-floor-closing';
```

### SmartSecondFloorLayoutRef

```ts
interface SmartSecondFloorLayoutRef {
  beginRefresh(delay?: number): boolean;
  finishRefresh(options?: { success?: boolean; delay?: number }): void;
  openSecondFloor(): boolean;
  closeSecondFloor(): boolean;
}
```

`beginRefresh` returns `false` while a refresh or second-floor transition is active. `openSecondFloor` returns
`true` once an idle, mounted native instance accepted the command, and `closeSecondFloor` returns `true` when an
opening or open floor accepted its close command. These boolean values describe command acceptance, not animation
completion; observe `onSecondFloorOpen`, `onSecondFloorClose`, or `onStateChange` for lifecycle completion.

Second-floor content can scroll, but the outer `TwoLevelHeader` takes over boundary drags. Enable
`nestedScrollEnabled` on an inner `ScrollView` or `FlatList` and avoid competing horizontal or edge gestures at the
same boundary.
