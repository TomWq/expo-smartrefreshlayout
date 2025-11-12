package expo.modules.smartrefreshlayout

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import expo.modules.kotlin.AppContext
import expo.modules.kotlin.viewevent.EventDispatcher
import expo.modules.kotlin.views.ExpoView
import com.scwang.smart.refresh.layout.SmartRefreshLayout
import com.scwang.smart.refresh.layout.api.RefreshFooter
import com.scwang.smart.refresh.layout.api.RefreshHeader
import com.scwang.smart.refresh.header.ClassicsHeader
import com.scwang.smart.refresh.header.MaterialHeader
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.layout.constant.SpinnerStyle
import com.scwang.smart.refresh.layout.listener.OnMultiListener
import com.scwang.smart.refresh.layout.constant.RefreshState as NativeRefreshState

/**
 * Expo view wrapper for SmartRefreshLayout (based on expo.modules.kotlin views pattern).
 * Events are dispatched via EventDispatcher and can be listened to in JS as props:
 * onRefresh, onLoadMore, onStateChanged, onHeaderMoving, onFooterMoving
 */
class ExpoSmartrefreshlayoutView(context: Context, appContext: AppContext) : ExpoView(context, appContext) {

    private val mainHandler = Handler(Looper.getMainLooper())

    // Events (JS 端以 onXxx={...} 监听)
    private val onRefresh by EventDispatcher()
    private val onLoadMore by EventDispatcher()
    private val onStateChanged by EventDispatcher()
    private val onHeaderMoving by EventDispatcher()
    private val onFooterMoving by EventDispatcher()

    // 属性处理器
    private lateinit var headerPropsHandler: HeaderPropsHandler
    private lateinit var footerPropsHandler: FooterPropsHandler

    // 内部 SmartRefreshLayout 实例
    internal val srl: SmartRefreshLayout = SmartRefreshLayout(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT
        )
        
        // 默认 header/footer
        setRefreshHeader(ClassicsHeader(context).apply { 
            setSpinnerStyle(SpinnerStyle.Translate)
        })
        setRefreshFooter(ClassicsFooter(context))
        
        // ========== 设置合理的默认值 ==========
        setDragRate(0.5f)  // 阻尼系数：显示高度/手指滑动距离（官方默认 0.5）
        setEnableRefresh(true)
        setEnableLoadMore(true)
        setEnablePureScrollMode(false)
        setEnableScrollContentWhenRefreshed(true)
        setEnableScrollContentWhenLoaded(true)
        setEnableOverScrollDrag(true)
        setEnableOverScrollBounce(true)
        setEnableNestedScroll(true)
        setEnableAutoLoadMore(false)
        setEnableHeaderTranslationContent(true)
        setEnableFooterTranslationContent(true)
        setEnableLoadMoreWhenContentNotFull(false)
        setHeaderMaxDragRate(2.0f)
        setFooterMaxDragRate(2.0f)
        setHeaderTriggerRate(1.0f)
        setFooterTriggerRate(1.0f)
        setReboundDuration(300)
        
        android.util.Log.d("SmartRefresh", "默认配置已应用")
    }

    init {
        // 初始化属性处理器
        headerPropsHandler = HeaderPropsHandler(srl, resources)
        footerPropsHandler = FooterPropsHandler(srl, resources)
        
        // 把 srl 添加为子 view
        addView(srl)
        
        // 安装事件监听器
        installListeners()
        
        android.util.Log.d("SmartRefresh", "ExpoSmartrefreshlayoutView 初始化完成")
    }

    // 自定义 Header/Footer 标志
    private var hasCustomHeader = false
    private var hasCustomFooter = false
    private var customHeaderSet = false
    private var customFooterSet = false
    
    // -----------------------
    // View 管理
    // -----------------------
    
    override fun addView(child: View?) {
        android.util.Log.d("SmartRefresh", "addView: ${child?.javaClass?.simpleName}")
        if (child == null) return
        
        if (child == srl) {
            super.addView(child, ViewGroup.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            ))
            android.util.Log.d("SmartRefresh", "SmartRefreshLayout 已添加到 ExpoView")
        } else {
            // 如果启用了自定义 Header 且还没设置过，第一个子视图就是 Header
            if (hasCustomHeader && !customHeaderSet) {
                android.util.Log.d("SmartRefresh", "检测到自定义 Header")
                setCustomHeader(child)
                customHeaderSet = true
                return
            }
            
            // 普通内容视图（FlatList）
            srl.addView(child, ViewGroup.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            ))
            android.util.Log.d("SmartRefresh", "子组件已添加到 SmartRefreshLayout, 子组件数量: ${srl.childCount}")
        }
    }

    override fun addView(child: View?, index: Int) {
        android.util.Log.d("SmartRefresh", "addView with index: ${child?.javaClass?.simpleName}, index=$index, hasCustomHeader=$hasCustomHeader, customHeaderSet=$customHeaderSet, hasCustomFooter=$hasCustomFooter, customFooterSet=$customFooterSet")
        if (child == null) return
        
        if (child == srl) {
            super.addView(child, index)
        } else {
            // 如果启用了自定义 Header 且还没设置过，第一个子视图就是 Header
            if (hasCustomHeader && !customHeaderSet) {
                android.util.Log.d("SmartRefresh", "检测到自定义 Header（通过 addView with index）")
                setCustomHeader(child)
                customHeaderSet = true
                return
            }
            
            // 如果启用了自定义 Footer 且还没设置过
            // 由于无法提前知道哪个是最后一个子视图，我们需要在布局时处理
            // 这里先正常添加
            srl.addView(child, index)
            android.util.Log.d("SmartRefresh", "子组件已添加到 SmartRefreshLayout (带index), 子组件数量: ${srl.childCount}")
        }
    }

    override fun addView(child: View?, params: ViewGroup.LayoutParams?) {
        android.util.Log.d("SmartRefresh", "addView with params: ${child?.javaClass?.simpleName}")
        if (child == null) return
        
        if (child == srl) {
            super.addView(child, params)
        } else {
            srl.addView(child, params)
            android.util.Log.d("SmartRefresh", "子组件已添加到 SmartRefreshLayout (带params), 子组件数量: ${srl.childCount}")
        }
    }

    override fun removeView(child: View?) {
        if (child == null) return
        if (child == srl) {
            super.removeView(child)
        } else {
            srl.removeView(child)
        }
    }

    override fun removeViewAt(index: Int) {
        if (index >= 0 && index < srl.childCount) {
            srl.removeViewAt(index)
        }
    }

    // -----------------------
    // 事件监听
    // -----------------------
    
    private fun installListeners() {
        srl.setOnMultiListener(object : OnMultiListener {
            override fun onRefresh(refreshLayout: com.scwang.smart.refresh.layout.api.RefreshLayout) {
                android.util.Log.d("SmartRefresh", "onRefresh 触发")
                onRefresh(mapOf())
                
                mainHandler.postDelayed({
                    if (srl.state == NativeRefreshState.Refreshing) {
                        android.util.Log.d("SmartRefresh", "自动结束刷新")
                        srl.finishRefresh(true)
                    }
                }, 3000)
            }

            override fun onLoadMore(refreshLayout: com.scwang.smart.refresh.layout.api.RefreshLayout) {
                android.util.Log.d("SmartRefresh", "onLoadMore 触发")
                onLoadMore(mapOf())
                
                mainHandler.postDelayed({
                    if (srl.state == NativeRefreshState.Loading) {
                        android.util.Log.d("SmartRefresh", "自动结束加载")
                        srl.finishLoadMore(true)
                    }
                }, 3000)
            }

            override fun onHeaderMoving(
                header: RefreshHeader?,
                isDragging: Boolean,
                percent: Float,
                offset: Int,
                headerHeight: Int,
                maxDragHeight: Int
            ) {
                // 将物理像素转换回逻辑像素（dp），与 iOS 保持一致
                val density = resources.displayMetrics.density
                val offsetDp = (offset / density).toInt()
                val headerHeightDp = (headerHeight / density).toInt()
                
                onHeaderMoving(
                    mapOf(
                        "isDragging" to isDragging,
                        "percent" to percent.toDouble(),
                        "offset" to offsetDp,           // 转换为 dp（逻辑像素）
                        "headerHeight" to headerHeightDp  // 转换为 dp（逻辑像素）
                    )
                )
            }

            override fun onFooterMoving(
                footer: RefreshFooter?,
                isDragging: Boolean,
                percent: Float,
                offset: Int,
                footerHeight: Int,
                maxDragHeight: Int
            ) {
                onFooterMoving(
                    mapOf(
                        "isDragging" to isDragging,
                        "percent" to percent.toDouble(),
                        "offset" to offset,
                        "footerHeight" to footerHeight
                    )
                )
            }

            @SuppressLint("RestrictedApi")
            override fun onStateChanged(
                refreshLayout: com.scwang.smart.refresh.layout.api.RefreshLayout,
                oldState: NativeRefreshState,
                newState: NativeRefreshState
            ) {
                android.util.Log.d("SmartRefresh", "状态变化: ${StateMapper.nativeStateToString(oldState)} -> ${StateMapper.nativeStateToString(newState)}")
                val refreshStateValue = StateMapper.nativeStateToRefreshState(newState)
                onStateChanged(mapOf("state" to refreshStateValue))
            }

            override fun onHeaderReleased(header: RefreshHeader?, headerHeight: Int, maxDragHeight: Int) {}
            override fun onHeaderStartAnimator(header: RefreshHeader?, headerHeight: Int, maxDragHeight: Int) {}
            override fun onHeaderFinish(header: RefreshHeader?, success: Boolean) {}
            override fun onFooterReleased(footer: RefreshFooter?, footerHeight: Int, maxDragHeight: Int) {}
            override fun onFooterStartAnimator(footer: RefreshFooter?, footerHeight: Int, maxDragHeight: Int) {}
            override fun onFooterFinish(footer: RefreshFooter?, success: Boolean) {}
        })
    }

    // -----------------------
    // Props 设置方法
    // -----------------------
    
    fun setEnableRefresh(value: Boolean) {
        mainHandler.post { srl.setEnableRefresh(value) }
    }

    fun setEnableLoadMore(value: Boolean) {
        mainHandler.post { srl.setEnableLoadMore(value) }
    }

    fun setEnableAutoLoadMore(value: Boolean) {
        mainHandler.post { srl.setEnableAutoLoadMore(value) }
    }

    fun setEnablePureScrollMode(value: Boolean) {
        mainHandler.post { srl.setEnablePureScrollMode(value) }
    }

    fun setEnableScrollContentWhenLoaded(value: Boolean) {
        mainHandler.post { srl.setEnableScrollContentWhenLoaded(value) }
    }

    fun setEnableScrollContentWhenRefreshed(value: Boolean) {
        mainHandler.post { srl.setEnableScrollContentWhenRefreshed(value) }
    }

    fun setEnableOverScrollDrag(value: Boolean) {
        mainHandler.post { srl.setEnableOverScrollDrag(value) }
    }

    fun setEnableOverScrollBounce(value: Boolean) {
        mainHandler.post { srl.setEnableOverScrollBounce(value) }
    }

    fun setEnableNestedScroll(value: Boolean) {
        mainHandler.post { srl.setEnableNestedScroll(value) }
    }

    fun setEnableHeaderTranslationContent(value: Boolean) {
        mainHandler.post { srl.setEnableHeaderTranslationContent(value) }
    }

    fun setEnableFooterTranslationContent(value: Boolean) {
        mainHandler.post { srl.setEnableFooterTranslationContent(value) }
    }

    fun setEnableLoadMoreWhenContentNotFull(value: Boolean) {
        mainHandler.post { srl.setEnableLoadMoreWhenContentNotFull(value) }
    }

    fun setHeaderType(type: String?) {
        type ?: return
        mainHandler.post {
            try {
                val header: RefreshHeader = when (type.lowercase()) {
                    "material" -> MaterialHeader(context)
                    "classics" -> ClassicsHeader(context).apply { setSpinnerStyle(SpinnerStyle.Translate) }
                    else -> ClassicsHeader(context).apply { setSpinnerStyle(SpinnerStyle.Translate) }
                }
                srl.setRefreshHeader(header)
            } catch (_: Throwable) {}
        }
    }

    fun setHeaderHeight(dp: Double) {
        mainHandler.post { srl.setHeaderHeight(dpToPx(dp)) }
    }

    fun setFooterHeight(dp: Double) {
        mainHandler.post { srl.setFooterHeight(dpToPx(dp)) }
    }

    fun setHeaderInsetStart(dp: Double) {
        mainHandler.post { srl.setHeaderInsetStart(dpToPx(dp)) }
    }

    fun setFooterInsetStart(dp: Double) {
        mainHandler.post { srl.setFooterInsetStart(dpToPx(dp)) }
    }

    fun setHeaderMaxDragRate(v: Double) {
        mainHandler.post { srl.setHeaderMaxDragRate(v.toFloat()) }
    }

    fun setFooterMaxDragRate(v: Double) {
        mainHandler.post { srl.setFooterMaxDragRate(v.toFloat()) }
    }

    fun setHeaderTriggerRate(v: Double) {
        mainHandler.post { srl.setHeaderTriggerRate(v.toFloat()) }
    }

    fun setFooterTriggerRate(v: Double) {
        mainHandler.post { srl.setFooterTriggerRate(v.toFloat()) }
    }

    fun setReboundDuration(ms: Int) {
        mainHandler.post { srl.setReboundDuration(ms) }
    }

    fun setDragRate(rate: Double) {
        mainHandler.post { srl.setDragRate(rate.toFloat()) }
    }

    /**
     * 设置经典样式 Header 属性（委托给 HeaderPropsHandler）
     */
    fun setClassicRefreshHeaderProps(props: Map<String, Any?>?) {
        mainHandler.post { headerPropsHandler.applyProps(props) }
    }

    /**
     * 设置经典样式 Footer 属性（委托给 FooterPropsHandler）
     */
    fun setClassicLoadMoreFooterProps(props: Map<String, Any?>?) {
        mainHandler.post { footerPropsHandler.applyProps(props) }
    }
    
    /**
     * 设置是否使用自定义 Header
     */
    fun setHasCustomHeader(value: Boolean) {
        android.util.Log.d("SmartRefresh", "setHasCustomHeader: $value")
        hasCustomHeader = value
        customHeaderSet = false  // 重置标志，允许重新设置
    }
    
    /**
     * 设置是否使用自定义 Footer
     */
    fun setHasCustomFooter(value: Boolean) {
        android.util.Log.d("SmartRefresh", "setHasCustomFooter: $value")
        hasCustomFooter = value
        customFooterSet = false  // 重置标志，允许重新设置
    }

    // -----------------------
    // 命令方法
    // -----------------------
    
    fun finishRefresh(success: Boolean = true, delay: Int = 0) {
        mainHandler.post { srl.finishRefresh(delay, success, false) }
    }

    fun finishLoadMore(success: Boolean = true, delay: Int = 0, noMoreData: Boolean = false) {
        mainHandler.post { srl.finishLoadMore(delay, success, noMoreData) }
    }

    fun autoRefresh(delay: Int = 0) {
        mainHandler.post { srl.autoRefresh(delay) }
    }

    fun autoLoadMore(delay: Int = 0) {
        mainHandler.post { srl.autoLoadMore(delay) }
    }

    fun setNoMoreData(noMore: Boolean) {
        mainHandler.post { srl.setNoMoreData(noMore) }
    }

    // -----------------------
    // 自定义 Header/Footer 支持
    // -----------------------
    
    
    /**
     * 设置自定义 Header
     */
    private fun setCustomHeader(view: View) {
        try {
            android.util.Log.d("SmartRefresh", "设置自定义 Header")
            
            // 创建一个包装 Header，实现 RefreshHeader 接口
            val customHeader = CustomRefreshHeader(context, view)
            srl.setRefreshHeader(customHeader)
            
            android.util.Log.d("SmartRefresh", "自定义 Header 设置成功")
        } catch (e: Exception) {
            android.util.Log.e("SmartRefresh", "设置自定义 Header 失败", e)
        }
    }
    
    /**
     * 设置自定义 Footer
     */
    private fun setCustomFooter(view: View) {
        try {
            android.util.Log.d("SmartRefresh", "设置自定义 Footer")
            
            // 创建一个包装 Footer，实现 RefreshFooter 接口
            val customFooter = CustomRefreshFooter(context, view)
            srl.setRefreshFooter(customFooter)
            
            android.util.Log.d("SmartRefresh", "自定义 Footer 设置成功")
        } catch (e: Exception) {
            android.util.Log.e("SmartRefresh", "设置自定义 Footer 失败", e)
        }
    }
    
    // -----------------------
    // 工具方法
    // -----------------------
    
    private fun dpToPx(dp: Double): Float {
        val density = resources.displayMetrics.density
        return (dp * density).toFloat()
    }

    // ===========================================================================
    // 生命周期管理
    // ===========================================================================
    
    /**
     * 视图从窗口分离时的清理工作
     * 
     * 移除所有监听器和回调，防止内存泄漏
     */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        try {
            srl.setOnRefreshListener(null)
            srl.setOnLoadMoreListener(null)
            srl.setOnMultiListener(null)
            srl.removeAllViews()
        } catch (_: Throwable) {}
        mainHandler.removeCallbacksAndMessages(null)
    }
}
