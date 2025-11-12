import UIKit
import MJRefresh

/**
 * 自定义 RefreshHeader 包装器
 * 
 * 将 React Native 视图包装成 MJRefresh 可用的 RefreshHeader
 */
class CustomRefreshHeader: MJRefreshHeader {
    
    private var customView: UIView
    private var customViewHeight: CGFloat = 100.0
    
    init(customView: UIView) {
        self.customView = customView
        super.init(frame: .zero)
        
        // 从自定义视图获取高度
        if customView.frame.height > 0 {
            customViewHeight = customView.frame.height
        }
        
        // 设置 Header 高度
        mj_h = customViewHeight
        
        // 添加自定义视图
        addSubview(customView)
        
        // 设置背景色为透明
        backgroundColor = .clear
        customView.backgroundColor = customView.backgroundColor ?? .clear
        
        // 允许交互（React Native 组件需要）
        customView.isUserInteractionEnabled = true
        isUserInteractionEnabled = true
        
        print("[SmartRefresh iOS] CustomRefreshHeader 初始化，高度: \(customViewHeight)")
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    override func prepare() {
        super.prepare()
        backgroundColor = .clear
        print("[SmartRefresh iOS] CustomRefreshHeader prepare")
    }
    
    override func placeSubviews() {
        super.placeSubviews()
        
        // 布局自定义视图 - 填充整个 Header
        customView.frame = CGRect(
            x: 0,
            y: 0,
            width: bounds.width,
            height: customViewHeight
        )
        
        print("[SmartRefresh iOS] CustomRefreshHeader 布局: \(customView.frame)")
    }
    
    override var pullingPercent: CGFloat {
        didSet {
            // 可以根据下拉进度更新视图
            // 这里保持简单，不做额外处理
        }
    }
    
    override var state: MJRefreshState {
        didSet {
            switch state {
            case .idle:
                print("[SmartRefresh iOS] Header 状态: idle")
            case .pulling:
                print("[SmartRefresh iOS] Header 状态: pulling")
            case .refreshing:
                print("[SmartRefresh iOS] Header 状态: refreshing")
            case .willRefresh:
                print("[SmartRefresh iOS] Header 状态: willRefresh")
            case .noMoreData:
                print("[SmartRefresh iOS] Header 状态: noMoreData")
            @unknown default:
                break
            }
        }
    }
}
