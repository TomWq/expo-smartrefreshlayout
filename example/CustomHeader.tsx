/*
 * @Author       : 尚博信_王强 wangqiang03@sunboxsoft.com
 * @Date         : 2025-11-13 11:04:47
 * @LastEditors  : 尚博信_王强 wangqiang03@sunboxsoft.com
 * @LastEditTime : 2025-11-13 11:25:22
 * @FilePath     : /expo-smartrefreshlayout/example/CoustomHeader.tsx
 * @Description  : LottieView 自定义 Header 示例 - 展示如何使用 Lottie 动画实现下拉刷新效果
 * 
 * Copyright (c) 2025 by 尚博信_王强, All Rights Reserved. 
 */
import { RefreshState ,ExpoSmartrefreshlayoutView, onHeaderMoveProps,ExpoSmartrefreshlayoutModule} from 'expo-smartrefreshlayout';

import { useState, useRef } from 'react';
import { FlatList, Text, View } from 'react-native';
import LottieView from 'lottie-react-native'


export default function CustomHeader() {
  const lottieRef = useRef<LottieView>(null);
  const [data, setData] = useState([1, 2, 3, 4, 5]);
  const [refreshState, setRefreshState] = useState(RefreshState.None);
  const [animationProgress, setAnimationProgress] = useState(0);

  const handleRefresh = async () => {
    // 执行刷新逻辑
    await new Promise(resolve => setTimeout(resolve, 2000));
    setData([...Array(10)].map((_, i) => i + 1));
    
    ExpoSmartrefreshlayoutModule.finishRefresh(true, 300);
  };
 
  const handleHeaderMoving = (event: onHeaderMoveProps) => {
    const { percent, isDragging } = event;
    
    // 根据下拉百分比控制动画进度
    // percent 范围是 0-1+，当达到触发刷新的阈值时会超过1
    // 我们将其限制在 0-1 之间来控制动画
    const progress = Math.min(percent, 1);
    
    if (isDragging) {
      // 在拖动时更新动画进度
      setAnimationProgress(progress);
    }
  };

  const handleStateChanged = (state: RefreshState) => {
    setRefreshState(state);
    
    // 当开始刷新时，播放完整动画
    if (state === RefreshState.Refreshing) {
      lottieRef.current?.play();
    }
  };

  return (
    <View style={{ flex: 1, marginTop: 100 }}>
      <ExpoSmartrefreshlayoutView
        style={{ flex: 1, backgroundColor: '#f5f5f5' }}
        headerHeight={80}
        renderHeader={() => (
          // 自定义 Lottie 动画 Header
          <View style={{ 
            height: 80, 
            justifyContent: 'center', 
            alignItems: 'center',
            backgroundColor: '#fff'
          }}>
            <LottieView
              ref={lottieRef}
              source={require('./assets/load.json')}
              style={{ width: 100, height: 100 }}
              loop={refreshState === RefreshState.Refreshing}
              autoPlay={refreshState === RefreshState.Refreshing}
              progress={refreshState === RefreshState.Refreshing ? undefined : animationProgress}
            />
          </View>
        )}
        onHeaderMoving={handleHeaderMoving}
        onStateChanged={handleStateChanged}
        onRefresh={handleRefresh}
      >
        {/* 内容列表 */}
        <FlatList
          data={data}
          style={{ flex: 1 }}
          renderItem={({ item }) => (
            <View style={{ padding: 20, borderBottomWidth: 1, borderBottomColor: '#e0e0e0' }}>
              <Text>Item {item}</Text>
            </View>
          )}
          keyExtractor={(item) => item.toString()}
        />
      </ExpoSmartrefreshlayoutView>
    </View>
  );
}
