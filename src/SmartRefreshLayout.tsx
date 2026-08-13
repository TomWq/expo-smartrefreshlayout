import React, {
  forwardRef,
  useCallback,
  useImperativeHandle,
  useRef,
  useState,
  useEffect,
} from 'react';
import { StyleSheet, type NativeSyntheticEvent } from 'react-native';

import NativeSmartRefreshHeaderSlot from './NativeSmartRefreshHeaderSlot';

import NativeSmartRefreshLayout, {
  Commands,
  type HeaderMovingEvent as NativeHeaderMovingEvent,
  type RequestEvent,
  type StateChangeEvent,
} from './NativeSmartRefreshLayout';
import type {
  FinishLoadMoreOptions,
  FinishRefreshOptions,
  HeaderMovingEvent,
  LoadMoreResult,
  RefreshRequest,
  RefreshState,
  SmartRefreshLayoutProps,
  SmartRefreshLayoutRef,
} from './SmartRefreshLayout.types';

const DEFAULT_CUSTOM_HEADER_HEIGHT = 80;
const DEFAULT_CUSTOM_HEADER_TRIGGER_RATE = 1;
const DEFAULT_CUSTOM_HEADER_MAX_DRAG_RATE = 2;
const DEFAULT_CUSTOM_HEADER_FINISH_DURATION = 0;
const MAX_CUSTOM_HEADER_HEIGHT = 10_000;
const MAX_CUSTOM_HEADER_TRIGGER_RATE = 1;
// SmartRefreshLayout interprets rates >= 10 as a physical-pixel height. Keep
// this below 10 so both native kernels always receive a multiplier.
const MAX_CUSTOM_HEADER_MAX_DRAG_RATE = 9;
const MAX_CUSTOM_HEADER_FINISH_DURATION = 60_000;

const DEFAULT_MESSAGES = {
  pullDown: 'Pull down to refresh',
  releaseToRefresh: 'Release to refresh',
  refreshing: 'Refreshing...',
  refreshComplete: 'Refresh complete',
  pullUp: 'Pull up to load more',
  releaseToLoadMore: 'Release to load more',
  loadingMore: 'Loading...',
  noMoreData: 'No more data',
} as const;

function normalizeDelay(delay: number | undefined): number {
  if (delay === undefined || !Number.isFinite(delay)) {
    return 0;
  }

  return Math.max(0, Math.round(delay));
}

function normalizePositiveNumber(
  value: number | undefined,
  fallback: number,
  maximum: number
): number {
  if (value === undefined || !Number.isFinite(value) || value <= 0) {
    return fallback;
  }
  return Math.min(value, maximum);
}

function normalizeNumberAtLeast(
  value: number | undefined,
  fallback: number,
  minimum: number,
  maximum: number
): number {
  if (value === undefined || !Number.isFinite(value) || value < minimum) {
    return fallback;
  }
  return Math.min(value, maximum);
}

function normalizeNonNegativeNumber(
  value: number | undefined,
  fallback: number,
  maximum: number
): number {
  if (value === undefined || !Number.isFinite(value) || value < 0) {
    return fallback;
  }
  return Math.min(Math.round(value), maximum);
}

type OperationKind = 'refresh' | 'load-more';

interface ActiveOperation {
  kind: OperationKind;
  /** 原生事件会回传该 id，完成命令也必须带回它，避免旧异步回调结束新请求。 */
  requestId: number;
  /** 已接受与当前锁匹配的原生请求事件。 */
  started: boolean;
  /** 已发送完成命令；重复事件必须忽略。 */
  finishing: boolean;
}

function normalizeSource(source: string): RefreshRequest['source'] {
  return source === 'programmatic' ? 'programmatic' : 'gesture';
}

export const SmartRefreshLayout = forwardRef<
  SmartRefreshLayoutRef,
  SmartRefreshLayoutProps
>(function SmartRefreshLayout(
  {
    children,
    refreshHeader,
    refreshHeaderHeight,
    refreshHeaderSpinnerStyle = 'translate',
    refreshHeaderTriggerRate,
    refreshHeaderMaxDragRate,
    refreshHeaderFinishDuration,
    refreshEnabled,
    loadMoreEnabled,
    loadMoreMode,
    autoLoadMoreEnabled = false,
    refreshing: controlledRefreshing,
    loadingMore: controlledLoadingMore,
    hasMore = true,
    hapticsEnabled = true,
    headerStyle = 'classic',
    primaryColor,
    indicatorColor,
    titleColor,
    classicSpinnerStyle = 'translate',
    classicEnableLastTime = true,
    materialShowBezierWave = false,
    materialEnableHeaderTranslationContent = false,
    materialProgressBackgroundColor,
    messages,
    onRefresh,
    onLoadMore,
    onRefreshError,
    onLoadMoreError,
    onStateChange,
    onHeaderMoving,
    onHeaderInitialized,
    onHeaderReleased,
    onHeaderStart,
    onHeaderFinish,
    ...viewProps
  },
  forwardedRef
) {
  const nativeRef = useRef<React.ElementRef<typeof NativeSmartRefreshLayout>>(null);
  const [internalRefreshing, setInternalRefreshing] = useState(false);
  const [internalLoadingMore, setInternalLoadingMore] = useState(false);
  const activeOperationRef = useRef<ActiveOperation | null>(null);
  const nextProgrammaticRequestIdRef = useRef(1);
  const previousControlledRefreshingRef = useRef(controlledRefreshing);
  const previousControlledLoadingMoreRef = useRef(controlledLoadingMore);
  const finishTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const hasMoreRef = useRef(hasMore);
  hasMoreRef.current = hasMore;
  const refreshing = controlledRefreshing ?? internalRefreshing;
  const loadingMore = refreshing
    ? false
    : controlledLoadingMore ?? internalLoadingMore;
  /* `loadMoreMode="auto"` 是当前 API；仅在未提供 mode 时回退到已废弃的布尔属性。 */
  const resolvedAutoLoadMoreEnabled =
    loadMoreMode === 'auto' ||
    (loadMoreMode === undefined && autoLoadMoreEnabled);
  const resolvedMessages = { ...DEFAULT_MESSAGES, ...messages };
  const normalizedRefreshHeaderHeight = Math.round(
    normalizePositiveNumber(
      refreshHeaderHeight,
      DEFAULT_CUSTOM_HEADER_HEIGHT,
      MAX_CUSTOM_HEADER_HEIGHT
    )
  );
  const normalizedRefreshHeaderTriggerRate = normalizePositiveNumber(
    refreshHeaderTriggerRate,
    DEFAULT_CUSTOM_HEADER_TRIGGER_RATE,
    MAX_CUSTOM_HEADER_TRIGGER_RATE
  );
  const normalizedRefreshHeaderMaxDragRate = normalizeNumberAtLeast(
    refreshHeaderMaxDragRate,
    DEFAULT_CUSTOM_HEADER_MAX_DRAG_RATE,
    1,
    MAX_CUSTOM_HEADER_MAX_DRAG_RATE
  );
  const normalizedRefreshHeaderFinishDuration = normalizeNonNegativeNumber(
    refreshHeaderFinishDuration,
    DEFAULT_CUSTOM_HEADER_FINISH_DURATION,
    MAX_CUSTOM_HEADER_FINISH_DURATION
  );

  /** 为 JS 发起的命令分配请求 id；上限与 Fabric Int32 事件/命令类型一致。 */
  const allocateProgrammaticRequestId = useCallback(() => {
    const current = nextProgrammaticRequestIdRef.current;
    nextProgrammaticRequestIdRef.current =
      current >= 2_147_483_647 ? 1 : current + 1;
    return current;
  }, []);

  const finishActiveOperation = useCallback(
    (
      kind: OperationKind,
      options: FinishRefreshOptions | FinishLoadMoreOptions = {}
    ) => {
      const active = activeOperationRef.current;
      if (active?.kind !== kind || active.finishing) {
        return;
      }

      /* 先标记完成再发送命令；有延迟时保留请求锁，防止收尾动画期间的过期事件启动回调。 */
      active.finishing = true;
      const delay = normalizeDelay(options.delay);
      const clearOperation = () => {
        if (activeOperationRef.current === active) {
          activeOperationRef.current = null;
        }
        finishTimerRef.current = null;
      };

      if (kind === 'refresh') {
        /* 非受控模式由组件维护 React 状态；受控模式由父组件把 prop 从 true 改为 false，
         * 这里仅通知原生结束同一个请求。 */
        if (controlledRefreshing === undefined) {
          setInternalRefreshing(false);
        }
        if (nativeRef.current) {
          Commands.finishRefresh(
            nativeRef.current,
            active.requestId,
            options.success ?? true,
            delay
          );
        }
      } else {
        /* 回调完成时把 `hasMore` 转换为原生相反含义的 `noMoreData` 标记。 */
        if (controlledLoadingMore === undefined) {
          setInternalLoadingMore(false);
        }
        const nextHasMore =
          (options as FinishLoadMoreOptions).hasMore ?? true;
        if (nativeRef.current) {
          Commands.finishLoadMore(
            nativeRef.current,
            active.requestId,
            options.success ?? true,
            !nextHasMore,
            delay
          );
        }
      }

      if (delay === 0) {
        clearOperation();
      } else {
        finishTimerRef.current = setTimeout(clearOperation, delay);
      }
    },
    [controlledLoadingMore, controlledRefreshing]
  );

  const handleControlledCompletion = useCallback(
    (kind: OperationKind) => {
      const active = activeOperationRef.current;
      if (active?.kind === kind) {
        /* 受控 prop 从 true 降为 false，表示父组件确认异步操作已完成。 */
        finishActiveOperation(
          kind,
          kind === 'load-more' ? { hasMore: hasMoreRef.current } : undefined
        );
      }
    },
    [finishActiveOperation]
  );

  useEffect(() => {
    /* 受控刷新/加载在各自 prop 的下降沿收尾；记录前值可避免初始 false 被误认为完成信号。 */
    if (
      previousControlledRefreshingRef.current === true &&
      controlledRefreshing === false
    ) {
      handleControlledCompletion('refresh');
    }
    previousControlledRefreshingRef.current = controlledRefreshing;
  }, [controlledRefreshing, handleControlledCompletion]);

  useEffect(() => {
    if (
      previousControlledLoadingMoreRef.current === true &&
      controlledLoadingMore === false
    ) {
      handleControlledCompletion('load-more');
    }
    previousControlledLoadingMoreRef.current = controlledLoadingMore;
  }, [controlledLoadingMore, handleControlledCompletion]);

  useEffect(
    () => () => {
      /* 卸载时取消延迟收尾，避免定时器在组件销毁后继续修改状态。 */
      if (finishTimerRef.current !== null) {
        clearTimeout(finishTimerRef.current);
      }
      activeOperationRef.current = null;
    },
    []
  );

  useImperativeHandle(
    forwardedRef,
    () => ({
      beginRefresh: (delay = 0) => {
        /* ref 方法与手势事件共用单请求锁；原生稍后会以该 id 回传请求事件。 */
        if (activeOperationRef.current !== null || !nativeRef.current) {
          return false;
        }
        const requestId = allocateProgrammaticRequestId();
        activeOperationRef.current = {
          kind: 'refresh',
          requestId,
          started: false,
          finishing: false,
        };
        Commands.beginRefresh(
          nativeRef.current,
          requestId,
          normalizeDelay(delay)
        );
        return true;
      },
      finishRefresh: ({ success = true, delay = 0 } = {}) => {
        finishActiveOperation('refresh', { success, delay });
      },
      beginLoadMore: (delay = 0) => {
        /* 最新 `hasMore` 表示已到列表末尾时，不再排队加载请求。 */
        if (
          activeOperationRef.current !== null ||
          !nativeRef.current ||
          !hasMoreRef.current
        ) {
          return false;
        }
        const requestId = allocateProgrammaticRequestId();
        activeOperationRef.current = {
          kind: 'load-more',
          requestId,
          started: false,
          finishing: false,
        };
        Commands.beginLoadMore(
          nativeRef.current,
          requestId,
          normalizeDelay(delay)
        );
        return true;
      },
      finishLoadMore: ({ success = true, hasMore: nextHasMore = true, delay = 0 } = {}) => {
        finishActiveOperation('load-more', {
          success,
          hasMore: nextHasMore,
          delay,
        });
      },
      resetNoMoreData: () => {
        if (nativeRef.current) {
          Commands.resetNoMoreData(nativeRef.current);
        }
      },
    }),
    [
      allocateProgrammaticRequestId,
      controlledLoadingMore,
      controlledRefreshing,
      finishActiveOperation,
    ]
  );

  /* 只有持有当前请求锁且 id 匹配的事件才能调用 JS；过期或属于另一操作的事件会用自身 id 立即收尾。 */
  const handleRefresh = useCallback(async (event: NativeSyntheticEvent<RequestEvent>) => {
    const request: RefreshRequest = {
      requestId: event.nativeEvent.requestId,
      source: normalizeSource(event.nativeEvent.source),
    };
    const current = activeOperationRef.current;
    if (current !== null) {
      if (current.kind !== 'refresh') {
        if (nativeRef.current) {
          Commands.finishRefresh(nativeRef.current, request.requestId, true, 0);
        }
        return;
      }
      if (current.requestId === request.requestId) {
        if (current.started || current.finishing) {
          return;
        }
        current.started = true;
      } else {
        if (nativeRef.current) {
          Commands.finishRefresh(nativeRef.current, request.requestId, true, 0);
        }
        return;
      }
    } else {
      activeOperationRef.current = {
        kind: 'refresh',
        requestId: request.requestId,
        started: true,
        finishing: false,
      };
    }

    /* 非受控模式由组件把请求反映到 React；受控模式要求父组件自行设置 refreshing，
     * 并通过上面的 prop 变化完成收尾。 */
    if (controlledRefreshing === undefined) {
      setInternalRefreshing(true);
    }
    let success = true;
    try {
      await onRefresh?.(request);
    } catch (error) {
      success = false;
      onRefreshError?.(error);
    } finally {
      const active = activeOperationRef.current;
      if (
        active?.kind === 'refresh' &&
        active.requestId === request.requestId &&
        !active.finishing
      ) {
        if (controlledRefreshing === undefined) {
          /* 非受控回调在此负责清理状态并把成功标记发送给原生；受控回调保留锁，
           * 直到父组件关闭 prop。 */
          setInternalRefreshing(false);
          activeOperationRef.current = null;
          if (nativeRef.current) {
            Commands.finishRefresh(nativeRef.current, request.requestId, success, 0);
          }
        }
      }
    }
  }, [controlledRefreshing, onRefresh, onRefreshError]);

  /* 加载更多沿用刷新请求 id 锁；调用回调前先检查 `hasMore`，回调结果决定是否进入无更多数据状态。 */
  const handleLoadMore = useCallback(async (event: NativeSyntheticEvent<RequestEvent>) => {
    const request: RefreshRequest = {
      requestId: event.nativeEvent.requestId,
      source: normalizeSource(event.nativeEvent.source),
    };
    const current = activeOperationRef.current;
    if (current !== null) {
      if (current.kind !== 'load-more') {
        if (nativeRef.current) {
          Commands.finishLoadMore(
            nativeRef.current,
            request.requestId,
            true,
            !hasMoreRef.current,
            0
          );
        }
        return;
      }
      if (current.requestId === request.requestId) {
        if (current.started || current.finishing) {
          return;
        }
        current.started = true;
      } else {
        if (nativeRef.current) {
          Commands.finishLoadMore(
            nativeRef.current,
            request.requestId,
            true,
            !hasMoreRef.current,
            0
          );
        }
        return;
      }
    } else {
      activeOperationRef.current = {
        kind: 'load-more',
        requestId: request.requestId,
        started: true,
        finishing: false,
      };
    }

    if (!hasMoreRef.current) {
      /* 列表到末尾后原生仍可能发出已排队的请求；直接结束它，不调用用户回调也不重新打开 footer。 */
      setInternalLoadingMore(false);
      activeOperationRef.current = null;
      if (nativeRef.current) {
        Commands.finishLoadMore(
          nativeRef.current,
          request.requestId,
          true,
          true,
          0
        );
      }
      return;
    }

    if (controlledLoadingMore === undefined) {
      /* 受控模式由父组件设置并清除 prop；非受控模式在本地镜像加载状态。 */
      setInternalLoadingMore(true);
    }
    let success = true;
    let result: void | LoadMoreResult = undefined;
    try {
      result = await onLoadMore?.(request);
    } catch (error) {
      success = false;
      onLoadMoreError?.(error);
    } finally {
      const active = activeOperationRef.current;
      if (
        active?.kind === 'load-more' &&
        active.requestId === request.requestId &&
        !active.finishing
      ) {
        if (controlledLoadingMore === undefined) {
          /* 优先使用回调返回值，否则读取 ref 中最新的 `hasMore`，避免闭包捕获旧值。 */
          const nextHasMore =
            result && typeof result.hasMore === 'boolean'
              ? result.hasMore
              : hasMoreRef.current;
          setInternalLoadingMore(false);
          activeOperationRef.current = null;
          if (nativeRef.current) {
            Commands.finishLoadMore(
              nativeRef.current,
              request.requestId,
              success,
              !nextHasMore,
              0
            );
          }
        }
      }
    }
  }, [controlledLoadingMore, onLoadMore, onLoadMoreError]);

  const handleStateChange = useCallback(
    (event: NativeSyntheticEvent<StateChangeEvent>) => {
      onStateChange?.(event.nativeEvent.state as RefreshState);
    },
    [onStateChange]
  );

  const handleHeaderMoving = useCallback(
    (event: NativeSyntheticEvent<NativeHeaderMovingEvent>) => {
      onHeaderMoving?.(event.nativeEvent);
    },
    [onHeaderMoving]
  );

  const handleHeaderInitialized = useCallback(
    (event: NativeSyntheticEvent<{ height: number; maxDragHeight: number }>) => {
      onHeaderInitialized?.(event.nativeEvent);
    },
    [onHeaderInitialized]
  );

  const handleHeaderReleased = useCallback(
    (event: NativeSyntheticEvent<{ height: number; maxDragHeight: number }>) => {
      onHeaderReleased?.(event.nativeEvent);
    },
    [onHeaderReleased]
  );

  const handleHeaderStart = useCallback(
    (event: NativeSyntheticEvent<{ height: number; maxDragHeight: number }>) => {
      onHeaderStart?.(event.nativeEvent);
    },
    [onHeaderStart]
  );

  const handleHeaderFinish = useCallback(
    (event: NativeSyntheticEvent<{ success: boolean }>) => {
      onHeaderFinish?.(event.nativeEvent);
    },
    [onHeaderFinish]
  );

  return (
    <NativeSmartRefreshLayout
      {...viewProps}
      ref={nativeRef}
      refreshEnabled={refreshEnabled ?? onRefresh !== undefined}
      loadMoreEnabled={loadMoreEnabled ?? onLoadMore !== undefined}
      autoLoadMoreEnabled={resolvedAutoLoadMoreEnabled}
      refreshing={refreshing}
      loadingMore={loadingMore}
      noMoreData={!hasMore}
      hapticsEnabled={hapticsEnabled}
      headerStyle={headerStyle}
      primaryColor={primaryColor}
      indicatorColor={indicatorColor}
      titleColor={titleColor}
      classicSpinnerStyle={classicSpinnerStyle}
      refreshHeaderHeight={normalizedRefreshHeaderHeight}
      refreshHeaderSpinnerStyle={refreshHeaderSpinnerStyle}
      refreshHeaderTriggerRate={normalizedRefreshHeaderTriggerRate}
      refreshHeaderMaxDragRate={normalizedRefreshHeaderMaxDragRate}
      refreshHeaderFinishDuration={normalizedRefreshHeaderFinishDuration}
      classicEnableLastTime={classicEnableLastTime}
      materialShowBezierWave={materialShowBezierWave}
      materialEnableHeaderTranslationContent={materialEnableHeaderTranslationContent}
      materialProgressBackgroundColor={materialProgressBackgroundColor}
      pullDownText={resolvedMessages.pullDown}
      releaseToRefreshText={resolvedMessages.releaseToRefresh}
      refreshingText={resolvedMessages.refreshing}
      refreshCompleteText={resolvedMessages.refreshComplete}
      pullUpText={resolvedMessages.pullUp}
      releaseToLoadMoreText={resolvedMessages.releaseToLoadMore}
      loadingMoreText={resolvedMessages.loadingMore}
      noMoreDataText={resolvedMessages.noMoreData}
      onRefresh={handleRefresh}
      onLoadMore={handleLoadMore}
      onStateChange={handleStateChange}
      onHeaderMoving={handleHeaderMoving}
      onHeaderInitialized={handleHeaderInitialized}
      onHeaderReleased={handleHeaderReleased}
      onHeaderStart={handleHeaderStart}
      onHeaderFinish={handleHeaderFinish}
    >
      {refreshHeader ? (
        <NativeSmartRefreshHeaderSlot
          collapsable={false}
          style={[styles.headerSlot, { height: normalizedRefreshHeaderHeight }]}
        >
          {refreshHeader}
        </NativeSmartRefreshHeaderSlot>
      ) : null}
      {children}
    </NativeSmartRefreshLayout>
  );
});

SmartRefreshLayout.displayName = 'SmartRefreshLayout';

const styles = StyleSheet.create({
  headerSlot: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    height: 80,
  },
});
