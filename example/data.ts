import type { ClassicSpinnerStyle, RefreshRequest } from 'expo-smartrefreshlayout';

export const PAGE_SIZE = 15;
export const TOTAL_PAGES = 4;

export type FeedItem = {
  id: string;
  title: string;
  detail: string;
  color: string;
};

const COLORS = ['#1677ff', '#13c2c2', '#52c41a', '#fa8c16', '#eb2f96'];

export type HeaderTheme = {
  id: string;
  label: string;
  primary: string;
  indicator: string;
  title: string;
};

export const CLASSIC_THEMES: HeaderTheme[] = [
  { id: 'default', label: '默认', primary: '#00000000', indicator: '#666666', title: '#666666' },
  { id: 'blue', label: '蓝色', primary: '#1677ff', indicator: '#ffffff', title: '#ffffff' },
  { id: 'green', label: '绿色', primary: '#52c41a', indicator: '#ffffff', title: '#ffffff' },
  { id: 'red', label: '红色', primary: '#f5222d', indicator: '#ffffff', title: '#ffffff' },
  { id: 'orange', label: '橙色', primary: '#fa8c16', indicator: '#ffffff', title: '#ffffff' },
];

export const MATERIAL_THEMES: HeaderTheme[] = CLASSIC_THEMES.slice(1);

export const TAOBAO_HOME_IMAGE = require('./assets/image_taobao.jpg');
export const TAOBAO_SECOND_FLOOR_BACKGROUND_IMAGE = require('./assets/image_second_floor.jpg');
export const TAOBAO_SECOND_FLOOR_CONTENT_IMAGE = require('./assets/image_second_floor_content.jpg');

export function createPage(page: number): FeedItem[] {
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

export const wait = (milliseconds: number) =>
  new Promise<void>((resolve) => setTimeout(resolve, milliseconds));

export function getErrorMessage(error: unknown): string {
  return error instanceof Error ? error.message : String(error);
}

export function requestSourceLabel(request: RefreshRequest): string {
  return request.source === 'programmatic' ? '主动调用' : '手势';
}

export type { ClassicSpinnerStyle };
