import UIKit
import MJRefresh

/**
 * Header 属性处理器
 * 
 * 负责处理 MJRefresh Header 的所有属性设置
 * 包括：
 * - 文字内容（下拉提示、释放提示、刷新中等各种状态的文字）
 * - 颜色设置（强调色、主色调）
 * - 字体大小（标题、时间文字大小）
 * - 箭头/进度图标的显示控制
 * - 其他配置（是否显示时间、最后更新时间前缀等）
 * 
 * 注意：iOS 的 MJRefresh 对某些属性的支持有限，如箭头大小、图标间距等
 */
class HeaderPropsHandler {
    
    /**
     * 应用 Header 属性到 MJRefreshNormalHeader
     * 
     * @param header MJRefreshNormalHeader 实例
     * @param props 属性映射表
     */
    static func applyProps(header: MJRefreshNormalHeader, props: [String: Any]) {
        // 设置文字内容
        setTextContent(header: header, props: props)
        
        // 设置颜色
        setColors(header: header, props: props)
        
        // 设置字体大小
        setTextSize(header: header, props: props)
        
        // 设置其他属性
        setOtherProps(header: header, props: props)
        
        // 设置箭头显示
        setArrowVisibility(header: header, props: props)
    }
    
    /**
     * 应用 Material Header 属性
     * 
     * @param header MJRefreshMaterialHeader 实例
     * @param props 属性映射表
     */
    static func applyMaterialProps(header: MJRefreshMaterialHeader, props: [String: Any]) {
        // 设置颜色
        if let accentColor = props["headerAccentColor"] as? String,
           let color = UIColor(hexString: accentColor) {
            header.accentColor = color
        }
        
        if let primaryColor = props["headerPrimaryColor"] as? String,
           let color = UIColor(hexString: primaryColor) {
            header.headerBackgroundColor = color
        }
    }
    
    // MARK: - 私有方法
    
    /**
     * 设置文字内容
     * 
     * 设置 Header 在不同状态下显示的文字：
     * - REFRESH_HEADER_PULLING: 下拉过程中的提示文字
     * - REFRESH_HEADER_RELEASE: 释放即可刷新的提示文字
     * - REFRESH_HEADER_REFRESHING: 刷新中的提示文字
     * - REFRESH_HEADER_FINISH: 刷新完成的提示文字
     * 
     * @param header MJRefreshNormalHeader 实例
     * @param props 属性映射表
     */
    private static func setTextContent(header: MJRefreshNormalHeader, props: [String: Any]) {
        if let pullingText = props["REFRESH_HEADER_PULLING"] as? String {
            header.setTitle(pullingText, for: .idle)
            header.setTitle(pullingText, for: .pulling)
        }
        
        if let releaseText = props["REFRESH_HEADER_RELEASE"] as? String {
            header.setTitle(releaseText, for: .willRefresh)
        }
        
        if let refreshingText = props["REFRESH_HEADER_REFRESHING"] as? String {
            header.setTitle(refreshingText, for: .refreshing)
        }
        
        if let finishText = props["REFRESH_HEADER_FINISH"] as? String {
            header.setTitle(finishText, for: .noMoreData)
        }
        
        // iOS MJRefresh 不支持的状态文字（静默处理）
        _ = props["REFRESH_HEADER_LOADING"]
        _ = props["REFRESH_HEADER_FAILED"]
        _ = props["REFRESH_HEADER_SECONDARY"]
    }
    
    /**
     * 设置颜色
     * 
     * - headerAccentColor: 强调色（文字、箭头、进度条的颜色）
     * - headerPrimaryColor: 主色调（背景色，iOS MJRefresh 不支持）
     * 
     * @param header MJRefreshNormalHeader 实例
     * @param props 属性映射表
     */
    private static func setColors(header: MJRefreshNormalHeader, props: [String: Any]) {
        if let accentColor = props["headerAccentColor"] as? String,
           let color = UIColor(hexString: accentColor) {
            header.stateLabel?.textColor = color
            header.lastUpdatedTimeLabel?.textColor = color
            // iOS 的 MJRefresh 箭头颜色与文字颜色绑定
            header.arrowView?.tintColor = color
            // 设置菊花（loading indicator）的颜色
            if let loadingView = header.loadingView as? UIActivityIndicatorView {
                loadingView.color = color
            }
        }
        
        // iOS MJRefresh 不支持背景色设置
        _ = props["headerPrimaryColor"]
    }
    
    /**
     * 设置字体大小
     * 
     * - headerTitleTextSize: 标题文字大小（单位：pt）
     * - headerTimeTextSize: 时间文字大小（单位：pt）
     * 
     * @param header MJRefreshNormalHeader 实例
     * @param props 属性映射表
     */
    private static func setTextSize(header: MJRefreshNormalHeader, props: [String: Any]) {
        if let titleSize = props["headerTitleTextSize"] as? CGFloat {
            header.stateLabel?.font = UIFont.systemFont(ofSize: titleSize)
        }
        
        if let timeSize = props["headerTimeTextSize"] as? CGFloat {
            header.lastUpdatedTimeLabel?.font = UIFont.systemFont(ofSize: timeSize)
        }
    }
    
    /**
     * 设置其他属性
     * 
     * - headerShowTime: 是否显示最后更新时间
     * - headerLastUpdateText: 自定义最后更新时间的前缀文字
     * - headerFinishDuration: 刷新完成后停留时长（iOS MJRefresh 不支持）
     * 
     * @param header MJRefreshNormalHeader 实例
     * @param props 属性映射表
     */
    private static func setOtherProps(header: MJRefreshNormalHeader, props: [String: Any]) {
        // 是否显示时间
        if let showTime = props["headerShowTime"] as? Bool {
            header.lastUpdatedTimeLabel?.isHidden = !showTime
        }
        
        // iOS MJRefresh 不支持的属性（静默处理）
        _ = props["headerLastUpdateText"]
        _ = props["headerFinishDuration"]
    }
    
    /**
     * 设置箭头显示
     * 
     * 支持两个属性（优先使用第一个）：
     * - headerDrawableArrowSize: 专门设置箭头大小
     * - headerDrawableSize: 通用图标大小设置
     * 
     * 特殊值：
     * - 0: 隐藏箭头
     * - 其他值: iOS MJRefresh 不支持动态调整箭头大小，仅记录日志
     * 
     * @param header MJRefreshNormalHeader 实例
     * @param props 属性映射表
     */
    private static func setArrowVisibility(header: MJRefreshNormalHeader, props: [String: Any]) {
        // 箭头大小设置（优先使用 headerDrawableArrowSize，回退到 headerDrawableSize）
        let arrowSize = props["headerDrawableArrowSize"] as? CGFloat ?? props["headerDrawableSize"] as? CGFloat
        
        if let size = arrowSize {
            // 如果设置为 0，隐藏箭头
            if size == 0 {
                header.arrowView?.isHidden = true
            } else {
                header.arrowView?.isHidden = false
                // iOS 的 MJRefresh 箭头大小是固定的，无法直接修改
            }
        }
        
        // iOS MJRefresh 不支持的属性（静默处理）
        _ = props["headerDrawableProgressSize"]
        _ = props["headerDrawableMarginRight"]
    }
}
