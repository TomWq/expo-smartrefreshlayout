import type { HostComponent, ViewProps } from 'react-native';
import { codegenNativeComponent } from 'react-native';

export interface NativeProps extends ViewProps {}

type NativeComponent = HostComponent<NativeProps>;

/**
 * A distinct Fabric host lets the native refresh container identify and reparent
 * custom header content without relying on React child order.
 */
export default codegenNativeComponent<NativeProps>(
  'ExpoSmartRefreshHeaderSlot'
) as NativeComponent;
