import type { ReactElement } from 'react';
import type { ColorValue, ViewProps } from 'react-native';

import type { NativeRefreshState } from './NativeSmartRefreshLayout';
import type { RequestSource } from './NativeSmartRefreshLayout';

export type RefreshState = NativeRefreshState;
/** 加载更多触发方式：手动上拉或达到边界后自动触发。 */
export type LoadMoreMode = 'pull' | 'auto';
/** Classic header motion modes supported on Android and iOS. */
export type ClassicSpinnerStyle = 'scale' | 'translate' | 'fixed-behind';

/** Motion modes supported by a React-provided refreshHeader on both platforms. */
export type RefreshHeaderSpinnerStyle = 'scale' | 'translate' | 'fixed-behind';

export interface RefreshRequest {
  /** 与原生请求/完成命令配对的序列号。 */
  requestId: number;
  /** 请求来自用户手势还是 ref 命令。 */
  source: RequestSource;
}

export interface LoadMoreResult {
  /** false 会让原生 footer 进入 no-more-data 状态。 */
  hasMore: boolean;
}

/** Native pull-distance information for a custom refresh header. */
export interface HeaderMovingEvent {
  /** Pull distance relative to the refresh trigger threshold. */
  percent: number;
  /** Current header pull distance in device-independent pixels. */
  offset: number;
  /** Native header height in device-independent pixels. */
  height: number;
  /** Maximum pull distance in device-independent pixels. */
  maxDragHeight: number;
  /** Whether the user is actively dragging the scroll view. */
  isDragging: boolean;
}

export interface HeaderLifecycleEvent {
  /** Header height used by the native refresh kernel, in dp/pt. */
  height: number;
  /** Maximum pull distance reported by the native refresh kernel, in dp/pt. */
  maxDragHeight: number;
}

export interface HeaderFinishEvent {
  /** Result reported by native Header completion, after onFinish/finishDuration begins. */
  success: boolean;
}

export interface RefreshMessages {
  pullDown: string;
  releaseToRefresh: string;
  refreshing: string;
  refreshComplete: string;
  pullUp: string;
  releaseToLoadMore: string;
  loadingMore: string;
  noMoreData: string;
}

export interface SmartRefreshLayoutProps extends ViewProps {
  children: ReactElement;
  /** React content mounted inside the native refresh header. */
  refreshHeader?: ReactElement;
  /** React custom Header's logical height in dp/pt. Defaults to 80. */
  refreshHeaderHeight?: number;
  /** Spinner mode for a React custom Header. Defaults to translate. */
  refreshHeaderSpinnerStyle?: RefreshHeaderSpinnerStyle;
  /** Trigger threshold multiplier for a React custom Header. Valid range: (0, 1]. */
  refreshHeaderTriggerRate?: number;
  /** Maximum pull multiplier for a React custom Header. Valid range: [1, 9]. */
  refreshHeaderMaxDragRate?: number;
  /** Native completion-state dwell/animation duration in milliseconds. */
  refreshHeaderFinishDuration?: number;
  refreshEnabled?: boolean;
  loadMoreEnabled?: boolean;
  loadMoreMode?: LoadMoreMode;
  /** @deprecated Use loadMoreMode="auto" instead. */
  autoLoadMoreEnabled?: boolean;
  refreshing?: boolean;
  loadingMore?: boolean;
  hasMore?: boolean;
  hapticsEnabled?: boolean;
  headerStyle?: 'classic' | 'material';
  /** Primary color for the Classic header/footer and Android Material header. */
  primaryColor?: ColorValue;
  indicatorColor?: ColorValue;
  titleColor?: ColorValue;
  /** Classic header motion: scale, translate, or fixed behind the content. */
  classicSpinnerStyle?: ClassicSpinnerStyle;
  /** Controls the last-refresh-time label on Classic headers. */
  classicEnableLastTime?: boolean;
  /** Android only. Maps to MaterialHeader.setShowBezierWave. */
  materialShowBezierWave?: boolean;
  /**
   * Android only. Controls whether content moves with a Material header.
   * It maps to SmartRefreshLayout.setEnableHeaderTranslationContent.
   */
  materialEnableHeaderTranslationContent?: boolean;
  /** Material progress indicator background color. */
  materialProgressBackgroundColor?: ColorValue;
  messages?: Partial<RefreshMessages>;
  onRefresh?: (request: RefreshRequest) => void | Promise<void>;
  onLoadMore?: (
    request: RefreshRequest
  ) => void | LoadMoreResult | Promise<void | LoadMoreResult>;
  onRefreshError?: (error: unknown) => void;
  onLoadMoreError?: (error: unknown) => void;
  onStateChange?: (state: RefreshState) => void;
  onHeaderMoving?: (event: HeaderMovingEvent) => void;
  onHeaderInitialized?: (event: HeaderLifecycleEvent) => void;
  onHeaderReleased?: (event: HeaderLifecycleEvent) => void;
  onHeaderStart?: (event: HeaderLifecycleEvent) => void;
  onHeaderFinish?: (event: HeaderFinishEvent) => void;
}

export interface FinishRefreshOptions {
  /** 原生完成动画显示成功或失败状态。 */
  success?: boolean;
  /** 延迟清除请求锁和结束动画的毫秒数。 */
  delay?: number;
}

export interface FinishLoadMoreOptions extends FinishRefreshOptions {
  /** 本次结果是否仍有下一页数据。 */
  hasMore?: boolean;
}

export interface SmartRefreshLayoutRef {
  beginRefresh: (delay?: number) => boolean;
  finishRefresh: (options?: FinishRefreshOptions) => void;
  beginLoadMore: (delay?: number) => boolean;
  finishLoadMore: (options?: FinishLoadMoreOptions) => void;
  resetNoMoreData: () => void;
}
