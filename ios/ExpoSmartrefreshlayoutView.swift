import ExpoModulesCore
import UIKit
import MJRefresh

/**
 * SmartRefreshLayout 的 Expo 视图包装器（iOS 实现）
 * 
 * 这是基于 MJRefresh 库实现的下拉刷新/上拉加载组件
 * 
 * 主要功能：
 * 1. 下拉刷新 (Pull to Refresh)
 * 2. 上拉加载更多 (Pull to Load More)
 * 3. 多种 Header/Footer 样式（经典样式、Material Design 样式等）
 * 4. 丰富的自定义属性（文字、颜色、大小等）
 * 5. 完整的事件回调系统
 * 
 * 事件回调：
 * - onRefresh: 触发下拉刷新时回调
 * - onLoadMore: 触发上拉加载时回调
 * - onStateChanged: 状态变化时回调（传递 RefreshState 枚举值）
 * - onHeaderMoving: Header 移动时实时回调（通过 UIScrollViewDelegate 实现）
 * - onFooterMoving: Footer 移动时实时回调（通过 UIScrollViewDelegate 实现）
 * 
 * 架构设计：
 * - 使用委托模式，将 Header 和 Footer 的属性处理委托给专门的 Handler 类
 * - 使用 StateMapper 工具类统一管理状态映射
 * - 通过 UIScrollViewDelegate 监听滚动事件，实现状态追踪和实时事件
 */
class ExpoSmartrefreshlayoutView: ExpoView, UIScrollViewDelegate {
    
    // ========== 事件分发器 ==========
    let onRefresh = EventDispatcher()           // 下拉刷新事件
    let onLoadMore = EventDispatcher()          // 上拉加载事件
    let onStateChanged = EventDispatcher()      // 状态变化事件
    let onHeaderMoving = EventDispatcher()      // Header 移动事件
    let onFooterMoving = EventDispatcher()      // Footer 移动事件
    
    // ========== 内部组件 ==========
    private var scrollView: UIScrollView?      // 内部滚动视图容器
    
    // ========== 配置属性 ==========
    private var enableRefreshValue: Bool = true
    private var enableLoadMoreValue: Bool = true
    private var enableAutoLoadMoreValue: Bool = false
    private var headerTypeValue: String = "classics"
    
    // 自定义 Header/Footer 标志
    private var hasCustomHeader: Bool = false
    private var customHeaderSet: Bool = false
    private var customHeaderView: UIView?
    
    // 触觉反馈相关
    private var enableHapticFeedback: Bool = true
    private var hasTriggeredRefreshHaptic: Bool = false
    private var hasTriggeredLoadMoreHaptic: Bool = false
    
    // 经典样式配置
    private var classicRefreshHeaderPropsValue: [String: Any]?
    private var classicLoadMoreFooterPropsValue: [String: Any]?
    
    // ========== 状态追踪 ==========
    private var currentState: String = "None"
    private var isRefreshing: Bool = false
    private var isLoadingMore: Bool = false
    
    required init(appContext: AppContext? = nil) {
        super.init(appContext: appContext)
        clipsToBounds = true
    }
    
    // ===========================================================================
    // 子视图管理
    // ===========================================================================
    
    override func didAddSubview(_ subview: UIView) {
        super.didAddSubview(subview)
        
        // 如果启用了自定义 Header 且还没设置过，第一个非ScrollView子视图就是 Header
        if hasCustomHeader && !customHeaderSet && customHeaderView == nil && !(subview is UIScrollView) {
            customHeaderView = subview
            customHeaderSet = true
            // 从视图层级中移除，稍后包装到 CustomRefreshHeader 中
            subview.removeFromSuperview()
            
            // 如果已经有 ScrollView，立即设置 Header
            if scrollView != nil {
                setupRefreshHeader()
            }
            return
        }
        
        // 查找 ScrollView 子类（FlatList、ScrollView 等）
        if let scrollView = subview as? UIScrollView {
            self.scrollView = scrollView
            scrollView.delegate = self
            setupRefreshControl()
        } else {
            // 递归查找子视图中的 ScrollView
            findScrollView(in: subview)
        }
    }
    
    /**
     * 递归查找 ScrollView
     */
    private func findScrollView(in view: UIView) {
        for subview in view.subviews {
            if let scrollView = subview as? UIScrollView {
                self.scrollView = scrollView
                scrollView.delegate = self
                setupRefreshControl()
                return
            }
            findScrollView(in: subview)
        }
    }
    
    // ===========================================================================
    // UIScrollViewDelegate - 状态追踪
    // ===========================================================================
    
    func scrollViewDidScroll(_ scrollView: UIScrollView) {
        let offsetY = scrollView.contentOffset.y
        let contentInsetTop = scrollView.contentInset.top
        let contentInsetBottom = scrollView.contentInset.bottom
        let contentHeight = scrollView.contentSize.height
        let scrollViewHeight = scrollView.bounds.height
        
        // MJRefresh 的触发距离通常是 54
        let triggerDistance: CGFloat = 54.0
        
        // ========== 处理下拉刷新状态和 onHeaderMoving 事件 ==========
        if enableRefreshValue && !isRefreshing {
            let pullDownDistance = -(offsetY + contentInsetTop)
            
            if pullDownDistance > 0 {
                // 获取 Header 高度
                let headerHeight: CGFloat = scrollView.mj_header?.mj_h ?? 54.0
                
                // 使用 MJRefresh 的 pullingPercent（这个值与 Android 的 percent 完全一致）
                let mjPercent = scrollView.mj_header?.pullingPercent ?? 0
                
                // percent 直接使用 pullingPercent（0-N 的小数，与 Android 一致）
                let percent = Double(mjPercent)
                
                // offset 使用 pullingPercent * headerHeight（与 Android 一致）
                let offset = Int(mjPercent * headerHeight)
                
                // 发送 onHeaderMoving 事件
                onHeaderMoving([
                    "isDragging": scrollView.isDragging,
                    "percent": percent,
                    "offset": offset,
                    "headerHeight": Int(headerHeight)
                ])
                
                // 触觉反馈：当下拉超过触发阈值时（percent >= 1.0），触发震动
                if enableHapticFeedback && scrollView.isDragging && percent >= 1.0 && !hasTriggeredRefreshHaptic {
                    let generator = UIImpactFeedbackGenerator(style: .light)
                    generator.impactOccurred()
                    hasTriggeredRefreshHaptic = true
                } else if percent < 1.0 {
                    hasTriggeredRefreshHaptic = false
                }
                
                // 更新状态
                if pullDownDistance < triggerDistance {
                    if currentState != "PullDownToRefresh" {
                        updateState(newState: "PullDownToRefresh")
                    }
                } else {
                    if currentState != "ReleaseToRefresh" && scrollView.isDragging {
                        updateState(newState: "ReleaseToRefresh")
                    }
                }
            } else {
                if currentState == "PullDownToRefresh" || currentState == "ReleaseToRefresh" {
                    updateState(newState: "None")
                }
            }
        }
        
        // ========== 处理上拉加载状态和 onFooterMoving 事件 ==========
        if enableLoadMoreValue && !isLoadingMore {
            let bottomOffset = offsetY + scrollViewHeight - contentHeight - contentInsetBottom
            
            if bottomOffset > 0 {
                // 获取 Footer 高度
                let footerHeight: CGFloat = scrollView.mj_footer?.mj_h ?? 54.0
                
                // 使用 MJRefresh 的 pullingPercent
                let mjPercent = scrollView.mj_footer?.pullingPercent ?? 0
                
                // percent 直接使用 pullingPercent（与 Android 一致）
                let percent = Double(mjPercent)
                
                // offset 使用 pullingPercent * footerHeight（与 Android 一致）
                let offset = Int(mjPercent * footerHeight)
                
                // 发送 onFooterMoving 事件
                onFooterMoving([
                    "isDragging": scrollView.isDragging,
                    "percent": percent,
                    "offset": offset,
                    "footerHeight": Int(footerHeight)
                ])
                
                // 触觉反馈：当上拉超过触发阈值时（percent >= 1.0），触发震动
                if enableHapticFeedback && scrollView.isDragging && percent >= 1.0 && !hasTriggeredLoadMoreHaptic {
                    let generator = UIImpactFeedbackGenerator(style: .light)
                    generator.impactOccurred()
                    hasTriggeredLoadMoreHaptic = true
                } else if percent < 1.0 {
                    hasTriggeredLoadMoreHaptic = false
                }
                
                // 更新状态
                if bottomOffset < triggerDistance {
                    if currentState != "PullUpToLoad" {
                        updateState(newState: "PullUpToLoad")
                    }
                } else {
                    if currentState != "ReleaseToLoad" && scrollView.isDragging {
                        updateState(newState: "ReleaseToLoad")
                    }
                }
            } else {
                if currentState == "PullUpToLoad" || currentState == "ReleaseToLoad" {
                    updateState(newState: "None")
                }
            }
        }
    }
    
    func scrollViewDidEndDragging(_ scrollView: UIScrollView, willDecelerate decelerate: Bool) {
        // 松手后，如果达到了触发距离，MJRefresh 会自动开始刷新
        // 这时状态会在 handleRefreshTriggered 中更新
    }
    
    // ===========================================================================
    // 刷新控件设置
    // ===========================================================================
    
    /**
     * 设置刷新控件（Header 和 Footer）
     */
    private func setupRefreshControl() {
        guard let scrollView = scrollView else { return }
        
        if enableRefreshValue {
            setupRefreshHeader()
        }
        
        if enableLoadMoreValue {
            setupRefreshFooter()
        }
    }
    
    /**
     * 设置 Header（下拉刷新）
     */
    private func setupRefreshHeader() {
        guard let scrollView = scrollView else { return }
        
        // 如果有自定义 Header 视图，使用它
        if let customView = customHeaderView {
            let header = CustomRefreshHeader(customView: customView)
            header.refreshingBlock = { [weak self] in
                self?.handleRefreshTriggered()
            }
            scrollView.mj_header = header
            return
        }
        
        if headerTypeValue.lowercased() == "material" {
            // Material Design 样式
            let header = MJRefreshMaterialHeader { [weak self] in
                self?.handleRefreshTriggered()
            }
            
            // 应用 Material 样式配置
            if let props = classicRefreshHeaderPropsValue {
                HeaderPropsHandler.applyMaterialProps(header: header, props: props)
            }
            
            scrollView.mj_header = header
        } else {
            // Classic 样式
            let header = MJRefreshNormalHeader { [weak self] in
                self?.handleRefreshTriggered()
            }
            
            // 应用经典样式配置
            if let props = classicRefreshHeaderPropsValue {
                HeaderPropsHandler.applyProps(header: header, props: props)
            }
            
            scrollView.mj_header = header
        }
    }
    
    /**
     * 设置 Footer（上拉加载）
     */
    private func setupRefreshFooter() {
        guard let scrollView = scrollView else { return }
        
        if enableAutoLoadMoreValue {
            // 自动加载更多
            let footer = MJRefreshAutoFooter { [weak self] in
                self?.handleLoadMoreTriggered()
            }
            
            // MJRefreshAutoFooter 不支持详细配置
            if let props = classicLoadMoreFooterPropsValue {
                FooterPropsHandler.applyGenericProps(footer: footer, props: props)
            }
            
            scrollView.mj_footer = footer
        } else {
            // 手动触发加载更多
            let footer = MJRefreshBackNormalFooter { [weak self] in
                self?.handleLoadMoreTriggered()
            }
            
            // 应用经典样式配置
            if let props = classicLoadMoreFooterPropsValue {
                FooterPropsHandler.applyProps(footer: footer, props: props)
            }
            
            scrollView.mj_footer = footer
        }
    }
    
    // ===========================================================================
    // 刷新回调处理
    // ===========================================================================
    
    /**
     * 处理下拉刷新触发
     */
    private func handleRefreshTriggered() {
        isRefreshing = true
        updateState(newState: "Refreshing")
        
        // 触发 JS 回调
        onRefresh([:])
        
        // 3秒后自动结束刷新（超时保护）
        DispatchQueue.main.asyncAfter(deadline: .now() + 3.0) { [weak self] in
            if self?.isRefreshing == true {
                self?.finishRefresh(success: true, delay: 0)
            }
        }
    }
    
    /**
     * 处理上拉加载触发
     */
    private func handleLoadMoreTriggered() {
        isLoadingMore = true
        updateState(newState: "Loading")
        
        // 触发 JS 回调
        onLoadMore([:])
        
        // 3秒后自动结束加载（超时保护）
        DispatchQueue.main.asyncAfter(deadline: .now() + 3.0) { [weak self] in
            if self?.isLoadingMore == true {
                self?.finishLoadMore(success: true, delay: 0, noMoreData: false)
            }
        }
    }
    
    /**
     * 更新状态并触发回调
     */
    private func updateState(newState: String) {
        let oldState = currentState
        currentState = newState
        
        if oldState != newState {
            // 使用 StateMapper 将状态字符串映射为 TypeScript 枚举值
            let refreshStateValue = StateMapper.stateStringToRefreshState(newState)
            onStateChanged([
                "state": refreshStateValue
            ])
            
            // 动态更新 Header/Footer 显示文字
            updateHeaderFooterText(forState: newState)
        }
    }
    
    /**
     * 根据当前状态动态更新 Header/Footer 的显示文字
     * 
     * 注意：对于 MJRefreshNormalHeader，我们需要确保 stateLabel 可见
     */
    private func updateHeaderFooterText(forState state: String) {
        // 处理 Header 状态
        if let header = scrollView?.mj_header as? MJRefreshNormalHeader,
           let props = classicRefreshHeaderPropsValue {
            
            // 确保 stateLabel 可见
            header.stateLabel?.isHidden = false
            
            switch state {
            case "PullDownToRefresh":
                if let text = props["REFRESH_HEADER_PULLING"] as? String {
                    header.setTitle(text, for: .idle)
                    header.setTitle(text, for: .pulling)
                }
            case "ReleaseToRefresh":
                if let text = props["REFRESH_HEADER_RELEASE"] as? String {
                    header.setTitle(text, for: .willRefresh)
                }
            case "Refreshing":
                if let text = props["REFRESH_HEADER_REFRESHING"] as? String {
                    header.setTitle(text, for: .refreshing)
                }
            case "RefreshFinish":
                if let text = props["REFRESH_HEADER_FINISH"] as? String {
                    header.setTitle(text, for: .noMoreData)
                }
            case "None":
                // 恢复到默认的 idle 文字
                if let text = props["REFRESH_HEADER_PULLING"] as? String {
                    header.setTitle(text, for: .idle)
                }
            default:
                break
            }
            
            // 强制刷新显示
            header.setNeedsLayout()
            header.layoutIfNeeded()
        }
        
        // 处理 Footer 状态
        if let footer = scrollView?.mj_footer as? MJRefreshBackNormalFooter,
           let footerProps = classicLoadMoreFooterPropsValue {
            
            switch state {
            case "PullUpToLoad":
                if let text = footerProps["REFRESH_FOOTER_PULLING"] as? String {
                    footer.stateLabel?.text = text
                }
            case "ReleaseToLoad":
                if let text = footerProps["REFRESH_FOOTER_RELEASE"] as? String {
                    footer.stateLabel?.text = text
                }
            case "Loading":
                if let text = footerProps["REFRESH_FOOTER_LOADING"] as? String {
                    footer.stateLabel?.text = text
                }
            case "LoadFinish":
                if let text = footerProps["REFRESH_FOOTER_FINISH"] as? String {
                    footer.stateLabel?.text = text
                }
            case "NoMoreData":
                if let text = footerProps["REFRESH_FOOTER_NOTHING"] as? String {
                    footer.stateLabel?.text = text
                }
            default:
                break
            }
        }
    }
    
    // ===========================================================================
    // Props 设置方法
    // ===========================================================================
    
    /**
     * 是否启用下拉刷新功能
     */
    func setEnableRefresh(_ value: Bool) {
        enableRefreshValue = value
        
        if let scrollView = scrollView {
            if value {
                if scrollView.mj_header == nil {
                    setupRefreshHeader()
                }
            } else {
                scrollView.mj_header = nil
            }
        }
    }
    
    /**
     * 是否启用上拉加载功能
     */
    func setEnableLoadMore(_ value: Bool) {
        enableLoadMoreValue = value
        
        if let scrollView = scrollView {
            if value {
                if scrollView.mj_footer == nil {
                    setupRefreshFooter()
                }
            } else {
                scrollView.mj_footer = nil
            }
        }
    }
    
    /**
     * 是否启用列表惯性滑动到底部时自动加载更多
     */
    func setEnableAutoLoadMore(_ value: Bool) {
        enableAutoLoadMoreValue = value
        
        // 重新设置 footer
        if enableLoadMoreValue {
            setupRefreshFooter()
        }
    }
    
    /**
     * Header 类型
     * - "classics": 经典样式
     * - "material": Material 样式
     */
    func setHeaderType(_ type: String) {
        headerTypeValue = type
        
        // 重新设置 header
        if enableRefreshValue {
            setupRefreshHeader()
        }
    }
    
    /**
     * 经典刷新头属性（委托给 HeaderPropsHandler）
     */
    func setClassicRefreshHeaderProps(_ props: [String: Any]) {
        classicRefreshHeaderPropsValue = props
        
        // 重新应用样式
        if let scrollView = scrollView {
            if let header = scrollView.mj_header as? MJRefreshNormalHeader {
                HeaderPropsHandler.applyProps(header: header, props: props)
            } else if let header = scrollView.mj_header as? MJRefreshMaterialHeader {
                HeaderPropsHandler.applyMaterialProps(header: header, props: props)
            }
        }
    }
    
    /**
     * 经典加载脚属性（委托给 FooterPropsHandler）
     */
    func setClassicLoadMoreFooterProps(_ props: [String: Any]) {
        classicLoadMoreFooterPropsValue = props
        
        // 重新应用样式
        if let scrollView = scrollView, let footer = scrollView.mj_footer {
            if let normalFooter = footer as? MJRefreshBackNormalFooter {
                FooterPropsHandler.applyProps(footer: normalFooter, props: props)
            } else {
                FooterPropsHandler.applyGenericProps(footer: footer, props: props)
            }
        }
    }
    
    /**
     * 设置是否启用触觉反馈
     */
    func setEnableHapticFeedback(_ value: Bool) {
        enableHapticFeedback = value
    }
    
    /**
     * 设置是否使用自定义 Header
     */
    func setHasCustomHeader(_ value: Bool) {
        let oldValue = hasCustomHeader
        hasCustomHeader = value
        
        // 如果从 true 变为 false，需要清理状态
        if oldValue && !value {
            customHeaderView = nil
            customHeaderSet = false
            
            // 重新设置默认 Header
            if let scrollView = scrollView, enableRefreshValue {
                setupRefreshHeader()
            }
            return
        }
        
        // 如果从 false 变为 true，尝试查找自定义 Header
        if !oldValue && value && !customHeaderSet {
            // 遍历子视图，找到第一个非 ScrollView 的视图
            for subview in subviews {
                if !(subview is UIScrollView) {
                    customHeaderView = subview
                    customHeaderSet = true
                    subview.removeFromSuperview()
                    
                    // 如果已经有 ScrollView，立即设置 Header
                    if scrollView != nil {
                        setupRefreshHeader()
                    }
                    break
                }
            }
        }
    }
    
    // ===========================================================================
    // 命令方法
    // ===========================================================================
    
    /**
     * 完成刷新操作
     */
    func finishRefresh(success: Bool, delay: Int) {
        let delaySeconds = Double(delay) / 1000.0
        
        DispatchQueue.main.asyncAfter(deadline: .now() + delaySeconds) { [weak self] in
            self?.scrollView?.mj_header?.endRefreshing()
            self?.isRefreshing = false
            self?.updateState(newState: success ? "RefreshFinish" : "None")
        }
    }
    
    /**
     * 完成加载更多操作
     */
    func finishLoadMore(success: Bool, delay: Int, noMoreData: Bool) {
        let delaySeconds = Double(delay) / 1000.0
        
        DispatchQueue.main.asyncAfter(deadline: .now() + delaySeconds) { [weak self] in
            if noMoreData {
                self?.scrollView?.mj_footer?.endRefreshingWithNoMoreData()
            } else {
                self?.scrollView?.mj_footer?.endRefreshing()
            }
            self?.isLoadingMore = false
            self?.updateState(newState: success ? "LoadFinish" : "None")
        }
    }
    
    /**
     * 自动刷新
     */
    func autoRefresh(delay: Int) {
        let delaySeconds = Double(delay) / 1000.0
        
        DispatchQueue.main.asyncAfter(deadline: .now() + delaySeconds) { [weak self] in
            self?.scrollView?.mj_header?.beginRefreshing()
        }
    }
    
    /**
     * 自动加载更多
     */
    func autoLoadMore(delay: Int) {
        let delaySeconds = Double(delay) / 1000.0
        
        DispatchQueue.main.asyncAfter(deadline: .now() + delaySeconds) { [weak self] in
            self?.scrollView?.mj_footer?.beginRefreshing()
        }
    }
    
    /**
     * 设置是否没有更多数据可加载
     */
    func setNoMoreData(noMore: Bool) {
        if noMore {
            scrollView?.mj_footer?.endRefreshingWithNoMoreData()
        } else {
            scrollView?.mj_footer?.resetNoMoreData()
        }
    }
    
    // ===========================================================================
    // 布局
    // ===========================================================================
    
    override func layoutSubviews() {
        super.layoutSubviews()
        
        // 确保子视图填充整个容器
        for subview in subviews {
            subview.frame = bounds
        }
    }
}

// MARK: - UIColor 扩展（支持十六进制颜色）

extension UIColor {
    convenience init?(hexString: String) {
        var hexSanitized = hexString.trimmingCharacters(in: .whitespacesAndNewlines)
        hexSanitized = hexSanitized.replacingOccurrences(of: "#", with: "")
        
        var rgb: UInt64 = 0
        
        guard Scanner(string: hexSanitized).scanHexInt64(&rgb) else { return nil }
        
        let length = hexSanitized.count
        let r, g, b, a: CGFloat
        
        if length == 6 {
            r = CGFloat((rgb & 0xFF0000) >> 16) / 255.0
            g = CGFloat((rgb & 0x00FF00) >> 8) / 255.0
            b = CGFloat(rgb & 0x0000FF) / 255.0
            a = 1.0
        } else if length == 8 {
            r = CGFloat((rgb & 0xFF000000) >> 24) / 255.0
            g = CGFloat((rgb & 0x00FF0000) >> 16) / 255.0
            b = CGFloat((rgb & 0x0000FF00) >> 8) / 255.0
            a = CGFloat(rgb & 0x000000FF) / 255.0
        } else {
            return nil
        }
        
        self.init(red: r, green: g, blue: b, alpha: a)
    }
}
