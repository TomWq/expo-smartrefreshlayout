#import <React/RCTViewComponentView.h>

NS_ASSUME_NONNULL_BEGIN

/**
 * SmartRefreshLayout 的 Fabric 原生视图入口。
 *
 * 该视图本身不负责滚动内容，而是在 Fabric 挂载的唯一子节点中找到真正的
 * UIScrollView，再把刷新 Header/Footer 绑定到该滚动容器。
 */
@interface ExpoSmartRefreshLayoutView : RCTViewComponentView
@end

NS_ASSUME_NONNULL_END
