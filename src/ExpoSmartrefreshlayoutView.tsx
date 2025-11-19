/*
 * @Author       : 尚博信_王强 wangqiang03@sunboxsoft.com
 * @Date         : 2025-11-12 10:14:48
 * @LastEditors  : 尚博信_王强 wangqiang03@sunboxsoft.com
 * @LastEditTime : 2025-11-19 14:09:44
 * @FilePath     : /expo-smartrefreshlayout/src/ExpoSmartrefreshlayoutView.tsx
 * @Description  : SmartRefreshLayout 视图组件
 *
 * Copyright (c) 2025 by 尚博信_王强, All Rights Reserved.
 */
import { requireNativeView } from 'expo';
import * as React from 'react';
import { View } from 'react-native';

import { ExpoSmartrefreshlayoutViewProps, NativeViewProps } from './ExpoSmartrefreshlayout.types';
import { useRefreshState } from './hooks/useRefreshState';
import { useEventHandlers } from './hooks/useEventHandlers';

const NativeView: React.ComponentType<NativeViewProps> =
  requireNativeView('ExpoSmartrefreshlayout');

/**
 * SmartRefreshLayout 视图组件
 *
 * 提供下拉刷新和上拉加载功能的容器组件
 */
export default function ExpoSmartrefreshlayoutView(props: ExpoSmartrefreshlayoutViewProps) {
  const { onStateChanged, onHeaderMoving, onFooterMoving, renderHeader, children, ...restProps } = props;
  
  // 使用自定义 Hook 管理状态
  const { headerParams, updateState, updateMoving } = useRefreshState();
  
  // 使用自定义 Hook 处理事件
  const { handleStateChanged, handleHeaderMoving, handleFooterMoving } = useEventHandlers({
    onStateChanged,
    onHeaderMoving,
    onFooterMoving,
    updateState,
    updateMoving
  });

  return (
    <NativeView
      {...restProps}
      hasCustomHeader={!!renderHeader}
      hasCustomFooter={false}
      onStateChanged={handleStateChanged}
      onHeaderMoving={handleHeaderMoving}
      onFooterMoving={handleFooterMoving}
    >
      {/* 自定义 Header */}
      {renderHeader && (
        <View collapsable={false} pointerEvents="none">
          {renderHeader(headerParams)}
        </View>
      )}
      
      {/* 主内容 */}
      {children}
    </NativeView>
  );
}
