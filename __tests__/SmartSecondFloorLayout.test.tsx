import React, { createRef } from 'react';
import { Platform } from 'react-native';
import TestRenderer, { act } from 'react-test-renderer';

jest.mock('../src/NativeSmartSecondFloorLayout', () => {
  const React = require('react');
  const commands = {
    beginRefresh: jest.fn(),
    finishRefresh: jest.fn(),
    openSecondFloor: jest.fn(),
    closeSecondFloor: jest.fn(),
  };
  const NativeView = React.forwardRef((props: object, ref: React.Ref<unknown>) => {
    React.useImperativeHandle(ref, () => ({ nativeView: true }));
    return React.createElement('NativeSmartSecondFloorLayout', props);
  });
  return {
    __esModule: true,
    default: NativeView,
    Commands: commands,
  };
});

jest.mock('../src/NativeSmartSecondFloorContentSlot', () => {
  const React = require('react');
  const Slot = (props: object) => React.createElement('NativeContentSlot', props);
  return { __esModule: true, default: Slot };
});

jest.mock('../src/NativeSmartSecondFloorFloorSlot', () => {
  const React = require('react');
  const Slot = (props: object) => React.createElement('NativeFloorSlot', props);
  return { __esModule: true, default: Slot };
});

jest.mock('../src/NativeSmartSecondFloorFloorContentSlot', () => {
  const React = require('react');
  const Slot = (props: object) => React.createElement('NativeFloorContentSlot', props);
  return { __esModule: true, default: Slot };
});

import NativeSmartSecondFloorLayout, {
  Commands,
} from '../src/NativeSmartSecondFloorLayout';
import { SmartSecondFloorLayout } from '../src/SmartSecondFloorLayout';
import type { SmartSecondFloorLayoutRef } from '../src/SmartSecondFloorLayout.types';

const mockedCommands = Commands as jest.Mocked<typeof Commands>;

function Content() {
  return React.createElement('mock-content');
}

function Floor() {
  return React.createElement('mock-floor');
}

function Backdrop() {
  return React.createElement('mock-floor-backdrop');
}

beforeEach(() => {
  jest.clearAllMocks();
  Object.defineProperty(Platform, 'OS', {
    configurable: true,
    value: 'android',
  });
});

it('mounts content, backdrop, and formal content through stable slot hosts', async () => {
  let renderer: TestRenderer.ReactTestRenderer;
  await act(async () => {
    renderer = TestRenderer.create(
      <SmartSecondFloorLayout
        secondFloor={<Floor />}
        secondFloorBackground={<Backdrop />}
        floorRate={9}
        maxRate={1}
        refreshRate={10}
        floorDuration={Number.NaN}
        bottomPullUpToCloseRate={-1}
        onRefresh={jest.fn()}
      >
        <Content />
      </SmartSecondFloorLayout>
    );
  });

  const nativeView = renderer!.root.findByType(NativeSmartSecondFloorLayout);
  const contentSlot = renderer!.root.findByType('NativeContentSlot');
  const floorSlot = renderer!.root.findByType('NativeFloorSlot');
  const floorContentSlot = renderer!.root.findByType('NativeFloorContentSlot');

  expect(contentSlot.props.collapsable).toBe(false);
  expect(floorSlot.props.collapsable).toBe(false);
  expect(floorContentSlot.props.collapsable).toBe(false);
  expect(contentSlot.props.children.type).toBe(Content);
  expect(floorSlot.props.children.type).toBe(Backdrop);
  expect(floorContentSlot.props.children.type).toBe(Floor);
  expect(nativeView.props.secondFloorEnabled).toBe(true);
  expect(nativeView.props.maxRate).toBe(1.2);
  expect(nativeView.props.floorRate).toBe(1.15);
  expect(nativeView.props.refreshRate).toBeCloseTo(1.1);
  expect(nativeView.props.floorDuration).toBe(1000);
  expect(nativeView.props.bottomPullUpToCloseRate).toBe(0.01);
});

it('keeps legacy secondFloor placement when no backdrop is supplied', async () => {
  let renderer: TestRenderer.ReactTestRenderer;
  await act(async () => {
    renderer = TestRenderer.create(
      <SmartSecondFloorLayout secondFloor={<Floor />}>
        <Content />
      </SmartSecondFloorLayout>
    );
  });

  const floorSlot = renderer!.root.findByType('NativeFloorSlot');
  expect(floorSlot.props.children.type).toBe(Floor);
  expect(renderer!.root.findAllByType('NativeFloorContentSlot')).toHaveLength(0);
});

it('normalizes and forwards the header inset in density-independent pixels', async () => {
  let renderer: TestRenderer.ReactTestRenderer;
  await act(async () => {
    renderer = TestRenderer.create(
      <SmartSecondFloorLayout headerInset={56.6} secondFloor={<Floor />}>
        <Content />
      </SmartSecondFloorLayout>
    );
  });

  const nativeView = renderer!.root.findByType(NativeSmartSecondFloorLayout);
  expect(nativeView.props.headerInset).toBe(57);
});

it('dispatches idempotent floor commands and blocks refresh while opening', async () => {
  const ref = createRef<SmartSecondFloorLayoutRef>();
  await act(async () => {
    TestRenderer.create(
      <SmartSecondFloorLayout ref={ref} secondFloor={<Floor />} onRefresh={jest.fn()}>
        <Content />
      </SmartSecondFloorLayout>
    );
  });

  expect(ref.current?.openSecondFloor()).toBe(true);
  expect(ref.current?.openSecondFloor()).toBe(false);
  expect(ref.current?.beginRefresh()).toBe(false);
  expect(mockedCommands.openSecondFloor).toHaveBeenCalledTimes(1);

  expect(ref.current?.closeSecondFloor()).toBe(true);
  expect(ref.current?.closeSecondFloor()).toBe(false);
  expect(mockedCommands.closeSecondFloor).toHaveBeenCalledTimes(1);
});

it('forwards a gesture refresh with the native request id and finishes it', async () => {
  const onRefresh = jest.fn().mockResolvedValue(undefined);
  const renderer = await (async () => {
    let result!: TestRenderer.ReactTestRenderer;
    await act(async () => {
      result = TestRenderer.create(
        <SmartSecondFloorLayout secondFloor={<Floor />} onRefresh={onRefresh}>
          <Content />
        </SmartSecondFloorLayout>
      );
    });
    return result;
  })();
  const nativeView = renderer.root.findByType(NativeSmartSecondFloorLayout);

  await act(async () => {
    await nativeView.props.onRefresh({
      nativeEvent: { requestId: -7, source: 'gesture' },
    });
  });

  expect(onRefresh).toHaveBeenCalledWith({ requestId: -7, source: 'gesture' });
  expect(mockedCommands.finishRefresh).toHaveBeenCalledWith(
    expect.anything(),
    -7,
    true,
    0
  );
});

it('publishes the public second-floor lifecycle and exposes open/close callbacks', async () => {
  const onStateChange = jest.fn();
  const onSecondFloorOpen = jest.fn();
  const onSecondFloorClose = jest.fn();
  let renderer: TestRenderer.ReactTestRenderer;
  await act(async () => {
    renderer = TestRenderer.create(
      <SmartSecondFloorLayout
        secondFloor={<Floor />}
        onStateChange={onStateChange}
        onSecondFloorOpen={onSecondFloorOpen}
        onSecondFloorClose={onSecondFloorClose}
      >
        <Content />
      </SmartSecondFloorLayout>
    );
  });
  const nativeView = renderer!.root.findByType(NativeSmartSecondFloorLayout);

  act(() => {
    nativeView.props.onStateChange({
      nativeEvent: { state: 'release-to-second-floor' },
    });
    nativeView.props.onStateChange({
      nativeEvent: { state: 'second-floor-opening' },
    });
    nativeView.props.onSecondFloorOpen({ nativeEvent: null });
    nativeView.props.onStateChange({
      nativeEvent: { state: 'second-floor' },
    });
    nativeView.props.onStateChange({
      nativeEvent: { state: 'second-floor-closing' },
    });
    nativeView.props.onSecondFloorClose({ nativeEvent: null });
    nativeView.props.onStateChange({ nativeEvent: { state: 'idle' } });
  });

  expect(onStateChange.mock.calls.map(([state]) => state)).toEqual([
    'release-to-second-floor',
    'second-floor-opening',
    'second-floor',
    'second-floor-closing',
    'idle',
  ]);
  expect(onSecondFloorOpen).toHaveBeenCalledTimes(1);
  expect(onSecondFloorClose).toHaveBeenCalledTimes(1);
});

it('does not dispatch commands after the host is unmounted', async () => {
  const ref = createRef<SmartSecondFloorLayoutRef>();
  let renderer: TestRenderer.ReactTestRenderer;
  await act(async () => {
    renderer = TestRenderer.create(
      <SmartSecondFloorLayout ref={ref} secondFloor={<Floor />}>
        <Content />
      </SmartSecondFloorLayout>
    );
  });
  const heldRef = ref.current!;
  await act(async () => {
    renderer!.unmount();
  });

  expect(heldRef.openSecondFloor()).toBe(false);
  expect(heldRef.closeSecondFloor()).toBe(false);
  expect(heldRef.beginRefresh()).toBe(false);
});

it('throws an explicit Android-only error on iOS', () => {
  Object.defineProperty(Platform, 'OS', {
    configurable: true,
    value: 'ios',
  });

  expect(() => {
    act(() => {
      TestRenderer.create(
        <SmartSecondFloorLayout secondFloor={<Floor />}>
          <Content />
        </SmartSecondFloorLayout>
      );
    });
  }).toThrow('SmartSecondFloorLayout is Android-only');
});
