import React, { createRef } from 'react';
import TestRenderer, { act } from 'react-test-renderer';

import NativeSmartRefreshLayout, {
  Commands,
} from '../src/NativeSmartRefreshLayout';
import { SmartRefreshLayout } from '../src/SmartRefreshLayout';
import type { SmartRefreshLayoutRef } from '../src/SmartRefreshLayout.types';

jest.mock('../src/NativeSmartRefreshLayout', () => {
  const React = require('react');
  const commands = {
    beginRefresh: jest.fn(),
    finishRefresh: jest.fn(),
    beginLoadMore: jest.fn(),
    finishLoadMore: jest.fn(),
    resetNoMoreData: jest.fn(),
  };
  const NativeView = React.forwardRef((props: object, ref: React.Ref<unknown>) => {
    React.useImperativeHandle(ref, () => ({ nativeView: true }));
    return React.createElement('NativeSmartRefreshLayout', props);
  });

  return {
    __esModule: true,
    default: NativeView,
    Commands: commands,
  };
});

jest.mock('../src/NativeSmartRefreshHeaderSlot', () => {
  const React = require('react');
  return function NativeSmartRefreshHeaderSlot(props: object) {
    return React.createElement('NativeSmartRefreshHeaderSlot', props);
  };
});

const mockedCommands = Commands as jest.Mocked<typeof Commands>;

function ScrollContent() {
  return React.createElement('mock-scroll-view');
}

async function renderLayout(
  props: Partial<React.ComponentProps<typeof SmartRefreshLayout>> = {}
) {
  let renderer: TestRenderer.ReactTestRenderer;
  await act(async () => {
    renderer = TestRenderer.create(
      <SmartRefreshLayout {...props}>
        <ScrollContent />
      </SmartRefreshLayout>
    );
  });
  return renderer!;
}

function request(requestId: number, source: 'gesture' | 'programmatic' = 'gesture') {
  return { nativeEvent: { requestId, source } };
}

beforeEach(() => {
  jest.clearAllMocks();
});

afterEach(() => {
  jest.useRealTimers();
});

it('finishes one async refresh using the native request id', async () => {
  const onRefresh = jest.fn().mockResolvedValue(undefined);
  const renderer = await renderLayout({ onRefresh });
  const nativeView = renderer.root.findByType(NativeSmartRefreshLayout);

  await act(async () => {
    await nativeView.props.onRefresh(request(-1));
  });

  expect(onRefresh).toHaveBeenCalledWith({ requestId: -1, source: 'gesture' });
  expect(mockedCommands.finishRefresh).toHaveBeenCalledWith(
    expect.anything(),
    -1,
    true,
    0
  );
});

it('reports refresh errors and finishes with a failed result', async () => {
  const error = new Error('network failed');
  const onRefreshError = jest.fn();
  const renderer = await renderLayout({
    onRefresh: jest.fn().mockRejectedValue(error),
    onRefreshError,
  });
  const nativeView = renderer.root.findByType(NativeSmartRefreshLayout);

  await act(async () => {
    await nativeView.props.onRefresh(request(-1));
  });

  expect(onRefreshError).toHaveBeenCalledWith(error);
  expect(mockedCommands.finishRefresh).toHaveBeenCalledWith(
    expect.anything(),
    -1,
    false,
    0
  );
});

it('leaves completion to the caller in controlled mode', async () => {
  const renderer = await renderLayout({
    refreshing: true,
    onRefresh: jest.fn().mockResolvedValue(undefined),
  });
  const nativeView = renderer.root.findByType(NativeSmartRefreshLayout);

  await act(async () => {
    await nativeView.props.onRefresh(request(-1));
  });

  expect(mockedCommands.finishRefresh).not.toHaveBeenCalled();
});

it('ignores duplicate requests and keeps refresh and load-more mutually exclusive', async () => {
  let resolveRefresh!: () => void;
  const onRefresh = jest.fn(
    () => new Promise<void>((resolve) => (resolveRefresh = resolve))
  );
  const onLoadMore = jest.fn().mockResolvedValue(undefined);
  const renderer = await renderLayout({ onRefresh, onLoadMore });
  const nativeView = renderer.root.findByType(NativeSmartRefreshLayout);

  let firstRefresh!: Promise<void>;
  act(() => {
    firstRefresh = nativeView.props.onRefresh(request(-1));
    nativeView.props.onRefresh(request(-1));
    nativeView.props.onLoadMore(request(-2));
  });

  expect(onRefresh).toHaveBeenCalledTimes(1);
  expect(onLoadMore).not.toHaveBeenCalled();
  expect(mockedCommands.finishLoadMore).toHaveBeenCalledWith(
    expect.anything(),
    -2,
    true,
    false,
    0
  );

  await act(async () => {
    resolveRefresh();
    await firstRefresh;
  });
  expect(mockedCommands.finishRefresh).toHaveBeenCalledWith(
    expect.anything(),
    -1,
    true,
    0
  );
});

it('uses a load-more result immediately, without waiting for a hasMore render', async () => {
  const onLoadMore = jest.fn().mockResolvedValue({ hasMore: false });
  const renderer = await renderLayout({ onLoadMore, hasMore: true });
  const nativeView = renderer.root.findByType(NativeSmartRefreshLayout);

  await act(async () => {
    await nativeView.props.onLoadMore(request(-3));
  });

  expect(mockedCommands.finishLoadMore).toHaveBeenCalledWith(
    expect.anything(),
    -3,
    true,
    true,
    0
  );
});

it('does not invoke load-more when the list is already exhausted', async () => {
  const ref = createRef<SmartRefreshLayoutRef>();
  const onLoadMore = jest.fn();
  const renderer = await renderLayout({ ref, onLoadMore, hasMore: false });
  const nativeView = renderer.root.findByType(NativeSmartRefreshLayout);

  expect(ref.current?.beginLoadMore()).toBe(false);
  expect(mockedCommands.beginLoadMore).not.toHaveBeenCalled();

  await act(async () => {
    await nativeView.props.onLoadMore(request(-4));
  });

  expect(onLoadMore).not.toHaveBeenCalled();
  expect(mockedCommands.finishLoadMore).toHaveBeenCalledWith(
    expect.anything(),
    -4,
    true,
    true,
    0
  );
});

it('serializes imperative operations and carries request ids through delayed commands', async () => {
  jest.useFakeTimers();
  const ref = createRef<SmartRefreshLayoutRef>();
  await act(async () => {
    TestRenderer.create(
      <SmartRefreshLayout ref={ref} onRefresh={jest.fn()} onLoadMore={jest.fn()}>
        <ScrollContent />
      </SmartRefreshLayout>
    );
  });

  expect(ref.current?.beginRefresh(0)).toBe(true);
  expect(ref.current?.beginLoadMore(0)).toBe(false);
  expect(mockedCommands.beginRefresh).toHaveBeenCalledWith(expect.anything(), 1, 0);

  ref.current?.finishRefresh({ success: false, delay: 10.6 });
  expect(mockedCommands.finishRefresh).toHaveBeenCalledWith(
    expect.anything(),
    1,
    false,
    11
  );
  expect(ref.current?.beginLoadMore()).toBe(false);

  act(() => {
    jest.advanceTimersByTime(11);
  });
  expect(ref.current?.beginLoadMore()).toBe(true);
  expect(mockedCommands.beginLoadMore).toHaveBeenCalledWith(expect.anything(), 2, 0);
});

it('does not let a stale async refresh finish a newer operation', async () => {
  let resolveFirst!: () => void;
  let resolveSecond!: () => void;
  const onRefresh = jest
    .fn()
    .mockImplementationOnce(() => new Promise<void>((resolve) => (resolveFirst = resolve)))
    .mockImplementationOnce(() => new Promise<void>((resolve) => (resolveSecond = resolve)));
  const ref = createRef<SmartRefreshLayoutRef>();
  let renderer: TestRenderer.ReactTestRenderer;
  await act(async () => {
    renderer = TestRenderer.create(
      <SmartRefreshLayout ref={ref} onRefresh={onRefresh}>
        <ScrollContent />
      </SmartRefreshLayout>
    );
  });
  const nativeView = renderer.root.findByType(NativeSmartRefreshLayout);

  let firstPromise!: Promise<void>;
  act(() => {
    firstPromise = nativeView.props.onRefresh(request(-5));
  });
  act(() => {
    ref.current?.finishRefresh();
  });
  let secondPromise!: Promise<void>;
  act(() => {
    secondPromise = nativeView.props.onRefresh(request(-6));
  });
  expect(onRefresh).toHaveBeenCalledTimes(2);
  await act(async () => {
    resolveFirst();
    await firstPromise;
  });
  expect(mockedCommands.finishRefresh).toHaveBeenCalledTimes(1);
  resolveSecond();
  await act(async () => {
    await secondPromise;
  });
  expect(mockedCommands.finishRefresh).toHaveBeenCalledWith(
    expect.anything(),
    -6,
    true,
    0
  );
});

it('only exposes auto loading as an explicit mode and does not trigger it on mount', async () => {
  const onLoadMore = jest.fn();
  const renderer = await renderLayout({ onLoadMore, loadMoreMode: 'auto' });
  const nativeView = renderer.root.findByType(NativeSmartRefreshLayout);

  expect(nativeView.props.autoLoadMoreEnabled).toBe(true);
  expect(onLoadMore).not.toHaveBeenCalled();
});

it('mounts custom header content in the native slot and forwards header movement', async () => {
  const onHeaderMoving = jest.fn();
  const refreshHeader = React.createElement('mock-refresh-header');
  const renderer = await renderLayout({ refreshHeader, onHeaderMoving });
  const nativeView = renderer.root.findByType(NativeSmartRefreshLayout);
  const headerSlot = renderer.root.findByType('NativeSmartRefreshHeaderSlot');
  const movement = {
    percent: 0.75,
    offset: 60,
    height: 80,
    maxDragHeight: 160,
    isDragging: true,
  };

  expect(headerSlot.props).toEqual(
    expect.objectContaining({ collapsable: false })
  );
  expect(headerSlot.findByType('mock-refresh-header')).toBeDefined();

  await act(async () => {
    nativeView.props.onHeaderMoving({ nativeEvent: movement });
  });

  expect(onHeaderMoving).toHaveBeenCalledWith(movement);
});

it('normalizes custom header configuration before it reaches native', async () => {
  const renderer = await renderLayout({
    refreshHeader: React.createElement('mock-refresh-header'),
    refreshHeaderHeight: Number.NaN,
    refreshHeaderTriggerRate: Number.POSITIVE_INFINITY,
    refreshHeaderMaxDragRate: -1,
    refreshHeaderFinishDuration: -10,
  });
  const nativeView = renderer.root.findByType(NativeSmartRefreshLayout);
  const headerSlot = renderer.root.findByType('NativeSmartRefreshHeaderSlot');

  expect(nativeView.props.refreshHeaderHeight).toBe(80);
  expect(nativeView.props.refreshHeaderSpinnerStyle).toBe('translate');
  expect(nativeView.props.refreshHeaderTriggerRate).toBe(1);
  expect(nativeView.props.refreshHeaderMaxDragRate).toBe(2);
  expect(nativeView.props.refreshHeaderFinishDuration).toBe(0);
  expect(headerSlot.props.style).toEqual(
    expect.arrayContaining([expect.objectContaining({ height: 80 })])
  );
});

it('forwards custom header geometry, spinner style, and completion duration', async () => {
  const renderer = await renderLayout({
    refreshHeader: React.createElement('mock-refresh-header'),
    refreshHeaderHeight: 96.6,
    refreshHeaderSpinnerStyle: 'fixed-behind',
    refreshHeaderTriggerRate: 1.25,
    refreshHeaderMaxDragRate: 2.5,
    refreshHeaderFinishDuration: 350.4,
  });
  const nativeView = renderer.root.findByType(NativeSmartRefreshLayout);
  const headerSlot = renderer.root.findByType('NativeSmartRefreshHeaderSlot');

  expect(nativeView.props.refreshHeaderHeight).toBe(97);
  expect(nativeView.props.refreshHeaderSpinnerStyle).toBe('fixed-behind');
  expect(nativeView.props.refreshHeaderTriggerRate).toBe(1);
  expect(nativeView.props.refreshHeaderMaxDragRate).toBe(2.5);
  expect(nativeView.props.refreshHeaderFinishDuration).toBe(350);
  expect(headerSlot.props.style).toEqual(
    expect.arrayContaining([expect.objectContaining({ height: 97 })])
  );
});

it('forwards dynamic custom header height and spinner style updates', async () => {
  const renderer = await renderLayout({
    refreshHeader: React.createElement('mock-refresh-header'),
    refreshHeaderHeight: 80,
    refreshHeaderSpinnerStyle: 'translate',
  });

  await act(async () => {
    renderer.update(
      <SmartRefreshLayout
        refreshHeader={React.createElement('mock-refresh-header')}
        refreshHeaderHeight={120}
        refreshHeaderSpinnerStyle="scale"
      >
        <ScrollContent />
      </SmartRefreshLayout>
    );
  });

  const nativeView = renderer.root.findByType(NativeSmartRefreshLayout);
  const headerSlot = renderer.root.findByType('NativeSmartRefreshHeaderSlot');
  expect(nativeView.props.refreshHeaderHeight).toBe(120);
  expect(nativeView.props.refreshHeaderSpinnerStyle).toBe('scale');
  expect(headerSlot.props.style).toEqual(
    expect.arrayContaining([expect.objectContaining({ height: 120 })])
  );
});

it('uses multiplier-only custom header rate ranges shared by both native kernels', async () => {
  const renderer = await renderLayout({
    refreshHeaderTriggerRate: 2,
    refreshHeaderMaxDragRate: 0.5,
    refreshHeader: React.createElement('mock-refresh-header'),
  });
  const nativeView = renderer.root.findByType(NativeSmartRefreshLayout);

  expect(nativeView.props.refreshHeaderTriggerRate).toBe(1);
  expect(nativeView.props.refreshHeaderMaxDragRate).toBe(2);

  await act(async () => {
    renderer.update(
      <SmartRefreshLayout
        refreshHeader={React.createElement('mock-refresh-header')}
        refreshHeaderTriggerRate={0.5}
        refreshHeaderMaxDragRate={10}
      >
        <ScrollContent />
      </SmartRefreshLayout>
    );
  });

  const updatedNativeView = renderer.root.findByType(NativeSmartRefreshLayout);
  expect(updatedNativeView.props.refreshHeaderTriggerRate).toBe(0.5);
  expect(updatedNativeView.props.refreshHeaderMaxDragRate).toBe(9);
});

it('forwards custom header lifecycle events without their native event wrapper', async () => {
  const onHeaderInitialized = jest.fn();
  const onHeaderReleased = jest.fn();
  const onHeaderStart = jest.fn();
  const onHeaderFinish = jest.fn();
  const renderer = await renderLayout({
    refreshHeader: React.createElement('mock-refresh-header'),
    onHeaderInitialized,
    onHeaderReleased,
    onHeaderStart,
    onHeaderFinish,
  });
  const nativeView = renderer.root.findByType(NativeSmartRefreshLayout);
  const lifecycle = { height: 96, maxDragHeight: 240 };

  await act(async () => {
    nativeView.props.onHeaderInitialized({ nativeEvent: lifecycle });
    nativeView.props.onHeaderReleased({ nativeEvent: lifecycle });
    nativeView.props.onHeaderStart({ nativeEvent: lifecycle });
    nativeView.props.onHeaderFinish({ nativeEvent: { success: false } });
  });

  expect(onHeaderInitialized).toHaveBeenCalledWith(lifecycle);
  expect(onHeaderReleased).toHaveBeenCalledWith(lifecycle);
  expect(onHeaderStart).toHaveBeenCalledWith(lifecycle);
  expect(onHeaderFinish).toHaveBeenCalledWith({ success: false });
});

it('forwards the official Classic header configuration to the native component', async () => {
  const renderer = await renderLayout({
    headerStyle: 'classic',
    primaryColor: '#1677ff',
    indicatorColor: '#ffffff',
    titleColor: '#ffffff',
    classicSpinnerStyle: 'fixed-behind',
    classicEnableLastTime: false,
  });
  const nativeView = renderer.root.findByType(NativeSmartRefreshLayout);

  expect(nativeView.props).toEqual(
    expect.objectContaining({
      headerStyle: 'classic',
      primaryColor: '#1677ff',
      indicatorColor: '#ffffff',
      titleColor: '#ffffff',
      classicSpinnerStyle: 'fixed-behind',
      classicEnableLastTime: false,
    })
  );
});

it('preserves the transparent FixedBehind Classic configuration used by the official sample', async () => {
  const renderer = await renderLayout({
    headerStyle: 'classic',
    primaryColor: '#00000000',
    indicatorColor: '#666666',
    titleColor: '#666666',
    classicSpinnerStyle: 'fixed-behind',
    classicEnableLastTime: true,
  });
  const nativeView = renderer.root.findByType(NativeSmartRefreshLayout);

  expect(nativeView.props).toEqual(
    expect.objectContaining({
      primaryColor: '#00000000',
      indicatorColor: '#666666',
      titleColor: '#666666',
      classicSpinnerStyle: 'fixed-behind',
      classicEnableLastTime: true,
    })
  );
});

it('forwards dynamic Classic spinner style changes in every supported mode', async () => {
  const renderer = await renderLayout({
    headerStyle: 'classic',
    classicSpinnerStyle: 'fixed-behind',
  });

  const updateSpinnerStyle = async (
    classicSpinnerStyle: 'scale' | 'translate' | 'fixed-behind'
  ) => {
    await act(async () => {
      renderer.update(
        <SmartRefreshLayout
          headerStyle="classic"
          classicSpinnerStyle={classicSpinnerStyle}
        >
          <ScrollContent />
        </SmartRefreshLayout>
      );
    });
    expect(
      renderer.root.findByType(NativeSmartRefreshLayout).props.classicSpinnerStyle
    ).toBe(classicSpinnerStyle);
  };

  await updateSpinnerStyle('scale');
  await updateSpinnerStyle('translate');
  await updateSpinnerStyle('fixed-behind');
});

it('keeps the final Classic configuration through rapid header family changes', async () => {
  const renderer = await renderLayout({
    headerStyle: 'classic',
    classicSpinnerStyle: 'translate',
  });

  await act(async () => {
    renderer.update(
      <SmartRefreshLayout
        headerStyle="material"
        materialShowBezierWave
        materialEnableHeaderTranslationContent
      >
        <ScrollContent />
      </SmartRefreshLayout>
    );
  });
  expect(renderer.root.findByType(NativeSmartRefreshLayout).props.headerStyle).toBe(
    'material'
  );

  await act(async () => {
    renderer.update(
      <SmartRefreshLayout
        headerStyle="classic"
        classicSpinnerStyle="scale"
        classicEnableLastTime={false}
      >
        <ScrollContent />
      </SmartRefreshLayout>
    );
  });
  expect(
    renderer.root.findByType(NativeSmartRefreshLayout).props.classicSpinnerStyle
  ).toBe('scale');

  await act(async () => {
    renderer.update(
      <SmartRefreshLayout
        headerStyle="classic"
        classicSpinnerStyle="fixed-behind"
        classicEnableLastTime
      >
        <ScrollContent />
      </SmartRefreshLayout>
    );
  });

  expect(renderer.root.findByType(NativeSmartRefreshLayout).props).toEqual(
    expect.objectContaining({
      headerStyle: 'classic',
      classicSpinnerStyle: 'fixed-behind',
      classicEnableLastTime: true,
    })
  );
});

it('forwards the official Material header configuration to the native component', async () => {
  const renderer = await renderLayout({
    headerStyle: 'material',
    primaryColor: '#52c41a',
    indicatorColor: '#ffffff',
    materialShowBezierWave: true,
    materialEnableHeaderTranslationContent: true,
    materialProgressBackgroundColor: '#52c41a',
  });
  const nativeView = renderer.root.findByType(NativeSmartRefreshLayout);

  expect(nativeView.props).toEqual(
    expect.objectContaining({
      headerStyle: 'material',
      primaryColor: '#52c41a',
      indicatorColor: '#ffffff',
      materialShowBezierWave: true,
      materialEnableHeaderTranslationContent: true,
      materialProgressBackgroundColor: '#52c41a',
    })
  );
});
