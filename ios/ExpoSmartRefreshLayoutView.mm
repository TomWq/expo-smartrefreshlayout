#import "ExpoSmartRefreshLayoutView.h"

#import "SmartRefreshControl/RNSmartRefreshAdapter.h"
#import <React/RCTComponentViewFactory.h>
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
  UIScrollView *_scrollView;
  UIRefreshHeader *_header;
  UIRefreshFooter *_footer;

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
  // Expo's generated third-party provider uses NSClassFromString. Registering
  // eagerly also covers static-library/linker configurations where that
  // lookup happens before the component class is materialized.
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

- (void)unmountChildComponentView:(UIView<RCTComponentViewProtocol> *)childComponentView
                             index:(NSInteger)index
{
  if (_scrollView != nil &&
      (_scrollView == (UIScrollView *)childComponentView ||
       [_scrollView isDescendantOfView:childComponentView])) {
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

  BOOL oldRefreshing = oldViewProps.refreshing;
  BOOL oldLoadingMore = oldViewProps.loadingMore;
  BOOL oldRefreshEnabled = oldViewProps.refreshEnabled;
  BOOL oldLoadMoreEnabled = oldViewProps.loadMoreEnabled;

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

  // Fabric can mount children after the initial props update. Reapply
  // controlled visuals without emitting duplicate request events.
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
  UIRefreshHeader *header = _materialHeader
      ? (UIRefreshHeader *)[RNSmartMaterialHeader new]
      : (UIRefreshHeader *)[RNSmartClassicsHeader new];
  header.colorAccent = _indicatorColor;
  if (_materialHeader) {
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

  header.refreshBlock = ^(UIRefreshHeader *component) {
    ExpoSmartRefreshLayoutView *strongSelf = weakSelf;
    if (strongSelf != nil) {
      [strongSelf refreshComponentDidRequest:component];
    }
  };
  if (_materialHeader) {
    RNSmartMaterialHeader *material = (RNSmartMaterialHeader *)header;
    material.statusChanged = ^(UIRefreshStatus oldStatus, UIRefreshStatus status) {
      ExpoSmartRefreshLayoutView *strongSelf = weakSelf;
      if (strongSelf != nil) {
        [strongSelf headerStateChanged:oldStatus status:status];
      }
    };
    material.scrollChanged = ^(CGFloat offset, CGFloat percent, BOOL isDragging) {
      (void)offset;
      (void)percent;
      (void)isDragging;
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
      (void)offset;
      (void)percent;
      (void)isDragging;
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
  if (_suppressNextRefreshRequest) {
    _suppressNextRefreshRequest = NO;
    return;
  }

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

#pragma mark - Commands

- (void)beginRefresh:(NSInteger)requestId delayMs:(NSInteger)delayMs
{
  if (requestId <= 0 || !_refreshEnabled || _header == nil ||
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
  NSInteger requestId = _nextGestureRequestId;
  _nextGestureRequestId = requestId == INT_MIN ? -1 : requestId - 1;
  return requestId;
}

- (void)beginRefreshVisualOnly
{
  if (_header == nil || _header.isRefreshing) {
    return;
  }
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
