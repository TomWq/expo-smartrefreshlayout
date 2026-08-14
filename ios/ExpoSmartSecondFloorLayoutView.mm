#import "ExpoSmartSecondFloorLayoutView.h"

#import "ExpoSmartSecondFloorSlotViews.h"
#import "SmartRefreshControl/RNSmartRefreshAdapter.h"

#import <React/RCTComponentViewFactory.h>
#import <React/RCTConversions.h>
#import <react/renderer/components/ExpoSmartRefreshLayoutSpec/ComponentDescriptors.h>
#import <react/renderer/components/ExpoSmartRefreshLayoutSpec/EventEmitters.h>
#import <react/renderer/components/ExpoSmartRefreshLayoutSpec/Props.h>
#import <react/renderer/components/ExpoSmartRefreshLayoutSpec/RCTComponentViewHelpers.h>

#include <cmath>
#include <limits.h>

using namespace facebook::react;

static NSString *RNSSecondFloorNSString(const std::string &value)
{
  return [NSString stringWithUTF8String:value.c_str()];
}

static void RNSSecondFloorFindScrollView(UIView *view, UIScrollView **best, CGFloat *bestArea)
{
  if ([view isKindOfClass:UIScrollView.class]) {
    UIScrollView *candidate = (UIScrollView *)view;
    CGFloat area = CGRectGetWidth(candidate.bounds) * CGRectGetHeight(candidate.bounds);
    if (*best == nil || area > *bestArea) {
      *best = candidate;
      *bestArea = area;
    }
  }
  for (UIView *subview in view.subviews) {
    RNSSecondFloorFindScrollView(subview, best, bestArea);
  }
}

static UIScrollView *RNSSecondFloorScrollView(UIView *view)
{
  UIScrollView *best = nil;
  CGFloat area = -1;
  RNSSecondFloorFindScrollView(view, &best, &area);
  return best;
}

typedef NS_ENUM(NSInteger, RNSSecondFloorLifecycle) {
  RNSSecondFloorLifecycleIdle,
  RNSSecondFloorLifecycleRelease,
  RNSSecondFloorLifecycleOpening,
  RNSSecondFloorLifecycleOpen,
  RNSSecondFloorLifecycleClosing,
};

@interface ExpoSmartSecondFloorLayoutView () <RCTExpoSmartSecondFloorLayoutViewViewProtocol>
- (void)attachToScrollView:(UIScrollView *)scrollView;
- (void)detachFromScrollView;
- (void)attachToFloorScrollView;
- (void)detachFromFloorScrollView;
- (void)configureHeader;
- (void)handlePan:(UIPanGestureRecognizer *)pan;
- (void)handleFloorPan:(UIPanGestureRecognizer *)pan;
- (void)headerDidRequest:(UIRefreshHeader *)header;
- (void)headerStateChanged:(UIRefreshStatus)oldStatus status:(UIRefreshStatus)status;
- (void)headerDidScrollWithPullDistance:(CGFloat)pulling isDragging:(BOOL)isDragging;
- (void)updateSecondFloorHintForPull:(CGFloat)pulling;
- (void)restoreRefreshHeaderLabels;
- (void)openSecondFloorAnimated:(BOOL)animated;
- (void)closeSecondFloorAnimated:(BOOL)animated;
- (void)finishOpen;
- (void)finishClose;
- (void)positionSecondFloorForPull:(CGFloat)pulling;
- (void)applySecondFloorOffset:(CGFloat)offset;
- (void)applyContentOffset:(CGFloat)offset;
- (void)configureFloorPresentation;
- (void)setFormalFloorContentVisible:(BOOL)visible;
- (UIView *)formalFloorContentView;
- (CGFloat)secondFloorHiddenOffset;
- (CGFloat)firstFloorContentTop;
- (BOOL)isScrollViewAtBottom:(UIScrollView *)scrollView;
- (void)emitState:(NSString *)state;
- (void)emitRefresh:(NSInteger)requestId source:(NSString *)source;
- (void)emitSecondFloorOpen;
- (void)emitSecondFloorClose;
@end

@implementation ExpoSmartSecondFloorLayoutView {
  ExpoSmartSecondFloorContentSlotView *_contentSlot;
  ExpoSmartSecondFloorFloorSlotView *_floorSlot;
  ExpoSmartSecondFloorFloorContentSlotView *_floorContentSlot;
  UIScrollView *_scrollView;
  UIScrollView *_floorScrollView;
  RNSmartClassicsHeader *_header;

  BOOL _refreshEnabled;
  BOOL _refreshing;
  BOOL _hapticsEnabled;
  BOOL _secondFloorEnabled;
  BOOL _pullToCloseEnabled;
  BOOL _didHaptic;
  // 记录拖拽期间是否已经越过二楼阈值。Classic Header 会在松手时调整
  // contentInset，此后瞬时 pullDistance 不再可靠，不能据此丢失二楼手势。
  BOOL _secondFloorArmed;
  BOOL _showingSecondFloorHint;
  BOOL _suppressNextRefreshRequest;
  CGFloat _floorPanUpwardTranslation;
  BOOL _floorOpenedInCycle;
  NSInteger _headerInset;
  CGFloat _floorRate;
  CGFloat _maxRate;
  CGFloat _refreshRate;
  NSInteger _floorDuration;
  CGFloat _bottomPullUpToCloseRate;
  UIColor *_primaryColor;
  UIColor *_indicatorColor;
  UIColor *_titleColor;
  CGFloat _titleTextSize;
  NSString *_pullDownText;
  NSString *_releaseToRefreshText;
  NSString *_refreshingText;
  NSString *_refreshCompleteText;
  NSString *_pullToSecondFloorText;
  NSString *_releaseToSecondFloorText;

  NSInteger _activeRequestId;
  NSInteger _scheduledRequestId;
  NSInteger _nextGestureRequestId;
  NSUInteger _beginGeneration;
  NSUInteger _finishGeneration;
  NSUInteger _floorGeneration;
  RNSSecondFloorLifecycle _lifecycle;
}

+ (ComponentDescriptorProvider)componentDescriptorProvider
{
  return concreteComponentDescriptorProvider<ExpoSmartSecondFloorLayoutViewComponentDescriptor>();
}

+ (void)load
{
  [RCTComponentViewFactory.currentComponentViewFactory registerComponentViewClass:self];
}

- (instancetype)initWithFrame:(CGRect)frame
{
  if (self = [super initWithFrame:frame]) {
    static const auto defaultProps = std::make_shared<const ExpoSmartSecondFloorLayoutViewProps>();
    _props = defaultProps;
    _refreshEnabled = YES;
    _hapticsEnabled = YES;
    _secondFloorEnabled = YES;
    _pullToCloseEnabled = YES;
    _floorRate = 1.9;
    _maxRate = 2.5;
    _refreshRate = 1;
    _floorDuration = 1000;
    _bottomPullUpToCloseRate = 1.0 / 6.0;
    _nextGestureRequestId = -1;
    _indicatorColor = UIColor.systemBlueColor;
    _titleColor = UIColor.secondaryLabelColor;
    _titleTextSize = 15;
    _primaryColor = UIColor.clearColor;
    self.clipsToBounds = YES;
  }
  return self;
}

- (void)mountChildComponentView:(UIView<RCTComponentViewProtocol> *)childComponentView index:(NSInteger)index
{
  if ([childComponentView isKindOfClass:ExpoSmartSecondFloorContentSlotView.class]) {
    NSAssert(_contentSlot == nil, @"SmartSecondFloorLayout accepts one content slot.");
    _contentSlot = (ExpoSmartSecondFloorContentSlotView *)childComponentView;
    // 一楼内容固定在二楼之上；展开时通过同步位移形成垂直堆叠，绝不翻转层级覆盖它。
    [self insertSubview:_contentSlot atIndex:0];
    __weak ExpoSmartSecondFloorLayoutView *weakSelf = self;
    dispatch_async(dispatch_get_main_queue(), ^{
      ExpoSmartSecondFloorLayoutView *strongSelf = weakSelf;
      if (strongSelf != nil) [strongSelf attachToScrollView:RNSSecondFloorScrollView(strongSelf->_contentSlot)];
    });
    return;
  }
  if ([childComponentView isKindOfClass:ExpoSmartSecondFloorFloorSlotView.class]) {
    NSAssert(_floorSlot == nil, @"SmartSecondFloorLayout accepts one floor background slot.");
    _floorSlot = (ExpoSmartSecondFloorFloorSlotView *)childComponentView;
    if (_contentSlot != nil) {
      [self insertSubview:_floorSlot belowSubview:_contentSlot];
    } else {
      [self insertSubview:_floorSlot atIndex:0];
    }
    __weak ExpoSmartSecondFloorLayoutView *weakSelf = self;
    dispatch_async(dispatch_get_main_queue(), ^{
      ExpoSmartSecondFloorLayoutView *strongSelf = weakSelf;
      if (strongSelf != nil) [strongSelf configureFloorPresentation];
    });
    [self positionSecondFloorForPull:0];
    return;
  }
  if ([childComponentView isKindOfClass:ExpoSmartSecondFloorFloorContentSlotView.class]) {
    NSAssert(_floorContentSlot == nil, @"SmartSecondFloorLayout accepts one floor content slot.");
    _floorContentSlot = (ExpoSmartSecondFloorFloorContentSlotView *)childComponentView;
    if (_contentSlot != nil) {
      [self insertSubview:_floorContentSlot belowSubview:_contentSlot];
    } else {
      [self insertSubview:_floorContentSlot atIndex:0];
    }
    if (_lifecycle == RNSSecondFloorLifecycleOpen) {
      [self applySecondFloorOffset:0];
      [self applyContentOffset:CGRectGetHeight(self.bounds)];
      [self attachToFloorScrollView];
    } else {
      [self positionSecondFloorForPull:0];
    }
    [self configureFloorPresentation];
    return;
  }
  NSAssert(NO, @"SmartSecondFloorLayout only accepts its three slot hosts.");
}

- (void)unmountChildComponentView:(UIView<RCTComponentViewProtocol> *)childComponentView index:(NSInteger)index
{
  (void)index;
  if (childComponentView == _contentSlot) {
    [self detachFromScrollView];
    _contentSlot = nil;
  } else if (childComponentView == _floorSlot) {
    [self detachFromFloorScrollView];
    _floorSlot = nil;
    [self closeSecondFloorAnimated:NO];
  } else if (childComponentView == _floorContentSlot) {
    [self detachFromFloorScrollView];
    _floorContentSlot = nil;
    [self configureFloorPresentation];
  }
  [childComponentView removeFromSuperview];
}

- (void)layoutSubviews
{
  [super layoutSubviews];
  if (_scrollView == nil && _contentSlot != nil) [self attachToScrollView:RNSSecondFloorScrollView(_contentSlot)];
  // 二楼只通过 transform 定位，frame 由 Fabric 布局管理，避免重复设置 frame
  // 与 transform 互相干扰导致跳动。
  if (_lifecycle == RNSSecondFloorLifecycleIdle) {
    [self positionSecondFloorForPull:[self pullDistance]];
  }
}

- (void)updateProps:(Props::Shared const &)props oldProps:(Props::Shared const &)oldProps
{
  const auto &oldViewProps = *std::static_pointer_cast<ExpoSmartSecondFloorLayoutViewProps const>(_props);
  const auto &newViewProps = *std::static_pointer_cast<ExpoSmartSecondFloorLayoutViewProps const>(props);
  BOOL rebuildHeader = oldViewProps.headerInset != newViewProps.headerInset ||
      oldViewProps.indicatorColor != newViewProps.indicatorColor ||
      oldViewProps.titleColor != newViewProps.titleColor ||
      oldViewProps.titleTextSize != newViewProps.titleTextSize ||
      oldViewProps.primaryColor != newViewProps.primaryColor ||
      oldViewProps.classicEnableLastTime != newViewProps.classicEnableLastTime ||
      oldViewProps.pullDownText != newViewProps.pullDownText ||
      oldViewProps.releaseToRefreshText != newViewProps.releaseToRefreshText ||
      oldViewProps.refreshingText != newViewProps.refreshingText ||
      oldViewProps.refreshCompleteText != newViewProps.refreshCompleteText ||
      oldViewProps.refreshRate != newViewProps.refreshRate;

  _refreshEnabled = newViewProps.refreshEnabled;
  _refreshing = newViewProps.refreshing;
  _hapticsEnabled = newViewProps.hapticsEnabled;
  _secondFloorEnabled = newViewProps.secondFloorEnabled;
  _pullToCloseEnabled = newViewProps.pullToCloseEnabled;
  _headerInset = MAX(0, MIN(newViewProps.headerInset, 10000));
  _maxRate = MIN(MAX(std::isfinite(newViewProps.maxRate) ? newViewProps.maxRate : 2.5, 1.2), 5);
  _floorRate = MIN(MAX(std::isfinite(newViewProps.floorRate) ? newViewProps.floorRate : 1.9, 1.1), _maxRate - .05);
  _refreshRate = MIN(MAX(std::isfinite(newViewProps.refreshRate) ? newViewProps.refreshRate : 1, .25), _floorRate - .05);
  _floorDuration = MAX(0, MIN(newViewProps.floorDuration, 10000));
  _bottomPullUpToCloseRate = MIN(MAX(std::isfinite(newViewProps.bottomPullUpToCloseRate) ? newViewProps.bottomPullUpToCloseRate : 1.0 / 6.0, .01), .5);
  _primaryColor = newViewProps.primaryColor ? RCTUIColorFromSharedColor(newViewProps.primaryColor) : UIColor.clearColor;
  _indicatorColor = newViewProps.indicatorColor ? RCTUIColorFromSharedColor(newViewProps.indicatorColor) : UIColor.systemBlueColor;
  _titleColor = newViewProps.titleColor ? RCTUIColorFromSharedColor(newViewProps.titleColor) : UIColor.secondaryLabelColor;
  _titleTextSize = MIN(MAX(std::isfinite(newViewProps.titleTextSize) ? newViewProps.titleTextSize : 15, 8), 40);
  _pullDownText = RNSSecondFloorNSString(newViewProps.pullDownText);
  _releaseToRefreshText = RNSSecondFloorNSString(newViewProps.releaseToRefreshText);
  _refreshingText = RNSSecondFloorNSString(newViewProps.refreshingText);
  _refreshCompleteText = RNSSecondFloorNSString(newViewProps.refreshCompleteText);
  _pullToSecondFloorText = RNSSecondFloorNSString(newViewProps.pullToSecondFloorText);
  _releaseToSecondFloorText = RNSSecondFloorNSString(newViewProps.releaseToSecondFloorText);

  if (!_secondFloorEnabled && _lifecycle != RNSSecondFloorLifecycleIdle) [self closeSecondFloorAnimated:YES];
  if (!_refreshEnabled && _header != nil) {
    [_header removeFromSuperview];
    _header = nil;
  } else if (_scrollView != nil && (rebuildHeader || _header == nil)) {
    [self configureHeader];
  }
  if (_refreshing && _header != nil && _lifecycle == RNSSecondFloorLifecycleIdle && !_header.isRefreshing) {
    _suppressNextRefreshRequest = YES;
    [_header beginRefresh];
  } else if (!_refreshing && oldViewProps.refreshing && _activeRequestId == 0) {
    [_header finishRefreshWithSuccess:YES];
  }
  [super updateProps:props oldProps:oldProps];
}

- (void)handleCommand:(NSString const *)commandName args:(NSArray const *)args
{
  RCTExpoSmartSecondFloorLayoutViewHandleCommand(self, commandName, args);
}

- (void)prepareForRecycle
{
  [self detachFromScrollView];
  [self detachFromFloorScrollView];
  _beginGeneration++;
  _finishGeneration++;
  _floorGeneration++;
  _activeRequestId = 0;
  _scheduledRequestId = 0;
  _lifecycle = RNSSecondFloorLifecycleIdle;
  _floorOpenedInCycle = NO;
  _secondFloorArmed = NO;
  _floorPanUpwardTranslation = 0;
  [super prepareForRecycle];
}

#pragma mark - Scroll and header

- (void)attachToScrollView:(UIScrollView *)scrollView
{
  if (scrollView == nil || _scrollView == scrollView) return;
  [self detachFromScrollView];
  _scrollView = scrollView;
  [_scrollView.panGestureRecognizer addTarget:self action:@selector(handlePan:)];
  if (_refreshEnabled) [self configureHeader];
}

- (void)detachFromScrollView
{
  [_scrollView.panGestureRecognizer removeTarget:self action:@selector(handlePan:)];
  [_header removeFromSuperview];
  _header = nil;
  _scrollView = nil;
}

- (void)attachToFloorScrollView
{
  UIView *floorHost = _floorContentSlot ?: _floorSlot;
  UIScrollView *scrollView = floorHost != nil ? RNSSecondFloorScrollView(floorHost) : nil;
  if (scrollView == _floorScrollView) return;
  [self detachFromFloorScrollView];
  _floorScrollView = scrollView;
  [_floorScrollView.panGestureRecognizer addTarget:self action:@selector(handleFloorPan:)];
}

- (void)detachFromFloorScrollView
{
  [_floorScrollView.panGestureRecognizer removeTarget:self action:@selector(handleFloorPan:)];
  _floorScrollView = nil;
}

- (CGFloat)headerHeight
{
  return 85 + _headerInset;
}

- (CGFloat)pullDistance
{
  if (_scrollView == nil) return 0;
  return MAX(0, -(_scrollView.contentOffset.y + _scrollView.adjustedContentInset.top));
}

- (void)configureHeader
{
  if (_scrollView == nil || !_refreshEnabled) return;
  [_header removeFromSuperview];
  __weak ExpoSmartSecondFloorLayoutView *weakSelf = self;
  RNSmartClassicsHeader *header = [RNSmartClassicsHeader new];
  header.height = [self headerHeight];
  header.triggerRate = _refreshRate;
  header.colorPrimary = _primaryColor;
  header.colorAccent = _indicatorColor;
  header.labelTitle.textColor = _titleColor;
  header.labelTitle.font = [UIFont systemFontOfSize:_titleTextSize];
  header.labelLastTime.textColor = _titleColor;
  header.labelLastTime.hidden = !std::static_pointer_cast<ExpoSmartSecondFloorLayoutViewProps const>(_props)->classicEnableLastTime;
  header.pullDownText = _pullDownText;
  header.releaseToRefreshText = _releaseToRefreshText;
  header.refreshingText = _refreshingText;
  header.refreshCompleteText = _refreshCompleteText;
  header.refreshBlock = ^(UIRefreshHeader *component) {
    ExpoSmartSecondFloorLayoutView *strongSelf = weakSelf;
    if (strongSelf != nil) [strongSelf headerDidRequest:component];
  };
  header.statusChanged = ^(UIRefreshStatus oldStatus, UIRefreshStatus status) {
    ExpoSmartSecondFloorLayoutView *strongSelf = weakSelf;
    if (strongSelf != nil) [strongSelf headerStateChanged:oldStatus status:status];
  };
  header.scrollChanged = ^(CGFloat offset, CGFloat percent, BOOL isDragging) {
    (void)percent;
    ExpoSmartSecondFloorLayoutView *strongSelf = weakSelf;
    if (strongSelf != nil) [strongSelf headerDidScrollWithPullDistance:offset isDragging:isDragging];
  };
  _header = header;
  [header attach:_scrollView];
}

- (void)handlePan:(UIPanGestureRecognizer *)pan
{
  if (_scrollView == nil || _lifecycle != RNSSecondFloorLifecycleIdle) return;
  CGFloat pulling = [self pullDistance];
  [self headerDidScrollWithPullDistance:pulling isDragging:_scrollView.isDragging];
  if (pan.state == UIGestureRecognizerStateEnded || pan.state == UIGestureRecognizerStateCancelled || pan.state == UIGestureRecognizerStateFailed) {
    if (_secondFloorArmed && _secondFloorEnabled && _floorSlot != nil &&
        _activeRequestId == 0 && _scheduledRequestId == 0) {
      [self openSecondFloorAnimated:YES];
    }
    _secondFloorArmed = NO;
    _didHaptic = NO;
  }
}

- (void)headerDidScrollWithPullDistance:(CGFloat)pulling isDragging:(BOOL)isDragging
{
  if (_lifecycle != RNSSecondFloorLifecycleIdle) return;
  // Header 进入刷新态后会因 contentInset 的扩展/回收继续发送偏移回调。
  // 此时该偏移不代表用户的二楼拖拽；必须清掉预览位移，避免刷新结束后
  // content slot 留在已下移的位置。
  if (_activeRequestId != 0 || _scheduledRequestId != 0 || _header.isRefreshing) {
    [self positionSecondFloorForPull:0];
    return;
  }
  [self positionSecondFloorForPull:pulling];
  [self updateSecondFloorHintForPull:pulling];
  CGFloat floorThreshold = [self headerHeight] * _floorRate;
  if (isDragging) _secondFloorArmed = pulling >= floorThreshold;
  if (_hapticsEnabled && isDragging && pulling >= floorThreshold && !_didHaptic) {
    UIImpactFeedbackGenerator *generator =
        [[UIImpactFeedbackGenerator alloc] initWithStyle:UIImpactFeedbackStyleLight];
    [generator impactOccurred];
    _didHaptic = YES;
  }
}

- (void)handleFloorPan:(UIPanGestureRecognizer *)pan
{
  if (_lifecycle != RNSSecondFloorLifecycleOpen) {
    _floorPanUpwardTranslation = 0;
    return;
  }

  UIScrollView *floorScrollView = [pan.view isKindOfClass:UIScrollView.class]
      ? (UIScrollView *)pan.view
      : _floorScrollView;
  BOOL atBottom = floorScrollView == nil || [self isScrollViewAtBottom:floorScrollView];

  if (pan.state == UIGestureRecognizerStateBegan) {
    _floorPanUpwardTranslation = 0;
  }

  CGFloat translationY = [pan translationInView:self].y;
  // 底部上拉关闭：列表滚到底后继续上拉触发。
  if (atBottom && translationY < 0) {
    _floorPanUpwardTranslation = MAX(_floorPanUpwardTranslation, -translationY);
  }

  BOOL ended = pan.state == UIGestureRecognizerStateEnded ||
      pan.state == UIGestureRecognizerStateCancelled ||
      pan.state == UIGestureRecognizerStateFailed;
  if (ended) {
    CGFloat height = CGRectGetHeight(self.bounds);
    BOOL closeByPullUp = _floorPanUpwardTranslation >= height * _bottomPullUpToCloseRate;
    if (closeByPullUp) {
      [self closeSecondFloorAnimated:YES];
    }
    _floorPanUpwardTranslation = 0;
  }
}

- (BOOL)isScrollViewAtBottom:(UIScrollView *)scrollView
{
  UIEdgeInsets inset = scrollView.adjustedContentInset;
  CGFloat maximumOffset = MAX(-inset.top,
      scrollView.contentSize.height - CGRectGetHeight(scrollView.bounds) + inset.bottom);
  return scrollView.contentOffset.y >= maximumOffset - 1;
}

- (void)headerDidRequest:(UIRefreshHeader *)header
{
  if (_suppressNextRefreshRequest) {
    _suppressNextRefreshRequest = NO;
    return;
  }
  if (_lifecycle != RNSSecondFloorLifecycleIdle || !_refreshEnabled) {
    [header finishRefreshWithSuccess:NO];
    return;
  }
  // Programmatic begin owns its positive request ID and emits exactly one
  // programmatic event below. Its visual header callback must not become a
  // second gesture request.
  if (_activeRequestId > 0) {
    [self emitState:@"refreshing"];
    return;
  }
  // The Classic header reaches its ordinary release threshold before the
  // floor threshold. Re-check the public scroll position at release so this
  // gesture deterministically belongs to the second floor.
  if (_secondFloorEnabled && _floorSlot != nil && _secondFloorArmed) {
    // 先完成 Classic Header 的 inset 回收，再在下一轮主队列开始二楼动画。
    // 否则 Header 的异步回弹会与 content slot transform 竞争，导致页面错位。
    _secondFloorArmed = NO;
    [header finishRefreshWithSuccess:NO];
    dispatch_async(dispatch_get_main_queue(), ^{
      [self openSecondFloorAnimated:YES];
    });
    return;
  }
  // 普通刷新接管后不再保留二楼预览 transform。否则 Header 完成时
  // contentInset 回收，而 slot 仍保持下移，会让页面永久错位。
  _secondFloorArmed = NO;
  [self positionSecondFloorForPull:0];
  [self restoreRefreshHeaderLabels];
  if (_activeRequestId == 0) {
    _activeRequestId = _nextGestureRequestId;
    _nextGestureRequestId = _nextGestureRequestId == INT_MIN ? -1 : _nextGestureRequestId - 1;
    [self emitRefresh:_activeRequestId source:@"gesture"];
  }
  [self emitState:@"refreshing"];
}

- (void)headerStateChanged:(UIRefreshStatus)oldStatus status:(UIRefreshStatus)status
{
  (void)oldStatus;
  if (_lifecycle != RNSSecondFloorLifecycleIdle) return;
  if (!_secondFloorArmed) [self restoreRefreshHeaderLabels];
  switch (status) {
    case UIRefreshStatusPullToRefresh: [self emitState:@"pulling"]; break;
    case UIRefreshStatusReleaseToRefresh:
      if (_hapticsEnabled && !_didHaptic) {
        UIImpactFeedbackGenerator *generator =
            [[UIImpactFeedbackGenerator alloc] initWithStyle:UIImpactFeedbackStyleLight];
        [generator impactOccurred];
        _didHaptic = YES;
      }
      [self emitState:@"ready"];
      break;
    case UIRefreshStatusWillRefresh:
    case UIRefreshStatusReleasing:
    case UIRefreshStatusRefreshing: [self emitState:@"refreshing"]; break;
    default: [self emitState:@"idle"]; break;
  }
}

#pragma mark - Second floor

- (void)updateSecondFloorHintForPull:(CGFloat)pulling
{
  if (_header == nil || !_secondFloorEnabled || _floorSlot == nil) return;
  CGFloat headerHeight = [self headerHeight];
  CGFloat refreshThreshold = headerHeight * _refreshRate;
  CGFloat floorThreshold = headerHeight * _floorRate;
  // 保留 Classic Header 的“释放刷新”区间。二楼提示只在普通刷新阈值之后
  // 的后半段开始接管，不能刚越过 refreshThreshold 就覆盖它。
  CGFloat secondFloorHintThreshold = (refreshThreshold + floorThreshold) / 2;
  NSString *text = nil;
  if (pulling >= floorThreshold) {
    text = _releaseToSecondFloorText;
  } else if (pulling > secondFloorHintThreshold) {
    text = _pullToSecondFloorText;
  }
  if (text.length == 0) {
    [self restoreRefreshHeaderLabels];
    return;
  }
  _showingSecondFloorHint = YES;
  _header.labelTitle.text = text;
  _header.labelLastTime.hidden = YES;
  [_header setNeedsLayout];
}

- (void)restoreRefreshHeaderLabels
{
  if (!_showingSecondFloorHint || _header == nil) return;
  _showingSecondFloorHint = NO;
  _header.labelLastTime.hidden = !std::static_pointer_cast<ExpoSmartSecondFloorLayoutViewProps const>(_props)->classicEnableLastTime;
  [_header restoreDefaultText];
}

- (CGFloat)secondFloorHiddenOffset
{
  // Fabric 的 absolute-fill 二楼槽位以本组件的 (0, 0) 为原点。以前把
  // adjustedContentInset.top 混入这里，但 ScrollView 的实际内容坐标并不
  // 使用同一套基准，安全区或 Header inset 存在时会留下白缝。
  return -CGRectGetHeight(self.bounds);
}

- (void)applySecondFloorOffset:(CGFloat)offset
{
  CGAffineTransform transform = CGAffineTransformMakeTranslation(0, offset);
  if (_floorSlot != nil) _floorSlot.transform = transform;
  if (_floorContentSlot != nil) _floorContentSlot.transform = transform;
}

- (void)applyContentOffset:(CGFloat)offset
{
  if (_contentSlot != nil) {
    _contentSlot.transform = CGAffineTransformMakeTranslation(0, offset);
  }
}

- (UIView *)formalFloorContentView
{
  if (_floorContentSlot != nil) return _floorContentSlot;
  if (_floorSlot == nil) return nil;

  // 没有单独背景槽时，secondFloor 被渲染在 floorSlot 内。预览态只保留
  // 其根背景色，打开后二楼内容才需要显示和接收滚动手势。
  UIScrollView *scrollView = RNSSecondFloorScrollView(_floorSlot);
  if (scrollView != nil) return scrollView;
  return _floorSlot.subviews.firstObject;
}

- (void)setFormalFloorContentVisible:(BOOL)visible
{
  UIView *formalContent = [self formalFloorContentView];
  if (formalContent != nil) formalContent.alpha = visible ? 1 : 0;
}

- (void)configureFloorPresentation
{
  UIView *formalContent = [self formalFloorContentView];
  if (_floorSlot != nil && _floorContentSlot == nil && formalContent != nil) {
    // 兼容只传 secondFloor 的旧用法：借用其根视图背景作为预览幕布。
    // 这样列表行不会在下拉过程中参与合成，消除绿黄渐变和内容重影。
    _floorSlot.backgroundColor = formalContent.backgroundColor;
  }
  BOOL visible = _lifecycle == RNSSecondFloorLifecycleOpening ||
      _lifecycle == RNSSecondFloorLifecycleOpen;
  [self setFormalFloorContentVisible:visible];
}

- (CGFloat)firstFloorContentTop
{
  if (_scrollView == nil) return NAN;

  // React Native 的 ScrollView 会将实际内容承载 View 作为直接子节点，
  // 刷新 Header 也是直接子节点。选择面积最大的非 Header 子节点并将其
  // 原点转换到本组件坐标，得到 UIKit 当前真正绘制的一楼内容起点。
  UIView *contentView = nil;
  CGFloat bestArea = -1;
  for (UIView *candidate in _scrollView.subviews) {
    if (candidate == _header || candidate.hidden) continue;
    CGFloat area = CGRectGetWidth(candidate.bounds) * CGRectGetHeight(candidate.bounds);
    if (area > bestArea) {
      contentView = candidate;
      bestArea = area;
    }
  }
  if (contentView == nil) return NAN;

  CGPoint origin = [self convertPoint:CGPointZero fromView:contentView];
  return std::isfinite(origin.y) ? MAX(0, origin.y) : NAN;
}

- (void)positionSecondFloorForPull:(CGFloat)pulling
{
  CGFloat height = CGRectGetHeight(self.bounds);
  if (height <= 0) return;
  // 预览时一楼仍由 UIScrollView 原生回弹。以其真实内容起点作为二楼
  // 底边，避免用 pullDistance/adjustedContentInset 推导而发生坐标漂移。
  // 这会让两层始终首尾相接，不暴露透明宿主的白色背景。
  CGFloat maxPull = [self headerHeight] * _maxRate;
  CGFloat fallbackTop = MIN(MAX(0, pulling), maxPull);
  CGFloat contentTop = [self firstFloorContentTop];
  // Header 自身已限制最大拖拽。这里不能再次对真实内容坐标裁剪，否则安全区
  // 会被误当成超额拖拽，二楼底边再次落在一楼内容之前。
  CGFloat seamTop = std::isfinite(contentTop) ? contentTop : fallbackTop;
  [self applyContentOffset:0];
  [self applySecondFloorOffset:[self secondFloorHiddenOffset] + seamTop];
}

- (void)openSecondFloorAnimated:(BOOL)animated
{
  if (_lifecycle != RNSSecondFloorLifecycleIdle || !_secondFloorEnabled || _floorSlot == nil ||
      _activeRequestId != 0 || _scheduledRequestId != 0) return;
  _lifecycle = RNSSecondFloorLifecycleRelease;
  _secondFloorArmed = NO;
  [self emitState:@"release-to-second-floor"];
  // 手势路径已先结束 Header；命令式打开仍需清理可能残留的原生刷新视觉。
  if (_header.isRefreshing) [_header finishRefreshWithSuccess:NO];
  _lifecycle = RNSSecondFloorLifecycleOpening;
  [self emitState:@"second-floor-opening"];
  NSUInteger generation = ++_floorGeneration;
  NSTimeInterval duration = animated ? _floorDuration / 1000.0 : 0;
  void (^finish)(BOOL) = ^(__unused BOOL completed) {
    if (generation == self->_floorGeneration) [self finishOpen];
  };
  void (^changes)(void) = ^{
    [self applySecondFloorOffset:0];
    // 与 Android TwoLevelHeader 一致：二楼展开到当前窗口，一楼整体被
    // 推到二楼下方，而不是通过 z-order 覆盖一楼。
    [self applyContentOffset:CGRectGetHeight(self.bounds)];
    [self setFormalFloorContentVisible:YES];
  };
  if (duration == 0) { changes(); finish(YES); }
  else [UIView animateWithDuration:duration animations:changes completion:finish];
}

- (void)finishOpen
{
  if (_lifecycle != RNSSecondFloorLifecycleOpening) return;
  _lifecycle = RNSSecondFloorLifecycleOpen;
  [self attachToFloorScrollView];
  [self emitState:@"second-floor"];
  if (!_floorOpenedInCycle) {
    _floorOpenedInCycle = YES;
    [self emitSecondFloorOpen];
  }
}

- (void)closeSecondFloorAnimated:(BOOL)animated
{
  if (_lifecycle == RNSSecondFloorLifecycleIdle || _lifecycle == RNSSecondFloorLifecycleClosing) return;
  _lifecycle = RNSSecondFloorLifecycleClosing;
  [self emitState:@"second-floor-closing"];
  NSUInteger generation = ++_floorGeneration;
  NSTimeInterval duration = animated ? _floorDuration / 1000.0 : 0;
  void (^finish)(BOOL) = ^(__unused BOOL completed) {
    if (generation == self->_floorGeneration) [self finishClose];
  };
  void (^changes)(void) = ^{
    [self applySecondFloorOffset:[self secondFloorHiddenOffset]];
    [self applyContentOffset:0];
    [self setFormalFloorContentVisible:NO];
  };
  if (duration == 0) { changes(); finish(YES); }
  else [UIView animateWithDuration:duration animations:changes completion:finish];
}

- (void)finishClose
{
  BOOL wasOpen = _floorOpenedInCycle;
  [self detachFromFloorScrollView];
  _lifecycle = RNSSecondFloorLifecycleIdle;
  _floorOpenedInCycle = NO;
  [self applyContentOffset:0];
  [self configureFloorPresentation];
  [self positionSecondFloorForPull:0];
  [self emitState:@"idle"];
  if (wasOpen) [self emitSecondFloorClose];
}

#pragma mark - Commands

- (void)beginRefresh:(NSInteger)requestId delayMs:(NSInteger)delayMs
{
  if (requestId <= 0 || !_refreshEnabled || _header == nil || _activeRequestId != 0 ||
      _scheduledRequestId != 0 || _lifecycle != RNSSecondFloorLifecycleIdle) return;
  _scheduledRequestId = requestId;
  NSUInteger generation = ++_beginGeneration;
  __weak ExpoSmartSecondFloorLayoutView *weakSelf = self;
  dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(MAX(0, delayMs) * NSEC_PER_MSEC)), dispatch_get_main_queue(), ^{
    ExpoSmartSecondFloorLayoutView *strongSelf = weakSelf;
    if (strongSelf == nil || generation != strongSelf->_beginGeneration || strongSelf->_scheduledRequestId != requestId || strongSelf->_lifecycle != RNSSecondFloorLifecycleIdle) return;
    strongSelf->_scheduledRequestId = 0;
    strongSelf->_activeRequestId = requestId;
    strongSelf->_suppressNextRefreshRequest = NO;
    [strongSelf->_header beginRefresh];
    [strongSelf emitState:@"refreshing"];
    [strongSelf emitRefresh:requestId source:@"programmatic"];
  });
}

- (void)finishRefresh:(NSInteger)requestId success:(BOOL)success delayMs:(NSInteger)delayMs
{
  BOOL scheduled = _scheduledRequestId == requestId;
  BOOL active = _activeRequestId == requestId;
  if (!scheduled && !active) return;
  NSUInteger generation = ++_finishGeneration;
  __weak ExpoSmartSecondFloorLayoutView *weakSelf = self;
  dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(MAX(0, delayMs) * NSEC_PER_MSEC)), dispatch_get_main_queue(), ^{
    ExpoSmartSecondFloorLayoutView *strongSelf = weakSelf;
    if (strongSelf == nil || generation != strongSelf->_finishGeneration) return;
    if (strongSelf->_scheduledRequestId == requestId) {
      strongSelf->_beginGeneration++;
      strongSelf->_scheduledRequestId = 0;
    }
    if (strongSelf->_activeRequestId == requestId) strongSelf->_activeRequestId = 0;
    [strongSelf->_header finishRefreshWithSuccess:success];
    [strongSelf emitState:@"idle"];
  });
}

- (void)openSecondFloor { [self openSecondFloorAnimated:YES]; }
- (void)closeSecondFloor { [self closeSecondFloorAnimated:YES]; }

#pragma mark - Events

- (void)emitState:(NSString *)state
{
  auto emitter = std::static_pointer_cast<const ExpoSmartSecondFloorLayoutViewEventEmitter>(_eventEmitter);
  if (emitter != nullptr) emitter->onStateChange({[state UTF8String]});
}

- (void)emitRefresh:(NSInteger)requestId source:(NSString *)source
{
  auto emitter = std::static_pointer_cast<const ExpoSmartSecondFloorLayoutViewEventEmitter>(_eventEmitter);
  if (emitter != nullptr) emitter->onRefresh({(int)requestId, [source UTF8String]});
}

- (void)emitSecondFloorOpen
{
  auto emitter = std::static_pointer_cast<const ExpoSmartSecondFloorLayoutViewEventEmitter>(_eventEmitter);
  if (emitter != nullptr) emitter->onSecondFloorOpen({});
}

- (void)emitSecondFloorClose
{
  auto emitter = std::static_pointer_cast<const ExpoSmartSecondFloorLayoutViewEventEmitter>(_eventEmitter);
  if (emitter != nullptr) emitter->onSecondFloorClose({});
}

@end
