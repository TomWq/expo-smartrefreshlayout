import type { ReactElement } from 'react';
import type { ColorValue, ViewProps } from 'react-native';

import type {
  NativeSecondFloorState,
  RequestSource,
} from './NativeSmartSecondFloorLayout';

export type SecondFloorState = NativeSecondFloorState;

export interface SecondFloorRefreshRequest {
  /** 与原生事件及 finishRefresh 命令配对的序列号。 */
  requestId: number;
  /** 请求来源，便于区分手势刷新和程序化刷新。 */
  source: RequestSource;
}

export interface SecondFloorMessages {
  pullDown: string;
  releaseToRefresh: string;
  refreshing: string;
  refreshComplete: string;
}

export interface SmartSecondFloorLayoutProps extends ViewProps {
  /** The normal page content shown before and after the second floor opens. */
  children: ReactElement;
  /**
   * The full-screen formal content mounted inside TwoLevelHeader. When no
   * backdrop is supplied it keeps the legacy single-floor-slot behavior.
   */
  secondFloor: ReactElement;
  /**
   * Optional revealed backdrop behind secondFloor. Supplying it enables the
   * official two-layer floor composition and native formal-content fade.
   */
  secondFloorBackground?: ReactElement;
  refreshEnabled?: boolean;
  refreshing?: boolean;
  hapticsEnabled?: boolean;
  secondFloorEnabled?: boolean;
  /**
   * Top inset reserved above the Classic refresh header, expressed in logical
   * layout points. Use this when an overlay toolbar covers the layout.
   */
  headerInset?: number;
  floorRate?: number;
  maxRate?: number;
  refreshRate?: number;
  floorDuration?: number;
  pullToCloseEnabled?: boolean;
  bottomPullUpToCloseRate?: number;
  primaryColor?: ColorValue;
  indicatorColor?: ColorValue;
  titleColor?: ColorValue;
  classicEnableLastTime?: boolean;
  messages?: Partial<SecondFloorMessages>;
  onRefresh?: (request: SecondFloorRefreshRequest) => void | Promise<void>;
  onRefreshError?: (error: unknown) => void;
  onStateChange?: (state: SecondFloorState) => void;
  onSecondFloorOpen?: () => void;
  onSecondFloorClose?: () => void;
}

export interface FinishSecondFloorRefreshOptions {
  /** 原生刷新头显示成功或失败结果。 */
  success?: boolean;
  /** 延迟释放刷新请求锁的毫秒数。 */
  delay?: number;
}

export interface SmartSecondFloorLayoutRef {
  /**
   * Queues a programmatic refresh when the native host is idle. Returns false
   * when the host is unavailable, a refresh is active, or the second floor is
   * open/opening/closing.
   */
  beginRefresh: (delay?: number) => boolean;
  finishRefresh: (options?: FinishSecondFloorRefreshOptions) => void;
  /**
   * Returns true once the command was dispatched to an idle mounted native
   * host. It does not wait for the native opening animation to complete.
   */
  openSecondFloor: () => boolean;
  /**
   * Returns true once the command was dispatched to an opening/open second
   * floor. It does not wait for the native closing animation to complete.
   */
  closeSecondFloor: () => boolean;
}
