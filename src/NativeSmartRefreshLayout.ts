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

export type RequestSource = 'gesture' | 'programmatic';

export type RequestEvent = Readonly<{
  requestId: CodegenTypes.Int32;
  source: string;
}>;

export type StateChangeEvent = Readonly<{
  state: string;
}>;

export interface NativeProps extends ViewProps {
  refreshEnabled?: CodegenTypes.WithDefault<boolean, true>;
  loadMoreEnabled?: CodegenTypes.WithDefault<boolean, false>;
  autoLoadMoreEnabled?: CodegenTypes.WithDefault<boolean, false>;
  refreshing?: CodegenTypes.WithDefault<boolean, false>;
  loadingMore?: CodegenTypes.WithDefault<boolean, false>;
  noMoreData?: CodegenTypes.WithDefault<boolean, false>;
  hapticsEnabled?: CodegenTypes.WithDefault<boolean, true>;
  headerStyle?: CodegenTypes.WithDefault<'classic' | 'material', 'classic'>;
  indicatorColor?: ColorValue;
  titleColor?: ColorValue;
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
}

type NativeComponent = HostComponent<NativeProps>;

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

export default codegenNativeComponent<NativeProps>(
  'ExpoSmartRefreshLayoutView'
);
