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

export type NativeSecondFloorState =
  | 'idle'
  | 'pulling'
  | 'ready'
  | 'refreshing'
  | 'release-to-second-floor'
  | 'second-floor-opening'
  | 'second-floor'
  | 'second-floor-closing';

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
  refreshing?: CodegenTypes.WithDefault<boolean, false>;
  hapticsEnabled?: CodegenTypes.WithDefault<boolean, true>;
  secondFloorEnabled?: CodegenTypes.WithDefault<boolean, true>;
  headerInset?: CodegenTypes.WithDefault<CodegenTypes.Int32, 0>;
  floorRate?: CodegenTypes.WithDefault<CodegenTypes.Float, 1.9>;
  maxRate?: CodegenTypes.WithDefault<CodegenTypes.Float, 2.5>;
  refreshRate?: CodegenTypes.WithDefault<CodegenTypes.Float, 1>;
  floorDuration?: CodegenTypes.WithDefault<CodegenTypes.Int32, 1000>;
  pullToCloseEnabled?: CodegenTypes.WithDefault<boolean, true>;
  bottomPullUpToCloseRate?: CodegenTypes.WithDefault<
    CodegenTypes.Float,
    0.16666667
  >;
  primaryColor?: ColorValue;
  indicatorColor?: ColorValue;
  titleColor?: ColorValue;
  classicEnableLastTime?: CodegenTypes.WithDefault<boolean, true>;
  pullDownText?: string;
  releaseToRefreshText?: string;
  refreshingText?: string;
  refreshCompleteText?: string;
  onRefresh?: CodegenTypes.DirectEventHandler<RequestEvent>;
  onStateChange?: CodegenTypes.DirectEventHandler<StateChangeEvent>;
  onSecondFloorOpen?: CodegenTypes.DirectEventHandler<null>;
  onSecondFloorClose?: CodegenTypes.DirectEventHandler<null>;
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
  openSecondFloor: (viewRef: React.ElementRef<NativeComponent>) => void;
  closeSecondFloor: (viewRef: React.ElementRef<NativeComponent>) => void;
}

export const Commands = codegenNativeCommands<NativeCommands>({
  supportedCommands: [
    'beginRefresh',
    'finishRefresh',
    'openSecondFloor',
    'closeSecondFloor',
  ],
});

export default codegenNativeComponent<NativeProps>(
  'ExpoSmartSecondFloorLayoutView',
  { excludedPlatforms: ['iOS'] }
);
