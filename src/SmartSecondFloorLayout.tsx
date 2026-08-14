import React, {
  forwardRef,
  useCallback,
  useEffect,
  useImperativeHandle,
  useRef,
  useState,
} from 'react';
import { StyleSheet, type NativeSyntheticEvent } from 'react-native';

import NativeSmartSecondFloorContentSlot from './NativeSmartSecondFloorContentSlot';
import NativeSmartSecondFloorFloorContentSlot from './NativeSmartSecondFloorFloorContentSlot';
import NativeSmartSecondFloorFloorSlot from './NativeSmartSecondFloorFloorSlot';
import NativeSmartSecondFloorLayout, {
  Commands,
  type NativeSecondFloorState,
  type RequestEvent,
  type StateChangeEvent,
} from './NativeSmartSecondFloorLayout';
import type {
  FinishSecondFloorRefreshOptions,
  SecondFloorRefreshRequest,
  SecondFloorState,
  SmartSecondFloorLayoutProps,
  SmartSecondFloorLayoutRef,
} from './SmartSecondFloorLayout.types';

const DEFAULT_MESSAGES = {
  pullDown: 'Pull down to refresh',
  releaseToRefresh: 'Release to refresh',
  refreshing: 'Refreshing...',
  refreshComplete: 'Refresh complete',
  pullToSecondFloor: 'Pull down to enter second floor',
  releaseToSecondFloor: 'Release to enter second floor',
} as const;

const MIN_MAX_RATE = 1.2;
const MAX_MAX_RATE = 5;
const MIN_FLOOR_RATE = 1.1;
const MIN_REFRESH_RATE = 0.25;
const RATE_GAP = 0.05;
const MIN_BOTTOM_PULL_UP_TO_CLOSE_RATE = 0.01;
const MAX_BOTTOM_PULL_UP_TO_CLOSE_RATE = 0.5;
const MAX_FLOOR_DURATION = 10_000;
const MAX_HEADER_INSET = 10_000;
const MIN_TITLE_TEXT_SIZE = 8;
const MAX_TITLE_TEXT_SIZE = 40;

type RefreshOperation = {
  /** 原生请求事件回传的 id；完成命令必须匹配它。 */
  requestId: number;
  /** 是否已接收匹配事件，防止同一请求重复触发回调。 */
  started: boolean;
  /** 已发出收尾命令；延迟期间忽略过期事件。 */
  finishing: boolean;
};

type NormalizedConfiguration = {
  maxRate: number;
  floorRate: number;
  refreshRate: number;
  floorDuration: number;
  bottomPullUpToCloseRate: number;
  headerInset: number;
};

function clamp(value: number, minimum: number, maximum: number): number {
  return Math.min(Math.max(value, minimum), maximum);
}

function finiteOr(value: number | undefined, fallback: number): number {
  return value === undefined || !Number.isFinite(value) ? fallback : value;
}

function normalizeDelay(delay: number | undefined): number {
  return Math.max(0, Math.round(finiteOr(delay, 0)));
}

function normalizeConfiguration({
  maxRate,
  floorRate,
  refreshRate,
  floorDuration,
  bottomPullUpToCloseRate,
  headerInset,
}: Pick<
  SmartSecondFloorLayoutProps,
  | 'maxRate'
  | 'floorRate'
  | 'refreshRate'
  | 'floorDuration'
  | 'bottomPullUpToCloseRate'
  | 'headerInset'
>): NormalizedConfiguration {
  /* 二楼组件依赖多个有序比例。先限制 maxRate，再用它约束 floorRate，
   * 最后约束 refreshRate，确保三者始终保持安全间隔。 */
  const normalizedMaxRate = clamp(finiteOr(maxRate, 2.5), MIN_MAX_RATE, MAX_MAX_RATE);
  const normalizedFloorRate = clamp(
    finiteOr(floorRate, 1.9),
    MIN_FLOOR_RATE,
    normalizedMaxRate - RATE_GAP
  );
  const normalizedRefreshRate = clamp(
    finiteOr(refreshRate, 1),
    MIN_REFRESH_RATE,
    normalizedFloorRate - RATE_GAP
  );

  return {
    maxRate: normalizedMaxRate,
    floorRate: normalizedFloorRate,
    refreshRate: normalizedRefreshRate,
    /* 原生动画时长和顶部 inset 也限制在有限范围，避免异常输入传入 Fabric。 */
    floorDuration: clamp(
      Math.round(finiteOr(floorDuration, 1000)),
      0,
      MAX_FLOOR_DURATION
    ),
    bottomPullUpToCloseRate: clamp(
      finiteOr(bottomPullUpToCloseRate, 1 / 6),
      MIN_BOTTOM_PULL_UP_TO_CLOSE_RATE,
      MAX_BOTTOM_PULL_UP_TO_CLOSE_RATE
    ),
    headerInset: clamp(
      Math.round(finiteOr(headerInset, 0)),
      0,
      MAX_HEADER_INSET
    ),
  };
}

function normalizeSource(source: string): SecondFloorRefreshRequest['source'] {
  return source === 'programmatic' ? 'programmatic' : 'gesture';
}

function isSecondFloorActive(state: SecondFloorState): boolean {
  // opening/open/closing 状态与普通刷新互斥。
  return state !== 'idle' && state !== 'pulling' && state !== 'ready' && state !== 'refreshing';
}

function toSecondFloorState(state: string): SecondFloorState {
  // Fabric 事件携带字符串；未知值降级为空闲，避免把非法状态扩散给业务层。
  const knownStates: NativeSecondFloorState[] = [
    'idle',
    'pulling',
    'ready',
    'refreshing',
    'release-to-second-floor',
    'second-floor-opening',
    'second-floor',
    'second-floor-closing',
  ];
  return knownStates.includes(state as NativeSecondFloorState)
    ? (state as SecondFloorState)
    : 'idle';
}

export const SmartSecondFloorLayout = forwardRef<
  SmartSecondFloorLayoutRef,
  SmartSecondFloorLayoutProps
>(function SmartSecondFloorLayout(
  {
    children,
    secondFloor,
    secondFloorBackground,
    refreshEnabled,
    refreshing: controlledRefreshing,
    hapticsEnabled = true,
    secondFloorEnabled = true,
    headerInset,
    floorRate,
    maxRate,
    refreshRate,
    floorDuration,
    pullToCloseEnabled = true,
    bottomPullUpToCloseRate,
    primaryColor,
    indicatorColor,
    titleColor,
    titleTextSize: requestedTitleTextSize,
    classicEnableLastTime = true,
    messages,
    onRefresh,
    onRefreshError,
    onStateChange,
    onSecondFloorOpen,
    onSecondFloorClose,
    ...viewProps
  },
  forwardedRef
) {
  const nativeRef = useRef<React.ElementRef<typeof NativeSmartSecondFloorLayout>>(null);
  const [internalRefreshing, setInternalRefreshing] = useState(false);
  const refreshOperationRef = useRef<RefreshOperation | null>(null);
  const nextProgrammaticRequestIdRef = useRef(1);
  const previousControlledRefreshingRef = useRef(controlledRefreshing);
  const finishTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const mountedRef = useRef(false);
  const secondFloorStateRef = useRef<SecondFloorState>('idle');
  const refreshing = controlledRefreshing ?? internalRefreshing;
  const resolvedMessages = { ...DEFAULT_MESSAGES, ...messages };
  const titleTextSize = clamp(
    finiteOr(requestedTitleTextSize, 15),
    MIN_TITLE_TEXT_SIZE,
    MAX_TITLE_TEXT_SIZE
  );
  const configuration = normalizeConfiguration({
    maxRate,
    floorRate,
    refreshRate,
    floorDuration,
    bottomPullUpToCloseRate,
    headerInset,
  });

  /** 为 JS 发起的刷新命令分配 Fabric Int32 范围内的请求 id。 */
  const allocateProgrammaticRequestId = useCallback(() => {
    const current = nextProgrammaticRequestIdRef.current;
    nextProgrammaticRequestIdRef.current =
      current >= 2_147_483_647 ? 1 : current + 1;
    return current;
  }, []);

  const finishRefreshOperation = useCallback(
    (options: FinishSecondFloorRefreshOptions = {}) => {
      const operation = refreshOperationRef.current;
      if (operation === null || operation.finishing) {
        return;
      }

      /* 先锁定再发送完成命令；delay > 0 时保持锁到动画结束，防止旧命令收尾新请求。 */
      operation.finishing = true;
      const delay = normalizeDelay(options.delay);
      const clearOperation = () => {
        if (refreshOperationRef.current === operation) {
          refreshOperationRef.current = null;
        }
        finishTimerRef.current = null;
      };

      /* 非受控模式由本组件清除状态；受控模式等待父组件关闭 refreshing prop。 */
      if (controlledRefreshing === undefined) {
        setInternalRefreshing(false);
      }
      if (nativeRef.current) {
        Commands.finishRefresh(
          nativeRef.current,
          operation.requestId,
          options.success ?? true,
          delay
        );
      }

      if (delay === 0) {
        clearOperation();
      } else {
        finishTimerRef.current = setTimeout(clearOperation, delay);
      }
    },
    [controlledRefreshing]
  );

  useEffect(() => {
    /* 受控刷新在 true -> false 的下降沿完成，初始 false 不会触发收尾。 */
    if (
      previousControlledRefreshingRef.current === true &&
      controlledRefreshing === false
    ) {
      finishRefreshOperation();
    }
    previousControlledRefreshingRef.current = controlledRefreshing;
  }, [controlledRefreshing, finishRefreshOperation]);

  useEffect(() => {
    mountedRef.current = true;
    return () => {
      /* 清理延迟收尾和本地状态锁，避免卸载后的异步回调影响新实例。 */
      mountedRef.current = false;
      if (finishTimerRef.current !== null) {
        clearTimeout(finishTimerRef.current);
      }
      refreshOperationRef.current = null;
      secondFloorStateRef.current = 'idle';
    };
  }, []);

  useImperativeHandle(
    forwardedRef,
    () => ({
      beginRefresh: (delay = 0) => {
        /* 刷新请求与二楼展开互斥；命令成功派发后才返回 true。 */
        if (
          !mountedRef.current ||
          refreshOperationRef.current !== null ||
          nativeRef.current === null ||
          isSecondFloorActive(secondFloorStateRef.current)
        ) {
          return false;
        }
        const requestId = allocateProgrammaticRequestId();
        refreshOperationRef.current = {
          requestId,
          started: false,
          finishing: false,
        };
        Commands.beginRefresh(nativeRef.current, requestId, normalizeDelay(delay));
        return true;
      },
      finishRefresh: (options = {}) => {
        finishRefreshOperation(options);
      },
      openSecondFloor: () => {
        /* 只有挂载且空闲时允许展开，展开中状态立即写入本地锁。 */
        if (
          !mountedRef.current ||
          nativeRef.current === null ||
          refreshOperationRef.current !== null ||
          isSecondFloorActive(secondFloorStateRef.current) ||
          !secondFloorEnabled
        ) {
          return false;
        }
        secondFloorStateRef.current = 'second-floor-opening';
        Commands.openSecondFloor(nativeRef.current);
        return true;
      },
      closeSecondFloor: () => {
        /* 仅允许从已展开/展开中的状态关闭，避免重复派发关闭命令。 */
        if (
          !mountedRef.current ||
          nativeRef.current === null ||
          !isSecondFloorActive(secondFloorStateRef.current) ||
          secondFloorStateRef.current === 'second-floor-closing'
        ) {
          return false;
        }
        secondFloorStateRef.current = 'second-floor-closing';
        Commands.closeSecondFloor(nativeRef.current);
        return true;
      },
    }),
    [allocateProgrammaticRequestId, finishRefreshOperation, secondFloorEnabled]
  );

  const handleRefresh = useCallback(
    async (event: NativeSyntheticEvent<RequestEvent>) => {
      /* 二楼打开、展开或关闭期间不接受普通刷新事件。 */
      if (isSecondFloorActive(secondFloorStateRef.current)) {
        return;
      }

      const request: SecondFloorRefreshRequest = {
        requestId: event.nativeEvent.requestId,
        source: normalizeSource(event.nativeEvent.source),
      };
      const current = refreshOperationRef.current;
      if (current !== null) {
        if (current.requestId !== request.requestId || current.started || current.finishing) {
          // id 不匹配表示过期事件，立即结束它，不让它调用业务回调。
          if (current.requestId !== request.requestId && nativeRef.current) {
            Commands.finishRefresh(nativeRef.current, request.requestId, true, 0);
          }
          return;
        }
        current.started = true;
      } else {
        refreshOperationRef.current = {
          requestId: request.requestId,
          started: true,
          finishing: false,
        };
      }

      if (controlledRefreshing === undefined) {
        // 非受控模式由组件反映 refreshing；受控模式由父组件提供该状态。
        setInternalRefreshing(true);
      }

      let success = true;
      try {
        await onRefresh?.(request);
      } catch (error) {
        success = false;
        onRefreshError?.(error);
      } finally {
        const operation = refreshOperationRef.current;
        if (
          operation?.requestId === request.requestId &&
          !operation.finishing &&
          controlledRefreshing === undefined
        ) {
          /* 非受控回调在 finally 中发送成功/失败结果；受控模式保留操作锁，
           * 等待父组件把 refreshing 设为 false。 */
          setInternalRefreshing(false);
          refreshOperationRef.current = null;
          if (nativeRef.current) {
            Commands.finishRefresh(nativeRef.current, request.requestId, success, 0);
          }
        }
      }
    },
    [controlledRefreshing, onRefresh, onRefreshError]
  );

  const handleStateChange = useCallback(
    (event: NativeSyntheticEvent<StateChangeEvent>) => {
      // 将原生字符串归一化后再暴露给业务，保证状态属于公开联合类型。
      const state = toSecondFloorState(event.nativeEvent.state);
      secondFloorStateRef.current = state;
      onStateChange?.(state);
    },
    [onStateChange]
  );

  const handleSecondFloorOpen = useCallback(() => {
    // 原生动画完成后才把本地状态推进为已展开。
    secondFloorStateRef.current = 'second-floor';
    onSecondFloorOpen?.();
  }, [onSecondFloorOpen]);

  const handleSecondFloorClose = useCallback(() => {
    // 原生关闭完成后恢复空闲，解除与刷新的互斥。
    secondFloorStateRef.current = 'idle';
    onSecondFloorClose?.();
  }, [onSecondFloorClose]);

  return (
    <NativeSmartSecondFloorLayout
      {...viewProps}
      ref={nativeRef}
      refreshEnabled={refreshEnabled ?? onRefresh !== undefined}
      refreshing={refreshing}
      hapticsEnabled={hapticsEnabled}
      secondFloorEnabled={secondFloorEnabled}
      headerInset={configuration.headerInset}
      maxRate={configuration.maxRate}
      floorRate={configuration.floorRate}
      refreshRate={configuration.refreshRate}
      floorDuration={configuration.floorDuration}
      pullToCloseEnabled={pullToCloseEnabled}
      bottomPullUpToCloseRate={configuration.bottomPullUpToCloseRate}
      primaryColor={primaryColor}
      indicatorColor={indicatorColor}
      titleColor={titleColor}
      titleTextSize={titleTextSize}
      classicEnableLastTime={classicEnableLastTime}
      pullDownText={resolvedMessages.pullDown}
      releaseToRefreshText={resolvedMessages.releaseToRefresh}
      refreshingText={resolvedMessages.refreshing}
      refreshCompleteText={resolvedMessages.refreshComplete}
      pullToSecondFloorText={resolvedMessages.pullToSecondFloor}
      releaseToSecondFloorText={resolvedMessages.releaseToSecondFloor}
      onRefresh={handleRefresh}
      onStateChange={handleStateChange}
      onSecondFloorOpen={handleSecondFloorOpen}
      onSecondFloorClose={handleSecondFloorClose}
    >
      <NativeSmartSecondFloorContentSlot
        collapsable={false}
        style={styles.contentSlot}
      >
        {children}
      </NativeSmartSecondFloorContentSlot>
      <NativeSmartSecondFloorFloorSlot
        collapsable={false}
        style={styles.floorSlot}
      >
        {secondFloorBackground ?? secondFloor}
      </NativeSmartSecondFloorFloorSlot>
      {secondFloorBackground ? (
        <NativeSmartSecondFloorFloorContentSlot
          collapsable={false}
          style={styles.floorSlot}
        >
          {secondFloor}
        </NativeSmartSecondFloorFloorContentSlot>
      ) : null}
    </NativeSmartSecondFloorLayout>
  );
});

SmartSecondFloorLayout.displayName = 'SmartSecondFloorLayout';

const styles = StyleSheet.create({
  contentSlot: {
    flex: 1,
  },
  floorSlot: {
    ...StyleSheet.absoluteFillObject,
  },
});
