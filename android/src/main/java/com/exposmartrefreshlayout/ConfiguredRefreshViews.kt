package com.exposmartrefreshlayout

import android.content.Context
import android.util.TypedValue
import android.view.Gravity
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
import kotlin.math.ceil

internal class ConfiguredClassicsHeader(
  context: Context,
  private val onHeaderInitialized: (ConfiguredClassicsHeader) -> Unit = {},
  private val onTitleLayoutChanged: () -> Unit = {},
) : ClassicsHeader(context) {
  private var transientTitle: String? = null
  private var secondFloorPullingText: String? = null

  init {
    // ClassicsHeader 默认让进度图标以 GONE 启动；嵌入 TwoLevelHeader 后这会跳过首次布局，
    // 后续切到 VISIBLE 时仍是 0x0。改用 INVISIBLE 可保持占位和测量结果，同时不提前显示。
    mProgressView.visibility = View.INVISIBLE
    mTitleText.visibility = View.VISIBLE
    mTitleText.gravity =
      (mTitleText.gravity and Gravity.VERTICAL_GRAVITY_MASK) or Gravity.CENTER_HORIZONTAL
    updateStableTitleWidth()
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
    // TwoLevelHeader 没有“继续下拉进入二楼”的独立状态，只有这段需要覆盖标题。
    transientTitle?.let { mTitleText.text = it }
    if (newState == RefreshState.None && mProgressView.visibility == View.GONE) {
      mProgressView.visibility = View.INVISIBLE
    }
  }

  fun setTransientTitle(
    value: String?,
    refreshLayout: RefreshLayout,
    currentState: RefreshState,
  ) {
    if (transientTitle == value) return
    transientTitle = value
    // Re-run the normal state rendering when clearing the override, so the
    // "release to refresh" title is restored instead of retaining stale text.
    onStateChanged(refreshLayout, currentState, currentState)
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
    updateStableTitleWidth()
    onStateChanged(refreshLayout, currentState, currentState)
  }

  fun setSecondFloorMessages(
    pulling: String?,
    release: String?,
    refreshLayout: RefreshLayout,
    currentState: RefreshState,
  ) {
    // 官方 ClassicsHeader 在 ReleaseToTwoLevel 状态读取这个字段，走原生状态机
    // 才会得到与默认“释放进入二楼”一致的完整布局和箭头动画。
    secondFloorPullingText = pulling
    release?.let { mTextSecondary = it }
    updateStableTitleWidth()
    // Fabric 更新文案时组件可能正停在二楼临界状态；仅更新字段不会触发
    // ClassicsHeader 重绘，因此需要按当前状态重新渲染并扩展标题容器。
    onStateChanged(refreshLayout, currentState, currentState)
  }

  fun setColors(primaryColor: Int, indicatorColor: Int, titleColor: Int) {
    setPrimaryColor(primaryColor)
    setAccentColor(indicatorColor)
    mTitleText.setTextColor(titleColor)
    mLastUpdateText.setTextColor(titleColor)
    invalidate()
  }

  fun setTitleTextSizeSp(value: Float) {
    mTitleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, value)
    updateStableTitleWidth()
  }

  private fun updateStableTitleWidth() {
    // SmartRefreshLayout 拖拽时会缓存 Header 的测量结果。进入二楼后再扩宽标题已经太晚，
    // 因此在手势开始前按所有状态文案的最大宽度固定一个 minWidth。
    val titles = listOfNotNull(
      mTextPulling,
      mTextRelease,
      mTextRefreshing,
      mTextFinish,
      mTextFailed,
      mTextLoading,
      mTextSecondary,
      secondFloorPullingText,
    )
    val requiredWidth = titles.maxOfOrNull { title ->
      ceil(mTitleText.paint.measureText(title)).toInt() +
        mTitleText.compoundPaddingLeft +
        mTitleText.compoundPaddingRight
    } ?: 0
    if (mTitleText.minWidth != requiredWidth) {
      mTitleText.minWidth = requiredWidth
      mTitleText.requestLayout()
      (mTitleText.parent as? View)?.requestLayout()
      requestLayout()
      onTitleLayoutChanged()
    }
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
