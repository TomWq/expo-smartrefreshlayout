import type {
  CodegenTypes,
  ColorValue,
  HostComponent,
  ViewProps,
} from 'react-native';
import {
  codegenNativeCommands,
  codegenNativeComponent,
} from 'react-native';

export type NativeRefreshState =
  | 'idle'
  | 'pulling'
  | 'ready'
  | 'refreshing'
  | 'loading'
  | 'no-more-data';

// 原生事件使用 requestId 与 JS 完成命令配对；source 区分手势和 ref 调用。
export type RequestSource = 'gesture' | 'programmatic';

export type RequestEvent = Readonly<{
  requestId: CodegenTypes.Int32;
  source: string;
}>;

// 状态事件保留字符串形态，由上层组件负责归一化到公开状态联合类型。
export type StateChangeEvent = Readonly<{
  state: string;
}>;

export type HeaderMovingEvent = Readonly<{
  percent: CodegenTypes.Float;
  offset: CodegenTypes.Int32;
  height: CodegenTypes.Int32;
  maxDragHeight: CodegenTypes.Int32;
  isDragging: boolean;
}>;

export interface NativeProps extends ViewProps {
  // Fabric WithDefault 会在原生侧补默认值，避免 JS 与 Android/iOS 默认配置分叉。
  refreshEnabled?: CodegenTypes.WithDefault<boolean, true>;
  loadMoreEnabled?: CodegenTypes.WithDefault<boolean, false>;
  autoLoadMoreEnabled?: CodegenTypes.WithDefault<boolean, false>;
  refreshing?: CodegenTypes.WithDefault<boolean, false>;
  loadingMore?: CodegenTypes.WithDefault<boolean, false>;
  noMoreData?: CodegenTypes.WithDefault<boolean, false>;
  hapticsEnabled?: CodegenTypes.WithDefault<boolean, true>;
  headerStyle?: CodegenTypes.WithDefault<'classic' | 'material', 'classic'>;
  primaryColor?: ColorValue;
  indicatorColor?: ColorValue;
  titleColor?: ColorValue;
  classicSpinnerStyle?: CodegenTypes.WithDefault<
    'scale' | 'translate' | 'fixed-behind',
    'translate'
  >;
  classicEnableLastTime?: CodegenTypes.WithDefault<boolean, true>;
  materialShowBezierWave?: CodegenTypes.WithDefault<boolean, false>;
  materialEnableHeaderTranslationContent?: CodegenTypes.WithDefault<boolean, false>;
  materialProgressBackgroundColor?: ColorValue;
  pullDownText?: string;
  releaseToRefreshText?: string;
  refreshingText?: string;
  refreshCompleteText?: string;
  pullUpText?: string;
  releaseToLoadMoreText?: string;
  loadingMoreText?: string;
  noMoreDataText?: string;
  onRefresh?: CodegenTypes.DirectEventHandler<RequestEvent>;
  onLoadMore?: CodegenTypes.DirectEventHandler<RequestEvent>;
  onStateChange?: CodegenTypes.DirectEventHandler<StateChangeEvent>;
  onHeaderMoving?: CodegenTypes.DirectEventHandler<HeaderMovingEvent>;
}

type NativeComponent = HostComponent<NativeProps>;

// 命令参数必须包含 requestId；原生据此丢弃过期的完成命令。
interface NativeCommands {
  beginRefresh: (
    viewRef: React.ElementRef<NativeComponent>,
    requestId: CodegenTypes.Int32,
    delayMs: CodegenTypes.Int32
  ) => void;
  finishRefresh: (
    viewRef: React.ElementRef<NativeComponent>,
    requestId: CodegenTypes.Int32,
    success: boolean,
    delayMs: CodegenTypes.Int32
  ) => void;
  beginLoadMore: (
    viewRef: React.ElementRef<NativeComponent>,
    requestId: CodegenTypes.Int32,
    delayMs: CodegenTypes.Int32
  ) => void;
  finishLoadMore: (
    viewRef: React.ElementRef<NativeComponent>,
    requestId: CodegenTypes.Int32,
    success: boolean,
    noMoreData: boolean,
    delayMs: CodegenTypes.Int32
  ) => void;
  resetNoMoreData: (viewRef: React.ElementRef<NativeComponent>) => void;
}

export const Commands = codegenNativeCommands<NativeCommands>({
  supportedCommands: [
    'beginRefresh',
    'finishRefresh',
    'beginLoadMore',
    'finishLoadMore',
    'resetNoMoreData',
  ],
});

// Fabric 原生组件声明；事件和命令由 codegen 生成跨平台调用封装。
export default codegenNativeComponent<NativeProps>(
  'ExpoSmartRefreshLayoutView'
);
