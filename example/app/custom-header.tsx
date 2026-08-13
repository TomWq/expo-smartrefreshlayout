import { useCallback, useRef, useState } from 'react';
import { FlatList, Pressable, StyleSheet, Text, View } from 'react-native';
import { SmartRefreshLayout } from 'expo-smartrefreshlayout';
import type {
  HeaderFinishEvent,
  HeaderLifecycleEvent,
  HeaderMovingEvent,
  RefreshHeaderSpinnerStyle,
  RefreshRequest,
  RefreshState,
  SmartRefreshLayoutRef,
} from 'expo-smartrefreshlayout';

import { createPage, getErrorMessage, requestSourceLabel, wait } from '../data';
import { styles } from '../styles';

const HEADER_HEIGHT_OPTIONS = [
  { value: 64, label: '64' },
  { value: 80, label: '80' },
  { value: 112, label: '112' },
];

const SPINNER_STYLE_OPTIONS: Array<{
  value: RefreshHeaderSpinnerStyle;
  label: string;
}> = [
  { value: 'scale', label: '缩放' },
  { value: 'translate', label: '平移' },
  { value: 'fixed-behind', label: '固定背后' },
];

const TRIGGER_RATE_OPTIONS = [
  { value: 0.5, label: '0.5x' },
  { value: 0.75, label: '0.75x' },
  { value: 1, label: '1x' },
];

const MAX_DRAG_RATE_OPTIONS = [
  { value: 1, label: '1x' },
  { value: 2, label: '2x' },
  { value: 9, label: '9x' },
];

const FINISH_DURATION_OPTIONS = [
  { value: 0, label: '0ms' },
  { value: 350, label: '350ms' },
  { value: 900, label: '900ms' },
];

type SelectorValue = number | string;

type LifecycleEntry = {
  id: number;
  name: string;
  detail: string;
};

const INITIAL_MOVEMENT: HeaderMovingEvent = {
  percent: 0,
  offset: 0,
  height: 80,
  maxDragHeight: 160,
  isDragging: false,
};

function formatNumber(value: number, fractionDigits = 0): string {
  if (!Number.isFinite(value)) {
    return '--';
  }

  return Number.isInteger(value) ? String(value) : value.toFixed(fractionDigits);
}

function ConfigSelector<T extends SelectorValue>({
  label,
  value,
  options,
  onChange,
}: {
  label: string;
  value: T;
  options: Array<{ value: T; label: string }>;
  onChange: (nextValue: T) => void;
}) {
  return (
    <View style={styles.optionRow}>
      <Text style={styles.optionLabel}>{label}</Text>
      <View style={styles.segmentedControl}>
        {options.map((option) => {
          const selected = option.value === value;

          return (
            <Pressable
              key={String(option.value)}
              accessibilityRole="button"
              accessibilityLabel={`设置${label}为${option.label}`}
              accessibilityState={{ selected }}
              style={[styles.segment, selected && styles.segmentActive]}
              onPress={() => onChange(option.value)}
            >
              <Text style={[styles.segmentText, selected && styles.segmentTextActive]}>
                {option.label}
              </Text>
            </Pressable>
          );
        })}
      </View>
    </View>
  );
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <View style={pageStyles.metric}>
      <Text style={pageStyles.metricLabel}>{label}</Text>
      <Text style={pageStyles.metricValue}>{value}</Text>
    </View>
  );
}

export default function CustomHeaderConfigurationPage() {
  const refreshRef = useRef<SmartRefreshLayoutRef>(null);
  const lifecycleIdRef = useRef(1);
  const [revision, setRevision] = useState(1);
  const [headerHeight, setHeaderHeight] = useState(80);
  const [spinnerStyle, setSpinnerStyle] = useState<RefreshHeaderSpinnerStyle>('translate');
  const [triggerRate, setTriggerRate] = useState(1);
  const [maxDragRate, setMaxDragRate] = useState(2);
  const [finishDuration, setFinishDuration] = useState(350);
  const [movement, setMovement] = useState<HeaderMovingEvent>(INITIAL_MOVEMENT);
  const [nativeState, setNativeState] = useState<RefreshState>('idle');
  const [headerRefreshing, setHeaderRefreshing] = useState(false);
  const [lifecycleEvents, setLifecycleEvents] = useState<LifecycleEntry[]>([]);
  const [notice, setNotice] = useState('调整任意属性后下拉列表，观察原生 Header 的实际几何与生命周期。');

  const appendLifecycleEvent = useCallback((name: string, detail: string) => {
    const entry = {
      id: lifecycleIdRef.current,
      name,
      detail,
    };
    lifecycleIdRef.current += 1;

    setLifecycleEvents((current) => [entry, ...current].slice(0, 6));
  }, []);

  const updateLifecycleMetrics = useCallback((event: HeaderLifecycleEvent) => {
    setMovement((current) => ({
      ...current,
      height: event.height,
      maxDragHeight: event.maxDragHeight,
    }));
  }, []);

  const handleHeaderMoving = useCallback((event: HeaderMovingEvent) => {
    setMovement(event);
  }, []);

  const handleHeaderInitialized = useCallback(
    (event: HeaderLifecycleEvent) => {
      updateLifecycleMetrics(event);
      appendLifecycleEvent(
        'onHeaderInitialized',
        `height ${event.height} · maxDragHeight ${event.maxDragHeight}`
      );
      setNotice('原生 Header 尺寸已初始化。');
    },
    [appendLifecycleEvent, updateLifecycleMetrics]
  );

  const handleHeaderReleased = useCallback(
    (event: HeaderLifecycleEvent) => {
      updateLifecycleMetrics(event);
      appendLifecycleEvent(
        'onHeaderReleased',
        `height ${event.height} · maxDragHeight ${event.maxDragHeight}`
      );
      setNotice('手势已释放，正在观察原生回弹或刷新启动。');
    },
    [appendLifecycleEvent, updateLifecycleMetrics]
  );

  const handleHeaderStart = useCallback(
    (event: HeaderLifecycleEvent) => {
      updateLifecycleMetrics(event);
      setHeaderRefreshing(true);
      appendLifecycleEvent(
        'onHeaderStart',
        `height ${event.height} · maxDragHeight ${event.maxDragHeight}`
      );
      setNotice('原生刷新动画已开始。');
    },
    [appendLifecycleEvent, updateLifecycleMetrics]
  );

  const handleHeaderFinish = useCallback(
    (event: HeaderFinishEvent) => {
      setHeaderRefreshing(false);
      appendLifecycleEvent('onHeaderFinish', `success ${event.success ? 'true' : 'false'}`);
      setNotice(event.success ? '原生 Header 已进入成功完成态。' : '原生 Header 已进入失败完成态。');
    },
    [appendLifecycleEvent]
  );

  const handleRefresh = useCallback(async (request: RefreshRequest) => {
    setNotice(`刷新请求开始 · ${requestSourceLabel(request)} · requestId ${request.requestId}`);
    await wait(850);
    setRevision((current) => current + 1);
    setNotice('JS 刷新任务已完成，等待原生完成态和 onHeaderFinish。');
  }, []);

  const beginRefresh = useCallback(() => {
    const accepted = refreshRef.current?.beginRefresh(120) === true;
    setNotice(accepted ? '已发起主动刷新 · 120ms 后进入原生 Header。' : '当前原生 Header 正在处理请求。');
  }, []);

  const clearLifecycleEvents = useCallback(() => {
    setLifecycleEvents([]);
    setNotice('生命周期事件已清空。');
  }, []);

  const headerLabel = headerRefreshing
    ? '正在刷新...'
    : movement.percent >= 1
      ? '松开刷新'
      : `下拉 ${formatNumber(movement.offset)} dp/pt`;

  return (
    <SmartRefreshLayout
      ref={refreshRef}
      style={styles.refreshLayout}
      loadMoreEnabled={false}
      refreshHeaderHeight={headerHeight}
      refreshHeaderSpinnerStyle={spinnerStyle}
      refreshHeaderTriggerRate={triggerRate}
      refreshHeaderMaxDragRate={maxDragRate}
      refreshHeaderFinishDuration={finishDuration}
      refreshHeader={
        <View pointerEvents="none" style={[pageStyles.nativeHeader, { height: headerHeight }]}>
          <Text style={pageStyles.nativeHeaderTitle}>{headerLabel}</Text>
          <Text style={pageStyles.nativeHeaderDetail}>
            {`${formatNumber(movement.percent, 2)}x · ${formatNumber(movement.offset)} / ${formatNumber(
              movement.maxDragHeight
            )} dp/pt`}
          </Text>
        </View>
      }
      onHeaderMoving={handleHeaderMoving}
      onHeaderInitialized={handleHeaderInitialized}
      onHeaderReleased={handleHeaderReleased}
      onHeaderStart={handleHeaderStart}
      onHeaderFinish={handleHeaderFinish}
      onStateChange={setNativeState}
      onRefresh={handleRefresh}
      onRefreshError={(error) => setNotice(`刷新失败 · ${getErrorMessage(error)}`)}
    >
      <FlatList
        style={styles.list}
        data={createPage(revision)}
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
          <View>
            <View style={styles.listHeader}>
              <Text style={styles.listHeaderTitle}>自定义 Header 属性验证</Text>
              <Text style={styles.listHeaderText}>{notice}</Text>
              <View style={styles.actionRow}>
                <Pressable
                  accessibilityRole="button"
                  accessibilityLabel="主动触发自定义 Header 刷新"
                  style={styles.primaryButton}
                  onPress={beginRefresh}
                >
                  <Text style={styles.primaryButtonText}>主动刷新</Text>
                </Pressable>
                <Pressable
                  accessibilityRole="button"
                  accessibilityLabel="清空自定义 Header 生命周期事件"
                  style={styles.secondaryButton}
                  onPress={clearLifecycleEvents}
                >
                  <Text style={styles.secondaryButtonText}>清空事件</Text>
                </Pressable>
                <Text style={pageStyles.nativeState}>原生: {nativeState}</Text>
              </View>
            </View>

            <View style={pageStyles.panel}>
              <Text style={pageStyles.panelTitle}>配置</Text>
              <ConfigSelector
                label="高度"
                value={headerHeight}
                options={HEADER_HEIGHT_OPTIONS}
                onChange={setHeaderHeight}
              />
              <ConfigSelector
                label="布局"
                value={spinnerStyle}
                options={SPINNER_STYLE_OPTIONS}
                onChange={setSpinnerStyle}
              />
              <ConfigSelector
                label="触发"
                value={triggerRate}
                options={TRIGGER_RATE_OPTIONS}
                onChange={setTriggerRate}
              />
              <ConfigSelector
                label="最大"
                value={maxDragRate}
                options={MAX_DRAG_RATE_OPTIONS}
                onChange={setMaxDragRate}
              />
              <ConfigSelector
                label="完成"
                value={finishDuration}
                options={FINISH_DURATION_OPTIONS}
                onChange={setFinishDuration}
              />
            </View>

            <View style={pageStyles.panel}>
              <Text style={pageStyles.panelTitle}>onHeaderMoving</Text>
              <View style={pageStyles.metricGrid}>
                <Metric label="percent" value={formatNumber(movement.percent, 2)} />
                <Metric label="offset" value={`${formatNumber(movement.offset)} dp/pt`} />
                <Metric label="height" value={`${formatNumber(movement.height)} dp/pt`} />
                <Metric
                  label="maxDragHeight"
                  value={`${formatNumber(movement.maxDragHeight)} dp/pt`}
                />
                <Metric label="isDragging" value={movement.isDragging ? 'true' : 'false'} />
                <Metric label="配置" value={`${headerHeight} · ${spinnerStyle}`} />
              </View>
            </View>

            <View style={pageStyles.panel}>
              <Text style={pageStyles.panelTitle}>生命周期事件</Text>
              {lifecycleEvents.length === 0 ? (
                <Text style={pageStyles.emptyEvents}>等待原生 Header 事件...</Text>
              ) : (
                lifecycleEvents.map((event) => (
                  <View key={event.id} style={pageStyles.eventRow}>
                    <Text style={pageStyles.eventName}>{event.name}</Text>
                    <Text style={pageStyles.eventDetail}>{event.detail}</Text>
                  </View>
                ))
              )}
            </View>
          </View>
        }
        ListFooterComponent={
          <View style={styles.listFooter}>
            <Text style={styles.listFooterText}>
              切换配置后回到列表顶部下拉；手势刷新可验证 released、start、finish 的完整链路。
            </Text>
          </View>
        }
      />
    </SmartRefreshLayout>
  );
}

const pageStyles = StyleSheet.create({
  nativeHeader: {
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#e6f4ff',
  },
  nativeHeaderTitle: {
    color: '#0958d9',
    fontSize: 14,
    fontWeight: '700',
  },
  nativeHeaderDetail: {
    marginTop: 3,
    color: '#1677ff',
    fontSize: 11,
  },
  nativeState: {
    marginLeft: 'auto',
    color: '#8c8c8c',
    fontSize: 11,
  },
  panel: {
    marginBottom: 10,
    paddingHorizontal: 14,
    paddingVertical: 12,
    backgroundColor: '#ffffff',
    borderRadius: 6,
  },
  panelTitle: {
    color: '#262626',
    fontSize: 14,
    fontWeight: '600',
  },
  metricGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
    marginTop: 10,
  },
  metric: {
    width: '48%',
    minHeight: 52,
    justifyContent: 'center',
    paddingHorizontal: 10,
    paddingVertical: 7,
    backgroundColor: '#f5f7fa',
    borderRadius: 4,
  },
  metricLabel: {
    color: '#8c8c8c',
    fontSize: 10,
  },
  metricValue: {
    marginTop: 3,
    color: '#262626',
    fontSize: 13,
    fontWeight: '600',
  },
  emptyEvents: {
    marginTop: 10,
    color: '#8c8c8c',
    fontSize: 12,
  },
  eventRow: {
    paddingVertical: 8,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: '#f0f0f0',
  },
  eventName: {
    color: '#0958d9',
    fontSize: 12,
    fontWeight: '600',
  },
  eventDetail: {
    marginTop: 2,
    color: '#595959',
    fontSize: 11,
  },
});
