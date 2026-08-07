#import <UIKit/UIKit.h>

#import "Component/UIRefreshHeader.h"
#import "Component/UIRefreshFooter.h"
#import "Header/UIRefreshClassicsHeader.h"
#import "Header/UIRefreshMaterialHeader.h"
#import "Footer/UIRefreshClassicsFooter.h"

NS_ASSUME_NONNULL_BEGIN

typedef void (^RNSmartHeaderStatusChanged)(UIRefreshStatus oldStatus, UIRefreshStatus status);
typedef void (^RNSmartHeaderScrollChanged)(CGFloat offset, CGFloat percent, BOOL isDragging);
typedef void (^RNSmartFooterStatusChanged)(UISmartFooterStatus oldStatus, UISmartFooterStatus status);
typedef void (^RNSmartFooterScrollChanged)(CGFloat offset, CGFloat percent, BOOL isDragging);

/**
 * The Android ClassicsHeader spinner styles have direct geometry equivalents
 * in a UIScrollView-backed control. Translate follows the pulled content,
 * Scale grows only through the revealed distance, and FixedBehind stays
 * pinned behind the content while it moves away from the top edge.
 */
typedef NS_ENUM(NSInteger, RNSmartClassicSpinnerStyle) {
  RNSmartClassicSpinnerStyleScale,
  RNSmartClassicSpinnerStyleTranslate,
  RNSmartClassicSpinnerStyleFixedBehind,
};

/**
 * Thin callback/text adapters around the vendored SmartRefreshControl views.
 * The Fabric bridge owns request IDs and operation state; these classes only
 * expose lifecycle callbacks and keep the stock visuals in sync with RN text.
 */
@interface RNSmartClassicsHeader : UIRefreshClassicsHeader

@property (nonatomic, copy, nullable) RNSmartHeaderStatusChanged statusChanged;
@property (nonatomic, copy, nullable) RNSmartHeaderScrollChanged scrollChanged;
@property (nonatomic, copy) NSString *pullDownText;
@property (nonatomic, copy) NSString *releaseToRefreshText;
@property (nonatomic, copy) NSString *refreshingText;
@property (nonatomic, copy) NSString *refreshCompleteText;
@property (nonatomic, assign) BOOL showingCompletionText;
@property (nonatomic, assign) RNSmartClassicSpinnerStyle classicSpinnerStyle;

- (void)restoreDefaultText;
- (void)refreshSpinnerStyleLayout;

@end

@interface RNSmartMaterialHeader : UIRefreshMaterialHeader

@property (nonatomic, copy, nullable) RNSmartHeaderStatusChanged statusChanged;
@property (nonatomic, copy, nullable) RNSmartHeaderScrollChanged scrollChanged;

@end

@interface RNSmartClassicsFooter : UIRefreshClassicsFooter

@property (nonatomic, copy, nullable) RNSmartFooterStatusChanged statusChanged;
@property (nonatomic, copy, nullable) RNSmartFooterScrollChanged scrollChanged;
@property (nonatomic, copy) NSString *pullUpText;
@property (nonatomic, copy) NSString *releaseToLoadMoreText;
@property (nonatomic, copy) NSString *loadingMoreText;
@property (nonatomic, copy) NSString *noMoreDataText;

/** Automatic footers stay dormant until a forward drag arms them. */
@property (nonatomic, assign) BOOL automaticRequestsArmed;

- (void)beginProgrammaticLoadMore;
- (void)disarmAutomaticRequests;

@end

NS_ASSUME_NONNULL_END
