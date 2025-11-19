/*
 * @Author       : 尚博信_王强 wangqiang03@sunboxsoft.com
 * @Date         : 2025-11-19 12:54:00
 * @LastEditors  : 尚博信_王强 wangqiang03@sunboxsoft.com
 * @LastEditTime : 2025-11-19 14:57:18
 * @FilePath     : /expo-smartrefreshlayout/src/components/DefaultRefreshHeader.tsx
 * @Description  : 默认的下拉刷新 Header 组件
 * 
 * Copyright (c) 2025 by 尚博信_王强, All Rights Reserved. 
 */
import React, { useEffect, useState, useMemo } from 'react';
import { ActivityIndicator, Animated, StyleSheet, Text, View } from 'react-native';
import { DefaultRefreshHeaderProps, RefreshState } from '../ExpoSmartrefreshlayout.types';


export function DefaultRefreshHeader(props: DefaultRefreshHeaderProps) {
  const { params, textColor = '#666666', style, ...restProps } = props;
  const { state } = params;
  
  const [text, setText] = useState('下拉可以刷新');
  const [showArrow, setShowArrow] = useState(true);
  const [lastUpdated, setLastUpdated] = useState(new Date());
  
  // 箭头旋转动画 - 使用 useMemo 避免 React Compiler 警告
  const rotateAnim = useMemo(() => new Animated.Value(0), []);
  
  // 格式化时间
  const formatTime = (date: Date) => {
    const month = date.getMonth() + 1;
    const day = date.getDate();
    const hours = date.getHours().toString().padStart(2, '0');
    const minutes = date.getMinutes().toString().padStart(2, '0');
    return `${month}-${day} ${hours}:${minutes}`;
  };

  // 根据状态更新文字和箭头动画
  useEffect(() => {
    if (state === RefreshState.None || state === RefreshState.PullDownToRefresh) {
      setText('下拉可以刷新');
      setShowArrow(true);
      // 箭头向下（0度）
      Animated.timing(rotateAnim, {
        toValue: 0,
        duration: 200,
        useNativeDriver: true,
      }).start();
    } else if (state === RefreshState.ReleaseToRefresh) {
      setText('释放立即刷新');
      setShowArrow(true);
      // 箭头向上（180度）
      Animated.timing(rotateAnim, {
        toValue: 1,
        duration: 200,
        useNativeDriver: true,
      }).start();
    } else if (state === RefreshState.Refreshing) {
      setText('正在刷新...');
      setShowArrow(false);
      setLastUpdated(new Date());
    } else if (state === RefreshState.RefreshFinish) {
      setText('刷新完成');
      setShowArrow(false);
    }
  }, [state, rotateAnim]);

  // 插值旋转角度 - 使用 useMemo 缓存
  const rotate = useMemo(
    () => rotateAnim.interpolate({
      inputRange: [0, 1],
      outputRange: ['0deg', '180deg'],
    }),
    [rotateAnim]
  );

  return (
    <View style={[styles.header, style]} {...restProps}>
      <View style={styles.content}>
        {!showArrow ? (
          <ActivityIndicator size="small" color={textColor} />
        ) : (
          <Animated.Text
            style={[
              styles.arrow,
              { color: textColor, transform: [{ rotate }] }
            ]}
          >
            ↓
          </Animated.Text>
        )}
        <View style={styles.rightView}>
          <Text style={[styles.text, { color: textColor }]}>
            {text}
          </Text>
          <Text style={[styles.timer, { color: textColor }]}>
            上次更新: {formatTime(lastUpdated)}
          </Text>
        </View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  header: {
    height: 60,
    alignItems: 'center',
    justifyContent: 'flex-end',
    paddingBottom: 10,
  },
  content: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 10,
  },
  arrow: {
    fontSize: 24,
    fontWeight: 'bold',
  },
  rightView: {
    alignItems: 'center',
    justifyContent: 'center',
    gap: 5,
  },
  text: {
    fontSize: 14,
  },
  timer: {
    fontSize: 11,
  },
});