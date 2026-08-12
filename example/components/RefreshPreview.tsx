import type { ReactNode } from 'react';
import { useCallback, useEffect, useRef, useState } from 'react';
import { FlatList, Pressable, Switch, Text, View } from 'react-native';
import { SmartRefreshLayout } from 'expo-smartrefreshlayout';
import type {
  ClassicSpinnerStyle,
  RefreshRequest,
  RefreshState,
  SmartRefreshLayoutRef,
} from 'expo-smartrefreshlayout';

import {
  TOTAL_PAGES,
  createPage,
  getErrorMessage,
  requestSourceLabel,
  wait,
} from '../data';
import { styles } from '../styles';

type RefreshPreviewProps = {
  title: string;
  subtitle: string;
  headerStyle: 'classic' | 'material';
  primaryColor: string;
  indicatorColor: string;
  titleColor: string;
  classicSpinnerStyle?: ClassicSpinnerStyle;
  classicEnableLastTime?: boolean;
  materialShowBezierWave?: boolean;
  materialEnableHeaderTranslationContent?: boolean;
  materialProgressBackgroundColor?: string;
  loadMoreEnabled: boolean;
  controls: ReactNode;
};

export function RefreshPreview({
  title,
  subtitle,
  headerStyle,
  primaryColor,
  indicatorColor,
  titleColor,
  classicSpinnerStyle,
  classicEnableLastTime,
  materialShowBezierWave,
  materialEnableHeaderTranslationContent,
  materialProgressBackgroundColor,
  loadMoreEnabled,
  controls,
}: RefreshPreviewProps) {
  const refreshRef = useRef<SmartRefreshLayoutRef>(null);
  const pageRef = useRef(1);
  const failNextRequestRef = useRef(false);

  const [items, setItems] = useState(() => createPage(1));
  const [hasMore, setHasMore] = useState(true);
  const [failNextRequest, setFailNextRequest] = useState(false);
  const [nativeState, setNativeState] = useState<RefreshState>('idle');
  const [notice, setNotice] = useState('准备就绪：下拉刷新可立即查看当前配置');
  const configurationRefreshTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    configurationRefreshTimerRef.current = setTimeout(() => {
      configurationRefreshTimerRef.current = null;
      const accepted = refreshRef.current?.beginRefresh(120) === true;
      if (!accepted) {
        setNotice('预览尚未就绪，等待当前原生操作结束');
      }
    }, 220);

    return () => {
      if (configurationRefreshTimerRef.current !== null) {
        clearTimeout(configurationRefreshTimerRef.current);
        configurationRefreshTimerRef.current = null;
      }
    };
  }, []);

  const consumeFailureSwitch = useCallback(() => {
    const shouldFail = failNextRequestRef.current;
    failNextRequestRef.current = false;
    setFailNextRequest(false);
    return shouldFail;
  }, []);

  const refresh = useCallback(
    async (request: RefreshRequest) => {
      setNotice(`刷新开始 · ${requestSourceLabel(request)} · requestId ${request.requestId}`);
      await wait(900);

      if (consumeFailureSwitch()) {
        throw new Error('模拟刷新失败：网络请求被拒绝');
      }

      pageRef.current = 1;
      setItems(createPage(1));
      setHasMore(true);
      setNotice(`刷新完成 · ${requestSourceLabel(request)} · 已回到第 1 页`);
    },
    [consumeFailureSwitch]
  );

  const loadMore = useCallback(
    async (request: RefreshRequest) => {
      const nextPage = pageRef.current + 1;
      setNotice(
        `分页开始 · ${requestSourceLabel(request)} · requestId ${request.requestId} · 第 ${nextPage} 页`
      );
      await wait(700);

      if (consumeFailureSwitch()) {
        throw new Error('模拟分页失败：服务端返回错误');
      }

      if (nextPage > TOTAL_PAGES) {
        setHasMore(false);
        return { hasMore: false };
      }

      const nextItems = createPage(nextPage);
      const nextHasMore = nextPage < TOTAL_PAGES;
      pageRef.current = nextPage;
      setItems((currentItems) => [...currentItems, ...nextItems]);
      setHasMore(nextHasMore);
      setNotice(`分页完成 · 当前第 ${nextPage} 页 · ${nextHasMore ? '还可继续加载' : '已到末页'}`);
      return { hasMore: nextHasMore };
    },
    [consumeFailureSwitch]
  );

  const handleProgrammaticRefresh = useCallback(() => {
    const accepted = refreshRef.current?.beginRefresh(150) === true;
    setNotice(accepted ? '已排队主动刷新 · 150ms 后触发' : '刷新/分页正在进行，本次主动刷新被忽略');
  }, []);

  const resetPagination = useCallback(() => {
    pageRef.current = 1;
    setItems(createPage(1));
    setHasMore(true);
    refreshRef.current?.resetNoMoreData();
    setNotice(loadMoreEnabled ? '分页已重置 · 当前为第 1 页' : '测试列表已重置');
  }, [loadMoreEnabled]);

  const toggleFailure = useCallback((value: boolean) => {
    failNextRequestRef.current = value;
    setFailNextRequest(value);
    setNotice(value ? '下一次刷新或分页会失败' : '已取消下一次失败模拟');
  }, []);

  return (
    <View style={styles.preview}>
      <View style={styles.toolbar}>
        <View style={styles.headingRow}>
          <View style={styles.headingCopy}>
            <Text style={styles.title}>{title}</Text>
            <Text style={styles.subtitle}>{subtitle}</Text>
          </View>
          <View style={styles.countBadge}>
            <Text style={styles.countValue}>{items.length}</Text>
            <Text style={styles.countLabel}>条</Text>
          </View>
        </View>

        <View style={styles.configurationBand}>{controls}</View>

        <View style={styles.actionRow}>
          <Pressable style={styles.primaryButton} onPress={handleProgrammaticRefresh}>
            <Text style={styles.primaryButtonText}>主动刷新</Text>
          </Pressable>
          <Pressable style={styles.secondaryButton} onPress={resetPagination}>
            <Text style={styles.secondaryButtonText}>
              {loadMoreEnabled ? '重置分页' : '重置列表'}
            </Text>
          </Pressable>
          <View style={styles.failureSwitch}>
            <Text style={styles.switchLabel}>下次失败</Text>
            <Switch
              value={failNextRequest}
              onValueChange={toggleFailure}
              trackColor={{ false: '#d9d9d9', true: '#ff7875' }}
              thumbColor="#ffffff"
            />
          </View>
        </View>

        <View style={styles.statusRow}>
          <View style={[styles.stateDot, nativeState === 'idle' && styles.stateDotIdle]} />
          <Text style={styles.statusText} numberOfLines={2}>
            {notice}
          </Text>
          <Text style={styles.nativeState}>原生: {nativeState}</Text>
        </View>
      </View>

      <SmartRefreshLayout
        ref={refreshRef}
        style={styles.refreshLayout}
        loadMoreMode="pull"
        loadMoreEnabled={loadMoreEnabled}
        hasMore={hasMore}
        headerStyle={headerStyle}
        hapticsEnabled
        primaryColor={primaryColor}
        indicatorColor={indicatorColor}
        titleColor={titleColor}
        classicSpinnerStyle={classicSpinnerStyle}
        classicEnableLastTime={classicEnableLastTime}
        materialShowBezierWave={materialShowBezierWave}
        materialEnableHeaderTranslationContent={materialEnableHeaderTranslationContent}
        materialProgressBackgroundColor={materialProgressBackgroundColor}
        messages={{
          pullDown: '下拉刷新',
          releaseToRefresh: '松开刷新',
          refreshing: '正在刷新...',
          refreshComplete: '刷新完成',
          pullUp: '上拉加载更多',
          releaseToLoadMore: '松开加载',
          loadingMore: '正在加载...',
          noMoreData: '没有更多数据了',
        }}
        onRefresh={refresh}
        onLoadMore={loadMoreEnabled ? loadMore : undefined}
        onRefreshError={(error) => setNotice(`刷新失败 · ${getErrorMessage(error)}`)}
        onLoadMoreError={(error) => setNotice(`分页失败 · ${getErrorMessage(error)}`)}
        onStateChange={setNativeState}
      >
        <FlatList
          style={styles.list}
          data={items}
          keyExtractor={(item) => item.id}
          contentContainerStyle={styles.listContent}
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
              <Text style={styles.listHeaderTitle}>{title} 官方配置预览</Text>
              <Text style={styles.listHeaderText}>
                {loadMoreEnabled
                  ? '该页保留当前示例的上拉加载能力，不使用 FlatList.onEndReached。'
                  : '该页对应官方 Material 示例：只保留下拉刷新，不启用加载更多。'}
              </Text>
            </View>
          }
          ListFooterComponent={
            <View style={styles.listFooter}>
              <Text style={styles.listFooterText}>
                {loadMoreEnabled
                  ? hasMore
                    ? '继续向上滚动并释放，触发下一页'
                    : '已加载全部模拟数据，可点击“重置分页”'
                  : 'Material 官方页未启用加载更多'}
              </Text>
            </View>
          }
        />
      </SmartRefreshLayout>
    </View>
  );
}
