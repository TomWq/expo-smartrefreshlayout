#import "ExpoSmartRefreshLayoutView.h"
#import "ExpoSmartRefreshHeaderSlotView.h"
#import "RNSmartCustomHeader.h"

#import "SmartRefreshControl/RNSmartRefreshAdapter.h"
#import <React/RCTComponentViewFactory.h>
#import <React/RCTConversions.h>

#import <react/renderer/components/ExpoSmartRefreshLayoutSpec/ComponentDescriptors.h>
#import <react/renderer/components/ExpoSmartRefreshLayoutSpec/EventEmitters.h>
#import <react/renderer/components/ExpoSmartRefreshLayoutSpec/Props.h>
#import <react/renderer/components/ExpoSmartRefreshLayoutSpec/RCTComponentViewHelpers.h>

#import "RCTFabricComponentsPlugins.h"

#include <limits.h>
#include <cmath>

using namespace facebook::react;

static NSString *RNSStringFromStdString(const std::string &value)
{
  return [NSString stringWithUTF8String:value.c_str()];
}

static void RNSFindBestScrollView(UIView *view, UIScrollView **best, CGFloat *bestArea)
{
  // FlatList/ScrollView 在 Fabric 下可能包含包装视图。选择可视面积最大的
  // UIScrollView，避免误绑定内部尺寸较小的辅助滚动容器。
  if ([view isKindOfClass:[UIScrollView class]]) {
    UIScrollView *candidate = (UIScrollView *)view;
    CGFloat area = CGRectGetWidth(candidate.bounds) * CGRectGetHeight(candidate.bounds);
    if (*best == nil || area > *bestArea) {
      *best = candidate;
      *bestArea = area;
    }
  }

  for (UIView *subview in view.subviews) {
    RNSFindBestScrollView(subview, best, bestArea);
  }
}

static UIScrollView *RNSFindScrollView(UIView *view)
{
  UIScrollView *best = nil;
  CGFloat bestArea = -1;
  RNSFindBestScrollView(view, &best, &bestArea);
  return best;
}

typedef NS_ENUM(NSInteger, RNSOperationKind) {
  RNSOperationKindNone = 0,
  RNSOperationKindRefresh,
  RNSOperationKindLoadMore,
};

@interface ExpoSmartRefreshLayoutView () <RCTExpoSmartRefreshLayoutViewViewProtocol>
- (void)emitRefresh:(NSInteger)requestId source:(NSString *)source;
- (void)emitLoadMore:(NSInteger)requestId source:(NSString *)source;
- (void)emitState:(NSString *)state;
- (void)emitHeaderMovingWithOffset:(CGFloat)offset
                           percent:(CGFloat)percent
                        isDragging:(BOOL)isDragging;
- (NSInteger)allocateGestureRequestId;
- (void)beginRefreshVisualOnly;
- (void)beginLoadMoreVisualOnly;
- (void)disarmAutoLoadMore;
- (void)cancelOperation:(RNSOperationKind)kind;
- (void)invalidateDelayedOperations;
- (void)refreshComponentDidRequest:(UIRefreshHeader *)header;
- (void)loadMoreComponentDidRequest:(UIRefreshFooter *)footer;
- (void)headerStateChanged:(UIRefreshStatus)oldStatus status:(UIRefreshStatus)status;
- (void)footerStateChanged:(UISmartFooterStatus)oldStatus status:(UISmartFooterStatus)status;
- (void)attachToScrollView:(UIScrollView *)scrollView;
- (void)detachFromScrollView;
- (void)configureHeader;
- (void)configureFooter;
@end

@implementation ExpoSmartRefreshLayoutView {
  // Fabric 子树中的真实滚动容器，以及附着在它上面的原生刷新组件。
  UIScrollView *_scrollView;
  UIRefreshHeader *_header;
  UIRefreshFooter *_footer;
  ExpoSmartRefreshHeaderSlotView *_headerSlot;
  UIView<RCTComponentViewProtocol> *_contentComponentView;

  BOOL _refreshEnabled;
  BOOL _loadMoreEnabled;
  BOOL _autoLoadMoreEnabled;
  BOOL _hapticsEnabled;
  BOOL _materialHeader;
  RNSmartClassicSpinnerStyle _classicSpinnerStyle;
  BOOL _classicEnableLastTime;
  UISmartScrollMode _classicScrollMode;
  BOOL _refreshing;
  BOOL _loadingMore;
  BOOL _noMoreData;
  BOOL _didTriggerHeaderHaptic;
  BOOL _suppressNextRefreshRequest;
  BOOL _suppressNextLoadMoreRequest;
  BOOL _refreshEventEmitted;
  BOOL _loadMoreEventEmitted;

  // active 表示已开始的请求，scheduled 表示仍在等待 delayMs 的命令。
  // 两者共用一把操作锁，保证刷新和加载更多不能并发或重复触发。
  RNSOperationKind _activeOperationKind;
  NSInteger _activeRequestId;
  RNSOperationKind _scheduledOperationKind;
  NSInteger _scheduledRequestId;
  // 手势请求使用负数 ID，与 JS 发起命令时使用的正数 ID 分开，便于追踪来源。
  NSInteger _nextGestureRequestId;

  // dispatch_after 无法直接取消。每次调度/作废时递增 generation，延迟回调
  // 只有在代次仍一致时才可执行，防止旧命令影响后续请求或已回收的视图。
  NSUInteger _refreshBeginGeneration;
  NSUInteger _loadMoreBeginGeneration;
  NSUInteger _refreshFinishGeneration;
  NSUInteger _loadMoreFinishGeneration;

  UIColor *_indicatorColor;
  UIColor *_titleColor;
  UIColor *_primaryColor;
  UIColor *_materialProgressBackgroundColor;
  NSString *_pullDownText;
  NSString *_releaseToRefreshText;
  NSString *_refreshingText;
  NSString *_refreshCompleteText;
  NSString *_pullUpText;
  NSString *_releaseToLoadMoreText;
  NSString *_loadingMoreText;
  NSString *_noMoreDataText;
}

+ (ComponentDescriptorProvider)componentDescriptorProvider
{
  return concreteComponentDescriptorProvider<ExpoSmartRefreshLayoutViewComponentDescriptor>();
}

+ (void)load
{
  // Expo 生成的第三方 provider 通过 NSClassFromString 查找组件。这里提前注册，
  // 也兼容静态库/链接器在组件类尚未实例化前就执行查找的情况。
  [RCTComponentViewFactory.currentComponentViewFactory registerComponentViewClass:self];
}

- (instancetype)initWithFrame:(CGRect)frame
{
  if (self = [super initWithFrame:frame]) {
    static const auto defaultProps = std::make_shared<const ExpoSmartRefreshLayoutViewProps>();
    _props = defaultProps;
    _refreshEnabled = YES;
    _hapticsEnabled = YES;
    _classicSpinnerStyle = RNSmartClassicSpinnerStyleTranslate;
    _nextGestureRequestId = -1;
    _indicatorColor = UIColor.systemBlueColor;
    _titleColor = UIColor.secondaryLabelColor;
    _primaryColor = UIColor.clearColor;
    _materialProgressBackgroundColor = UIColor.whiteColor;
    _classicScrollMode = UISmartScrollModeMove;
    self.clipsToBounds = YES;
  }
  return self;
}

- (void)mountChildComponentView:(UIView<RCTComponentViewProtocol> *)childComponentView
                          index:(NSInteger)index
{
  if ([childComponentView isKindOfClass:[ExpoSmartRefreshHeaderSlotView class]]) {
    NSAssert(_headerSlot == nil, @"SmartRefreshLayout accepts one custom refresh header.");
    NSAssert(childComponentView.superview == nil, @"Custom refresh header is already mounted.");
    _headerSlot = (ExpoSmartRefreshHeaderSlotView *)childComponentView;
    if (_scrollView != nil && _refreshEnabled) {
      [self configureHeader];
    }
    return;
  }

  NSAssert(_contentComponentView == nil, @"SmartRefreshLayout accepts exactly one scroll-content child.");
  NSAssert(childComponentView.superview == nil, @"SmartRefreshLayout content is already mounted.");
  _contentComponentView = childComponentView;
  [self insertSubview:childComponentView atIndex:0];

  // Fabric 完成 mount 回调时，子树里的 ScrollView 可能还未完成 UIKit 层级挂载。
  // 延迟到下一轮主队列再搜索，layoutSubviews 中还会提供一次兜底绑定。
  __weak ExpoSmartRefreshLayoutView *weakSelf = self;
  dispatch_async(dispatch_get_main_queue(), ^{
    ExpoSmartRefreshLayoutView *strongSelf = weakSelf;
    if (strongSelf == nil || childComponentView.superview == nil) {
      return;
    }
    [strongSelf attachToScrollView:RNSFindScrollView(childComponentView)];
  });
}

- (void)unmountChildComponentView:(UIView<RCTComponentViewProtocol> *)childComponentView
                             index:(NSInteger)index
{
  if (childComponentView == _headerSlot) {
    if (_header != nil && [_header isKindOfClass:[RNSmartCustomHeader class]]) {
      [_header removeFromSuperview];
      _header = nil;
    }
    [childComponentView removeFromSuperview];
    _headerSlot = nil;
    if (_scrollView != nil && _refreshEnabled) {
      [self configureHeader];
    }
    return;
  }
  if (_scrollView != nil &&
      (_scrollView == (UIScrollView *)childComponentView ||
       [_scrollView isDescendantOfView:childComponentView])) {
    [self detachFromScrollView];
  }
  if (childComponentView == _contentComponentView) {
    _contentComponentView = nil;
  }
  [childComponentView removeFromSuperview];
}

- (void)layoutSubviews
{
  [super layoutSubviews];
  if (_scrollView == nil) {
    [self attachToScrollView:RNSFindScrollView(_contentComponentView ?: self)];
  }
  if ([_header isKindOfClass:[RNSmartCustomHeader class]] && _headerSlot != nil) {
    [(RNSmartCustomHeader *)_header updateContentHeight:CGRectGetHeight(_headerSlot.bounds)];
  }
}

- (void)updateProps:(Props::Shared const &)props oldProps:(Props::Shared const &)oldProps
{
  const auto &oldViewProps = *std::static_pointer_cast<ExpoSmartRefreshLayoutViewProps const>(_props);
  const auto &newViewProps = *std::static_pointer_cast<ExpoSmartRefreshLayoutViewProps const>(props);

  BOOL oldRefreshing = oldViewProps.refreshing;
  BOOL oldLoadingMore = oldViewProps.loadingMore;
  BOOL oldRefreshEnabled = oldViewProps.refreshEnabled;
  BOOL oldLoadMoreEnabled = oldViewProps.loadMoreEnabled;

  // SmartRefreshControl 的样式和文案在组件实例上配置。相关 prop 改变时重建，
  // 避免旧 Header/Footer 留下内部布局或状态缓存。
  BOOL rebuildHeader =
      oldViewProps.headerStyle != newViewProps.headerStyle ||
      oldViewProps.indicatorColor != newViewProps.indicatorColor ||
      oldViewProps.titleColor != newViewProps.titleColor ||
      oldViewProps.primaryColor != newViewProps.primaryColor ||
      oldViewProps.classicSpinnerStyle != newViewProps.classicSpinnerStyle ||
      oldViewProps.materialProgressBackgroundColor != newViewProps.materialProgressBackgroundColor ||
      oldViewProps.classicEnableLastTime != newViewProps.classicEnableLastTime ||
      oldViewProps.pullDownText != newViewProps.pullDownText ||
      oldViewProps.releaseToRefreshText != newViewProps.releaseToRefreshText ||
      oldViewProps.refreshingText != newViewProps.refreshingText ||
      oldViewProps.refreshCompleteText != newViewProps.refreshCompleteText;
  BOOL rebuildFooter =
      oldViewProps.titleColor != newViewProps.titleColor ||
      oldViewProps.indicatorColor != newViewProps.indicatorColor ||
      oldViewProps.primaryColor != newViewProps.primaryColor ||
      oldViewProps.pullUpText != newViewProps.pullUpText ||
      oldViewProps.releaseToLoadMoreText != newViewProps.releaseToLoadMoreText ||
      oldViewProps.loadingMoreText != newViewProps.loadingMoreText ||
      oldViewProps.noMoreDataText != newViewProps.noMoreDataText ||
      oldViewProps.autoLoadMoreEnabled != newViewProps.autoLoadMoreEnabled;

  _refreshEnabled = newViewProps.refreshEnabled;
  _loadMoreEnabled = newViewProps.loadMoreEnabled;
  _autoLoadMoreEnabled = newViewProps.autoLoadMoreEnabled;
  _hapticsEnabled = newViewProps.hapticsEnabled;
  _refreshing = newViewProps.refreshing;
  _loadingMore = newViewProps.loadingMore;
  _materialHeader = newViewProps.headerStyle == ExpoSmartRefreshLayoutViewHeaderStyle::Material;
  switch (newViewProps.classicSpinnerStyle) {
    case ExpoSmartRefreshLayoutViewClassicSpinnerStyle::Scale:
      _classicSpinnerStyle = RNSmartClassicSpinnerStyleScale;
      break;
    case ExpoSmartRefreshLayoutViewClassicSpinnerStyle::FixedBehind:
      _classicSpinnerStyle = RNSmartClassicSpinnerStyleFixedBehind;
      break;
    case ExpoSmartRefreshLayoutViewClassicSpinnerStyle::Translate:
    default:
      _classicSpinnerStyle = RNSmartClassicSpinnerStyleTranslate;
      break;
  }
  _classicEnableLastTime = newViewProps.classicEnableLastTime;
  switch (newViewProps.classicSpinnerStyle) {
    case ExpoSmartRefreshLayoutViewClassicSpinnerStyle::Scale:
      _classicScrollMode = UISmartScrollModeStretch;
      break;
    case ExpoSmartRefreshLayoutViewClassicSpinnerStyle::FixedBehind:
      _classicScrollMode = UISmartScrollModeFront;
      break;
    case ExpoSmartRefreshLayoutViewClassicSpinnerStyle::Translate:
    default:
      _classicScrollMode = UISmartScrollModeMove;
      break;
  }
  _indicatorColor = newViewProps.indicatorColor
      ? RCTUIColorFromSharedColor(newViewProps.indicatorColor)
      : UIColor.systemBlueColor;
  _titleColor = newViewProps.titleColor
      ? RCTUIColorFromSharedColor(newViewProps.titleColor)
      : UIColor.secondaryLabelColor;
  _primaryColor = newViewProps.primaryColor
      ? RCTUIColorFromSharedColor(newViewProps.primaryColor)
      : UIColor.clearColor;
  _materialProgressBackgroundColor = newViewProps.materialProgressBackgroundColor
      ? RCTUIColorFromSharedColor(newViewProps.materialProgressBackgroundColor)
      : UIColor.whiteColor;
  _pullDownText = RNSStringFromStdString(newViewProps.pullDownText);
  _releaseToRefreshText = RNSStringFromStdString(newViewProps.releaseToRefreshText);
  _refreshingText = RNSStringFromStdString(newViewProps.refreshingText);
  _refreshCompleteText = RNSStringFromStdString(newViewProps.refreshCompleteText);
  _pullUpText = RNSStringFromStdString(newViewProps.pullUpText);
  _releaseToLoadMoreText = RNSStringFromStdString(newViewProps.releaseToLoadMoreText);
  _loadingMoreText = RNSStringFromStdString(newViewProps.loadingMoreText);
  _noMoreDataText = RNSStringFromStdString(newViewProps.noMoreDataText);
  _noMoreData = newViewProps.noMoreData;

  if (_scrollView != nil) {
    if (!_refreshEnabled) {
      [self cancelOperation:RNSOperationKindRefresh];
      [_header removeFromSuperview];
      _header = nil;
    } else if (rebuildHeader || _header == nil || oldRefreshEnabled != _refreshEnabled) {
      [self configureHeader];
    }

    if (!_loadMoreEnabled) {
      [self cancelOperation:RNSOperationKindLoadMore];
      [_footer removeFromSuperview];
      _footer = nil;
    } else if (rebuildFooter || _footer == nil || oldLoadMoreEnabled != _loadMoreEnabled) {
      [self configureFooter];
    }

    // refreshing/loadingMore 是受控视觉状态。这里只同步动画，不创建 requestId，
    // 也不向 JS 重复发请求事件；真正的请求生命周期仍由手势或实例命令建立。
    if ((!oldRefreshing || rebuildHeader) && newViewProps.refreshing && _header != nil) {
      [self beginRefreshVisualOnly];
    } else if (oldRefreshing && !newViewProps.refreshing &&
               _activeOperationKind != RNSOperationKindRefresh &&
               _scheduledOperationKind != RNSOperationKindRefresh) {
      _suppressNextRefreshRequest = NO;
      [_header finishRefreshWithSuccess:NO];
    }

    if ((!oldLoadingMore || rebuildFooter) && newViewProps.loadingMore && _footer != nil) {
      [self beginLoadMoreVisualOnly];
    } else if (oldLoadingMore && !newViewProps.loadingMore &&
               _activeOperationKind != RNSOperationKindLoadMore &&
               _scheduledOperationKind != RNSOperationKindLoadMore) {
      _suppressNextLoadMoreRequest = NO;
      [_footer finishLoadMoreWithSuccess:NO];
    }

    if (_noMoreData && _footer != nil) {
      [self disarmAutoLoadMore];
      [self cancelOperation:RNSOperationKindLoadMore];
      [_footer finishLoadMoreWithNoMoreData];
    } else if (!_noMoreData && _footer != nil) {
      [_footer resetNoMoreData];
    }
  }

  [super updateProps:props oldProps:oldProps];
}

- (void)handleCommand:(NSString const *)commandName args:(NSArray const *)args
{
  RCTExpoSmartRefreshLayoutViewHandleCommand(self, commandName, args);
}

- (void)prepareForRecycle
{
  [self detachFromScrollView];
  _refreshing = NO;
  _loadingMore = NO;
  _didTriggerHeaderHaptic = NO;
  [self invalidateDelayedOperations];
  [super prepareForRecycle];
}

#pragma mark - SmartRefreshControl attachment

- (void)attachToScrollView:(UIScrollView *)scrollView
{
  if (scrollView == nil || _scrollView == scrollView) {
    return;
  }

  [self detachFromScrollView];
  _scrollView = scrollView;

  if (_refreshEnabled) {
    [self configureHeader];
  }
  if (_loadMoreEnabled) {
    [self configureFooter];
  }

  // Fabric 可能在首次 props 更新之后才挂载子节点。绑定成功后重新应用受控视觉，
  // begin*VisualOnly 会屏蔽原生组件的回调，因此不会重复发出请求事件。
  if (_refreshing) {
    [self beginRefreshVisualOnly];
  }
  if (_loadingMore) {
    [self beginLoadMoreVisualOnly];
  }
  if (_noMoreData && _footer != nil) {
    [_footer finishLoadMoreWithNoMoreData];
  }
}

- (void)detachFromScrollView
{
  // 拆卸时先让所有延迟命令失效。移除组件过程中原生库可能产生状态回调，
  // suppress 标记确保这些内部清理动作不会被当成新的业务请求。
  [self invalidateDelayedOperations];
  _suppressNextRefreshRequest = YES;
  _suppressNextLoadMoreRequest = YES;
  [_header removeFromSuperview];
  [_footer removeFromSuperview];
  _header = nil;
  _footer = nil;
  _scrollView = nil;
  _suppressNextRefreshRequest = NO;
  _suppressNextLoadMoreRequest = NO;
}

- (void)configureHeader
{
  if (_scrollView == nil) {
    return;
  }
  [_header removeFromSuperview];
  _header = nil;

  __weak ExpoSmartRefreshLayoutView *weakSelf = self;
  UIRefreshHeader *header = _headerSlot != nil
      ? [[RNSmartCustomHeader alloc] initWithContentView:_headerSlot]
      : (_materialHeader
          ? (UIRefreshHeader *)[RNSmartMaterialHeader new]
          : (UIRefreshHeader *)[RNSmartClassicsHeader new]);
  header.colorAccent = _indicatorColor;
  if (_headerSlot != nil) {
    header.colorPrimary = UIColor.clearColor;
  } else if (_materialHeader) {
    header.colorPrimary = _materialProgressBackgroundColor;
  } else {
    header.colorPrimary = _primaryColor;
    RNSmartClassicsHeader *classic = (RNSmartClassicsHeader *)header;
    classic.classicSpinnerStyle = _classicSpinnerStyle;
    classic.pullDownText = _pullDownText;
    classic.releaseToRefreshText = _releaseToRefreshText;
    classic.refreshingText = _refreshingText;
    classic.refreshCompleteText = _refreshCompleteText;
    classic.scrollMode = _classicScrollMode;
    classic.staysBehindContent = _classicScrollMode == UISmartScrollModeFront;
    classic.labelTitle.textColor = _titleColor;
    classic.labelLastTime.hidden = !_classicEnableLastTime;
    classic.labelLastTime.textColor = _titleColor;
  }

  // 适配器只报告原生生命周期；requestId、互斥锁和事件去重由桥接视图统一管理。
  header.refreshBlock = ^(UIRefreshHeader *component) {
    ExpoSmartRefreshLayoutView *strongSelf = weakSelf;
    if (strongSelf != nil) {
      [strongSelf refreshComponentDidRequest:component];
    }
  };
  if (_headerSlot != nil) {
    RNSmartCustomHeader *customHeader = (RNSmartCustomHeader *)header;
    customHeader.statusChanged = ^(UIRefreshStatus oldStatus, UIRefreshStatus status) {
      ExpoSmartRefreshLayoutView *strongSelf = weakSelf;
      if (strongSelf != nil) {
        [strongSelf headerStateChanged:oldStatus status:status];
      }
    };
    customHeader.scrollChanged = ^(CGFloat offset, CGFloat percent, BOOL isDragging) {
      ExpoSmartRefreshLayoutView *strongSelf = weakSelf;
      if (strongSelf != nil) {
        [strongSelf emitHeaderMovingWithOffset:offset percent:percent isDragging:isDragging];
      }
    };
  } else if (_materialHeader) {
    RNSmartMaterialHeader *material = (RNSmartMaterialHeader *)header;
    material.statusChanged = ^(UIRefreshStatus oldStatus, UIRefreshStatus status) {
      ExpoSmartRefreshLayoutView *strongSelf = weakSelf;
      if (strongSelf != nil) {
        [strongSelf headerStateChanged:oldStatus status:status];
      }
    };
    material.scrollChanged = ^(CGFloat offset, CGFloat percent, BOOL isDragging) {
      ExpoSmartRefreshLayoutView *strongSelf = weakSelf;
      if (strongSelf != nil) {
        [strongSelf emitHeaderMovingWithOffset:offset percent:percent isDragging:isDragging];
      }
    };
  } else {
    RNSmartClassicsHeader *classic = (RNSmartClassicsHeader *)header;
    classic.statusChanged = ^(UIRefreshStatus oldStatus, UIRefreshStatus status) {
      ExpoSmartRefreshLayoutView *strongSelf = weakSelf;
      if (strongSelf != nil) {
        [strongSelf headerStateChanged:oldStatus status:status];
      }
    };
    classic.scrollChanged = ^(CGFloat offset, CGFloat percent, BOOL isDragging) {
      ExpoSmartRefreshLayoutView *strongSelf = weakSelf;
      if (strongSelf != nil) {
        [strongSelf emitHeaderMovingWithOffset:offset percent:percent isDragging:isDragging];
      }
    };
  }

  _header = header;
  [header attach:_scrollView];
}

- (void)configureFooter
{
  if (_scrollView == nil) {
    return;
  }
  [_footer removeFromSuperview];
  _footer = nil;

  __weak ExpoSmartRefreshLayoutView *weakSelf = self;
  // iOS 加载更多统一使用 Classic Footer。auto 模式仍需真实向上拖动后才会解锁，
  // 防止首次布局、内容尺寸变化或程序化滚动无意触发请求。
  RNSmartClassicsFooter *footer = [RNSmartClassicsFooter new];
  footer.isAutoLoadMore = _autoLoadMoreEnabled;
  footer.colorAccent = _indicatorColor;
  footer.colorPrimary = _primaryColor;
  footer.pullUpText = _pullUpText;
  footer.releaseToLoadMoreText = _releaseToLoadMoreText;
  footer.loadingMoreText = _loadingMoreText;
  footer.noMoreDataText = _noMoreDataText;
  footer.labelTitle.textColor = _titleColor;
  footer.loadMoreBlock = ^(UIRefreshFooter *component) {
    ExpoSmartRefreshLayoutView *strongSelf = weakSelf;
    if (strongSelf != nil) {
      [strongSelf loadMoreComponentDidRequest:component];
    }
  };
  footer.statusChanged = ^(UISmartFooterStatus oldStatus, UISmartFooterStatus status) {
    ExpoSmartRefreshLayoutView *strongSelf = weakSelf;
    if (strongSelf != nil) {
      [strongSelf footerStateChanged:oldStatus status:status];
    }
  };
  footer.scrollChanged = ^(CGFloat offset, CGFloat percent, BOOL isDragging) {
    (void)offset;
    (void)percent;
    (void)isDragging;
  };

  _footer = footer;
  [footer attach:_scrollView];
  if (_noMoreData) {
    [footer finishLoadMoreWithNoMoreData];
  }
}

#pragma mark - Component callbacks

- (void)refreshComponentDidRequest:(UIRefreshHeader *)header
{
  // 受控视觉同步和组件拆装也会调用原生 beginRefresh；这类回调只用于展示，
  // 必须在进入请求状态机之前消费掉。
  if (_suppressNextRefreshRequest) {
    _suppressNextRefreshRequest = NO;
    return;
  }

  // 程序化 begin 命令先登记 active/requestId，再由原生回调发事件。
  // eventEmitted 防止同一个请求被底层组件重复通知。
  if (_activeOperationKind == RNSOperationKindRefresh) {
    if (_refreshEventEmitted) {
      return;
    }
    _refreshEventEmitted = YES;
    _refreshing = YES;
    [self emitState:@"refreshing"];
    [self emitRefresh:_activeRequestId source:@"programmatic"];
    return;
  }

  // 一把全局操作锁同时保护刷新和加载更多，避免两个方向竞争 contentInset。
  if (_activeOperationKind != RNSOperationKindNone ||
      _scheduledOperationKind != RNSOperationKindNone) {
    [header finishRefreshWithSuccess:NO];
    return;
  }

  NSInteger requestId = [self allocateGestureRequestId];
  _activeOperationKind = RNSOperationKindRefresh;
  _activeRequestId = requestId;
  _refreshEventEmitted = YES;
  _refreshing = YES;
  [self disarmAutoLoadMore];
  [self emitState:@"refreshing"];
  [self emitRefresh:requestId source:@"gesture"];
}

- (void)loadMoreComponentDidRequest:(UIRefreshFooter *)footer
{
  if (_suppressNextLoadMoreRequest) {
    _suppressNextLoadMoreRequest = NO;
    return;
  }

  if (_noMoreData) {
    [footer finishLoadMoreWithNoMoreData];
    return;
  }

  // 与刷新相同：程序化命令沿用已登记的正 requestId，手势请求稍后分配负 ID。
  if (_activeOperationKind == RNSOperationKindLoadMore) {
    if (_loadMoreEventEmitted) {
      return;
    }
    _loadMoreEventEmitted = YES;
    _loadingMore = YES;
    [self emitState:@"loading"];
    [self emitLoadMore:_activeRequestId source:@"programmatic"];
    return;
  }

  if (_activeOperationKind != RNSOperationKindNone ||
      _scheduledOperationKind != RNSOperationKindNone) {
    [footer finishLoadMoreWithSuccess:NO];
    return;
  }

  NSInteger requestId = [self allocateGestureRequestId];
  _activeOperationKind = RNSOperationKindLoadMore;
  _activeRequestId = requestId;
  _loadMoreEventEmitted = YES;
  _loadingMore = YES;
  [self disarmAutoLoadMore];
  [self emitState:@"loading"];
  [self emitLoadMore:requestId source:@"gesture"];
}

- (void)headerStateChanged:(UIRefreshStatus)oldStatus status:(UIRefreshStatus)status
{
  (void)oldStatus;
  // 将第三方控件的细粒度状态收敛为 JS 公共 API 的 pulling/ready/
  // refreshing/idle；触觉反馈只在一次拖拽首次越过释放阈值时触发。
  switch (status) {
    case UIRefreshStatusPullToRefresh:
      _didTriggerHeaderHaptic = NO;
      [self emitState:@"pulling"];
      break;
    case UIRefreshStatusReleaseToRefresh:
      if (_hapticsEnabled && !_didTriggerHeaderHaptic) {
        UIImpactFeedbackGenerator *generator =
            [[UIImpactFeedbackGenerator alloc] initWithStyle:UIImpactFeedbackStyleLight];
        [generator impactOccurred];
        _didTriggerHeaderHaptic = YES;
      }
      [self emitState:@"ready"];
      break;
    case UIRefreshStatusWillRefresh:
    case UIRefreshStatusReleasing:
    case UIRefreshStatusRefreshing:
      [self emitState:@"refreshing"];
      break;
    case UIRefreshStatusFinish:
    case UIRefreshStatusIdle:
    default:
      _didTriggerHeaderHaptic = NO;
      if (status == UIRefreshStatusIdle) {
        _suppressNextRefreshRequest = NO;
      }
      [self emitState:@"idle"];
      break;
  }
}

- (void)footerStateChanged:(UISmartFooterStatus)oldStatus status:(UISmartFooterStatus)status
{
  (void)oldStatus;
  // Footer 状态同样映射为跨平台公共状态；NoMoreData 单独保留，便于 JS
  // 区分正常结束与数据已经全部加载完毕。
  switch (status) {
    case UISmartFooterStatusReleaseToLoadMore:
    case UISmartFooterStatusReleasing:
      [self emitState:@"ready"];
      break;
    case UISmartFooterStatusWillLoadMore:
    case UISmartFooterStatusLoading:
      [self emitState:@"loading"];
      break;
    case UISmartFooterStatusNoMoreData:
      [self emitState:@"no-more-data"];
      break;
    case UISmartFooterStatusFinish:
    case UISmartFooterStatusIdle:
    case UISmartFooterStatusPullToLoadMore:
    default:
      if (status == UISmartFooterStatusIdle) {
        _suppressNextLoadMoreRequest = NO;
      }
      [self emitState:@"idle"];
      break;
  }
}

#pragma mark - Events

// Fabric EventEmitter 可能在视图挂载/回收的边界暂时为空，所有事件都需判空。
// 请求事件携带 requestId 和 source，使 JS 能拒绝迟到结果并区分手势/命令来源。
- (void)emitRefresh:(NSInteger)requestId source:(NSString *)source
{
  auto emitter = std::static_pointer_cast<const ExpoSmartRefreshLayoutViewEventEmitter>(_eventEmitter);
  if (emitter != nullptr) {
    emitter->onRefresh({(int)requestId, [source UTF8String]});
  }
}

- (void)emitLoadMore:(NSInteger)requestId source:(NSString *)source
{
  auto emitter = std::static_pointer_cast<const ExpoSmartRefreshLayoutViewEventEmitter>(_eventEmitter);
  if (emitter != nullptr) {
    emitter->onLoadMore({(int)requestId, [source UTF8String]});
  }
}

- (void)emitState:(NSString *)state
{
  auto emitter = std::static_pointer_cast<const ExpoSmartRefreshLayoutViewEventEmitter>(_eventEmitter);
  if (emitter != nullptr) {
    emitter->onStateChange({[state UTF8String]});
  }
}

- (void)emitHeaderMovingWithOffset:(CGFloat)offset
                           percent:(CGFloat)percent
                        isDragging:(BOOL)isDragging
{
  auto emitter = std::static_pointer_cast<const ExpoSmartRefreshLayoutViewEventEmitter>(_eventEmitter);
  if (emitter != nullptr) {
    CGFloat height = MAX(_header.expandHeight, 1);
    emitter->onHeaderMoving({
      (Float)percent,
      (int)std::lround(offset),
      (int)std::lround(height),
      (int)std::lround(height * 2),
      (bool)isDragging,
    });
  }
}

#pragma mark - Commands

- (void)beginRefresh:(NSInteger)requestId delayMs:(NSInteger)delayMs
{
  // JS 命令必须使用正 ID；已有 active/scheduled 操作时直接拒绝，保证一个实例
  // 任意时刻只有一个请求拥有结束动画的权限。
  if (requestId <= 0 || !_refreshEnabled || _header == nil ||
      _activeOperationKind != RNSOperationKindNone ||
      _scheduledOperationKind != RNSOperationKindNone) {
    return;
  }

  _scheduledOperationKind = RNSOperationKindRefresh;
  _scheduledRequestId = requestId;
  // generation 相当于可取消令牌。即使旧 dispatch_after 已进入队列，后续取消、
  // 重建或回收也会改变代次，使旧回调无法启动刷新。
  NSUInteger generation = ++_refreshBeginGeneration;
  __weak ExpoSmartRefreshLayoutView *weakSelf = self;
  dispatch_after(
      dispatch_time(DISPATCH_TIME_NOW, (int64_t)(MAX(0, delayMs) * NSEC_PER_MSEC)),
      dispatch_get_main_queue(), ^{
        ExpoSmartRefreshLayoutView *strongSelf = weakSelf;
        if (strongSelf == nil ||
            strongSelf->_refreshBeginGeneration != generation ||
            strongSelf->_scheduledOperationKind != RNSOperationKindRefresh ||
            strongSelf->_scheduledRequestId != requestId ||
            strongSelf->_header == nil) {
          return;
        }
        strongSelf->_scheduledOperationKind = RNSOperationKindNone;
        strongSelf->_scheduledRequestId = 0;
        strongSelf->_activeOperationKind = RNSOperationKindRefresh;
        strongSelf->_activeRequestId = requestId;
        strongSelf->_refreshEventEmitted = NO;
        strongSelf->_refreshing = YES;
        [strongSelf disarmAutoLoadMore];
        [strongSelf->_header beginRefresh];
      });
}

- (void)finishRefresh:(NSInteger)requestId success:(BOOL)success delayMs:(NSInteger)delayMs
{
  BOOL matchesActive =
      _activeOperationKind == RNSOperationKindRefresh && _activeRequestId == requestId;
  BOOL matchesScheduled =
      _scheduledOperationKind == RNSOperationKindRefresh && _scheduledRequestId == requestId;
  // requestId=0 专供受控视觉收尾，不代表业务请求；非零 ID 必须精确命中当前
  // active/scheduled 请求，旧 Promise 的完成结果不能结束一个更新的刷新。
  BOOL visualOnly = requestId == 0 && _activeOperationKind == RNSOperationKindNone;
  if (!matchesActive && !matchesScheduled && !visualOnly) {
    return;
  }

  // finish 也可能带 delayMs，因此使用独立 generation，后发结束命令覆盖先发命令。
  NSUInteger generation = ++_refreshFinishGeneration;
  __weak ExpoSmartRefreshLayoutView *weakSelf = self;
  dispatch_after(
      dispatch_time(DISPATCH_TIME_NOW, (int64_t)(MAX(0, delayMs) * NSEC_PER_MSEC)),
      dispatch_get_main_queue(), ^{
        ExpoSmartRefreshLayoutView *strongSelf = weakSelf;
        if (strongSelf == nil || strongSelf->_refreshFinishGeneration != generation) {
          return;
        }
        BOOL stillActive =
            strongSelf->_activeOperationKind == RNSOperationKindRefresh &&
            strongSelf->_activeRequestId == requestId;
        BOOL stillScheduled =
            strongSelf->_scheduledOperationKind == RNSOperationKindRefresh &&
            strongSelf->_scheduledRequestId == requestId;
        BOOL stillVisualOnly =
            requestId == 0 && strongSelf->_activeOperationKind == RNSOperationKindNone;
        if (!stillActive && !stillScheduled && !stillVisualOnly) {
          return;
        }

        if (stillScheduled) {
          strongSelf->_refreshBeginGeneration++;
          strongSelf->_scheduledOperationKind = RNSOperationKindNone;
          strongSelf->_scheduledRequestId = 0;
        }
        if (stillActive) {
          strongSelf->_activeOperationKind = RNSOperationKindNone;
          strongSelf->_activeRequestId = 0;
        }
        strongSelf->_refreshEventEmitted = NO;
        strongSelf->_suppressNextRefreshRequest = NO;

        RNSmartClassicsHeader *classic =
            [strongSelf->_header isKindOfClass:[RNSmartClassicsHeader class]]
                ? (RNSmartClassicsHeader *)strongSelf->_header
                : nil;
        if (classic != nil) {
          classic.showingCompletionText = success && strongSelf->_refreshCompleteText.length > 0;
        }
        [strongSelf->_header finishRefreshWithSuccess:success];
        strongSelf->_refreshing = NO;
        [strongSelf emitState:@"idle"];

        if (classic != nil && classic.showingCompletionText) {
          __weak RNSmartClassicsHeader *weakHeader = classic;
          dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(350 * NSEC_PER_MSEC)),
                         dispatch_get_main_queue(), ^{
                           RNSmartClassicsHeader *header = weakHeader;
                           if (header != nil && !header.isRefreshing) {
                             [header restoreDefaultText];
                           }
                         });
        }
      });
}

- (void)beginLoadMore:(NSInteger)requestId delayMs:(NSInteger)delayMs
{
  if (requestId <= 0 || !_loadMoreEnabled || _footer == nil || _noMoreData ||
      _activeOperationKind != RNSOperationKindNone ||
      _scheduledOperationKind != RNSOperationKindNone) {
    return;
  }

  // 先占用 scheduled 锁，再等待 delayMs，避免延迟期间手势插入另一项操作。
  _scheduledOperationKind = RNSOperationKindLoadMore;
  _scheduledRequestId = requestId;
  NSUInteger generation = ++_loadMoreBeginGeneration;
  __weak ExpoSmartRefreshLayoutView *weakSelf = self;
  dispatch_after(
      dispatch_time(DISPATCH_TIME_NOW, (int64_t)(MAX(0, delayMs) * NSEC_PER_MSEC)),
      dispatch_get_main_queue(), ^{
        ExpoSmartRefreshLayoutView *strongSelf = weakSelf;
        if (strongSelf == nil ||
            strongSelf->_loadMoreBeginGeneration != generation ||
            strongSelf->_scheduledOperationKind != RNSOperationKindLoadMore ||
            strongSelf->_scheduledRequestId != requestId ||
            strongSelf->_footer == nil) {
          return;
        }
        strongSelf->_scheduledOperationKind = RNSOperationKindNone;
        strongSelf->_scheduledRequestId = 0;
        strongSelf->_activeOperationKind = RNSOperationKindLoadMore;
        strongSelf->_activeRequestId = requestId;
        strongSelf->_loadMoreEventEmitted = NO;
        strongSelf->_loadingMore = YES;
        [strongSelf disarmAutoLoadMore];
        RNSmartClassicsFooter *footer =
            [strongSelf->_footer isKindOfClass:[RNSmartClassicsFooter class]]
                ? (RNSmartClassicsFooter *)strongSelf->_footer
                : nil;
        if (footer != nil) {
          [footer beginProgrammaticLoadMore];
        }
      });
}

- (void)finishLoadMore:(NSInteger)requestId
               success:(BOOL)success
            noMoreData:(BOOL)noMoreData
               delayMs:(NSInteger)delayMs
{
  BOOL matchesActive =
      _activeOperationKind == RNSOperationKindLoadMore && _activeRequestId == requestId;
  BOOL matchesScheduled =
      _scheduledOperationKind == RNSOperationKindLoadMore && _scheduledRequestId == requestId;
  // 与刷新一致，结束命令必须匹配 requestId；0 仅用于受控 prop 的视觉同步。
  BOOL visualOnly = requestId == 0 && _activeOperationKind == RNSOperationKindNone;
  if (!matchesActive && !matchesScheduled && !visualOnly) {
    return;
  }

  NSUInteger generation = ++_loadMoreFinishGeneration;
  __weak ExpoSmartRefreshLayoutView *weakSelf = self;
  dispatch_after(
      dispatch_time(DISPATCH_TIME_NOW, (int64_t)(MAX(0, delayMs) * NSEC_PER_MSEC)),
      dispatch_get_main_queue(), ^{
        ExpoSmartRefreshLayoutView *strongSelf = weakSelf;
        if (strongSelf == nil || strongSelf->_loadMoreFinishGeneration != generation) {
          return;
        }
        BOOL stillActive =
            strongSelf->_activeOperationKind == RNSOperationKindLoadMore &&
            strongSelf->_activeRequestId == requestId;
        BOOL stillScheduled =
            strongSelf->_scheduledOperationKind == RNSOperationKindLoadMore &&
            strongSelf->_scheduledRequestId == requestId;
        BOOL stillVisualOnly =
            requestId == 0 && strongSelf->_activeOperationKind == RNSOperationKindNone;
        if (!stillActive && !stillScheduled && !stillVisualOnly) {
          return;
        }

        if (stillScheduled) {
          strongSelf->_loadMoreBeginGeneration++;
          strongSelf->_scheduledOperationKind = RNSOperationKindNone;
          strongSelf->_scheduledRequestId = 0;
        }
        if (stillActive) {
          strongSelf->_activeOperationKind = RNSOperationKindNone;
          strongSelf->_activeRequestId = 0;
        }
        strongSelf->_loadMoreEventEmitted = NO;
        strongSelf->_suppressNextLoadMoreRequest = NO;
        // 一次自动加载完成后重新上锁，必须等待下一次真实向上拖动再解锁。
        [strongSelf disarmAutoLoadMore];
        if (noMoreData) {
          [strongSelf->_footer finishLoadMoreWithNoMoreData];
          [strongSelf emitState:@"no-more-data"];
        } else {
          [strongSelf->_footer finishLoadMoreWithSuccess:success];
          [strongSelf emitState:@"idle"];
        }
        strongSelf->_loadingMore = NO;
        strongSelf->_noMoreData = noMoreData;
      });
}

- (void)resetNoMoreData
{
  _noMoreData = NO;
  [self disarmAutoLoadMore];
  [_footer resetNoMoreData];
  [self emitState:@"idle"];
}

#pragma mark - Operation bookkeeping

- (NSInteger)allocateGestureRequestId
{
  // 负数空间专用于原生手势，INT_MIN 后回绕到 -1，始终不与 JS 正 ID 冲突。
  NSInteger requestId = _nextGestureRequestId;
  _nextGestureRequestId = requestId == INT_MIN ? -1 : requestId - 1;
  return requestId;
}

- (void)beginRefreshVisualOnly
{
  if (_header == nil || _header.isRefreshing) {
    return;
  }
  // 原生 begin 会走 refreshBlock；提前设置 suppress，仅同步动画而不创建请求。
  _suppressNextRefreshRequest = YES;
  [_header beginRefresh];
}

- (void)beginLoadMoreVisualOnly
{
  if (_footer == nil || _footer.isLoading) {
    return;
  }
  _suppressNextLoadMoreRequest = YES;
  RNSmartClassicsFooter *footer =
      [_footer isKindOfClass:[RNSmartClassicsFooter class]]
          ? (RNSmartClassicsFooter *)_footer
          : nil;
  if (footer != nil) {
    [footer beginProgrammaticLoadMore];
  }
}

- (void)disarmAutoLoadMore
{
  if ([_footer isKindOfClass:[RNSmartClassicsFooter class]]) {
    [(RNSmartClassicsFooter *)_footer disarmAutomaticRequests];
  }
}

- (void)cancelOperation:(RNSOperationKind)kind
{
  // 取消既清除锁和 requestId，也递增相应 generation，让已经排队的 begin/finish
  // 回调在醒来后自行失效。
  if (_scheduledOperationKind == kind) {
    if (kind == RNSOperationKindRefresh) {
      _refreshBeginGeneration++;
      _suppressNextRefreshRequest = YES;
    } else {
      _loadMoreBeginGeneration++;
      _suppressNextLoadMoreRequest = YES;
    }
    _scheduledOperationKind = RNSOperationKindNone;
    _scheduledRequestId = 0;
  }
  if (_activeOperationKind == kind) {
    _activeOperationKind = RNSOperationKindNone;
    _activeRequestId = 0;
    if (kind == RNSOperationKindRefresh) {
      _refreshEventEmitted = NO;
      _suppressNextRefreshRequest = YES;
    } else {
      _loadMoreEventEmitted = NO;
      _suppressNextLoadMoreRequest = YES;
    }
  }
  if (kind == RNSOperationKindRefresh) {
    _refreshFinishGeneration++;
  } else if (kind == RNSOperationKindLoadMore) {
    _loadMoreFinishGeneration++;
  }
}

- (void)invalidateDelayedOperations
{
  // Fabric 回收或更换滚动子树时统一作废全部异步工作，防止复用后的组件收到旧命令。
  _refreshBeginGeneration++;
  _loadMoreBeginGeneration++;
  _refreshFinishGeneration++;
  _loadMoreFinishGeneration++;
  _scheduledOperationKind = RNSOperationKindNone;
  _scheduledRequestId = 0;
  _activeOperationKind = RNSOperationKindNone;
  _activeRequestId = 0;
  _refreshEventEmitted = NO;
  _loadMoreEventEmitted = NO;
  _suppressNextRefreshRequest = NO;
  _suppressNextLoadMoreRequest = NO;
}

@end
