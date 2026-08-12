#import <UIKit/UIKit.h>

#import "SmartRefreshControl/Component/UIRefreshHeader.h"

NS_ASSUME_NONNULL_BEGIN

typedef void (^RNSmartCustomHeaderScrollChanged)(CGFloat offset, CGFloat percent, BOOL isDragging);
typedef void (^RNSmartCustomHeaderStatusChanged)(UIRefreshStatus oldStatus, UIRefreshStatus status);

@interface RNSmartCustomHeader : UIRefreshHeader

@property (nonatomic, copy, nullable) RNSmartCustomHeaderScrollChanged scrollChanged;
@property (nonatomic, copy, nullable) RNSmartCustomHeaderStatusChanged statusChanged;

- (instancetype)initWithContentView:(UIView *)contentView;
- (void)updateContentHeight:(CGFloat)height;

@end

NS_ASSUME_NONNULL_END
