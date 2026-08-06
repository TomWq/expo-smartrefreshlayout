import { useCallback, useRef, useState } from 'react';
import {
  FlatList,
  Pressable,
  SafeAreaView,
  StatusBar,
  StyleSheet,
  Switch,
  Text,
  View,
} from 'react-native';
import { SmartRefreshLayout } from 'expo-smartrefreshlayout';
import type {
  LoadMoreMode,
  RefreshRequest,
  RefreshState,
  SmartRefreshLayoutRef,
} from 'expo-smartrefreshlayout';

const PAGE_SIZE = 15;
const TOTAL_PAGES = 5;

type FeedItem = {
  id: string;
  title: string;
  detail: string;
  color: string;
};

const COLORS = ['#1677ff', '#13c2c2', '#52c41a', '#fa8c16', '#eb2f96'];

function createPage(page: number): FeedItem[] {
  const start = (page - 1) * PAGE_SIZE;

  return Array.from({ length: PAGE_SIZE }, (_, offset) => {
    const number = start + offset + 1;
    return {
      id: `item-${number}`,
      title: `消息条目 ${number}`,
      detail: `第 ${page} 页 · 本地模拟数据 · ${number % 2 === 0 ? '已读' : '未读'}`,
      color: COLORS[offset % COLORS.length],
    };
  });
}

const wait = (milliseconds: number) =>
  new Promise<void>((resolve) => setTimeout(resolve, milliseconds));

function getErrorMessage(error: unknown): string {
  return error instanceof Error ? error.message : String(error);
}

function requestSourceLabel(request: RefreshRequest): string {
  return request.source === 'programmatic' ? '主动调用' : '手势';
}

export default function App() {
  const refreshRef = useRef<SmartRefreshLayoutRef>(null);
  const pageRef = useRef(1);
  const failNextRequestRef = useRef(false);

  const [items, setItems] = useState<FeedItem[]>(() => createPage(1));
  const [hasMore, setHasMore] = useState(true);
  const [loadMoreMode, setLoadMoreMode] = useState<LoadMoreMode>('pull');
  const [headerStyle, setHeaderStyle] = useState<'classic' | 'material'>('classic');
  const [failNextRequest, setFailNextRequest] = useState(false);
  const [nativeState, setNativeState] = useState<RefreshState>('idle');
  const [notice, setNotice] = useState('准备就绪：先试试下拉刷新或上拉加载');

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
    setNotice('分页已重置 · 当前为第 1 页');
  }, []);

  const toggleFailure = useCallback((value: boolean) => {
    failNextRequestRef.current = value;
    setFailNextRequest(value);
    setNotice(value ? '下一次刷新或分页会失败' : '已取消下一次失败模拟');
  }, []);

  return (
    <SafeAreaView style={styles.screen}>
      <StatusBar barStyle="dark-content" />

      <View style={styles.toolbar}>
        <View style={styles.headingRow}>
          <View style={styles.headingCopy}>
            <Text style={styles.title}>SmartRefreshLayout</Text>
            <Text style={styles.subtitle}>Fabric + Codegen 真机验证页</Text>
          </View>
          <View style={styles.countBadge}>
            <Text style={styles.countValue}>{items.length}</Text>
            <Text style={styles.countLabel}>条</Text>
          </View>
        </View>

        <View style={styles.actionRow}>
          <Pressable style={styles.primaryButton} onPress={handleProgrammaticRefresh}>
            <Text style={styles.primaryButtonText}>主动刷新</Text>
          </Pressable>
          <Pressable style={styles.secondaryButton} onPress={resetPagination}>
            <Text style={styles.secondaryButtonText}>重置分页</Text>
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

        <View style={styles.optionRow}>
          <Text style={styles.optionLabel}>分页触发</Text>
          <View style={styles.segmentedControl}>
            <Pressable
              style={[styles.segment, loadMoreMode === 'pull' && styles.segmentActive]}
              onPress={() => setLoadMoreMode('pull')}
            >
              <Text style={[styles.segmentText, loadMoreMode === 'pull' && styles.segmentTextActive]}>
                上拉释放
              </Text>
            </Pressable>
            <Pressable
              style={[styles.segment, loadMoreMode === 'auto' && styles.segmentActive]}
              onPress={() => setLoadMoreMode('auto')}
            >
              <Text style={[styles.segmentText, loadMoreMode === 'auto' && styles.segmentTextActive]}>
                滚动到底
              </Text>
            </Pressable>
          </View>
          <Text style={styles.optionLabel}>Header</Text>
          <View style={styles.segmentedControlSmall}>
            <Pressable
              style={[styles.segment, headerStyle === 'classic' && styles.segmentActive]}
              onPress={() => setHeaderStyle('classic')}
            >
              <Text style={[styles.segmentText, headerStyle === 'classic' && styles.segmentTextActive]}>
                Classic
              </Text>
            </Pressable>
            <Pressable
              style={[styles.segment, headerStyle === 'material' && styles.segmentActive]}
              onPress={() => setHeaderStyle('material')}
            >
              <Text style={[styles.segmentText, headerStyle === 'material' && styles.segmentTextActive]}>
                Material
              </Text>
            </Pressable>
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
        loadMoreMode={loadMoreMode}
        hasMore={hasMore}
        headerStyle={headerStyle}
        hapticsEnabled
        indicatorColor="#1677ff"
        titleColor="#595959"
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
        onLoadMore={loadMore}
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
              <Text style={styles.listHeaderTitle}>可滚动测试列表</Text>
              <Text style={styles.listHeaderText}>
                不使用 FlatList.onEndReached，分页完全由 SmartRefreshLayout 负责。
              </Text>
            </View>
          }
          ListFooterComponent={
            <View style={styles.listFooter}>
              <Text style={styles.listFooterText}>
                {hasMore ? '继续向上滚动，或上拉释放触发下一页' : '已加载全部模拟数据，可点击“重置分页”'}
              </Text>
            </View>
          }
        />
      </SmartRefreshLayout>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: '#f5f7fa',
  },
  toolbar: {
    paddingHorizontal: 16,
    paddingTop: 12,
    paddingBottom: 10,
    backgroundColor: '#ffffff',
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: '#d9d9d9',
  },
  headingRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  headingCopy: {
    flex: 1,
  },
  title: {
    color: '#1f1f1f',
    fontSize: 20,
    fontWeight: '700',
  },
  subtitle: {
    marginTop: 3,
    color: '#8c8c8c',
    fontSize: 12,
  },
  countBadge: {
    minWidth: 54,
    paddingVertical: 5,
    paddingHorizontal: 8,
    alignItems: 'center',
    backgroundColor: '#e6f4ff',
    borderRadius: 6,
  },
  countValue: {
    color: '#0958d9',
    fontSize: 18,
    fontWeight: '700',
  },
  countLabel: {
    color: '#1677ff',
    fontSize: 11,
  },
  actionRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: 12,
    gap: 8,
  },
  primaryButton: {
    paddingVertical: 9,
    paddingHorizontal: 14,
    backgroundColor: '#1677ff',
    borderRadius: 6,
  },
  primaryButtonText: {
    color: '#ffffff',
    fontSize: 14,
    fontWeight: '600',
  },
  secondaryButton: {
    paddingVertical: 9,
    paddingHorizontal: 14,
    backgroundColor: '#f0f5ff',
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: '#adc6ff',
    borderRadius: 6,
  },
  secondaryButtonText: {
    color: '#0958d9',
    fontSize: 14,
    fontWeight: '600',
  },
  failureSwitch: {
    flexDirection: 'row',
    alignItems: 'center',
    marginLeft: 'auto',
  },
  switchLabel: {
    color: '#595959',
    fontSize: 12,
  },
  optionRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: 10,
    gap: 7,
  },
  optionLabel: {
    color: '#8c8c8c',
    fontSize: 11,
  },
  segmentedControl: {
    flexDirection: 'row',
    flex: 1,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: '#d9d9d9',
    borderRadius: 5,
    overflow: 'hidden',
  },
  segmentedControlSmall: {
    flexDirection: 'row',
    flex: 1,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: '#d9d9d9',
    borderRadius: 5,
    overflow: 'hidden',
  },
  segment: {
    flex: 1,
    minHeight: 30,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#ffffff',
  },
  segmentActive: {
    backgroundColor: '#e6f4ff',
  },
  segmentText: {
    color: '#8c8c8c',
    fontSize: 11,
  },
  segmentTextActive: {
    color: '#0958d9',
    fontWeight: '600',
  },
  statusRow: {
    flexDirection: 'row',
    alignItems: 'center',
    minHeight: 26,
    marginTop: 8,
  },
  stateDot: {
    width: 7,
    height: 7,
    marginRight: 7,
    borderRadius: 4,
    backgroundColor: '#fa8c16',
  },
  stateDotIdle: {
    backgroundColor: '#52c41a',
  },
  statusText: {
    flex: 1,
    color: '#595959',
    fontSize: 11,
  },
  nativeState: {
    marginLeft: 8,
    color: '#8c8c8c',
    fontSize: 10,
  },
  refreshLayout: {
    flex: 1,
  },
  list: {
    flex: 1,
  },
  listContent: {
    padding: 12,
    paddingBottom: 28,
  },
  listHeader: {
    marginBottom: 10,
    padding: 14,
    backgroundColor: '#ffffff',
    borderRadius: 6,
  },
  listHeaderTitle: {
    color: '#262626',
    fontSize: 15,
    fontWeight: '600',
  },
  listHeaderText: {
    marginTop: 4,
    color: '#8c8c8c',
    fontSize: 12,
    lineHeight: 18,
  },
  itemRow: {
    flexDirection: 'row',
    alignItems: 'center',
    minHeight: 68,
    marginBottom: 8,
    padding: 14,
    backgroundColor: '#ffffff',
    borderRadius: 6,
  },
  itemMarker: {
    width: 4,
    height: 38,
    marginRight: 12,
    borderRadius: 2,
  },
  itemCopy: {
    flex: 1,
  },
  itemTitle: {
    color: '#262626',
    fontSize: 15,
    fontWeight: '600',
  },
  itemDetail: {
    marginTop: 4,
    color: '#8c8c8c',
    fontSize: 12,
  },
  listFooter: {
    alignItems: 'center',
    paddingVertical: 14,
  },
  listFooterText: {
    color: '#8c8c8c',
    fontSize: 12,
    textAlign: 'center',
  },
});
