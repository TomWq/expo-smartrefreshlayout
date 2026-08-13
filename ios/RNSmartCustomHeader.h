#import <UIKit/UIKit.h>

#import "SmartRefreshControl/Component/UIRefreshHeader.h"

NS_ASSUME_NONNULL_BEGIN

typedef void (^RNSmartCustomHeaderScrollChanged)(CGFloat offset, CGFloat percent, BOOL isDragging);
typedef void (^RNSmartCustomHeaderStatusChanged)(UIRefreshStatus oldStatus, UIRefreshStatus status);
typedef void (^RNSmartCustomHeaderInitialized)(CGFloat height, CGFloat maxDragHeight);
typedef void (^RNSmartCustomHeaderLifecycle)(CGFloat height, CGFloat maxDragHeight);
typedef void (^RNSmartCustomHeaderFinished)(BOOL success);

typedef NS_ENUM(NSInteger, RNSmartCustomHeaderSpinnerStyle) {
  RNSmartCustomHeaderSpinnerStyleScale,
  RNSmartCustomHeaderSpinnerStyleTranslate,
  RNSmartCustomHeaderSpinnerStyleFixedBehind,
};

@interface RNSmartCustomHeader : UIRefreshHeader

@property (nonatomic, copy, nullable) RNSmartCustomHeaderScrollChanged scrollChanged;
@property (nonatomic, copy, nullable) RNSmartCustomHeaderStatusChanged statusChanged;
@property (nonatomic, copy, nullable) RNSmartCustomHeaderInitialized initialized;
@property (nonatomic, copy, nullable) RNSmartCustomHeaderLifecycle released;
@property (nonatomic, copy, nullable) RNSmartCustomHeaderLifecycle started;
@property (nonatomic, copy, nullable) RNSmartCustomHeaderFinished finished;
@property (nonatomic, assign) RNSmartCustomHeaderSpinnerStyle customSpinnerStyle;
@property (nonatomic, assign) CGFloat maxDragRate;
@property (nonatomic, readonly) CGFloat maxDragHeight;

- (instancetype)initWithContentView:(UIView *)contentView;
- (void)updateContentHeight:(CGFloat)height;

@end

NS_ASSUME_NONNULL_END
