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
 * Android ClassicsHeader 的 spinnerStyle 在 UIScrollView 控件中的几何映射：
 * Translate 跟随被下拉的内容移动；Scale 只按已经露出的距离增长；
 * FixedBehind 固定在内容后方，由内容离开顶部后逐渐显露。
 */
typedef NS_ENUM(NSInteger, RNSmartClassicSpinnerStyle) {
  RNSmartClassicSpinnerStyleScale,
  RNSmartClassicSpinnerStyleTranslate,
  RNSmartClassicSpinnerStyleFixedBehind,
};

/**
 * 对内置 SmartRefreshControl 视图的轻量回调/文案适配层。
 * requestId 和请求状态由 Fabric 桥接视图管理；这里仅透传生命周期回调，
 * 并把原生视觉状态与 React Native 传入的文案保持同步。
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

/** 自动加载 Footer 默认锁定，只有用户向上拖动后才允许发起请求。 */
@property (nonatomic, assign) BOOL automaticRequestsArmed;

- (void)beginProgrammaticLoadMore;
- (void)disarmAutomaticRequests;

@end

NS_ASSUME_NONNULL_END
