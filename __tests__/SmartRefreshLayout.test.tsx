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
