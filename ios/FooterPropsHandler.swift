import UIKit
import MJRefresh

/**
 * Footer 属性处理器
 * 
 * 负责处理 MJRefresh Footer 的所有属性设置
 * 包括：
 * - 文字内容（上拉提示、释放提示、加载中、没有更多数据等各种状态的文字）
 * - 颜色设置（强调色）
 * - 字体大小
 * - 箭头/进度图标的显示控制
 * - 其他配置
 * 
 * 注意：
 * - MJRefreshAutoFooter 没有 stateLabel，不支持自定义文字和样式
 * - MJRefreshBackNormalFooter 支持完整的文字和样式自定义
 */
class FooterPropsHandler {
    
    /**
     * 应用 Footer 属性到 MJRefreshBackNormalFooter
     * 
     * @param footer MJRefreshBackNormalFooter 实例
     * @param props 属性映射表
     */
    static func applyProps(footer: MJRefreshBackNormalFooter, props: [String: Any]) {
        // 设置文字内容
        setTextContent(footer: footer, props: props)
        
        // 设置颜色
        setColors(footer: footer, props: props)
        
        // 设置字体大小
        setTextSize(footer: footer, props: props)
        
        // 设置箭头显示
        setArrowVisibility(footer: footer, props: props)
        
        // 设置其他属性
        setOtherProps(footer: footer, props: props)
    }
    
    /**
     * 应用 Footer 属性到通用 MJRefreshFooter
     * 
     * 用于 MJRefreshAutoFooter 等不支持详细配置的 Footer
     * 
     * @param footer MJRefreshFooter 实例
     * @param props 属性映射表
     */
    static func applyGenericProps(footer: MJRefreshFooter, props: [String: Any]) {
        // MJRefreshAutoFooter 的样式配置较为有限（静默处理）
        _ = props["REFRESH_FOOTER_PULLING"]
        _ = props["REFRESH_FOOTER_LOADING"]
        _ = props["REFRESH_FOOTER_NOTHING"]
    }
    
    // MARK: - 私有方法
    
    /**
     * 设置文字内容
     * 
     * 设置 Footer 在不同状态下显示的文字：
     * - REFRESH_FOOTER_PULLING: 上拉过程中的提示文字
     * - REFRESH_FOOTER_RELEASE: 释放即可加载的提示文字
     * - REFRESH_FOOTER_LOADING: 加载中的提示文字
     * - REFRESH_FOOTER_NOTHING: 没有更多数据的提示文字
     * 
     * @param footer MJRefreshBackNormalFooter 实例
     * @param props 属性映射表
     */
    private static func setTextContent(footer: MJRefreshBackNormalFooter, props: [String: Any]) {
        if let pullingText = props["REFRESH_FOOTER_PULLING"] as? String {
            footer.setTitle(pullingText, for: .idle)
            footer.setTitle(pullingText, for: .pulling)
        }
        
        if let releaseText = props["REFRESH_FOOTER_RELEASE"] as? String {
            footer.setTitle(releaseText, for: .willRefresh)
        }
        
        if let loadingText = props["REFRESH_FOOTER_LOADING"] as? String {
            footer.setTitle(loadingText, for: .refreshing)
        }
        
        if let nothingText = props["REFRESH_FOOTER_NOTHING"] as? String {
            footer.setTitle(nothingText, for: .noMoreData)
        }
        
        // iOS MJRefresh 不支持的状态文字（静默处理）
        _ = props["REFRESH_FOOTER_REFRESHING"]
        _ = props["REFRESH_FOOTER_FINISH"]
        _ = props["REFRESH_FOOTER_FAILED"]
    }
    
    /**
     * 设置颜色
     * 
     * - footerAccentColor: 强调色（文字、箭头、进度条的颜色）
     * - footerPrimaryColor: 主色调（背景色，iOS MJRefresh 不支持）
     * 
     * @param footer MJRefreshBackNormalFooter 实例
     * @param props 属性映射表
     */
    private static func setColors(footer: MJRefreshBackNormalFooter, props: [String: Any]) {
        if let accentColor = props["footerAccentColor"] as? String,
           let color = UIColor(hexString: accentColor) {
            footer.stateLabel?.textColor = color
            // iOS 的 MJRefresh 箭头颜色与文字颜色绑定
            footer.arrowView?.tintColor = color
        }
        
        // iOS MJRefresh 不支持背景色设置
        _ = props["footerPrimaryColor"]
    }
    
    /**
     * 设置字体大小
     * 
     * - footerTitleTextSize: 标题文字大小（单位：pt）
     * 
     * @param footer MJRefreshBackNormalFooter 实例
     * @param props 属性映射表
     */
    private static func setTextSize(footer: MJRefreshBackNormalFooter, props: [String: Any]) {
        if let titleSize = props["footerTitleTextSize"] as? CGFloat {
            footer.stateLabel?.font = UIFont.systemFont(ofSize: titleSize)
        }
    }
    
    /**
     * 设置箭头显示
     * 
     * 支持两个属性（优先使用第一个）：
     * - footerDrawableArrowSize: 专门设置箭头大小
     * - footerDrawableSize: 通用图标大小设置
     * 
     * 特殊值：
     * - 0: 隐藏箭头
     * - 其他值: iOS MJRefresh 不支持动态调整箭头大小，仅记录日志
     * 
     * @param footer MJRefreshBackNormalFooter 实例
     * @param props 属性映射表
     */
    private static func setArrowVisibility(footer: MJRefreshBackNormalFooter, props: [String: Any]) {
        // 箭头大小设置（优先使用 footerDrawableArrowSize，回退到 footerDrawableSize）
        let arrowSize = props["footerDrawableArrowSize"] as? CGFloat ?? props["footerDrawableSize"] as? CGFloat
        
        if let size = arrowSize {
            // 如果设置为 0，隐藏箭头
            if size == 0 {
                footer.arrowView?.isHidden = true
            } else {
                footer.arrowView?.isHidden = false
                // iOS 的 MJRefresh 箭头大小是固定的，无法直接修改
            }
        }
        
        // iOS MJRefresh 不支持的属性（静默处理）
        _ = props["footerDrawableProgressSize"]
        _ = props["footerDrawableMarginRight"]
    }
    
    /**
     * 设置其他属性
     * 
     * - footerFinishDuration: 加载完成后停留时长（iOS MJRefresh 不支持）
     * 
     * @param footer MJRefreshBackNormalFooter 实例
     * @param props 属性映射表
     */
    private static func setOtherProps(footer: MJRefreshBackNormalFooter, props: [String: Any]) {
        // iOS MJRefresh 不支持的属性（静默处理）
        _ = props["footerFinishDuration"]
    }
}
