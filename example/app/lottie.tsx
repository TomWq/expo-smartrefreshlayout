import { useCallback, useEffect, useRef, useState } from 'react';
import { FlatList, Text, View } from 'react-native';
import LottieView from 'lottie-react-native';
import { SmartRefreshLayout } from 'expo-smartrefreshlayout';
import type { HeaderMovingEvent, RefreshRequest } from 'expo-smartrefreshlayout';

import { createPage, getErrorMessage, requestSourceLabel, wait } from '../data';
import { styles } from '../styles';

export default function LottieRefreshPage() {
  const lottieRef = useRef<LottieView>(null);
  const refreshingRef = useRef(false);
  const [revision, setRevision] = useState(1);
  const [refreshing, setRefreshing] = useState(false);
  const [headerProgress, setHeaderProgress] = useState(0);
  const [headerOffset, setHeaderOffset] = useState(0);
  const [notice, setNotice] = useState('下拉列表，观察 Header 动画随位移变化。');

  refreshingRef.current = refreshing;

  useEffect(() => {
    const animation = lottieRef.current;
    if (refreshing) {
      animation?.play();
      return;
    }

    animation?.pause();
    animation?.reset();
  }, [refreshing]);

  const handleHeaderMoving = useCallback(({ percent, offset }: HeaderMovingEvent) => {
    if (refreshingRef.current) {
      return;
    }

    setHeaderProgress(Math.min(Math.max(percent, 0), 1));
    setHeaderOffset(Math.max(0, Math.round(offset)));
  }, []);

  const handleRefresh = useCallback(async (request: RefreshRequest) => {
    refreshingRef.current = true;
    setRefreshing(true);
    setNotice(`刷新开始 · ${requestSourceLabel(request)}`);

    try {
      await wait(900);
      setRevision((current) => current + 1);
      setNotice('刷新完成');
    } finally {
      refreshingRef.current = false;
      setRefreshing(false);
      setHeaderProgress(0);
      setHeaderOffset(0);
    }
  }, []);

  const handleAnimationLoaded = useCallback(() => {
    if (refreshingRef.current) {
      lottieRef.current?.play();
    }
  }, []);

  return (
    <SmartRefreshLayout
      style={styles.refreshLayout}
      headerStyle="classic"
      loadMoreEnabled={false}
      refreshHeader={
        <View style={styles.lottieHeader} pointerEvents="none">
          <LottieView
            ref={lottieRef}
            source={require('../assets/load.json')}
            progress={refreshing ? undefined : headerProgress}
            loop
            style={styles.lottieAnimation}
            resizeMode="contain"
            onAnimationLoaded={handleAnimationLoaded}
          />
          <Text style={styles.lottieHeaderText}>
            {refreshing
              ? '正在刷新...'
              : headerProgress >= 1
                ? '松开刷新'
                : `下拉 ${headerOffset} px`}
          </Text>
        </View>
      }
      onHeaderMoving={handleHeaderMoving}
      onRefresh={handleRefresh}
      onRefreshError={(error) => setNotice(`刷新失败 · ${getErrorMessage(error)}`)}
    >
      <FlatList
        style={styles.list}
        data={createPage(revision)}
        keyExtractor={(item) => item.id}
        contentContainerStyle={styles.lottieListContent}
        renderItem={({ item }) => (
          <View style={styles.itemRow}>
            <View style={[styles.itemMarker, { backgroundColor: item.color }]} />
            <View style={styles.itemCopy}>
              <Text style={styles.itemTitle}>{item.title}</Text>
              <Text style={styles.itemDetail}>{item.detail}</Text>
            </View>
          </View>
        )}
        ListHeaderComponent={
          <View style={styles.listHeader}>
            <Text style={styles.listHeaderTitle}>Lottie 刷新示例</Text>
            <Text style={styles.listHeaderText}>{notice}</Text>
          </View>
        }
        ListFooterComponent={
          <View style={styles.listFooter}>
            <Text style={styles.listFooterText}>下拉可验证 SmartRefreshLayout 原生刷新生命周期。</Text>
          </View>
        }
      />
    </SmartRefreshLayout>
  );
}
