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
  // Translate 直接沿用原生 Move 实现。Scale/FixedBehind 选用 Stretch，是为了
  // 避免原生布局把自定义 frame 重置到 -height；实际几何由下方 adjustFrame 提供。
  self.scrollMode = classicSpinnerStyle == RNSmartClassicSpinnerStyleTranslate
      ? UISmartScrollModeMove
      : UISmartScrollModeStretch;
  // FixedBehind 保持桥接层选定的前置几何位置，但仍需扩展 contentInset，
  // 让滚动内容下移并从后方露出 Header。
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

  // 滚动视图中的可视坐标为 frame.origin.y - contentOffset.y。锚定 Header 时要
  // 排除 Header 自身扩展的 inset：它只应把内容下移，不能同时把 Header 下移。
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
  // 将第三方 Header 状态映射到 JS 配置的四组文案。Finish/Idle 期间只有成功
  // 完成且配置了完成文案时才暂时保留 refreshCompleteText。
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
    // FixedBehind 依靠扩展 inset 从内容后方显露。始终放到滚动内容背后，
    // 否则进入刷新态后视觉效果会退化成 Translate。
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
  // UIRefreshClassicsFooter 默认开启自动加载，RN API 则要求显式启用。
  // 因此初始关闭 auto，避免普通上拉 Footer 意外获得自动触发入口。
  self.isAutoLoadMore = NO;
  self.automaticRequestsArmed = NO;
}

- (void)beginLoadMore
{
  // auto 模式只有在用户真实向上拖动解锁后才能由底层触发；实例命令通过
  // forceProgrammaticBegin 临时旁路该限制，但不会永久解锁自动加载。
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
    // 只在内容超过一屏且用户正在向上拖动时解锁。这样首次布局、短列表、
    // contentSize 调整和程序化滚动都不会直接触发自动加载。
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
  // 将 Footer 的细粒度状态映射为上拉、释放、加载中和无更多数据四组文案。
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
