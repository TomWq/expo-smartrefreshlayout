/*
 * @Author       : 尚博信_王强 wangqiang03@sunboxsoft.com
 * @Date         : 2025-11-19 13:52:00
 * @LastEditors  : 尚博信_王强 wangqiang03@sunboxsoft.com
 * @LastEditTime : 2025-11-19 13:52:00
 * @FilePath     : /expo-smartrefreshlayout/src/hooks/useRefreshState.ts
 * @Description  : 管理刷新状态的自定义 Hook
 * 
 * Copyright (c) 2025 by 尚博信_王强, All Rights Reserved. 
 */
import { useState, useCallback, useMemo } from 'react';
import { RefreshState, onHeaderMoveProps, RenderHeaderParams } from '../ExpoSmartrefreshlayout.types';

/**
 * 管理刷新状态的 Hook
 */
export function useRefreshState() {
  const [currentState, setCurrentState] = useState<RefreshState>(RefreshState.None);
  const [movingParams, setMovingParams] = useState<onHeaderMoveProps>({
    isDragging: false,
    percent: 0,
    offset: 0,
    headerHeight: 0
  });
  const [isRefreshing, setIsRefreshing] = useState(false);

  const updateState = useCallback((stateValue: RefreshState) => {
    setCurrentState(stateValue);
    setIsRefreshing(stateValue === RefreshState.Refreshing);
  }, []);

  const updateMoving = useCallback((params: onHeaderMoveProps) => {
    setMovingParams(params);
  }, []);

  const headerParams: RenderHeaderParams = useMemo(() => ({
    state: currentState,
    moving: movingParams,
    refreshing: isRefreshing
  }), [currentState, movingParams, isRefreshing]);

  return {
    currentState,
    movingParams,
    isRefreshing,
    headerParams,
    updateState,
    updateMoving
  };
}