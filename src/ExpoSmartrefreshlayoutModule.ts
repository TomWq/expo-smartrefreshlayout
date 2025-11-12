import { NativeModule, requireNativeModule } from 'expo';

import { ExpoSmartrefreshlayoutModuleEvents } from './ExpoSmartrefreshlayout.types';

declare class ExpoSmartrefreshlayoutModule extends NativeModule<ExpoSmartrefreshlayoutModuleEvents> {
 
}

// This call loads the native module object from the JSI.
export default requireNativeModule<ExpoSmartrefreshlayoutModule>('ExpoSmartrefreshlayout');
