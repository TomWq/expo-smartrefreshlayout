#import "ExpoSmartRefreshLayoutView.h"

#import <MJRefresh/MJRefresh.h>
#import <React/RCTConversions.h>

#import <react/renderer/components/ExpoSmartRefreshLayoutSpec/ComponentDescriptors.h>
#import <react/renderer/components/ExpoSmartRefreshLayoutSpec/EventEmitters.h>
#import <react/renderer/components/ExpoSmartRefreshLayoutSpec/Props.h>
#import <react/renderer/components/ExpoSmartRefreshLayoutSpec/RCTComponentViewHelpers.h>

#import "RCTFabricComponentsPlugins.h"

#include <limits.h>

using namespace facebook::react;

static NSString *RNSStringFromStdString(const std::string &value)
{
  return [NSString stringWithUTF8String:value.c_str()];
}

static void RNSFindBestScrollView(UIView *view, UIScrollView **best, CGFloat *bestArea)
{
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

@interface RNSRefreshHeader : MJRefreshNormalHeader
@property (nonatomic, copy, nullable) void (^stateChanged)(MJRefreshState state, CGFloat percent);
@end

@implementation RNSRefreshHeader

- (void)setState:(MJRefreshState)state
{
  MJRefreshState previousState = self.state;
  [super setState:state];
  if (previousState != state && self.stateChanged != nil) {
    self.stateChanged(state, self.pullingPercent);
  }
}

- (void)setPullingPercent:(CGFloat)pullingPercent
{
  [super setPullingPercent:pullingPercent];
  if (self.stateChanged != nil && self.state == MJRefreshStateIdle && pullingPercent > 0) {
    self.stateChanged(self.state, pullingPercent);
  }
}

@end

@interface RNSRefreshFooter : MJRefreshBackNormalFooter
@property (nonatomic, copy, nullable) void (^stateChanged)(MJRefreshState state);
@end

@implementation RNSRefreshFooter

- (void)setState:(MJRefreshState)state
{
  MJRefreshState previousState = self.state;
  [super setState:state];
  if (previousState != state && self.stateChanged != nil) {
    self.stateChanged(state);
  }
}

@end

@interface RNSAutoRefreshFooter : MJRefreshAutoNormalFooter
@property (nonatomic, copy, nullable) void (^stateChanged)(MJRefreshState state);
@property (nonatomic, assign) BOOL requestArmed;
@end

@implementation RNSAutoRefreshFooter

- (void)setState:(MJRefreshState)state
{
  MJRefreshState previousState = self.state;
  [super setState:state];
  if (previousState != state && self.stateChanged != nil) {
    self.stateChanged(state);
  }
}

- (void)scrollViewContentOffsetDidChange:(NSDictionary *)change
{
  if (!self.requestArmed) {
    CGPoint oldOffset = [change[NSKeyValueChangeOldKey] CGPointValue];
    CGPoint newOffset = [change[NSKeyValueChangeNewKey] CGPointValue];
    UIEdgeInsets inset = self.scrollView.adjustedContentInset;
    BOOL contentExceedsViewport =
        self.scrollView.contentSize.height + inset.top + inset.bottom >
        CGRectGetHeight(self.scrollView.bounds) + 1;
    if (self.scrollView.isDragging && newOffset.y > oldOffset.y && contentExceedsViewport) {
      self.requestArmed = YES;
      self.automaticallyRefresh = YES;
    }
  }

  if (self.requestArmed) {
    [super scrollViewContentOffsetDidChange:change];
  }
}

- (void)scrollViewPanStateDidChange:(NSDictionary *)change
{
  if (self.requestArmed) {
    [super scrollViewPanStateDidChange:change];
  }
}

@end

typedef NS_ENUM(NSInteger, RNSOperationKind) {
  RNSOperationKindNone = 0,
  RNSOperationKindRefresh,
  RNSOperationKindLoadMore,
};

@interface ExpoSmartRefreshLayoutView () <RCTExpoSmartRefreshLayoutViewViewProtocol>
- (void)emitRefresh:(NSInteger)requestId source:(NSString *)source;
- (void)emitLoadMore:(NSInteger)requestId source:(NSString *)source;
- (void)emitState:(NSString *)state;
- (NSInteger)allocateGestureRequestId;
- (void)beginRefreshVisualOnly;
- (void)beginLoadMoreVisualOnly;
- (void)disarmAutoLoadMore;
- (void)cancelOperation:(RNSOperationKind)kind;
- (void)invalidateDelayedOperations;
@end

@implementation ExpoSmartRefreshLayoutView {
  UIScrollView *_scrollView;
  BOOL _refreshEnabled;
  BOOL _loadMoreEnabled;
  BOOL _autoLoadMoreEnabled;
  BOOL _hapticsEnabled;
  BOOL _materialHeader;
  BOOL _refreshing;
  BOOL _loadingMore;
  BOOL _noMoreData;
  BOOL _didTriggerHeaderHaptic;
  BOOL _suppressNextRefreshRequest;
  BOOL _suppressNextLoadMoreRequest;
  RNSOperationKind _activeOperationKind;
  NSInteger _activeRequestId;
  RNSOperationKind _scheduledOperationKind;
  NSInteger _scheduledRequestId;
  NSInteger _nextGestureRequestId;
  NSUInteger _refreshBeginGeneration;
  NSUInteger _loadMoreBeginGeneration;
  NSUInteger _refreshFinishGeneration;
  NSUInteger _loadMoreFinishGeneration;
  UIColor *_indicatorColor;
  UIColor *_titleColor;
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

- (instancetype)initWithFrame:(CGRect)frame
{
  if (self = [super initWithFrame:frame]) {
    static const auto defaultProps = std::make_shared<const ExpoSmartRefreshLayoutViewProps>();
    _props = defaultProps;
    _refreshEnabled = YES;
    _hapticsEnabled = YES;
    _nextGestureRequestId = -1;
    _indicatorColor = UIColor.systemBlueColor;
    _titleColor = UIColor.secondaryLabelColor;
    self.clipsToBounds = YES;
  }
  return self;
}

- (void)mountChildComponentView:(UIView<RCTComponentViewProtocol> *)childComponentView index:(NSInteger)index
{
  NSAssert(index == 0, @"SmartRefreshLayout accepts exactly one child.");
  [super mountChildComponentView:childComponentView index:index];

  __weak ExpoSmartRefreshLayoutView *weakSelf = self;
  dispatch_async(dispatch_get_main_queue(), ^{
    ExpoSmartRefreshLayoutView *strongSelf = weakSelf;
    if (strongSelf == nil || childComponentView.superview == nil) {
      return;
    }
    [strongSelf attachToScrollView:RNSFindScrollView(childComponentView)];
  });
}

- (void)unmountChildComponentView:(UIView<RCTComponentViewProtocol> *)childComponentView index:(NSInteger)index
{
  if (_scrollView != nil && [_scrollView isDescendantOfView:childComponentView]) {
    [self detachFromScrollView];
  }
  [super unmountChildComponentView:childComponentView index:index];
}

- (void)layoutSubviews
{
  [super layoutSubviews];
  if (_scrollView == nil) {
    [self attachToScrollView:RNSFindScrollView(self)];
  }
}

- (void)updateProps:(Props::Shared const &)props oldProps:(Props::Shared const &)oldProps
{
  const auto &oldViewProps = *std::static_pointer_cast<ExpoSmartRefreshLayoutViewProps const>(_props);
  const auto &newViewProps = *std::static_pointer_cast<ExpoSmartRefreshLayoutViewProps const>(props);

  BOOL rebuildHeader =
      oldViewProps.headerStyle != newViewProps.headerStyle ||
      oldViewProps.indicatorColor != newViewProps.indicatorColor ||
      oldViewProps.titleColor != newViewProps.titleColor ||
      oldViewProps.pullDownText != newViewProps.pullDownText ||
      oldViewProps.releaseToRefreshText != newViewProps.releaseToRefreshText ||
      oldViewProps.refreshingText != newViewProps.refreshingText ||
      oldViewProps.refreshCompleteText != newViewProps.refreshCompleteText;
  BOOL rebuildFooter =
      oldViewProps.titleColor != newViewProps.titleColor ||
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
  _indicatorColor = newViewProps.indicatorColor
      ? RCTUIColorFromSharedColor(newViewProps.indicatorColor)
      : UIColor.systemBlueColor;
  _titleColor = newViewProps.titleColor
      ? RCTUIColorFromSharedColor(newViewProps.titleColor)
      : UIColor.secondaryLabelColor;
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
      _scrollView.mj_header = nil;
    } else if (rebuildHeader || _scrollView.mj_header == nil) {
      [self configureHeader];
    }

    if (!_loadMoreEnabled) {
      _scrollView.mj_footer = nil;
    } else if (rebuildFooter || _scrollView.mj_footer == nil) {
      [self configureFooter];
    }

    if ((!oldViewProps.refreshing || rebuildHeader) && newViewProps.refreshing) {
      [self beginRefreshVisualOnly];
    } else if (oldViewProps.refreshing && !newViewProps.refreshing) {
      [self cancelOperation:RNSOperationKindRefresh];
      [_scrollView.mj_header endRefreshing];
    }

    if ((!oldViewProps.loadingMore || rebuildFooter) && newViewProps.loadingMore) {
      [self beginLoadMoreVisualOnly];
    } else if (oldViewProps.loadingMore && !newViewProps.loadingMore) {
      [self cancelOperation:RNSOperationKindLoadMore];
      [_scrollView.mj_footer endRefreshing];
    }

    if (_noMoreData) {
      [self disarmAutoLoadMore];
      [self cancelOperation:RNSOperationKindLoadMore];
      [_scrollView.mj_footer endRefreshingWithNoMoreData];
    } else {
      [_scrollView.mj_footer resetNoMoreData];
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

  // Fabric can mount the child after the first props update. Reapply state
  // here so an initially controlled refresh or loading operation is not lost.
  if (_refreshing) {
    [self beginRefreshVisualOnly];
  }
  if (_loadingMore) {
    [self beginLoadMoreVisualOnly];
  }
  if (_noMoreData) {
    [_scrollView.mj_footer endRefreshingWithNoMoreData];
  }
}

- (void)detachFromScrollView
{
  [self invalidateDelayedOperations];
  [_scrollView.mj_header endRefreshing];
  [_scrollView.mj_footer endRefreshing];
  _scrollView.mj_header = nil;
  _scrollView.mj_footer = nil;
  _scrollView = nil;
}

- (void)configureHeader
{
  __weak ExpoSmartRefreshLayoutView *weakSelf = self;
  RNSRefreshHeader *header = [RNSRefreshHeader headerWithRefreshingBlock:^{
    ExpoSmartRefreshLayoutView *strongSelf = weakSelf;
    if (strongSelf == nil) {
      return;
    }
    if (strongSelf->_suppressNextRefreshRequest) {
      strongSelf->_suppressNextRefreshRequest = NO;
      return;
    }
    if (
        strongSelf->_activeOperationKind != RNSOperationKindNone ||
        strongSelf->_scheduledOperationKind != RNSOperationKindNone) {
      [strongSelf->_scrollView.mj_header endRefreshing];
      return;
    }
    NSInteger requestId = [strongSelf allocateGestureRequestId];
    strongSelf->_activeOperationKind = RNSOperationKindRefresh;
    strongSelf->_activeRequestId = requestId;
    [strongSelf disarmAutoLoadMore];
    strongSelf->_refreshing = YES;
    [strongSelf emitState:@"refreshing"];
    [strongSelf emitRefresh:requestId source:@"gesture"];
  }];

  [header setTitle:_pullDownText forState:MJRefreshStateIdle];
  [header setTitle:_releaseToRefreshText forState:MJRefreshStatePulling];
  [header setTitle:_refreshingText forState:MJRefreshStateRefreshing];
  header.stateLabel.textColor = _titleColor;
  header.lastUpdatedTimeLabel.hidden = YES;
  header.loadingView.color = _indicatorColor;
  header.arrowView.tintColor = _indicatorColor;

  if (_materialHeader) {
    header.stateLabel.hidden = YES;
    header.arrowView.hidden = YES;
  }

  header.stateChanged = ^(MJRefreshState state, CGFloat percent) {
    ExpoSmartRefreshLayoutView *strongSelf = weakSelf;
    if (strongSelf == nil) {
      return;
    }

    if (state == MJRefreshStatePulling) {
      if (strongSelf->_hapticsEnabled && !strongSelf->_didTriggerHeaderHaptic) {
        UIImpactFeedbackGenerator *generator =
            [[UIImpactFeedbackGenerator alloc] initWithStyle:UIImpactFeedbackStyleLight];
        [generator impactOccurred];
        strongSelf->_didTriggerHeaderHaptic = YES;
      }
      [strongSelf emitState:@"ready"];
    } else if (state == MJRefreshStateRefreshing) {
      [strongSelf emitState:@"refreshing"];
    } else if (state == MJRefreshStateIdle && percent > 0) {
      strongSelf->_didTriggerHeaderHaptic = NO;
      [strongSelf emitState:@"pulling"];
    } else if (state == MJRefreshStateIdle) {
      strongSelf->_didTriggerHeaderHaptic = NO;
      [strongSelf emitState:@"idle"];
    }
  };

  _scrollView.mj_header = header;
}

- (void)configureFooter
{
  __weak ExpoSmartRefreshLayoutView *weakSelf = self;
  MJRefreshComponentAction action = ^{
    ExpoSmartRefreshLayoutView *strongSelf = weakSelf;
    if (strongSelf == nil) {
      return;
    }
    if (strongSelf->_suppressNextLoadMoreRequest) {
      strongSelf->_suppressNextLoadMoreRequest = NO;
      return;
    }
    if (
        strongSelf->_noMoreData ||
        strongSelf->_activeOperationKind != RNSOperationKindNone ||
        strongSelf->_scheduledOperationKind != RNSOperationKindNone) {
      [strongSelf->_scrollView.mj_footer endRefreshing];
      return;
    }
    NSInteger requestId = [strongSelf allocateGestureRequestId];
    strongSelf->_activeOperationKind = RNSOperationKindLoadMore;
    strongSelf->_activeRequestId = requestId;
    [strongSelf disarmAutoLoadMore];
    strongSelf->_loadingMore = YES;
    [strongSelf emitState:@"loading"];
    [strongSelf emitLoadMore:requestId source:@"gesture"];
  };

  void (^stateChanged)(MJRefreshState) = ^(MJRefreshState state) {
    ExpoSmartRefreshLayoutView *strongSelf = weakSelf;
    if (strongSelf == nil) {
      return;
    }
    if (state == MJRefreshStateRefreshing) {
      [strongSelf emitState:@"loading"];
    } else if (state == MJRefreshStateNoMoreData) {
      [strongSelf emitState:@"no-more-data"];
    } else if (state == MJRefreshStatePulling) {
      [strongSelf emitState:@"ready"];
    } else {
      [strongSelf emitState:@"idle"];
    }
  };

  if (_autoLoadMoreEnabled) {
    RNSAutoRefreshFooter *footer = [RNSAutoRefreshFooter footerWithRefreshingBlock:action];
    [footer setTitle:_pullUpText forState:MJRefreshStateIdle];
    [footer setTitle:_loadingMoreText forState:MJRefreshStateRefreshing];
    [footer setTitle:_noMoreDataText forState:MJRefreshStateNoMoreData];
    footer.stateLabel.textColor = _titleColor;
    footer.loadingView.color = _indicatorColor;
    footer.triggerAutomaticallyRefreshPercent = 1.0;
    footer.automaticallyRefresh = NO;
    footer.requestArmed = NO;
    footer.stateChanged = stateChanged;
    _scrollView.mj_footer = footer;
  } else {
    RNSRefreshFooter *footer = [RNSRefreshFooter footerWithRefreshingBlock:action];
    [footer setTitle:_pullUpText forState:MJRefreshStateIdle];
    [footer setTitle:_releaseToLoadMoreText forState:MJRefreshStatePulling];
    [footer setTitle:_loadingMoreText forState:MJRefreshStateRefreshing];
    [footer setTitle:_noMoreDataText forState:MJRefreshStateNoMoreData];
    footer.stateLabel.textColor = _titleColor;
    footer.loadingView.color = _indicatorColor;
    footer.stateChanged = stateChanged;
    _scrollView.mj_footer = footer;
  }

  if (_noMoreData) {
    [_scrollView.mj_footer endRefreshingWithNoMoreData];
  }
}

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

- (void)beginRefresh:(NSInteger)requestId delayMs:(NSInteger)delayMs
{
  if (
      requestId <= 0 ||
      _activeOperationKind != RNSOperationKindNone ||
      _scheduledOperationKind != RNSOperationKindNone) {
    return;
  }

  _scheduledOperationKind = RNSOperationKindRefresh;
  _scheduledRequestId = requestId;
  NSUInteger generation = ++_refreshBeginGeneration;
  __weak ExpoSmartRefreshLayoutView *weakSelf = self;
  dispatch_after(
      dispatch_time(DISPATCH_TIME_NOW, (int64_t)(MAX(0, delayMs) * NSEC_PER_MSEC)),
      dispatch_get_main_queue(), ^{
        ExpoSmartRefreshLayoutView *strongSelf = weakSelf;
        if (
            strongSelf == nil ||
            strongSelf->_refreshBeginGeneration != generation ||
            strongSelf->_scheduledOperationKind != RNSOperationKindRefresh ||
            strongSelf->_scheduledRequestId != requestId) {
          return;
        }
        strongSelf->_scheduledOperationKind = RNSOperationKindNone;
        strongSelf->_scheduledRequestId = 0;
        strongSelf->_activeOperationKind = RNSOperationKindRefresh;
        strongSelf->_activeRequestId = requestId;
        strongSelf->_refreshing = YES;
        [strongSelf disarmAutoLoadMore];
        [strongSelf beginRefreshVisualOnly];
        [strongSelf emitState:@"refreshing"];
        [strongSelf emitRefresh:requestId source:@"programmatic"];
      });
}

- (void)finishRefresh:(NSInteger)requestId success:(BOOL)success delayMs:(NSInteger)delayMs
{
  BOOL matchesActive =
      _activeOperationKind == RNSOperationKindRefresh && _activeRequestId == requestId;
  BOOL matchesScheduled =
      _scheduledOperationKind == RNSOperationKindRefresh && _scheduledRequestId == requestId;
  BOOL visualOnly = requestId == 0 && _activeOperationKind == RNSOperationKindNone;
  if (!matchesActive && !matchesScheduled && !visualOnly) {
    return;
  }

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

        RNSRefreshHeader *header = (RNSRefreshHeader *)strongSelf->_scrollView.mj_header;
        if (
            success &&
            [header isKindOfClass:[RNSRefreshHeader class]] &&
            strongSelf->_refreshCompleteText.length > 0) {
          [header setTitle:strongSelf->_refreshCompleteText forState:MJRefreshStateIdle];
          NSString *pullDownText = strongSelf->_pullDownText;
          [header endRefreshingWithCompletionBlock:^{
            dispatch_after(
                dispatch_time(DISPATCH_TIME_NOW, (int64_t)(350 * NSEC_PER_MSEC)),
                dispatch_get_main_queue(), ^{
                  if (header.state == MJRefreshStateIdle) {
                    [header setTitle:pullDownText forState:MJRefreshStateIdle];
                  }
                });
          }];
        } else {
          [header endRefreshing];
        }
        strongSelf->_refreshing = NO;
        [strongSelf emitState:@"idle"];
      });
}

- (void)beginLoadMore:(NSInteger)requestId delayMs:(NSInteger)delayMs
{
  if (
      requestId <= 0 ||
      _activeOperationKind != RNSOperationKindNone ||
      _scheduledOperationKind != RNSOperationKindNone) {
    return;
  }

  _scheduledOperationKind = RNSOperationKindLoadMore;
  _scheduledRequestId = requestId;
  NSUInteger generation = ++_loadMoreBeginGeneration;
  __weak ExpoSmartRefreshLayoutView *weakSelf = self;
  dispatch_after(
      dispatch_time(DISPATCH_TIME_NOW, (int64_t)(MAX(0, delayMs) * NSEC_PER_MSEC)),
      dispatch_get_main_queue(), ^{
        ExpoSmartRefreshLayoutView *strongSelf = weakSelf;
        if (
            strongSelf == nil ||
            strongSelf->_loadMoreBeginGeneration != generation ||
            strongSelf->_scheduledOperationKind != RNSOperationKindLoadMore ||
            strongSelf->_scheduledRequestId != requestId) {
          return;
        }
        strongSelf->_scheduledOperationKind = RNSOperationKindNone;
        strongSelf->_scheduledRequestId = 0;
        strongSelf->_activeOperationKind = RNSOperationKindLoadMore;
        strongSelf->_activeRequestId = requestId;
        strongSelf->_loadingMore = YES;
        [strongSelf disarmAutoLoadMore];
        [strongSelf beginLoadMoreVisualOnly];
        [strongSelf emitState:@"loading"];
        [strongSelf emitLoadMore:requestId source:@"programmatic"];
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
        [strongSelf disarmAutoLoadMore];
        if (noMoreData) {
          [strongSelf->_scrollView.mj_footer endRefreshingWithNoMoreData];
          [strongSelf emitState:@"no-more-data"];
        } else {
          [strongSelf->_scrollView.mj_footer endRefreshing];
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
  [_scrollView.mj_footer resetNoMoreData];
  [self emitState:@"idle"];
}

- (NSInteger)allocateGestureRequestId
{
  NSInteger requestId = _nextGestureRequestId;
  _nextGestureRequestId =
      requestId == INT_MIN ? -1 : requestId - 1;
  return requestId;
}

- (void)beginRefreshVisualOnly
{
  if (_scrollView.mj_header == nil || _scrollView.mj_header.isRefreshing) {
    return;
  }
  _suppressNextRefreshRequest = YES;
  [_scrollView.mj_header beginRefreshing];
  __weak ExpoSmartRefreshLayoutView *weakSelf = self;
  dispatch_async(dispatch_get_main_queue(), ^{
    ExpoSmartRefreshLayoutView *strongSelf = weakSelf;
    if (strongSelf != nil) {
      strongSelf->_suppressNextRefreshRequest = NO;
    }
  });
}

- (void)beginLoadMoreVisualOnly
{
  if (_scrollView.mj_footer == nil || _scrollView.mj_footer.isRefreshing) {
    return;
  }
  _suppressNextLoadMoreRequest = YES;
  [_scrollView.mj_footer beginRefreshing];
  __weak ExpoSmartRefreshLayoutView *weakSelf = self;
  dispatch_async(dispatch_get_main_queue(), ^{
    ExpoSmartRefreshLayoutView *strongSelf = weakSelf;
    if (strongSelf != nil) {
      strongSelf->_suppressNextLoadMoreRequest = NO;
    }
  });
}

- (void)disarmAutoLoadMore
{
  RNSAutoRefreshFooter *footer = (RNSAutoRefreshFooter *)_scrollView.mj_footer;
  if ([footer isKindOfClass:[RNSAutoRefreshFooter class]]) {
    footer.requestArmed = NO;
    footer.automaticallyRefresh = NO;
  }
}

- (void)cancelOperation:(RNSOperationKind)kind
{
  if (_scheduledOperationKind == kind) {
    if (kind == RNSOperationKindRefresh) {
      _refreshBeginGeneration++;
    } else {
      _loadMoreBeginGeneration++;
    }
    _scheduledOperationKind = RNSOperationKindNone;
    _scheduledRequestId = 0;
  }
  if (_activeOperationKind == kind) {
    _activeOperationKind = RNSOperationKindNone;
    _activeRequestId = 0;
  }
  if (kind == RNSOperationKindRefresh) {
    _refreshFinishGeneration++;
  } else {
    _loadMoreFinishGeneration++;
  }
}

- (void)invalidateDelayedOperations
{
  _refreshBeginGeneration++;
  _loadMoreBeginGeneration++;
  _refreshFinishGeneration++;
  _loadMoreFinishGeneration++;
  _scheduledOperationKind = RNSOperationKindNone;
  _scheduledRequestId = 0;
  _activeOperationKind = RNSOperationKindNone;
  _activeRequestId = 0;
  _suppressNextRefreshRequest = NO;
  _suppressNextLoadMoreRequest = NO;
}

@end
