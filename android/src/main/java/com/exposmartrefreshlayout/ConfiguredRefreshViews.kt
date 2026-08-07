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
    // ClassicsHeader starts this view as GONE. Inside TwoLevelHeader that
    // prevents its first layout, so the later VISIBLE transition has 0x0 bounds.
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
 * Adds a measured top gap to a translate-style header without changing the
 * wrapped Classic header's own layout. TwoLevelHeader uses this total height
 * for all of its refresh and floor thresholds.
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
