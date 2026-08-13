#import "RNSmartCustomHeader.h"

#include <cmath>

static const CGFloat RNSDefaultCustomHeaderTriggerRate = 1;
static const CGFloat RNSDefaultCustomHeaderMaxDragRate = 2;
static const CGFloat RNSMaximumCustomHeaderMaxDragRate = 9;

@implementation RNSmartCustomHeader {
  __weak UIView *_contentView;
  CGFloat _contentHeight;
  CGFloat _maxDragRate;
  BOOL _didEmitInitialized;
  BOOL _didEmitReleased;
  BOOL _didEmitStarted;
  BOOL _didEmitFinished;
}

- (void)resetContentTransform
{
  UIView *contentView = _contentView;
  if (contentView == nil) {
    return;
  }
  contentView.layer.anchorPoint = CGPointMake(0.5, 0);
  contentView.layer.position = CGPointMake(CGRectGetMidX(self.bounds), 0);
  contentView.transform = CGAffineTransformIdentity;
}

- (void)applyContentScaleForOffset:(CGFloat)offset
{
  UIView *contentView = _contentView;
  if (contentView == nil) {
    return;
  }

  CGFloat baseHeight = MAX(_contentHeight, 1);
  CGFloat scale = MAX(0.01, MIN(offset / baseHeight, _maxDragRate));
  contentView.layer.anchorPoint = CGPointMake(0.5, 0);
  contentView.layer.position = CGPointMake(CGRectGetMidX(self.bounds), 0);
  contentView.transform = CGAffineTransformMakeScale(scale, scale);
}

- (instancetype)initWithContentView:(UIView *)contentView
{
  if (self = [super initWithFrame:CGRectZero]) {
    _contentView = contentView;
    _contentHeight = MAX(CGRectGetHeight(contentView.bounds), 80);
    _maxDragRate = RNSDefaultCustomHeaderMaxDragRate;
    self.customSpinnerStyle = RNSmartCustomHeaderSpinnerStyleTranslate;
    self.clipsToBounds = YES;
    [self addSubview:contentView];
    [self updateContentHeight:_contentHeight];
  }
  return self;
}

- (void)setCustomSpinnerStyle:(RNSmartCustomHeaderSpinnerStyle)customSpinnerStyle
{
  _customSpinnerStyle = customSpinnerStyle;
  [self resetContentTransform];
  switch (customSpinnerStyle) {
    case RNSmartCustomHeaderSpinnerStyleScale:
      self.scrollMode = UISmartScrollModeStretch;
      self.expandsContentInset = NO;
      self.staysBehindContent = NO;
      self.clipsToBounds = YES;
      break;
    case RNSmartCustomHeaderSpinnerStyleFixedBehind:
      self.scrollMode = UISmartScrollModeFront;
      self.expandsContentInset = YES;
      self.staysBehindContent = YES;
      self.clipsToBounds = YES;
      break;
    case RNSmartCustomHeaderSpinnerStyleTranslate:
    default:
      self.scrollMode = UISmartScrollModeMove;
      self.expandsContentInset = NO;
      self.staysBehindContent = NO;
      self.clipsToBounds = YES;
      break;
  }
  [self setNeedsLayout];
}

- (void)setMaxDragRate:(CGFloat)maxDragRate
{
  _maxDragRate = std::isfinite(maxDragRate) && maxDragRate >= 1
      ? MIN(maxDragRate, RNSMaximumCustomHeaderMaxDragRate)
      : RNSDefaultCustomHeaderMaxDragRate;
}

- (void)setTriggerRate:(CGFloat)triggerRate
{
  [super setTriggerRate:std::isfinite(triggerRate) && triggerRate > 0
      ? MIN(triggerRate, RNSDefaultCustomHeaderTriggerRate)
      : RNSDefaultCustomHeaderTriggerRate];
}

- (void)updateContentHeight:(CGFloat)height
{
  CGFloat nextHeight = MAX(height, 1);
  if (_contentHeight == nextHeight && self.height == nextHeight) {
    return;
  }
  _contentHeight = nextHeight;
  self.height = nextHeight;
  _didEmitInitialized = NO;
  [self setNeedsLayout];
}

- (CGFloat)maxDragHeight
{
  return MAX(self.expandHeight * _maxDragRate, self.expandHeight);
}

- (void)scrollView:(UIScrollView *)scrollView
       didChange:(CGPoint)oldOffset
   contentOffset:(CGPoint)newOffset
{
  // The vendored iOS kernel has no maximum drag constraint. Clamp the actual
  // scroll position while the user is pulling so custom Header maxDragRate is
  // a real limit instead of a reporting-only value.
  if (scrollView.isDragging) {
    CGFloat inset = [self finalyContentInsetsFrom:scrollView];
    CGFloat minimumOffset = -inset - self.maxDragHeight;
    if (newOffset.y < minimumOffset) {
      CGPoint clampedOffset = newOffset;
      clampedOffset.y = minimumOffset;
      [scrollView setContentOffset:clampedOffset animated:NO];
      newOffset = clampedOffset;
    }
  }
  [super scrollView:scrollView didChange:oldOffset contentOffset:newOffset];
}

- (void)layoutSubviews
{
  [super layoutSubviews];
  UIView *contentView = _contentView;
  if (contentView != nil) {
    // Stretch changes the native Header bounds to the current pull distance.
    // Keep Fabric content at its configured height so its own transform can
    // visibly scale instead of being laid out as a fixed-behind header.
    contentView.bounds = CGRectMake(0, 0, CGRectGetWidth(self.bounds), _contentHeight);
    if (_customSpinnerStyle == RNSmartCustomHeaderSpinnerStyleScale) {
      contentView.layer.anchorPoint = CGPointMake(0.5, 0);
      contentView.layer.position = CGPointMake(CGRectGetMidX(self.bounds), 0);
    } else {
      [self resetContentTransform];
    }
  }
}

- (void)onScrollingWithOffset:(CGFloat)offset percent:(CGFloat)percent drag:(BOOL)isDragging
{
  [super onScrollingWithOffset:offset percent:percent drag:isDragging];
  if (_customSpinnerStyle == RNSmartCustomHeaderSpinnerStyleScale) {
    [self applyContentScaleForOffset:offset];
  } else {
    [self resetContentTransform];
  }
  if (self.scrollChanged != nil) {
    self.scrollChanged(offset, percent, isDragging);
  }
}

- (void)onStartAnimationWhenRealeasing
{
  [super onStartAnimationWhenRealeasing];
  if (!_didEmitReleased && self.released != nil) {
    _didEmitReleased = YES;
    _didEmitStarted = NO;
    _didEmitFinished = NO;
    self.released(self.expandHeight, self.maxDragHeight);
  }
}

- (void)onStartAnimationWhenRefreshing
{
  [super onStartAnimationWhenRefreshing];
  _didEmitFinished = NO;
  if (!_didEmitStarted && self.started != nil) {
    _didEmitStarted = YES;
    self.started(self.expandHeight, self.maxDragHeight);
  }
}

- (CGFloat)onRefreshFinishing:(BOOL)success
{
  CGFloat duration = [super onRefreshFinishing:success];
  if (!_didEmitFinished && self.finished != nil) {
    _didEmitFinished = YES;
    _didEmitReleased = NO;
    self.finished(success);
  }
  [self resetContentTransform];
  return duration;
}

- (void)didMoveToSuperview
{
  [super didMoveToSuperview];
  if (self.superview != nil && !_didEmitInitialized && self.initialized != nil) {
    _didEmitInitialized = YES;
    self.initialized(self.expandHeight, self.maxDragHeight);
  }
}

- (void)onStatus:(UIRefreshStatus)oldStatus changed:(UIRefreshStatus)status
{
  [super onStatus:oldStatus changed:status];
  if (self.statusChanged != nil) {
    self.statusChanged(oldStatus, status);
  }
}

@end
