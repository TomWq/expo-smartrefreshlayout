/*
 * @Author       : 尚博信_王强 wangqiang03@sunboxsoft.com
 * @Date         : 2025-11-12 10:14:48
 * @LastEditors  : 尚博信_王强 wangqiang03@sunboxsoft.com
 * @LastEditTime : 2025-11-13 11:10:16
 * @FilePath     : /expo-smartrefreshlayout/src/ExpoSmartrefreshlayout.types.ts
 * @Description  : 
 * 
 * Copyright (c) 2025 by 尚博信_王强, All Rights Reserved. 
 */

import { StyleProp, ViewStyle } from "react-native";


//经典刷新头的属性接口
export interface ClassicRefreshHeaderProps {

  REFRESH_HEADER_PULLING?: string; //"下拉可以刷新";
  REFRESH_HEADER_REFRESHING?: string;  //"正在刷新...";
  /**
   * @platform android
   * iOS 不支持此状态
   */
  REFRESH_HEADER_LOADING?: string;//"正在加载...";
  REFRESH_HEADER_RELEASE?: string; //"释放立即刷新";
  REFRESH_HEADER_FINISH?: string; //"刷新完成";
  /**
   * @platform android
   * iOS 的 MJRefresh 不支持失败状态
   */
  REFRESH_HEADER_FAILED?: string; //"刷新失败";
  /**
   * @platform android
   * iOS 的 MJRefresh 不支持二楼功能
   */
  REFRESH_HEADER_SECONDARY?: string; //"释放进入二楼";
  /**
   * @platform android
   * iOS 的 MJRefresh 时间格式是内置的
   */
  REFRESH_HEADER_UPDATE?: string; //"上次更新 M-d HH:mm";

  /**
   * 设置强调颜色
   */
  headerAccentColor?: string
  /**
   * 设置主题颜色
   * @platform android
   * iOS 的 MJRefresh 没有直接的主题色概念
   */
  headerPrimaryColor?: string

  /**
   * 设置标题文字大小（sp单位）
   */
  headerTitleTextSize?: number
  /**
   * 设置时间文字大小（sp单位）
   */
  headerTimeTextSize?: number

  /**
   * 设置标题文字的上边距（dp单位）
   * @platform android
   * iOS 的 MJRefresh 不支持单独设置时间标签的上边距
   */
  headerTimePaddingTop?: number

  /**
   * 是否显示时间
   */
  headerShowTime?: boolean
  /**
   * 设置刷新完成显示的停留时间（设为0可以关闭停留功能）
   * @platform android
   * iOS 需要手动实现延迟逻辑
   */
  headerFinishDuration?: number

  /**
   * 同时设置箭头和图片的大小（dp单位）
   * @platform android
   * iOS 的 MJRefresh 图标大小控制有限
   */
  headerDrawableSize?: number

  /**
   * 设置箭头的大小（dp单位）
   * @platform android
   * iOS 的 MJRefresh 图标大小控制有限
   */
  headerDrawableArrowSize?: number

  /**
   * 设置图片的大小（dp单位）
   * @platform android
   * iOS 的 MJRefresh 图标大小控制有限
   */
  headerDrawableProgressSize?: number

  /**
   * 设置图片和箭头和文字的间距（dp单位）
   * @platform android
   * iOS 的 MJRefresh 不支持精确控制间距
   */
  headerDrawableMarginRight?: number

  /**
   * 设置箭头图片
   * @platform android
   * iOS 支持但当前未实现
   */
  headerDrawableArrow?: string;
  /**
   * 设置转动图片
   * @platform android
   * iOS 支持但当前未实现
   */
  headerDrawableProgress?: string;

  /**
   * 设置时间格式化（时间会自动更新）
   * @platform android
   * iOS 的 MJRefresh 不支持自定义时间格式
   */
  headerTimeFormat?: string

  /**
   * 手动更新时间文字设置（将不会自动更新时间）
   * iOS 支持但当前未实现
   */
  headerLastUpdateText?: string

}

//经典加载脚的属性接口
export interface ClassicLoadMoreFooterProps {

  REFRESH_FOOTER_PULLING?: string; //"上拉可以加载更多";
  REFRESH_FOOTER_LOADING?: string;  //"正在加载...";
  /**
   * @platform android
   * iOS Footer 通常只有 loading 状态
   */
  REFRESH_FOOTER_REFRESHING?: string;//"正在加载...";
  /**
   * @platform android
   * iOS 的 MJRefreshAutoFooter 是自动触发的，没有"释放加载"状态
   */
  REFRESH_FOOTER_RELEASE?: string; //"释放立即加载";
  /**
   * @platform android
   * iOS 的 MJRefresh 没有单独的完成状态文字
   */
  REFRESH_FOOTER_FINISH?: string; //"加载完成";
  REFRESH_FOOTER_FAILED?: string; //"加载失败";
  REFRESH_FOOTER_NOTHING?: string; //"没有更多数据";

  /**
   * 设置强调颜色
   */
  footerAccentColor?: string

  /**
   * 设置主题颜色
   * @platform android
   * iOS 的 MJRefresh 没有主题色概念
   */
  footerPrimaryColor?: string

  /**
   * 设置标题文字大小（sp单位）
   */
  footerTitleTextSize?: number

  /**
   * 同时设置箭头和图片的大小（dp单位）
   * @platform android
   * iOS 的 MJRefresh 控制有限
   */
  footerDrawableSize?: number

  /**
   * 设置箭头的大小（dp单位）
   * @platform android
   * iOS 的 MJRefresh 控制有限
   */
  footerDrawableArrowSize?: number

  /**
   * 设置图片的大小（dp单位）
   * @platform android
   * iOS 的 MJRefresh 控制有限
   */
  footerDrawableProgressSize?: number

  /**
   * 设置图片和箭头和文字的间距（dp单位）
   * @platform android
   * iOS 的 MJRefresh 不支持精确控制间距
   */
  footerDrawableMarginRight?: number

  /**
   * 设置箭头图片
   * @platform android
   * iOS 当前未实现
   */
  footerDrawableArrow?: string;

  /**
   * 设置加载完成显示的停留时间（设为0可以关闭停留功能）
   * @default 1000
   * @platform android
   * iOS 当前未实现
   */
  footerFinishDuration?: number

}

export type onHeaderMoveProps = {
  isDragging: boolean; 
  percent: number; 
  offset: number; 
  headerHeight: number
}

export type ExpoSmartrefreshlayoutViewProps = {

  style?: StyleProp<ViewStyle>
  
  children?: React.ReactNode

  /**
   * 自定义下拉刷新头部
   */
  renderHeader?: () => React.ReactNode

  /**
   * 是否启用下拉刷新功能
   * @default true
   */
  enableRefresh?: boolean

  /**
   * 是否启用上拉加载功能
   * @default false
   * @description 默认关闭以避免与 FlatList 的 onEndReached 冲突。
   * 如需使用组件提供的加载更多功能，请显式设置为 true。
   */
  enableLoadMore?: boolean

  /**
   * 是否启用列表惯性滑动到底部时自动加载更多
   * @default false
   */
  enableAutoLoadMore?: boolean

  /**
   * 是否启用纯滚动模式
   * @default false
   * @platform android
   * iOS 当前仅存储值，未实际使用
   */
  enablePureScrollMode?: boolean

  /**
   * 是否在加载完成时滚动列表显示新的内容
   * @default true
   * @platform android
   * iOS 的 MJRefresh 不支持此功能
   */
  enableScrollContentWhenLoaded?: boolean

  /**
   * 是否在刷新完成时滚动列表显示新的内容
   * @default true
   * @platform android
   * iOS 的 MJRefresh 不支持此功能
   */
  enableScrollContentWhenRefreshed?: boolean

  /**
   * 是否下拉Header的时候向下平移列表或者内容
   * @default true
   * @platform android
   * iOS 的 MJRefresh 动画效果是固定的，无法控制
   */
  enableHeaderTranslationContent?: boolean

  /**
   * 是否上拉Footer的时候向上平移列表或者内容
   * @default true
   * @platform android
   * iOS 的 MJRefresh 动画效果是固定的，无法控制
   */
  enableFooterTranslationContent?: boolean

  /**
   * 是否在列表不满一页时候开启上拉加载功能
   * @default false
   * @platform android
   * iOS 映射到了 isAutomaticallyChangeAlpha（语义不同）
   */
  enableLoadMoreWhenContentNotFull?: boolean

  /**
   * 是否启用越界拖动（仿苹果效果）
   * @default true
   */
  enableOverScrollDrag?: boolean

  /**
   * 是否启用越界回弹
   * @default true
   */
  enableOverScrollBounce?: boolean


  /**
   * Header 标准高度
   * @default 60
   */
  headerHeight?: number

  /**
   * Header 拖动比率
   * @default 1
   * @platform android
   * iOS 的 MJRefresh 不支持拖动比率设置
   */
  dragRate?: number

  /**
   * Footer 标准高度
   * @default 60
   */
  footerHeight?: number

  /**
   * Header 起始位置偏移量
   * @default 0
   * @platform android
   * iOS 的 MJRefresh 不支持偏移量设置
   */
  headerInsetStart?: number

  /**
   * Footer 起始位置偏移量
   * @default 0
   * @platform android
   * iOS 的 MJRefresh 不支持偏移量设置
   */
  footerInsetStart?: number

  /**
   * Header 最大显示下拉高度/Header标准高度
   * @default 100
   * @platform android
   * iOS 的 MJRefresh 没有最大拖动率的概念
   */
  headerMaxDragRate?: number

  /**
   * Footer 最大显示上拉高度/Footer标准高度
   * @default 1
   * @platform android
   * iOS 的 MJRefresh 没有最大拖动率的概念
   */
  footerMaxDragRate?: number //最大显示上拉高度/Footer标准高度

  /**
   * 刷新触发比率
   * @default 1
   * @platform android
   * iOS 的 MJRefresh 触发高度是固定的
   */
  headerTriggerRate?: number

  /**
   * 加载更多触发比率
   * @default 1
   * @platform android
   * iOS 的 MJRefresh 触发高度是固定的
   */
  footerTriggerRate?: number

  /**
   * 回弹动画时长（毫秒）
   * @default 300
   * @platform android
   * iOS 的 MJRefresh 动画时长是内置的，不支持自定义
   */
  reboundDuration?: number

  /**
   * 是否启用嵌套滚动
   * @default true
   * @platform android
   * iOS 的滚动视图嵌套机制与 Android 完全不同
   */
  enableNestedScroll?: boolean

  /**
   * 是否启用触觉反馈（震动反馈）
   * 当下拉/上拉到可以释放刷新的临界点时，手机会震动提示
   * @default true
   */
  enableHapticFeedback?: boolean

  /**
   *  Header 类型
   * classics: 经典
   * material:  Material
   * 其他类型待扩展
   */
  headerType?: 'classics' | 'material' | string;

  /** 
   * 经典刷新头属性
   * 
   */
  classicRefreshHeaderProps?: ClassicRefreshHeaderProps; //经典刷新头属性

  /**
   * 经典加载脚属性
   * 
   */
  classicLoadMoreFooterProps?: ClassicLoadMoreFooterProps;

  /**
   * 下拉刷新回调
   */
  onRefresh?: () => void
  /**
   * 上拉加载回调
   */
  onLoadMore?: () => void

  /**
   * 状态改变回调
   * @param state - 当前状态
   * @note iOS 当前仅在 finishRefresh 和 finishLoadMore 中触发，未监听 MJRefresh 的完整状态变化
   */
  onStateChanged?: (state: RefreshState) => void

  /**
   * Header 移动回调
   * @platform android
   */
  onHeaderMoving?: (params: onHeaderMoveProps) => void

  /**
   * Footer 移动回调
   * @platform android
   * iOS 当前未实现，需要监听 scrollView 的 contentOffset
   */
  onFooterMoving?: (params:onHeaderMoveProps) => void


}

export type ExpoSmartrefreshlayoutModuleEvents  = {

  /**
 * 完成刷新操作
 * @param {boolean} [success] - 是否刷新成功，可选参数
 * @param {number} [delay] - 延迟时间（毫秒），可选参数
 */
  finishRefresh:(success?: boolean, delay?: number)=> void

  /**
 * 完成加载更多操作
 * @param {boolean} [success] - 是否加载成功，可选参数
 * @param {number} [delay] - 延迟时间(毫秒)，可选参数 
 * @param {boolean} [noMoreData] - 是否没有更多数据，可选参数
 * @returns {void}
 */
  finishLoadMore:(success?: boolean, delay?: number, noMoreData?: boolean)=> void


  /**
 * 自动刷新
 * @param {number} [delay] - 可选参数，延迟时间（毫秒）。如果未提供，则使用默认值
 * @returns {void} 无返回值
 */
  autoRefresh:(delay?: number)=> void


  /**
 * 自动加载更多
 * @param {number} [delay] - 自动加载更多的延迟时间（毫秒），可选参数
 * @returns {void}
 */
  autoLoadMore:(delay?: number)=>void


  /**
 * 设置是否没有更多数据可加载
 * @param {boolean} noMoreData - 是否没有更多数据可加载的标志
 * @returns {void}
 */
  setNoMoreData:(noMoreData: boolean)=> void

}

/**
 * 刷新状态枚举
 * @enum {number}
 */
export enum RefreshState {
  None = 0,           // 无状态
  PullDownToRefresh = 1,  // 下拉刷新
  ReleaseToRefresh = 2,   // 释放立即刷新
  Refreshing = 3,     // 正在刷新
  RefreshFinish = 4,  // 刷新完成
  PullUpToLoad = 5,   // 上拉加载
  ReleaseToLoad = 6,  // 释放立即加载
  Loading = 7,        // 正在加载
  LoadFinish = 8,     // 加载完成
  NoMoreData = 9,     // 没有更多数据
}
