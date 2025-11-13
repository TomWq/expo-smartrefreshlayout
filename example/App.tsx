/*
 * @Author       : 尚博信_王强 wangqiang03@sunboxsoft.com
 * @Date         : 2025-11-12 10:14:48
 * @LastEditors  : 尚博信_王强 wangqiang03@sunboxsoft.com
 * @LastEditTime : 2025-11-13 13:59:28
 * @FilePath     : /expo-smartrefreshlayout/example/App.tsx
 * @Description  : 
 * 
 * Copyright (c) 2025 by 尚博信_王强, All Rights Reserved. 
 */
import { ExpoSmartrefreshlayoutView, onHeaderMoveProps, RefreshState, } from 'expo-smartrefreshlayout';
import { useState, useCallback } from 'react';
import { FlatList, Text, View, StyleSheet, ActivityIndicator } from 'react-native';


export default function App() {
  const [data, setData] = useState<string[]>(
    Array.from({ length: 20 }, (_, i) => `Item ${i + 1}`)
  );
const [refreshState, setRefreshState] = useState(RefreshState.None);
const [headerOffset, setHeaderOffset] = useState(0);
  const [refreshing, setRefreshing] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);

  const handleRefresh = useCallback(() => {
    console.log('刷新触发');
    setRefreshing(true);

    // 模拟加载数据
    setTimeout(() => {
      setData(Array.from({ length: 20 }, (_, i) => `新Item ${i + 1}`));
      setRefreshing(false);
      console.log('刷新完成');
    }, 2000);
  }, []);

  const handleLoadMore = useCallback(() => {
    console.log('加载更多触发');
    setLoadingMore(true);

    // 模拟加载数据
    setTimeout(() => {
      const newData = Array.from({ length: 10 }, (_, i) => `新Item ${data.length + i + 1}`);
      setData(prevData => [...prevData, ...newData]);
      setLoadingMore(false);
      console.log('加载更多完成');
    }, 2000);
  }, [data.length]);


  const onHeaderMoving = (params: onHeaderMoveProps) => {
    setHeaderOffset(params.offset);
   
  }

  const onFooterMoving = useCallback((params: any) => {
    console.log('Footer 移动:', params);
  }, []);

  const handleStateChanged = (state: RefreshState) => {
    const stateNames: { [key: number]: string } = {
      [RefreshState.None]: '无状态',
      [RefreshState.PullDownToRefresh]: '下拉刷新',
      [RefreshState.ReleaseToRefresh]: '释放立即刷新',
      [RefreshState.Refreshing]: '正在刷新',
      [RefreshState.RefreshFinish]: '刷新完成',
      [RefreshState.PullUpToLoad]: '上拉加载',
      [RefreshState.ReleaseToLoad]: '释放立即加载',
      [RefreshState.Loading]: '正在加载',
      [RefreshState.LoadFinish]: '加载完成',
      [RefreshState.NoMoreData]: '没有更多数据',
    };
    setRefreshState(state);
    console.log('状态改变:', stateNames[state] || `未知状态(${state})`);
  };

  const renderItem = useCallback(({ item }: { item: string }) => (
    <View style={styles.item}>
      <Text style={styles.itemText}>{item}</Text>
    </View>
  ), []);

  return (

    <View style={styles.container}>
      <ExpoSmartrefreshlayoutView
        style={styles.refreshLayout}
         
        // renderHeader={() => (
        //     <View 
           
        //     style={{ height: 80, backgroundColor: 'red' ,justifyContent: 'center', alignItems: 'center'}}>
        //       {refreshState === RefreshState.PullDownToRefresh && (
        //         <Text style={{ fontSize: 16, color: 'white' }}>下拉刷新</Text>
        //       )}
        //       {refreshState === RefreshState.ReleaseToRefresh && (
        //         <Text style={{ fontSize: 16, color: 'white' }}>释放立即刷新</Text>
        //       )}
        //       {refreshState === RefreshState.Refreshing && (
        //         <ActivityIndicator color="white" />
        //       )}
        //       {/* 根据下拉距离显示不同内容 */}
        //       <Text style={{ fontSize: 14, color: 'white' }}>下拉距离: {headerOffset}px</Text>
        //     </View>
        //   )}
          
        // headerType="material"
        // classicRefreshHeaderProps={{
        //   // 文字定制
        //   REFRESH_HEADER_PULLING: '下拉刷新',
        //   REFRESH_HEADER_RELEASE: '释放刷新',
        //   REFRESH_HEADER_REFRESHING: '加载中...',
        //   REFRESH_HEADER_FINISH: '刷新完成啦',

        //   // 样式定制
        //   headerAccentColor: '#FF5722',
        //   headerPrimaryColor: '#ffffff',
        //   headerTitleTextSize: 14,
        //   headerFinishDuration: 300,
        //   headerDrawableArrowSize: 14,
        //   headerDrawableProgressSize: 24,

        // }}
        // classicLoadMoreFooterProps={{
        //   // Footer 定制
        //   REFRESH_FOOTER_PULLING: '上拉加载',
        //   REFRESH_FOOTER_LOADING: '加载中...',
        //   REFRESH_FOOTER_RELEASE: '释放加载更多',
        //   REFRESH_FOOTER_NOTHING: '没有更多数据了',

        //   footerAccentColor: '#4CAF50',
        //   footerTitleTextSize: 14,
        //   footerDrawableArrowSize: 14,

        //   footerFinishDuration: 0
        // }}
        onHeaderMoving={onHeaderMoving}
        onRefresh={handleRefresh}
        onStateChanged={handleStateChanged}
        

        onLoadMore={handleLoadMore}>
        <FlatList
          data={data}
          renderItem={renderItem}
          keyExtractor={(item, index) => index.toString()}
          style={styles.list}
          contentContainerStyle={styles.listContent}
         
        />

      </ExpoSmartrefreshlayoutView>

    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
    paddingTop: 60
  },
  refreshLayout: {
    flex: 1,

  },
  list: {
    flex: 1,
  },
  listContent: {
    padding: 10,
  },
  item: {
    backgroundColor: '#fff',
    padding: 20,
    marginVertical: 5,
    marginHorizontal: 10,
    borderRadius: 8,
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 2,
    },
    shadowOpacity: 0.1,
    shadowRadius: 3,
    elevation: 3,
  },
  itemText: {
    fontSize: 16,
    color: '#333',
  },
});
