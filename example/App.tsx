import type { ReactNode } from 'react';
import { useCallback, useEffect, useRef, useState } from 'react';
import {
  Animated,
  FlatList,
  Image,
  Platform,
  Pressable,
  SafeAreaView,
  ScrollView,
  StatusBar,
  StyleSheet,
  Switch,
  Text,
  View,
} from 'react-native';
import { SmartRefreshLayout, SmartSecondFloorLayout } from 'expo-smartrefreshlayout';
import type {
  ClassicSpinnerStyle,
  RefreshRequest,
  RefreshState,
  SecondFloorState,
  SmartRefreshLayoutRef,
  SmartSecondFloorLayoutRef,
} from 'expo-smartrefreshlayout';

const PAGE_SIZE = 15;
const TOTAL_PAGES = 4;

type FeedItem = {
  id: string;
  title: string;
  detail: string;
  color: string;
};

const COLORS = ['#1677ff', '#13c2c2', '#52c41a', '#fa8c16', '#eb2f96'];

type HeaderTheme = {
  id: string;
  label: string;
  primary: string;
  indicator: string;
  title: string;
};

const CLASSIC_THEMES: HeaderTheme[] = [
  { id: 'default', label: '默认', primary: '#00000000', indicator: '#666666', title: '#666666' },
  { id: 'blue', label: '蓝色', primary: '#1677ff', indicator: '#ffffff', title: '#ffffff' },
  { id: 'green', label: '绿色', primary: '#52c41a', indicator: '#ffffff', title: '#ffffff' },
  { id: 'red', label: '红色', primary: '#f5222d', indicator: '#ffffff', title: '#ffffff' },
  { id: 'orange', label: '橙色', primary: '#fa8c16', indicator: '#ffffff', title: '#ffffff' },
];

const MATERIAL_THEMES: HeaderTheme[] = CLASSIC_THEMES.slice(1);

const TAOBAO_HOME_IMAGE = require('./assets/image_taobao.jpg');
const TAOBAO_SECOND_FLOOR_BACKGROUND_IMAGE = require('./assets/image_second_floor.jpg');
const TAOBAO_SECOND_FLOOR_CONTENT_IMAGE = require('./assets/image_second_floor_content.jpg');

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

function ThemePicker({
  themes,
  selectedId,
  onSelect,
}: {
  themes: HeaderTheme[];
  selectedId: string;
  onSelect: (id: string) => void;
}) {
  const selectedTheme = themes.find((theme) => theme.id === selectedId) ?? themes[0];

  return (
    <View style={styles.themeRow}>
      <Text style={styles.optionLabel}>主题</Text>
      <View style={styles.swatchGroup}>
        {themes.map((theme) => (
          <Pressable
            key={theme.id}
            accessibilityRole="button"
            accessibilityLabel={`使用${theme.label}主题`}
            style={[
              styles.swatchButton,
              selectedId === theme.id && styles.swatchButtonActive,
            ]}
            onPress={() => onSelect(theme.id)}
          >
            <View style={[styles.swatch, { backgroundColor: theme.primary }]} />
          </Pressable>
        ))}
      </View>
      <Text style={styles.themeName}>{selectedTheme.label}</Text>
    </View>
  );
}

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

function RefreshPreview({
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

  const [items, setItems] = useState<FeedItem[]>(() => createPage(1));
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
        loadMoreMode="auto"
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
                  ? '该页保留官方 Classic 示例的上拉加载能力，不使用 FlatList.onEndReached。'
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

function SecondFloorDemoPage() {
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
  const toolbarOpacity = useRef(new Animated.Value(1)).current;
  const [toolbarInteractive, setToolbarInteractive] = useState(true);

  const animateToolbar = useCallback(
    (toValue: number, duration: number) => {
      toolbarOpacity.stopAnimation();
      Animated.timing(toolbarOpacity, {
        toValue,
        duration,
        useNativeDriver: true,
      }).start();
    },
    [toolbarOpacity]
  );

  useEffect(() => () => toolbarOpacity.stopAnimation(), [toolbarOpacity]);
  const handleStateChange = useCallback(
    (state: SecondFloorState) => {
      if (state === 'second-floor-closing') {
        animateToolbar(1, 260);
        return;
      }

      const floorIsVisible =
        state === 'release-to-second-floor' ||
        state === 'second-floor-opening' ||
        state === 'second-floor';
      setToolbarInteractive(!floorIsVisible);
      animateToolbar(floorIsVisible ? 0 : 1, floorIsVisible ? 180 : 160);
    },
    [animateToolbar]
  );

  const openSecondFloor = useCallback(() => {
    layoutRef.current?.openSecondFloor();
  }, []);

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
        headerInset={56}
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
        onStateChange={handleStateChange}
      >
        <ScrollView
          style={styles.taobaoScroll}
          contentContainerStyle={styles.taobaoContent}
          showsVerticalScrollIndicator={false}
        >
          <Image source={TAOBAO_HOME_IMAGE} style={styles.taobaoHomeImage} resizeMode="cover" />
        </ScrollView>
      </SmartSecondFloorLayout>
      <Animated.View
        pointerEvents={toolbarInteractive ? 'auto' : 'none'}
        style={[styles.taobaoToolbar, { opacity: toolbarOpacity }]}
      >
        <Pressable
          accessibilityRole="button"
          accessibilityLabel="打开淘宝二楼"
          style={styles.taobaoToolbarButton}
          onPress={openSecondFloor}
        >
          <Text style={styles.taobaoToolbarTitle}>淘宝二楼</Text>
        </Pressable>
      </Animated.View>
    </View>
  );
}

function ClassicConfigurationPage() {
  const [spinnerStyle, setSpinnerStyle] = useState<ClassicSpinnerStyle>('fixed-behind');
  const [enableLastTime, setEnableLastTime] = useState(true);
  const [themeId, setThemeId] = useState('default');
  const theme = CLASSIC_THEMES.find((item) => item.id === themeId) ?? CLASSIC_THEMES[0];

  return (
    <RefreshPreview
      title="Classic 配置"
      subtitle="ClassicsHeader：Spinner、最后更新时间与主题色"
      headerStyle="classic"
      primaryColor={theme.primary}
      indicatorColor={theme.indicator}
      titleColor={theme.title}
      classicSpinnerStyle={spinnerStyle}
      classicEnableLastTime={enableLastTime}
      loadMoreEnabled
      controls={
        <>
          <View style={styles.optionRow}>
            <Text style={styles.optionLabel}>Spinner</Text>
            <View style={styles.segmentedControl}>
              {[
                ['scale', '拉伸'],
                ['translate', '平移'],
                ['fixed-behind', '固定背后'],
              ].map(([value, label]) => (
                <Pressable
                  key={value}
                  style={[
                    styles.segment,
                    spinnerStyle === value && styles.segmentActive,
                  ]}
                  onPress={() => setSpinnerStyle(value as ClassicSpinnerStyle)}
                >
                  <Text
                    style={[
                      styles.segmentText,
                      spinnerStyle === value && styles.segmentTextActive,
                    ]}
                  >
                    {label}
                  </Text>
                </Pressable>
              ))}
            </View>
          </View>
          <View style={styles.switchRow}>
            <Text style={styles.optionLabel}>显示最后更新时间</Text>
            <Switch
              value={enableLastTime}
              onValueChange={setEnableLastTime}
              trackColor={{ false: '#d9d9d9', true: '#91caff' }}
              thumbColor="#ffffff"
            />
          </View>
          <ThemePicker
            themes={CLASSIC_THEMES}
            selectedId={themeId}
            onSelect={setThemeId}
          />
        </>
      }
    />
  );
}

function MaterialConfigurationPage() {
  const [showBezierWave, setShowBezierWave] = useState(false);
  const [translateContent, setTranslateContent] = useState(false);
  const [themeId, setThemeId] = useState('blue');
  const theme = MATERIAL_THEMES.find((item) => item.id === themeId) ?? MATERIAL_THEMES[0];

  return (
    <RefreshPreview
      title="Material 配置"
      subtitle="MaterialHeader：贝塞尔背景、内容偏移与进度圆主题"
      headerStyle="material"
      primaryColor={theme.primary}
      indicatorColor={theme.indicator}
      titleColor={theme.title}
      materialShowBezierWave={showBezierWave}
      materialEnableHeaderTranslationContent={translateContent}
      materialProgressBackgroundColor={theme.primary}
      loadMoreEnabled={false}
      controls={
        <>
          <View style={styles.switchRow}>
            <Text style={styles.optionLabel}>显示贝塞尔背景</Text>
            <Switch
              value={showBezierWave}
              onValueChange={setShowBezierWave}
              trackColor={{ false: '#d9d9d9', true: '#91caff' }}
              thumbColor="#ffffff"
            />
          </View>
          <View style={styles.switchRow}>
            <Text style={styles.optionLabel}>内容跟随 Header 偏移</Text>
            <Switch
              value={translateContent}
              onValueChange={setTranslateContent}
              trackColor={{ false: '#d9d9d9', true: '#91caff' }}
              thumbColor="#ffffff"
            />
          </View>
          <ThemePicker
            themes={MATERIAL_THEMES}
            selectedId={themeId}
            onSelect={setThemeId}
          />
        </>
      }
    />
  );
}

export default function App() {
  const [page, setPage] = useState<'classic' | 'material' | 'second-floor'>('classic');

  return (
    <SafeAreaView style={styles.screen}>
      <StatusBar barStyle="dark-content" />
      <View style={styles.pageTabs}>
        <Text style={styles.appTitle}>SmartRefreshLayout</Text>
        <View style={styles.pageTabControl}>
          <Pressable
            style={[styles.pageTab, page === 'classic' && styles.pageTabActive]}
            onPress={() => setPage('classic')}
          >
            <Text style={[styles.pageTabText, page === 'classic' && styles.pageTabTextActive]}>
              Classic
            </Text>
          </Pressable>
          <Pressable
            style={[styles.pageTab, page === 'material' && styles.pageTabActive]}
            onPress={() => setPage('material')}
          >
            <Text style={[styles.pageTabText, page === 'material' && styles.pageTabTextActive]}>
              Material
            </Text>
          </Pressable>
          <Pressable
            style={[styles.pageTab, page === 'second-floor' && styles.pageTabActive]}
            onPress={() => setPage('second-floor')}
          >
            <Text
              style={[styles.pageTabText, page === 'second-floor' && styles.pageTabTextActive]}
            >
              二楼
            </Text>
          </Pressable>
        </View>
      </View>
      {page === 'classic' ? (
        <ClassicConfigurationPage />
      ) : page === 'material' ? (
        <MaterialConfigurationPage />
      ) : (
        <SecondFloorDemoPage />
      )}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: '#f5f7fa',
  },
  preview: {
    flex: 1,
  },
  pageTabs: {

    paddingHorizontal: 16,
    paddingTop:32,
    paddingBlock:12,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    backgroundColor: '#ffffff',
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: '#d9d9d9',
  },
  appTitle: {
    flexShrink: 1,
    maxWidth: '42%',
    color: '#1f1f1f',
    fontSize: 17,
    fontWeight: '700',
  },
  pageTabControl: {
    flex: 1,
    marginLeft: 12,
    flexDirection: 'row',
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: '#d9d9d9',
    borderRadius: 5,
    overflow: 'hidden',
  },
  pageTab: {
    flex: 1,
    minWidth: 0,
    paddingHorizontal: 8,
    minHeight: 30,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#ffffff',
  },
  pageTabActive: {
    backgroundColor: '#e6f4ff',
  },
  pageTabText: {
    color: '#8c8c8c',
    fontSize: 12,
  },
  pageTabTextActive: {
    color: '#0958d9',
    fontWeight: '600',
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
  configurationBand: {
    marginTop: 10,
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
  switchRow: {
    minHeight: 34,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  optionLabel: {
    color: '#8c8c8c',
    fontSize: 11,
  },
  themeRow: {
    minHeight: 32,
    flexDirection: 'row',
    alignItems: 'center',
  },
  swatchGroup: {
    flexDirection: 'row',
    marginLeft: 10,
  },
  swatchButton: {
    width: 26,
    height: 26,
    marginRight: 7,
    padding: 3,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: 'transparent',
    borderRadius: 4,
  },
  swatchButtonActive: {
    borderColor: '#1677ff',
  },
  swatch: {
    flex: 1,
    borderRadius: 2,
  },
  themeName: {
    marginLeft: 'auto',
    color: '#595959',
    fontSize: 12,
  },
  segmentedControl: {
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
  taobaoPage: {
    flex: 1,
  },
  taobaoScroll: {
    flex: 1,
    backgroundColor: 'transparent',
  },
  taobaoContent: {
    paddingTop: 56,
  },
  taobaoHomeImage: {
    width: '100%',
    aspectRatio: 400 / 1073,
  },
  taobaoFloorImage: {
    ...StyleSheet.absoluteFillObject,
    width: '100%',
    height: '100%',
  },
  taobaoToolbar: {
    position: 'absolute',
    top: 0,
    right: 0,
    left: 0,
    height: 56,
    backgroundColor: '#fe1200',
    elevation: 2,
    zIndex: 2,
  },
  taobaoToolbarButton: {
    flex: 1,
    justifyContent: 'center',
    paddingHorizontal: 16,
  },
  taobaoToolbarTitle: {
    color: '#ffffff',
    fontSize: 19,
    fontWeight: '700',
  },
  androidOnlyNotice: {
    flex: 1,
    justifyContent: 'center',
    padding: 24,
    backgroundColor: '#ffffff',
  },
  androidOnlyTitle: {
    color: '#262626',
    fontSize: 20,
    fontWeight: '700',
    textAlign: 'center',
  },
  androidOnlyText: {
    marginTop: 10,
    color: '#8c8c8c',
    fontSize: 13,
    lineHeight: 20,
    textAlign: 'center',
  },
});
