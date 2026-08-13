package com.exposmartrefreshlayout

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.ClassicsHeader
import com.scwang.smart.refresh.layout.api.RefreshHeader
import com.scwang.smart.refresh.layout.api.RefreshKernel
import com.scwang.smart.refresh.layout.api.RefreshLayout
import com.scwang.smart.refresh.layout.constant.RefreshState
import com.scwang.smart.refresh.layout.constant.SpinnerStyle

internal class ConfiguredClassicsHeader(
  context: Context,
  private val onHeaderInitialized: (ConfiguredClassicsHeader) -> Unit = {}
) : ClassicsHeader(context) {
  init {
    // ClassicsHeader 默认让进度图标以 GONE 启动；嵌入 TwoLevelHeader 后这会跳过首次布局，
    // 后续切到 VISIBLE 时仍是 0x0。改用 INVISIBLE 可保持占位和测量结果，同时不提前显示。
    mProgressView.visibility = View.INVISIBLE
  }

  override fun onInitialized(kernel: RefreshKernel, height: Int, maxDragHeight: Int) {
    super.onInitialized(kernel, height, maxDragHeight)
    onHeaderInitialized(this)
  }

  override fun onStateChanged(
    refreshLayout: RefreshLayout,
    oldState: RefreshState,
    newState: RefreshState,
  ) {
    super.onStateChanged(refreshLayout, oldState, newState)
    if (newState == RefreshState.None && mProgressView.visibility == View.GONE) {
      mProgressView.visibility = View.INVISIBLE
    }
  }

  fun setMessages(
    pulling: String?,
    release: String?,
    refreshing: String?,
    complete: String?,
    refreshLayout: RefreshLayout,
    currentState: RefreshState
  ) {
    pulling?.let { mTextPulling = it }
    release?.let { mTextRelease = it }
    refreshing?.let { mTextRefreshing = it }
    complete?.let { mTextFinish = it }
    onStateChanged(refreshLayout, currentState, currentState)
  }

  fun setColors(primaryColor: Int, indicatorColor: Int, titleColor: Int) {
    setPrimaryColor(primaryColor)
    setAccentColor(indicatorColor)
    mTitleText.setTextColor(titleColor)
    mLastUpdateText.setTextColor(titleColor)
    invalidate()
  }
}

/**
 * 在不修改被包装 Classic Header 自身布局的前提下，为 Translate Header 增加可测量的顶部间距。
 * TwoLevelHeader 会用包含该间距的总高度计算普通刷新和二楼的所有触发阈值。
 */
internal class InsetRefreshHeader(
  context: Context,
  private val wrappedHeader: RefreshHeader,
  insetPx: Int = 0,
) : FrameLayout(context), RefreshHeader {
  private var headerInsetPx = insetPx.coerceAtLeast(0)

  init {
    addView(
      wrappedHeader.view,
      LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
      ).apply {
        topMargin = headerInsetPx
      },
    )
  }

  fun setInsetPx(value: Int): Boolean {
    // 返回是否真正变化，让调用方只在必要时清理库内的 Header 高度缓存，减少无效重测。
    val normalizedValue = value.coerceAtLeast(0)
    if (headerInsetPx == normalizedValue) return false

    headerInsetPx = normalizedValue
    val params = wrappedHeader.view.layoutParams as FrameLayout.LayoutParams
    params.topMargin = headerInsetPx
    wrappedHeader.view.layoutParams = params
    requestLayout()
    return true
  }

  override fun getView(): View = this

  override fun getSpinnerStyle(): SpinnerStyle = SpinnerStyle.Translate

  override fun setPrimaryColors(vararg colors: Int) {
    wrappedHeader.setPrimaryColors(*colors)
  }

  override fun onInitialized(kernel: RefreshKernel, height: Int, maxDragHeight: Int) {
    // 包装层只负责几何尺寸，生命周期仍透传给真实 Header，避免丢失库内核引用和动画初始化。
    wrappedHeader.onInitialized(kernel, height, maxDragHeight)
  }

  override fun onMoving(
    isDragging: Boolean,
    percent: Float,
    offset: Int,
    height: Int,
    maxDragHeight: Int,
  ) {
    wrappedHeader.onMoving(isDragging, percent, offset, height, maxDragHeight)
  }

  override fun onReleased(refreshLayout: RefreshLayout, height: Int, maxDragHeight: Int) {
    wrappedHeader.onReleased(refreshLayout, height, maxDragHeight)
  }

  override fun onStartAnimator(refreshLayout: RefreshLayout, height: Int, maxDragHeight: Int) {
    wrappedHeader.onStartAnimator(refreshLayout, height, maxDragHeight)
  }

  override fun onFinish(refreshLayout: RefreshLayout, success: Boolean): Int =
    wrappedHeader.onFinish(refreshLayout, success)

  override fun onHorizontalDrag(percentX: Float, offsetX: Int, offsetMax: Int) {
    wrappedHeader.onHorizontalDrag(percentX, offsetX, offsetMax)
  }

  override fun isSupportHorizontalDrag(): Boolean = wrappedHeader.isSupportHorizontalDrag

  override fun autoOpen(duration: Int, dragRate: Float, animationOnly: Boolean): Boolean =
    wrappedHeader.autoOpen(duration, dragRate, animationOnly)

  override fun onStateChanged(
    refreshLayout: RefreshLayout,
    oldState: RefreshState,
    newState: RefreshState,
  ) {
    wrappedHeader.onStateChanged(refreshLayout, oldState, newState)
  }
}

/**
 * SmartRefreshLayout adapter for a Fabric-owned custom header host. The host
 * is reparented here so SmartRefreshLayout controls its pull geometry while
 * React continues to own all descendants (for example, a LottieView).
 */
internal class SlotRefreshHeader(
  context: Context,
  val slotHost: View,
) : FrameLayout(context), RefreshHeader {
  private var customSpinnerStyle: SpinnerStyle = SpinnerStyle.Translate
  private var headerHeightPx: Int = 1
  var finishDurationMs: Int = 0
  var initialized: ((height: Int, maxDragHeight: Int) -> Unit)? = null

  fun setSpinnerStyle(value: SpinnerStyle) {
    customSpinnerStyle = value
    resetSlotTransform()
  }

  private fun resetSlotTransform() {
    slotHost.scaleX = 1f
    slotHost.scaleY = 1f
    slotHost.pivotX = slotHost.width / 2f
    slotHost.pivotY = 0f
  }

  init {
    setBackgroundColor(android.graphics.Color.TRANSPARENT)
    addView(
      slotHost,
      LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT,
      ),
    )
  }

  override fun getView(): View = this

  override fun getSpinnerStyle(): SpinnerStyle = customSpinnerStyle

  override fun setPrimaryColors(vararg colors: Int) = Unit

  override fun onInitialized(kernel: RefreshKernel, height: Int, maxDragHeight: Int) {
    headerHeightPx = height.coerceAtLeast(1)
    val params = layoutParams
    if (params != null && params.height != height) {
      params.height = height
      layoutParams = params
    }
    minimumHeight = height.coerceAtLeast(0)
    resetSlotTransform()
    requestLayout()
    initialized?.invoke(height, maxDragHeight)
  }

  override fun onMoving(
    isDragging: Boolean,
    percent: Float,
    offset: Int,
    height: Int,
    maxDragHeight: Int,
  ) {
    if (!customSpinnerStyle.scale) {
      resetSlotTransform()
      return
    }

    // Scale the React-owned content with the pull distance. SmartRefreshLayout
    // changes the header container geometry, but does not transform its child.
    val baseHeight = height.coerceAtLeast(headerHeightPx).coerceAtLeast(1)
    val maxScale = 1f + maxDragHeight.toFloat() / baseHeight
    val scale = (offset.toFloat() / baseHeight).coerceIn(0f, maxScale)
    slotHost.pivotX = slotHost.width / 2f
    slotHost.pivotY = 0f
    val visibleScale = scale.coerceAtLeast(0.01f)
    slotHost.scaleX = visibleScale
    slotHost.scaleY = visibleScale
  }

  override fun onReleased(refreshLayout: RefreshLayout, height: Int, maxDragHeight: Int) {
    if (!customSpinnerStyle.scale) resetSlotTransform()
  }

  override fun onStartAnimator(refreshLayout: RefreshLayout, height: Int, maxDragHeight: Int) = Unit

  override fun onFinish(refreshLayout: RefreshLayout, success: Boolean): Int {
    resetSlotTransform()
    return finishDurationMs
  }

  override fun onHorizontalDrag(percentX: Float, offsetX: Int, offsetMax: Int) = Unit

  override fun isSupportHorizontalDrag(): Boolean = false

  // Returning true tells SmartRefreshLayout that the header has taken over the
  // programmatic opening animation. This slot has no separate animator, so
  // defer to the layout to preserve beginRefresh and controlled refreshing.
  override fun autoOpen(duration: Int, dragRate: Float, animationOnly: Boolean): Boolean = false

  override fun onStateChanged(
    refreshLayout: RefreshLayout,
    oldState: RefreshState,
    newState: RefreshState,
  ) = Unit
}

internal class ConfiguredClassicsFooter(context: Context) : ClassicsFooter(context) {
  fun setMessages(
    pulling: String?,
    release: String?,
    loading: String?,
    noMoreData: String?
  ) {
    pulling?.let { mTextPulling = it }
    release?.let { mTextRelease = it }
    loading?.let {
      // 不同 SmartRefreshLayout 版本可能读取 loading 或 refreshing 字段，两者同步可保持兼容。
      mTextLoading = it
      mTextRefreshing = it
    }
    noMoreData?.let { mTextNothing = it }
  }

  fun setColors(primaryColor: Int, indicatorColor: Int, titleColor: Int) {
    setPrimaryColor(primaryColor)
    setAccentColor(indicatorColor)
    mTitleText.setTextColor(titleColor)
  }
}
