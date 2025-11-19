/*
 * @Author       : 尚博信_王强 wangqiang03@sunboxsoft.com
 * @Date         : 2025-11-19 13:53:00
 * @LastEditors  : 尚博信_王强 wangqiang03@sunboxsoft.com
 * @LastEditTime : 2025-11-19 14:55:40
 * @FilePath     : /expo-smartrefreshlayout/src/hooks/useEventHandlers.ts
 * @Description  : 事件处理器的自定义 Hook
 * 
 * Copyright (c) 2025 by 尚博信_王强, All Rights Reserved. 
 */
import { useCallback } from 'react';
import { RefreshState, onHeaderMoveProps } from '../ExpoSmartrefreshlayout.types';

interface UseEventHandlersProps {
  onStateChanged?: (state: RefreshState) => void;
  onHeaderMoving?: (params: onHeaderMoveProps) => void;
  onFooterMoving?: (params: onHeaderMoveProps) => void;
  updateState: (state: RefreshState) => void;
  updateMoving: (params: onHeaderMoveProps) => void;
}

interface NativeEvent<T> {
  nativeEvent: T;
}

/**
 * 管理事件处理器的 Hook
 */
export function useEventHandlers({
  onStateChanged,
  onHeaderMoving,
  onFooterMoving,
  updateState,
  updateMoving
}: UseEventHandlersProps) {
  
  const handleStateChanged = useCallback((event: NativeEvent<{ state: RefreshState }>) => {
    const stateValue = event.nativeEvent?.state;
    if (stateValue !== undefined) {
      updateState(stateValue);
      onStateChanged?.(stateValue);
    }
  }, [onStateChanged, updateState]);

  const handleHeaderMoving = useCallback((event: NativeEvent<onHeaderMoveProps>) => {
    const params = event.nativeEvent;
    if (params) {
      updateMoving(params);
      onHeaderMoving?.(params);
    }
  }, [onHeaderMoving, updateMoving]);

  const handleFooterMoving = useCallback((event: NativeEvent<onHeaderMoveProps>) => {
    const params = event.nativeEvent;
    if (params) {
      onFooterMoving?.(params);
    }
  }, [onFooterMoving]);

  return {
    handleStateChanged,
    handleHeaderMoving,
    handleFooterMoving
  };
}