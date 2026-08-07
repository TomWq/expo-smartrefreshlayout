import React, {
  forwardRef,
  useCallback,
  useImperativeHandle,
  useRef,
  useState,
  useEffect,
} from 'react';
import type { NativeSyntheticEvent } from 'react-native';

import NativeSmartRefreshLayout, {
  Commands,
  type RequestEvent,
  type StateChangeEvent,
} from './NativeSmartRefreshLayout';
import type {
  FinishLoadMoreOptions,
  FinishRefreshOptions,
  LoadMoreResult,
  RefreshRequest,
  RefreshState,
  SmartRefreshLayoutProps,
  SmartRefreshLayoutRef,
} from './SmartRefreshLayout.types';

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

type OperationKind = 'refresh' | 'load-more';

interface ActiveOperation {
  kind: OperationKind;
  requestId: number;
  started: boolean;
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
  const resolvedAutoLoadMoreEnabled =
    loadMoreMode === 'auto' ||
    (loadMoreMode === undefined && autoLoadMoreEnabled);
  const resolvedMessages = { ...DEFAULT_MESSAGES, ...messages };

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

      active.finishing = true;
      const delay = normalizeDelay(options.delay);
      const clearOperation = () => {
        if (activeOperationRef.current === active) {
          activeOperationRef.current = null;
        }
        finishTimerRef.current = null;
      };

      if (kind === 'refresh') {
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
        finishActiveOperation(
          kind,
          kind === 'load-more' ? { hasMore: hasMoreRef.current } : undefined
        );
      }
    },
    [finishActiveOperation]
  );

  useEffect(() => {
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
          setInternalRefreshing(false);
          activeOperationRef.current = null;
          if (nativeRef.current) {
            Commands.finishRefresh(nativeRef.current, request.requestId, success, 0);
          }
        }
      }
    }
  }, [controlledRefreshing, onRefresh, onRefreshError]);

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
    >
      {children}
    </NativeSmartRefreshLayout>
  );
});

SmartRefreshLayout.displayName = 'SmartRefreshLayout';
