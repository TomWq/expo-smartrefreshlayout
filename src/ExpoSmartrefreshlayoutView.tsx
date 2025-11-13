/*
 * @Author       : 尚博信_王强 wangqiang03@sunboxsoft.com
 * @Date         : 2025-11-12 10:14:48
 * @LastEditors  : 尚博信_王强 wangqiang03@sunboxsoft.com
 * @LastEditTime : 2025-11-13 11:10:11
 * @FilePath     : /expo-smartrefreshlayout/src/ExpoSmartrefreshlayoutView.tsx
 * @Description  : 
 * 
 * Copyright (c) 2025 by 尚博信_王强, All Rights Reserved. 
 */
import { requireNativeView } from 'expo';
import * as React from 'react';
import { View } from 'react-native';

import { ExpoSmartrefreshlayoutViewProps, onHeaderMoveProps, RefreshState } from './ExpoSmartrefreshlayout.types';

// 原生视图的 Props 类型（原生层传递的是对象格式）
type NativeViewProps = Omit<ExpoSmartrefreshlayoutViewProps, 'onStateChanged' | 'onHeaderMoving' | 'onFooterMoving' | 'renderHeader' | 'renderFooter'> & {
 
  /**
   * 状态改变回调
   * @param state - 当前状态
   * @note iOS 当前仅在 finishRefresh 和 finishLoadMore 中触发，未监听 MJRefresh 的完整状态变化
   */
  onStateChanged?: (event: { nativeEvent: { state: RefreshState } }) => void;
   /**
   * Header 移动回调
   * @platform android
   */
  onHeaderMoving?: (event: { nativeEvent: onHeaderMoveProps }) => void;
    /**
   * Footer 移动回调
   * @platform android
   * iOS 当前未实现，需要监听 scrollView 的 contentOffset
   */
  onFooterMoving?: (event: { nativeEvent: onHeaderMoveProps }) => void;
  /**
   * 自定义 Header
   * @platform android
   */
  renderHeader?: () => React.ReactNode;
  /**
   * 自定义 Footer
   * @platform android
   */
  renderFooter?: () => React.ReactNode;
  /**
   * 是否有自定义 Header
   * @platform android
   */
  hasCustomHeader?: boolean;
  /**
   * 是否有自定义 Footer
   * @platform android
   */
  hasCustomFooter?: boolean;
};

const NativeView: React.ComponentType<NativeViewProps> =
  requireNativeView('ExpoSmartrefreshlayout');

export default function ExpoSmartrefreshlayoutView(props: ExpoSmartrefreshlayoutViewProps) {
  const { onStateChanged, onHeaderMoving, onFooterMoving, renderHeader, children, ...restProps } = props;
  
  // 包装 onStateChanged 事件，从原生的 event.nativeEvent.state 提取 RefreshState 值
  const handleStateChanged = React.useCallback((event: any) => {
    if (onStateChanged) {
      const stateValue = event.nativeEvent?.state;
      if (stateValue !== undefined) {
        onStateChanged(stateValue);
      }
    }
  }, [onStateChanged]);

  // 包装 onHeaderMoving 事件
  const handleHeaderMoving = React.useCallback((event: any) => {
    if (onHeaderMoving) {
      const params = event.nativeEvent;
      if (params) {
        onHeaderMoving(params);
      }
    }
  }, [onHeaderMoving]);

  // 包装 onFooterMoving 事件
  const handleFooterMoving = React.useCallback((event: any) => {
    if (onFooterMoving) {
      const params = event.nativeEvent;
      if (params) {
        onFooterMoving(params);
      }
    }
  }, [onFooterMoving]);

  return (
    <NativeView 
      {...restProps}
      hasCustomHeader={!!renderHeader}
      hasCustomFooter={false}
      onStateChanged={onStateChanged ? handleStateChanged : undefined}
      onHeaderMoving={onHeaderMoving ? handleHeaderMoving : undefined}
      onFooterMoving={onFooterMoving ? handleFooterMoving : undefined}
    >
      {/* 如果有自定义 Header，先渲染它（不要包装，让原生层直接识别） */}
      {/* {renderHeader && renderHeader()} */}
        {renderHeader && (
        <View collapsable={false} pointerEvents="none">
          {renderHeader()}
        </View>
      )}
      
      {/* 主内容 */}
      {children}
    </NativeView>
  );
}
