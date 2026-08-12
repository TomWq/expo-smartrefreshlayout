import { useCallback, useRef } from 'react';
import { Image, Platform, ScrollView, Text, View } from 'react-native';
import { SmartSecondFloorLayout } from 'expo-smartrefreshlayout';
import type { SmartSecondFloorLayoutRef } from 'expo-smartrefreshlayout';

import {
  TAOBAO_HOME_IMAGE,
  TAOBAO_SECOND_FLOOR_BACKGROUND_IMAGE,
  TAOBAO_SECOND_FLOOR_CONTENT_IMAGE,
  wait,
} from '../data';
import { styles } from '../styles';

export default function SecondFloorDemoPage() {
  if (Platform.OS !== 'android') {
    return (
      <View style={styles.androidOnlyNotice}>
        <Text style={styles.androidOnlyTitle}>淘宝二楼仅支持 Android</Text>
        <Text style={styles.androidOnlyText}>
          iOS 继续使用 Classic 或 Material 下拉刷新，不挂载二楼原生组件。
        </Text>
      </View>
    );
  }

  return <AndroidSecondFloorDemoPage />;
}

function AndroidSecondFloorDemoPage() {
  const layoutRef = useRef<SmartSecondFloorLayoutRef>(null);

  const refresh = useCallback(async () => {
    await wait(900);
  }, []);

  return (
    <View style={styles.taobaoPage}>
      <SmartSecondFloorLayout
        ref={layoutRef}
        style={styles.refreshLayout}
        secondFloorBackground={
          <Image
            source={TAOBAO_SECOND_FLOOR_BACKGROUND_IMAGE}
            style={styles.taobaoFloorImage}
            resizeMode="cover"
          />
        }
        secondFloor={
          <Image
            source={TAOBAO_SECOND_FLOOR_CONTENT_IMAGE}
            style={styles.taobaoFloorImage}
            resizeMode="cover"
          />
        }
        hapticsEnabled
        primaryColor="transparent"
        indicatorColor="#ffffff"
        titleColor="#ffffff"
        classicEnableLastTime
        messages={{
          pullDown: '下拉刷新',
          releaseToRefresh: '释放刷新',
          refreshing: '正在刷新',
          refreshComplete: '刷新完成',
        }}
        onRefresh={refresh}
      >
        <ScrollView
          style={styles.taobaoScroll}
          showsVerticalScrollIndicator={false}
        >
          <Image source={TAOBAO_HOME_IMAGE} style={styles.taobaoHomeImage} resizeMode="cover" />
        </ScrollView>
      </SmartSecondFloorLayout>
    </View>
  );
}
