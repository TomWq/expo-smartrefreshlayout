#import "RNSmartCustomHeader.h"

@implementation RNSmartCustomHeader {
  __weak UIView *_contentView;
  CGFloat _contentHeight;
}

- (instancetype)initWithContentView:(UIView *)contentView
{
  if (self = [super initWithFrame:CGRectZero]) {
    _contentView = contentView;
    _contentHeight = MAX(CGRectGetHeight(contentView.bounds), 80);
    self.scrollMode = UISmartScrollModeMove;
    self.clipsToBounds = YES;
    [self addSubview:contentView];
    [self updateContentHeight:_contentHeight];
  }
  return self;
}

- (void)updateContentHeight:(CGFloat)height
{
  CGFloat nextHeight = MAX(height, 1);
  if (_contentHeight == nextHeight && self.height == nextHeight) {
    return;
  }
  _contentHeight = nextHeight;
  self.height = nextHeight;
  [self setNeedsLayout];
}

- (void)layoutSubviews
{
  [super layoutSubviews];
  UIView *contentView = _contentView;
  if (contentView != nil) {
    contentView.frame = self.bounds;
  }
}

- (void)onScrollingWithOffset:(CGFloat)offset percent:(CGFloat)percent drag:(BOOL)isDragging
{
  [super onScrollingWithOffset:offset percent:percent drag:isDragging];
  if (self.scrollChanged != nil) {
    self.scrollChanged(offset, percent, isDragging);
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
