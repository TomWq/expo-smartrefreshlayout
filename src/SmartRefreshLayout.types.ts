import type { ReactElement } from 'react';
import type { ColorValue, ViewProps } from 'react-native';

import type { NativeRefreshState } from './NativeSmartRefreshLayout';
import type { RequestSource } from './NativeSmartRefreshLayout';

export type RefreshState = NativeRefreshState;
export type LoadMoreMode = 'pull' | 'auto';
/** Classic header motion modes supported on Android and iOS. */
export type ClassicSpinnerStyle = 'scale' | 'translate' | 'fixed-behind';

export interface RefreshRequest {
  requestId: number;
  source: RequestSource;
}

export interface LoadMoreResult {
  hasMore: boolean;
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
}

export interface FinishRefreshOptions {
  success?: boolean;
  delay?: number;
}

export interface FinishLoadMoreOptions extends FinishRefreshOptions {
  hasMore?: boolean;
}

export interface SmartRefreshLayoutRef {
  beginRefresh: (delay?: number) => boolean;
  finishRefresh: (options?: FinishRefreshOptions) => void;
  beginLoadMore: (delay?: number) => boolean;
  finishLoadMore: (options?: FinishLoadMoreOptions) => void;
  resetNoMoreData: () => void;
}
