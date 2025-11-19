/*
 * @Author       : 尚博信_王强 wangqiang03@sunboxsoft.com
 * @Date         : 2025-11-19 13:05:00
 * @LastEditors  : 尚博信_王强 wangqiang03@sunboxsoft.com
 * @LastEditTime : 2025-11-19 14:30:18
 * @FilePath     : /expo-smartrefreshlayout/example/DefaultHeaderExample.tsx
 * @Description  : 使用 DefaultRefreshHeader 的示例
 * 
 * Copyright (c) 2025 by 尚博信_王强, All Rights Reserved. 
 */
import React, { useState } from 'react';
import { FlatList, StyleSheet, Text, View,SafeAreaView } from 'react-native';
import { ExpoSmartrefreshlayoutView, DefaultRefreshHeader } from 'expo-smartrefreshlayout';

export default function DefaultHeaderExample() {
  const [refreshing, setRefreshing] = useState(false);
  const [data, setData] = useState(new Array(20).fill(0).map((_, i) => i + 1));

  const handleRefresh = () => {
    setRefreshing(true);
    
    // 模拟网络请求
    setTimeout(() => {
      setData([...Array(10)].map((_, i) => i + 1));
      setRefreshing(false);
    }, 2000);
  };

  return (
    <SafeAreaView style={{ flex: 1 }}> 
        <ExpoSmartrefreshlayoutView
      style={styles.container}
      enableRefresh={true}
      enableLoadMore={false}
      onRefresh={handleRefresh}
      renderHeader={(params) => (
        <DefaultRefreshHeader
          params={params}
          textColor="#333"
        />
      )}
    >
      <FlatList
        data={data}
        keyExtractor={(item) => item.toString()}
        renderItem={({ item }) => (
          <View style={styles.item}>
            <Text style={styles.itemText}>Item {item}</Text>
          </View>
        )}
      />
    </ExpoSmartrefreshlayoutView>
    </SafeAreaView>
  
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  item: {
    padding: 20,
    borderBottomWidth: 1,
    borderBottomColor: '#e0e0e0',
    backgroundColor: '#fff',
  },
  itemText: {
    fontSize: 16,
  },
});