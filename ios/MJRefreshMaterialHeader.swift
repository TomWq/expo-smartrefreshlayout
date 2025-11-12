import UIKit
import MJRefresh

/**
 * Material Design 风格的刷新 Header
 * 完全模仿 Android Material Design 的原生效果
 */
class MJRefreshMaterialHeader: MJRefreshHeader {
    
    // MARK: - 属性
    
    /// 圆形进度指示器
    private let circularProgressView = MaterialCircularProgressView()
    
    /// 进度指示器的大小（包含背景圆形）
    private let indicatorSize: CGFloat = 40
    
    /// 主题颜色（默认 Material Blue）
    var accentColor: UIColor = UIColor(red: 0.26, green: 0.52, blue: 0.96, alpha: 1.0) {
        didSet {
            circularProgressView.accentColor = accentColor
        }
    }
    
    /// 背景颜色
    var headerBackgroundColor: UIColor = .clear {
        didSet {
            backgroundColor = headerBackgroundColor
        }
    }
    
    // MARK: - 初始化
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupViews()
    }
    
    required init?(coder: NSCoder) {
        super.init(coder: coder)
        setupViews()
    }
    
    private func setupViews() {
        backgroundColor = .clear
        
        // 添加圆形进度指示器
        addSubview(circularProgressView)
        circularProgressView.accentColor = accentColor
    }
    
    // MARK: - 布局
    
    override func layoutSubviews() {
        super.layoutSubviews()
        
        // 居中显示进度指示器
        let x = (bounds.width - indicatorSize) / 2
        let y = (bounds.height - indicatorSize) / 2
        circularProgressView.frame = CGRect(x: x, y: y, width: indicatorSize, height: indicatorSize)
    }
    
    // MARK: - 状态处理
    
    override var state: MJRefreshState {
        didSet {
            guard state != oldValue else { return }
            
            switch state {
            case .idle:
                // 闲置状态
                circularProgressView.stopAnimating()
                circularProgressView.progress = 0
                
            case .pulling:
                // 下拉中
                circularProgressView.stopAnimating()
                // 进度指示器不显示（或根据下拉百分比显示）
                
            case .refreshing:
                // 刷新中
                circularProgressView.startAnimating()
                
            case .willRefresh:
                // 即将刷新
                circularProgressView.stopAnimating()
                
            case .noMoreData:
                // 没有更多数据
                circularProgressView.stopAnimating()
                
            @unknown default:
                break
            }
        }
    }
    
    override var pullingPercent: CGFloat {
        didSet {
            // 根据下拉百分比更新进度（非刷新状态时）
            if state != .refreshing && state != .willRefresh {
                let progress = min(pullingPercent, 1.0)
                circularProgressView.progress = progress
            }
        }
    }
    
    // MARK: - 准备
    
    override func prepare() {
        super.prepare()
        mj_h = 60 // 设置 Header 高度
    }
}

// MARK: - Material 圆形进度视图（完全模仿 Android 原生）

private class MaterialCircularProgressView: UIView {
    
    // MARK: - 属性
    
    /// 进度值（0.0 - 1.0）
    var progress: CGFloat = 0 {
        didSet {
            updateArcDisplay()
        }
    }
    
    /// 主题颜色（单色，符合 Android Material Design）
    var accentColor: UIColor = UIColor(red: 0.26, green: 0.52, blue: 0.96, alpha: 1.0) {
        didSet {
            arcLayer.strokeColor = accentColor.cgColor
            updateArcDisplay()
        }
    }
    
    /// 是否正在动画
    private var isAnimating = false
    
    /// 动画定时器
    private var displayLink: CADisplayLink?
    
    /// 当前旋转角度
    private var rotationAngle: CGFloat = 0
    
    /// 圆弧起始角度
    private var startAngle: CGFloat = 0
    
    /// 圆弧结束角度
    private var endAngle: CGFloat = 0
    
    /// 动画阶段（0: 扩展, 1: 收缩）
    private var animationPhase: Int = 0
    
    /// 圆弧线宽（Android 原生）
    private let lineWidth: CGFloat = 3.0
    
    /// 圆形背景层
    private let backgroundCircleLayer = CAShapeLayer()
    
    /// 圆弧进度层
    private let arcLayer = CAShapeLayer()
    
    // MARK: - 初始化
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupLayers()
    }
    
    required init?(coder: NSCoder) {
        super.init(coder: coder)
        setupLayers()
    }
    
    private func setupLayers() {
        backgroundColor = .clear
        
        // 设置白色圆形背景 + 阴影（Android Material Design 特征）
        backgroundCircleLayer.fillColor = UIColor.white.cgColor
        backgroundCircleLayer.shadowColor = UIColor.black.cgColor
        backgroundCircleLayer.shadowOffset = CGSize(width: 0, height: 2)
        backgroundCircleLayer.shadowOpacity = 0.2
        backgroundCircleLayer.shadowRadius = 4
        layer.addSublayer(backgroundCircleLayer)
        
        // 设置圆弧层（在背景层之上）
        arcLayer.fillColor = UIColor.clear.cgColor
        arcLayer.strokeColor = accentColor.cgColor
        arcLayer.lineWidth = lineWidth
        arcLayer.lineCap = .round
        layer.addSublayer(arcLayer)
    }
    
    override func layoutSubviews() {
        super.layoutSubviews()
        
        // 更新圆形背景路径
        let circlePath = UIBezierPath(ovalIn: bounds)
        backgroundCircleLayer.path = circlePath.cgPath
        backgroundCircleLayer.frame = bounds
        
        // 更新圆弧层位置
        arcLayer.frame = bounds
    }
    
    // MARK: - 更新圆弧显示
    
    private func updateArcDisplay() {
        let center = CGPoint(x: bounds.width / 2, y: bounds.height / 2)
        let radius = min(bounds.width, bounds.height) / 2 - lineWidth / 2 - 4
        
        if isAnimating {
            // 动画状态：绘制旋转的圆弧
            let actualStartAngle = startAngle + rotationAngle - .pi / 2
            let actualEndAngle = endAngle + rotationAngle - .pi / 2
            
            let path = UIBezierPath(
                arcCenter: center,
                radius: radius,
                startAngle: actualStartAngle,
                endAngle: actualEndAngle,
                clockwise: true
            )
            
            arcLayer.path = path.cgPath
            arcLayer.strokeColor = accentColor.cgColor
            
        } else if progress > 0 {
            // 下拉状态：根据进度绘制静态圆弧
            let startAngle: CGFloat = -.pi / 2
            let endAngle = startAngle + (.pi * 2 * progress)
            
            let path = UIBezierPath(
                arcCenter: center,
                radius: radius,
                startAngle: startAngle,
                endAngle: endAngle,
                clockwise: true
            )
            
            arcLayer.path = path.cgPath
            arcLayer.strokeColor = accentColor.cgColor
            
        } else {
            // 没有进度时清空路径
            arcLayer.path = nil
        }
    }
    
    // MARK: - 动画控制
    
    func startAnimating() {
        guard !isAnimating else { return }
        
        isAnimating = true
        rotationAngle = 0
        startAngle = 0
        endAngle = 0
        animationPhase = 0
        
        // 使用 CADisplayLink 实现流畅动画
        displayLink = CADisplayLink(target: self, selector: #selector(updateAnimation))
        displayLink?.add(to: .main, forMode: .common)
    }
    
    func stopAnimating() {
        guard isAnimating else { return }
        
        isAnimating = false
        displayLink?.invalidate()
        displayLink = nil
        progress = 0
        arcLayer.path = nil
    }
    
    @objc private func updateAnimation() {
        // Android Material Design 的标准动画：
        // 圆弧一边旋转，一边做扩展-收缩的变化
        
        // 整体旋转速度
        let rotationSpeed: CGFloat = 0.05
        rotationAngle += rotationSpeed
        
        // 圆弧的扩展收缩动画
        let arcSpeed: CGFloat = 0.035
        
        if animationPhase == 0 {
            // 扩展阶段：尾部追上头部
            endAngle += arcSpeed * 2.2
            startAngle += arcSpeed * 0.5
            
            // 当圆弧长度达到约 300 度时切换到收缩阶段
            if endAngle - startAngle >= .pi * 1.67 {
                animationPhase = 1
            }
        } else {
            // 收缩阶段：头部追上尾部
            startAngle += arcSpeed * 2.2
            endAngle += arcSpeed * 0.5
            
            // 当圆弧收缩到很小时，重新开始扩展
            if endAngle - startAngle <= 0.15 {
                animationPhase = 0
                startAngle = endAngle
            }
        }
        
        updateArcDisplay()
    }
    
    deinit {
        displayLink?.invalidate()
    }
}
