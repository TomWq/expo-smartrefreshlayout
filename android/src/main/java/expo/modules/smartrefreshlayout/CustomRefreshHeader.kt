package expo.modules.smartrefreshlayout

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.scwang.smart.refresh.layout.api.RefreshHeader
import com.scwang.smart.refresh.layout.api.RefreshKernel
import com.scwang.smart.refresh.layout.api.RefreshLayout
import com.scwang.smart.refresh.layout.constant.RefreshState
import com.scwang.smart.refresh.layout.constant.SpinnerStyle

/**
 * 自定义 RefreshHeader 包装器
 * 
 * 将 React Native 视图包装成 SmartRefreshLayout 可用的 RefreshHeader
 */
@SuppressLint("RestrictedApi")
class CustomRefreshHeader @JvmOverloads constructor(
    context: Context,
    private val customView: View,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs), RefreshHeader {

    init {
        // 将自定义视图添加到容器中
        addView(customView, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))
        
        // 关键：让容器完全透明，不参与触摸事件处理
        isClickable = false
        isFocusable = false
        isEnabled = false
        
        // 确保自定义视图本身也不拦截事件
        customView.isClickable = false
        customView.isFocusable = false
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        // 永远不拦截触摸事件
        return false
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        // 永远不消费触摸事件
        return false
    }
    
    override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        // 不阻止父视图拦截触摸事件
        parent?.requestDisallowInterceptTouchEvent(disallowIntercept)
    }

    override fun getView(): View = this

    override fun getSpinnerStyle(): SpinnerStyle = SpinnerStyle.Translate

    override fun setPrimaryColors(vararg colors: Int) {
        // 自定义视图不需要设置主题色
    }

    override fun onInitialized(kernel: RefreshKernel, height: Int, maxDragHeight: Int) {
        // 初始化完成
    }

    override fun onMoving(isDragging: Boolean, percent: Float, offset: Int, height: Int, maxDragHeight: Int) {
        // 可以在这里根据拖动进度更新自定义视图的状态
        // 例如：通过 JavaScript 事件通知 React Native 组件
    }

    override fun onReleased(refreshLayout: RefreshLayout, height: Int, maxDragHeight: Int) {
        // 释放
    }

    override fun onStartAnimator(refreshLayout: RefreshLayout, height: Int, maxDragHeight: Int) {
        // 开始动画
    }

    override fun onFinish(refreshLayout: RefreshLayout, success: Boolean): Int {
        return 0 // 返回 0 表示立即完成，不延迟
    }

    override fun onHorizontalDrag(percentX: Float, offsetX: Int, offsetMax: Int) {
        // 水平拖动，通常不需要处理
    }

    override fun isSupportHorizontalDrag(): Boolean = false

    override fun onStateChanged(refreshLayout: RefreshLayout, oldState: RefreshState, newState: RefreshState) {
        // 可以根据状态变化更新自定义视图
    }

    override fun autoOpen(duration: Int, dragRate: Float, animationOnly: Boolean): Boolean {
        // 自动打开功能，通常用于自动刷新
        return false
    }
}
