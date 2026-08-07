#import "RNSmartRefreshAdapter.h"

@interface RNSmartClassicsHeader ()
- (void)applyTextForStatus:(UIRefreshStatus)status;
@end

@implementation RNSmartClassicsHeader

- (void)setUpComponent
{
  [super setUpComponent];
  self.showingCompletionText = NO;
  self.classicSpinnerStyle = RNSmartClassicSpinnerStyleTranslate;
}

- (void)setClassicSpinnerStyle:(RNSmartClassicSpinnerStyle)classicSpinnerStyle
{
  if (_classicSpinnerStyle == classicSpinnerStyle) {
    return;
  }

  _classicSpinnerStyle = classicSpinnerStyle;
  // Move is the stock implementation used by Translate. Stretch avoids the
  // stock layout pass resetting the custom Scale/FixedBehind frame to -height;
  // the actual geometry is supplied by -adjustFrameWithHeight:... below.
  self.scrollMode = classicSpinnerStyle == RNSmartClassicSpinnerStyleTranslate
      ? UISmartScrollModeMove
      : UISmartScrollModeStretch;
  // FixedBehind keeps the header at the front-positioned geometry selected by
  // the bridge, but still needs an expanded inset so the scroll content moves
  // down and exposes the header from behind.
  self.expandsContentInset =
      classicSpinnerStyle == RNSmartClassicSpinnerStyleFixedBehind;
  self.clipsToBounds = classicSpinnerStyle == RNSmartClassicSpinnerStyleScale;
  [self refreshSpinnerStyleLayout];
}

- (void)refreshSpinnerStyleLayout
{
  UIScrollView *scrollView = self.scrollView;
  if (scrollView == nil || self.classicSpinnerStyle == RNSmartClassicSpinnerStyleTranslate) {
    return;
  }

  CGFloat insetTop = [self finalyContentInsetsFrom:scrollView];
  [self adjustFrameWithHeight:self.expandHeight
                         inset:insetTop
                       expand:self.isExpanded
                       offset:scrollView.contentOffset.y];
  [self setNeedsLayout];
}

- (void)adjustFrameWithHeight:(CGFloat)expandHeight
                          inset:(CGFloat)insetTop
                         expand:(BOOL)isExpanded
                         offset:(CGFloat)offset
{
  if (self.classicSpinnerStyle == RNSmartClassicSpinnerStyleTranslate) {
    [super adjustFrameWithHeight:expandHeight
                             inset:insetTop
                           expand:isExpanded
                           offset:offset];
    return;
  }

  // The scroll view's visual coordinate is frame.origin.y - contentOffset.y.
  // Exclude the header's own expanded inset when anchoring the frame: that
  // inset moves the content down, but must not move the header down with it.
  CGFloat fullHeight = MAX(expandHeight, 1);
  CGFloat contentInsetTop = insetTop - (isExpanded ? fullHeight : 0);
  CGFloat pullDistance = MAX(0, -offset - contentInsetTop);
  CGFloat visibleHeight = self.classicSpinnerStyle == RNSmartClassicSpinnerStyleScale
      ? MIN(fullHeight, pullDistance)
      : fullHeight;
  CGRect frame = self.frame;
  frame.origin.x = 0;
  frame.origin.y = offset + contentInsetTop;
  frame.size.width = self.width;
  frame.size.height = isExpanded ? fullHeight : visibleHeight;

  if (!CGRectEqualToRect(self.frame, frame)) {
    [self setFrame:frame];
  }
}

- (void)scrollView:(UIScrollView *)scrollView
       didChange:(CGPoint)oldOffset
   contentOffset:(CGPoint)newOffset
{
  [super scrollView:scrollView didChange:oldOffset contentOffset:newOffset];
  [self refreshSpinnerStyleLayout];
}

- (void)applyTextForStatus:(UIRefreshStatus)status
{
  NSString *text = self.pullDownText;
  switch (status) {
    case UIRefreshStatusReleaseToRefresh:
      text = self.releaseToRefreshText;
      break;
    case UIRefreshStatusWillRefresh:
    case UIRefreshStatusReleasing:
    case UIRefreshStatusRefreshing:
      text = self.refreshingText;
      break;
    case UIRefreshStatusFinish:
      if (self.showingCompletionText && self.refreshCompleteText.length > 0) {
        text = self.refreshCompleteText;
      }
      break;
    case UIRefreshStatusIdle:
    case UIRefreshStatusPullToRefresh:
    default:
      if (self.showingCompletionText && self.refreshCompleteText.length > 0) {
        text = self.refreshCompleteText;
      }
      break;
  }
  if (text.length > 0) {
    self.labelTitle.text = text;
    [self setNeedsLayout];
  }
}

- (void)onStatus:(UIRefreshStatus)oldStatus changed:(UIRefreshStatus)status
{
  [super onStatus:oldStatus changed:status];
  UIScrollView *scrollView = self.scrollView;
  if (scrollView != nil &&
      self.classicSpinnerStyle == RNSmartClassicSpinnerStyleFixedBehind) {
    // FixedBehind is revealed by its expanded content inset; moving it above
    // the scroll content changes it into Translate during the refresh state.
    [scrollView sendSubviewToBack:self];
  }
  [self applyTextForStatus:status];
  if (self.statusChanged != nil) {
    self.statusChanged(oldStatus, status);
  }
}

- (void)onScrollingWithOffset:(CGFloat)offset percent:(CGFloat)percent drag:(BOOL)isDragging
{
  [super onScrollingWithOffset:offset percent:percent drag:isDragging];
  if (self.scrollChanged != nil) {
    self.scrollChanged(offset, percent, isDragging);
  }
}

- (void)onRefreshFinished:(BOOL)success
{
  [super onRefreshFinished:success];
  self.showingCompletionText = success && self.refreshCompleteText.length > 0;
  [self applyTextForStatus:UIRefreshStatusFinish];
}

- (void)restoreDefaultText
{
  self.showingCompletionText = NO;
  [self applyTextForStatus:UIRefreshStatusIdle];
}

@end

@implementation RNSmartMaterialHeader

- (void)onStatus:(UIRefreshStatus)oldStatus changed:(UIRefreshStatus)status
{
  [super onStatus:oldStatus changed:status];
  if (self.statusChanged != nil) {
    self.statusChanged(oldStatus, status);
  }
}

- (void)onScrollingWithOffset:(CGFloat)offset percent:(CGFloat)percent drag:(BOOL)isDragging
{
  [super onScrollingWithOffset:offset percent:percent drag:isDragging];
  if (self.scrollChanged != nil) {
    self.scrollChanged(offset, percent, isDragging);
  }
}

@end

@interface RNSmartClassicsFooter ()
@property (nonatomic, assign) BOOL forceProgrammaticBegin;
- (void)applyTextForStatus:(UISmartFooterStatus)status;
@end

@implementation RNSmartClassicsFooter

- (void)setUpComponent
{
  [super setUpComponent];
  // UIRefreshClassicsFooter defaults to auto mode. The RN prop opts in to it
  // explicitly, so a regular pull footer never receives a stray tap target.
  self.isAutoLoadMore = NO;
  self.automaticRequestsArmed = NO;
}

- (void)beginLoadMore
{
  if (self.isAutoLoadMore && !self.automaticRequestsArmed && !self.forceProgrammaticBegin) {
    return;
  }
  [super beginLoadMore];
}

- (void)beginProgrammaticLoadMore
{
  self.forceProgrammaticBegin = YES;
  [super beginLoadMore];
  self.forceProgrammaticBegin = NO;
}

- (void)disarmAutomaticRequests
{
  self.automaticRequestsArmed = NO;
}

- (void)scrollView:(UIScrollView *)scrollView
             didChange:(CGPoint)oldOffset
             contentOffset:(CGPoint)newOffset
{
  if (self.isAutoLoadMore && !self.automaticRequestsArmed) {
    UIEdgeInsets inset = scrollView.adjustedContentInset;
    BOOL contentExceedsViewport =
        scrollView.contentSize.height + inset.top + inset.bottom >
        CGRectGetHeight(scrollView.bounds) + 1;
    if (scrollView.isDragging && newOffset.y > oldOffset.y && contentExceedsViewport) {
      self.automaticRequestsArmed = YES;
    }
  }
  [super scrollView:scrollView didChange:oldOffset contentOffset:newOffset];
}

- (void)applyTextForStatus:(UISmartFooterStatus)status
{
  NSString *text = self.pullUpText;
  switch (status) {
    case UISmartFooterStatusReleaseToLoadMore:
    case UISmartFooterStatusReleasing:
      text = self.releaseToLoadMoreText;
      break;
    case UISmartFooterStatusWillLoadMore:
    case UISmartFooterStatusLoading:
      text = self.loadingMoreText;
      break;
    case UISmartFooterStatusNoMoreData:
      text = self.noMoreDataText;
      break;
    case UISmartFooterStatusIdle:
    case UISmartFooterStatusPullToLoadMore:
    case UISmartFooterStatusFinish:
    default:
      break;
  }
  if (text.length > 0) {
    self.labelTitle.text = text;
    [self setNeedsLayout];
  }
}

- (void)onStatus:(UISmartFooterStatus)oldStatus changed:(UISmartFooterStatus)status
{
  [super onStatus:oldStatus changed:status];
  [self applyTextForStatus:status];
  if (self.statusChanged != nil) {
    self.statusChanged(oldStatus, status);
  }
}

- (void)onScrollingWithOffset:(CGFloat)offset percent:(CGFloat)percent drag:(BOOL)isDragging
{
  [super onScrollingWithOffset:offset percent:percent drag:isDragging];
  if (self.scrollChanged != nil) {
    self.scrollChanged(offset, percent, isDragging);
  }
}

@end
