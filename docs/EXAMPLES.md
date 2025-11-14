# 使用示例

## 基础示例

### 简单的下拉刷新

```tsx
import { ExpoSmartrefreshlayoutView, ExpoSmartrefreshlayoutModule } from 'expo-smartrefreshlayout';
import { FlatList, View, Text } from 'react-native';
import { useState } from 'react';

function BasicRefreshExample() {
  const [data, setData] = useState([1, 2, 3, 4, 5]);

  const handleRefresh = async () => {
    // 模拟网络请求
    await new Promise(resolve => setTimeout(resolve, 2000));
    setData([...Array(5)].map((_, i) => i + 1));
    ExpoSmartrefreshlayoutModule.finishRefresh(true, 300);
  };

  return (
    <ExpoSmartrefreshlayoutView
      style={{ flex: 1 }}
      onRefresh={handleRefresh}
    >
      <FlatList
        data={data}
        renderItem={({ item }) => (
          <View style={{ padding: 20, borderBottomWidth: 1 }}>
            <Text>Item {item}</Text>
          </View>
        )}
        keyExtractor={(item) => item.toString()}
      />
    </ExpoSmartrefreshlayoutView>
  );
}
```

### 下拉刷新 + 上拉加载

```tsx
import { ExpoSmartrefreshlayoutView, ExpoSmartrefreshlayoutModule } from 'expo-smartrefreshlayout';
import { FlatList, View, Text } from 'react-native';
import { useState } from 'react';

function RefreshAndLoadMoreExample() {
  const [data, setData] = useState([1, 2, 3, 4, 5]);

  const handleRefresh = async () => {
    await fetchData();
    ExpoSmartrefreshlayoutModule.finishRefresh(true, 300);
  };

  const handleLoadMore = async () => {
    const newData = await loadMoreData();
    
    if (newData.length === 0) {
      // 没有更多数据
      ExpoSmartrefreshlayoutModule.finishLoadMore(true, 0, true);
    } else {
      setData([...data, ...newData]);
      ExpoSmartrefreshlayoutModule.finishLoadMore(true, 300);
    }
  };

  return (
    <ExpoSmartrefreshlayoutView
      enableLoadMore={true}
      onRefresh={handleRefresh}
      onLoadMore={handleLoadMore}
    >
      <FlatList
        data={data}
        renderItem={({ item }) => (
          <View style={{ padding: 20, borderBottomWidth: 1 }}>
            <Text>Item {item}</Text>
          </View>
        )}
        keyExtractor={(item) => item.toString()}
      />
    </ExpoSmartrefreshlayoutView>
  );
}
```

## 加载更多场景

### 场景 1：使用 FlatList 的 onEndReached（推荐）

```tsx
import { ExpoSmartrefreshlayoutView, ExpoSmartrefreshlayoutModule } from 'expo-smartrefreshlayout';
import { FlatList } from 'react-native';

function FlatListLoadMoreExample() {
  const [data, setData] = useState([1, 2, 3, 4, 5]);

  const handleRefresh = async () => {
    await fetchData();
    ExpoSmartrefreshlayoutModule.finishRefresh(true, 300);
  };

  const handleEndReached = () => {
    // 使用 FlatList 自带的加载更多
    loadMoreData();
  };

  return (
    <ExpoSmartrefreshlayoutView onRefresh={handleRefresh}>
      <FlatList
        data={data}
        onEndReached={handleEndReached}
        onEndReachedThreshold={0.1}
        renderItem={({ item }) => <Item data={item} />}
      />
    </ExpoSmartrefreshlayoutView>
  );
}
```

### 场景 2：使用组件的 enableLoadMore

```tsx
import { ExpoSmartrefreshlayoutView, ExpoSmartrefreshlayoutModule } from 'expo-smartrefreshlayout';
import { FlatList } from 'react-native';

function ComponentLoadMoreExample() {
  const [data, setData] = useState([1, 2, 3, 4, 5]);

  const handleRefresh = async () => {
    await fetchData();
    ExpoSmartrefreshlayoutModule.finishRefresh(true, 300);
  };

  const handleLoadMore = async () => {
    const newData = await loadMoreData();
    
    if (newData.length === 0) {
      ExpoSmartrefreshlayoutModule.finishLoadMore(true, 0, true);
    } else {
      setData([...data, ...newData]);
      ExpoSmartrefreshlayoutModule.finishLoadMore(true, 300);
    }
  };

  return (
    <ExpoSmartrefreshlayoutView
      enableLoadMore={true}
      onRefresh={handleRefresh}
      onLoadMore={handleLoadMore}
      classicLoadMoreFooterProps={{
        footerAccentColor: '#007AFF',
        REFRESH_FOOTER_PULLING: '上拉加载更多',
        REFRESH_FOOTER_LOADING: '正在加载...',
        REFRESH_FOOTER_NOTHING: '没有更多了',
      }}
    >
      <FlatList
        data={data}
        renderItem={({ item }) => <Item data={item} />}
      />
    </ExpoSmartrefreshlayoutView>
  );
}
```

## 自定义样式

### Material Design 样式

```tsx
<ExpoSmartrefreshlayoutView
  headerType="material"
  classicRefreshHeaderProps={{
    headerAccentColor: '#FF5722',
  }}
  onRefresh={handleRefresh}
>
  <FlatList data={data} {...listProps} />
</ExpoSmartrefreshlayoutView>
```

### 自定义文字和颜色

```tsx
<ExpoSmartrefreshlayoutView
  onRefresh={handleRefresh}
  classicRefreshHeaderProps={{
    headerAccentColor: '#007AFF',
    headerPrimaryColor: '#FFFFFF',
    REFRESH_HEADER_PULLING: '下拉可以刷新',
    REFRESH_HEADER_RELEASE: '释放立即刷新',
    REFRESH_HEADER_REFRESHING: '正在刷新数据...',
    REFRESH_HEADER_FINISH: '刷新完成',
  }}
>
  <FlatList data={data} {...listProps} />
</ExpoSmartrefreshlayoutView>
```

## 自定义 Header

### 基础自定义 Header

```tsx
import { ExpoSmartrefreshlayoutView, ExpoSmartrefreshlayoutModule } from 'expo-smartrefreshlayout';
import { View, Text, FlatList } from 'react-native';
import { useState } from 'react';

function CustomHeaderExample() {
  const [data, setData] = useState([1, 2, 3, 4, 5]);
  const [offset, setOffset] = useState(0);
  const [percent, setPercent] = useState(0);
  
  const handleRefresh = async () => {
    await fetchData();
    ExpoSmartrefreshlayoutModule.finishRefresh(true, 300);
  };
  
  return (
    <ExpoSmartrefreshlayoutView
      headerHeight={80}
      renderHeader={() => (
        <View style={{ 
          height: 80, 
          justifyContent: 'center', 
          alignItems: 'center',
          backgroundColor: '#f0f0f0'
        }}>
          <Text>下拉距离: {offset}dp</Text>
          <Text>下拉进度: {(percent * 100).toFixed(0)}%</Text>
          <Text>{percent >= 1 ? '释放刷新' : '继续下拉'}</Text>
        </View>
      )}
      onHeaderMoving={(event) => {
        setOffset(event.offset);
        setPercent(event.percent);
      }}
      onRefresh={handleRefresh}
    >
      <FlatList
        data={data}
        renderItem={({ item }) => (
          <View style={{ padding: 20, borderBottomWidth: 1 }}>
            <Text>Item {item}</Text>
          </View>
        )}
        keyExtractor={(item) => item.toString()}
      />
    </ExpoSmartrefreshlayoutView>
  );
}
```

### 带动画的自定义 Header

```tsx
import { Animated, ActivityIndicator } from 'react-native';
import { useRef } from 'react';
import { RefreshState } from 'expo-smartrefreshlayout';

function AnimatedCustomHeader() {
  const [data, setData] = useState([1, 2, 3, 4, 5]);
  const [refreshState, setRefreshState] = useState(RefreshState.None);
  const rotateAnim = useRef(new Animated.Value(0)).current;
  const scaleAnim = useRef(new Animated.Value(0)).current;

  const handleStateChanged = (state: RefreshState) => {
    setRefreshState(state);
  };

  const handleHeaderMoving = (event) => {
    const { percent } = event;
    rotateAnim.setValue(percent * 360);
    scaleAnim.setValue(Math.min(percent, 1));
  };
  
  const handleRefresh = async () => {
    await new Promise(resolve => setTimeout(resolve, 2000));
    setData([...Array(10)].map((_, i) => i + 1));
    ExpoSmartrefreshlayoutModule.finishRefresh(true, 300);
  };
  
  const rotate = rotateAnim.interpolate({
    inputRange: [0, 360],
    outputRange: ['0deg', '360deg'],
  });
  
  const isRefreshing = refreshState === RefreshState.Refreshing;
  
  return (
    <ExpoSmartrefreshlayoutView
      renderHeader={() => (
        <View style={{ 
          height: 80, 
          justifyContent: 'center', 
          alignItems: 'center',
          backgroundColor: '#fff'
        }}>
          {isRefreshing ? (
            <ActivityIndicator size="large" color="#007AFF" />
          ) : (
            <Animated.View
              style={{
                transform: [
                  { rotate },
                  { scale: scaleAnim }
                ]
              }}
            >
              <Text style={{ fontSize: 40 }}>↓</Text>
            </Animated.View>
          )}
          <Text style={{ marginTop: 8, color: '#666' }}>
            {isRefreshing ? '正在刷新...' : '下拉刷新'}
          </Text>
        </View>
      )}
      onStateChanged={handleStateChanged}
      onHeaderMoving={handleHeaderMoving}
      onRefresh={handleRefresh}
    >
      <FlatList
        data={data}
        renderItem={({ item }) => (
          <View style={{ padding: 20, borderBottomWidth: 1 }}>
            <Text>Item {item}</Text>
          </View>
        )}
        keyExtractor={(item) => item.toString()}
      />
    </ExpoSmartrefreshlayoutView>
  );
}
```

### 使用 Lottie 动画

```bash
# 安装 lottie-react-native
npm install lottie-react-native
```

```tsx
import { RefreshState, ExpoSmartrefreshlayoutView } from 'expo-smartrefreshlayout';
import ExpoSmartrefreshlayoutModule from 'expo-smartrefreshlayout/ExpoSmartrefreshlayoutModule';
import { useState, useRef } from 'react';
import { FlatList, Text, View } from 'react-native';
import LottieView from 'lottie-react-native';

export default function LottieCustomHeader() {
  const lottieRef = useRef<LottieView>(null);
  const [data, setData] = useState([1, 2, 3, 4, 5]);
  const [refreshState, setRefreshState] = useState(RefreshState.None);
  const [animationProgress, setAnimationProgress] = useState(0);

  const handleRefresh = async () => {
    await new Promise(resolve => setTimeout(resolve, 2000));
    setData([...Array(10)].map((_, i) => i + 1));
    ExpoSmartrefreshlayoutModule.finishRefresh(true, 300);
  };
 
  const handleHeaderMoving = (event) => {
    const { percent, isDragging } = event;
    const progress = Math.min(percent, 1);
    
    if (isDragging) {
      setAnimationProgress(progress);
    }
  };

  const handleStateChanged = (state: RefreshState) => {
    setRefreshState(state);
    
    if (state === RefreshState.Refreshing) {
      lottieRef.current?.play();
    }
  };

  return (
    <View style={{ flex: 1 }}>
      <ExpoSmartrefreshlayoutView
        style={{ flex: 1, backgroundColor: '#f5f5f5' }}
        headerHeight={80}
        renderHeader={() => (
          <View style={{ 
            height: 80, 
            justifyContent: 'center', 
            alignItems: 'center',
            backgroundColor: '#fff'
          }}>
            <LottieView
              ref={lottieRef}
              source={require('./assets/refresh-animation.json')}
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
        <FlatList
          data={data}
          style={{ flex: 1 }}
          renderItem={({ item }) => (
            <View style={{ padding: 20, borderBottomWidth: 1 }}>
              <Text>Item {item}</Text>
            </View>
          )}
          keyExtractor={(item) => item.toString()}
        />
      </ExpoSmartrefreshlayoutView>
    </View>
  );
}
```

## 状态监听

### 监听刷新状态

```tsx
import { RefreshState } from 'expo-smartrefreshlayout';

function StateExample() {
  const handleStateChanged = (event: { state: RefreshState }) => {
    switch (event.state) {
      case RefreshState.PullDownToRefresh:
        console.log('下拉刷新');
        break;
      case RefreshState.ReleaseToRefresh:
        console.log('释放刷新');
        break;
      case RefreshState.Refreshing:
        console.log('正在刷新');
        break;
      case RefreshState.RefreshFinish:
        console.log('刷新完成');
        break;
    }
  };
  
  return (
    <ExpoSmartrefreshlayoutView
      onStateChanged={handleStateChanged}
      onRefresh={handleRefresh}
    >
      <FlatList data={data} {...listProps} />
    </ExpoSmartrefreshlayoutView>
  );
}
```

## 触觉反馈

### 启用震动提示

```tsx
<ExpoSmartrefreshlayoutView
  enableHapticFeedback={true}  // 默认为 true
  onRefresh={handleRefresh}
  onLoadMore={handleLoadMore}
>
  <FlatList data={data} {...listProps} />
</ExpoSmartrefreshlayoutView>
```

### 禁用震动提示

```tsx
<ExpoSmartrefreshlayoutView
  enableHapticFeedback={false}
  onRefresh={handleRefresh}
>
  <FlatList data={data} {...listProps} />
</ExpoSmartrefreshlayoutView>
```

## 完整示例

```tsx
import React, { useState } from 'react';
import { View, Text, FlatList, StyleSheet } from 'react-native';
import { 
  ExpoSmartrefreshlayoutView, 
  ExpoSmartrefreshlayoutModule,
  RefreshState 
} from 'expo-smartrefreshlayout';

export default function CompleteExample() {
  const [data, setData] = useState([1, 2, 3, 4, 5, 6, 7, 8, 9, 10]);
  const [refreshState, setRefreshState] = useState(RefreshState.None);

  const handleStateChanged = (state: RefreshState) => {
    console.log('状态改变:', state);
    setRefreshState(state);
  };

  const handleRefresh = async () => {
    try {
      await new Promise(resolve => setTimeout(resolve, 2000));
      const newData = Array.from({ length: 10 }, (_, i) => i + 1);
      setData(newData);
      ExpoSmartrefreshlayoutModule.finishRefresh(true, 300);
    } catch (error) {
      ExpoSmartrefreshlayoutModule.finishRefresh(false, 300);
    }
  };

  const handleLoadMore = async () => {
    try {
      await new Promise(resolve => setTimeout(resolve, 2000));
      
      if (data.length >= 50) {
        ExpoSmartrefreshlayoutModule.finishLoadMore(true, 0, true);
        return;
      }
      
      const moreData = Array.from(
        { length: 10 }, 
        (_, i) => data.length + i + 1
      );
      setData([...data, ...moreData]);
      ExpoSmartrefreshlayoutModule.finishLoadMore(true, 300);
    } catch (error) {
      ExpoSmartrefreshlayoutModule.finishLoadMore(false, 300);
    }
  };

  return (
    <View style={styles.container}>
      <ExpoSmartrefreshlayoutView
        style={styles.refresh}
        headerType="classics"
        enableRefresh={true}
        enableLoadMore={true}
        enableAutoLoadMore={false}
        onRefresh={handleRefresh}
        onLoadMore={handleLoadMore}
        onStateChanged={handleStateChanged}
        classicRefreshHeaderProps={{
          headerAccentColor: '#007AFF',
          headerPrimaryColor: '#FFFFFF',
          REFRESH_HEADER_PULLING: '下拉可以刷新',
          REFRESH_HEADER_RELEASE: '释放立即刷新',
          REFRESH_HEADER_REFRESHING: '正在刷新数据...',
          REFRESH_HEADER_FINISH: '刷新完成',
        }}
        classicLoadMoreFooterProps={{
          footerAccentColor: '#007AFF',
          footerPrimaryColor: '#FFFFFF',
          REFRESH_FOOTER_PULLING: '上拉加载更多',
          REFRESH_FOOTER_RELEASE: '释放立即加载',
          REFRESH_FOOTER_LOADING: '正在加载...',
          REFRESH_FOOTER_FINISH: '加载完成',
          REFRESH_FOOTER_NOTHING: '已经到底了',
        }}
      >
        <FlatList
          data={data}
          renderItem={({ item }) => (
            <View style={styles.item}>
              <Text style={styles.itemText}>Item {item}</Text>
            </View>
          )}
          keyExtractor={(item) => item.toString()}
        />
      </ExpoSmartrefreshlayoutView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  refresh: {
    flex: 1,
  },
  item: {
    padding: 20,
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: '#e0e0e0',
  },
  itemText: {
    fontSize: 16,
    color: '#333',
  },
});