import { useCallback, useRef } from 'react';
import { Image, ScrollView, Text, View } from 'react-native';
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
  const layoutRef = useRef<SmartSecondFloorLayoutRef>(null);

  const refresh = useCallback(async () => {
    await wait(900);
  }, []);

  return (
    <View style={styles.taobaoPage}>
      <SmartSecondFloorLayout
        ref={layoutRef}
        style={styles.refreshLayout}
        // secondFloorBackground={
        //   // <Image
        //   //   source={TAOBAO_SECOND_FLOOR_BACKGROUND_IMAGE}
        //   //   style={styles.taobaoFloorImage}
        //   //   resizeMode="cover"
        //   // />
        //   <View style={{flex:1,justifyContent:'center',alignItems:'center'}}>
        //     <Text>Second Floor</Text>
        //   </View>
        // }
        secondFloor={
          // <Image
          //   source={TAOBAO_SECOND_FLOOR_CONTENT_IMAGE}
          //   style={styles.taobaoFloorImage}
          //   resizeMode="cover"
          // />
           <ScrollView style={{flex:1,backgroundColor:'green'}}>
              {
                new Array(100).fill(0).map((_, index) => (
                  <View key={index} style={{height:50,backgroundColor:'yellow'}} > 
                    <Text>Item {index}</Text>
                  </View>
                ))
              }
            </ScrollView>
        }
        hapticsEnabled
        primaryColor="transparent"
        indicatorColor="#000000"
        titleColor="#000000"
        classicEnableLastTime
        messages={{
          pullDown: '下拉刷新',
          releaseToRefresh: '释放刷新',
          refreshing: '正在刷新',
          refreshComplete: '刷新完成',
          pullToSecondFloor: '下拉到第二层',
          releaseToSecondFloor: '释放到第二层',
        }}
        onRefresh={refresh}
      >
        <ScrollView
          style={styles.taobaoScroll}
          showsVerticalScrollIndicator={false}
        >
          {/* <Image source={TAOBAO_HOME_IMAGE} style={styles.taobaoHomeImage} /> */}
          {
            new Array(100).fill(0).map((_, index) => (
              <View key={index} style={{height:50,backgroundColor:'red'}} > 
                <Text>Item {index}</Text>
              </View>
            ))
          }
        </ScrollView>
      </SmartSecondFloorLayout>
    </View>
  );
}
