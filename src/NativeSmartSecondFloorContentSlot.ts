import type { HostComponent, ViewProps } from 'react-native';
import { codegenNativeComponent } from 'react-native';

export interface NativeProps extends ViewProps {}

type NativeComponent = HostComponent<NativeProps>;

export default codegenNativeComponent<NativeProps>(
  'ExpoSmartSecondFloorContentSlot'
) as NativeComponent;
