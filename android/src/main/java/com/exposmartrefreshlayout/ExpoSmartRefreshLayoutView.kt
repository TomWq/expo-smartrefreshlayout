package com.exposmartrefreshlayout

import android.graphics.Color
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.FrameLayout
import java.util.Locale
import com.facebook.react.uimanager.ThemedReactContext
import com.scwang.smart.refresh.header.MaterialHeader
import com.scwang.smart.refresh.layout.SmartRefreshLayout
import com.scwang.smart.refresh.layout.api.RefreshFooter
import com.scwang.smart.refresh.layout.api.RefreshHeader
import com.scwang.smart.refresh.layout.api.RefreshLayout
import com.scwang.smart.refresh.layout.constant.RefreshState
import com.scwang.smart.refresh.layout.constant.SpinnerStyle
import com.scwang.smart.refresh.layout.listener.OnMultiListener

internal class ExpoSmartRefreshLayoutView(
  context: ThemedReactContext
) : FrameLayout(context) {
  private enum class OperationKind { REFRESH, LOAD_MORE }

  private data class Operation(
    val kind: OperationKind,
    val requestId: Int,
    val source: String
  )

  private val refreshLayout = SmartRefreshLayout(context)
  private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
  private val delayedCallbacks = mutableSetOf<Runnable>()
  private var reactChild: View? = null
  private var header: RefreshHeader? = null
  private var footer: RefreshFooter? = null
  private var installedHeaderStyle: String? = null
  private var headerRebuildPosted = false
  private var headerRebuildPending = false
  private var activeOperation: Operation? = null
  private var scheduledOperation: Operation? = null
  private var nextGestureRequestId = -1
  private var disposed = false
  private var touchStartY = 0f
  private var autoLoadMoreArmed = false

  private var refreshEnabled = true
  private var loadMoreEnabled = false
  private var autoLoadMoreEnabled = false
  private var hapticsEnabled = true
  private var refreshing = false
  private var loadingMore = false
  private var noMoreData = false
  private var headerStyle = "classic"
  private var indicatorColor: Int? = null
  private var titleColor: Int? = null
  private var pullDownText: String? = null
  private var releaseToRefreshText: String? = null
  private var refreshingText: String? = null
  private var refreshCompleteText: String? = null
  private var pullUpText: String? = null
  private var releaseToLoadMoreText: String? = null
  private var loadingMoreText: String? = null
  private var noMoreDataText: String? = null

  var onRefresh: ((Int, String) -> Unit)? = null
  var onLoadMore: ((Int, String) -> Unit)? = null
  var onStateChange: ((String) -> Unit)? = null

  init {
    super.addView(
      refreshLayout,
      LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    )

    refreshLayout.apply {
      setDragRate(0.5f)
      setEnableOverScrollDrag(true)
      setEnableOverScrollBounce(true)
      setEnableNestedScroll(true)
      setEnableLoadMoreWhenContentNotFull(true)
      setEnableScrollContentWhenRefreshed(true)
      setEnableScrollContentWhenLoaded(true)
      // MaterialHeader changes the kernel default during initialization. Keep
      // this explicit so that switching back to Classic cannot inherit it.
      setEnableHeaderTranslationContent(true)
      setHeaderMaxDragRate(2.0f)
      setHeaderTriggerRate(1.0f)
      setReboundDuration(300)
    }

    rebuildHeader()
    rebuildFooter()
    applyEnabledState()
    installListeners()
  }

  override fun dispatchTouchEvent(event: MotionEvent): Boolean {
    if (autoLoadMoreEnabled && loadMoreEnabled && !noMoreData) {
      when (event.actionMasked) {
        MotionEvent.ACTION_DOWN -> touchStartY = event.y
        MotionEvent.ACTION_MOVE -> {
          val upwardDistance = touchStartY - event.y
          if (upwardDistance > touchSlop && contentExceedsViewport()) {
            armAutoLoadMore()
          } else if (upwardDistance < -touchSlop) {
            disarmAutoLoadMore()
          }
        }
      }
    }
    return super.dispatchTouchEvent(event)
  }

  fun addReactChild(child: View, index: Int) {
    check(reactChild == null) {
      "SmartRefreshLayout accepts exactly one child. Wrap multiple views in a React Native View."
    }
    check(index == 0) { "SmartRefreshLayout only supports a child at index 0." }

    reactChild = child
    refreshLayout.addView(
      child,
      ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
      )
    )
  }

  fun getReactChildCount(): Int = if (reactChild == null) 0 else 1

  fun getReactChildAt(index: Int): View? = if (index == 0) reactChild else null

  fun removeReactChild(index: Int) {
    if (index != 0) return
    reactChild?.let(refreshLayout::removeView)
    reactChild = null
  }

  fun setRefreshEnabled(value: Boolean) {
    refreshEnabled = value
    applyEnabledState()
  }

  fun setLoadMoreEnabled(value: Boolean) {
    loadMoreEnabled = value
    applyEnabledState()
  }

  fun setAutoLoadMoreEnabled(value: Boolean) {
    autoLoadMoreEnabled = value
    if (!value) disarmAutoLoadMore()
    applyAutoLoadMoreState()
  }

  fun setRefreshing(value: Boolean) {
    refreshing = value
    if (value) {
      if (activeOperation?.kind != OperationKind.LOAD_MORE && !refreshLayout.isRefreshing) {
        refreshLayout.autoRefreshAnimationOnly()
      }
    } else {
      cancelOperation(OperationKind.REFRESH)
      if (refreshLayout.isRefreshing) refreshLayout.finishRefresh()
    }
  }

  fun setLoadingMore(value: Boolean) {
    loadingMore = value
    if (value) {
      if (activeOperation?.kind != OperationKind.REFRESH && !refreshLayout.isLoading) {
        refreshLayout.autoLoadMoreAnimationOnly()
      }
    } else {
      cancelOperation(OperationKind.LOAD_MORE)
      if (refreshLayout.isLoading) refreshLayout.finishLoadMore()
    }
  }

  fun setNoMoreData(value: Boolean) {
    noMoreData = value
    if (value) disarmAutoLoadMore()
    refreshLayout.setNoMoreData(value)
    onStateChange?.invoke(if (value) "no-more-data" else "idle")
  }

  fun setHapticsEnabled(value: Boolean) {
    hapticsEnabled = value
  }

  fun setHeaderStyle(value: String?) {
    val nextValue = value?.lowercase(Locale.ROOT) ?: "classic"
    if (nextValue != "classic" && nextValue != "material") return
    if (headerStyle == nextValue && installedHeaderStyle == nextValue) return
    headerStyle = nextValue
    requestHeaderRebuild()
  }

  fun setIndicatorColor(value: Int?) {
    if (indicatorColor == value) return
    indicatorColor = value
    applyColors()
  }

  fun setTitleColor(value: Int?) {
    titleColor = value
    applyColors()
  }

  fun setPullDownText(value: String?) {
    pullDownText = value
    applyMessages()
  }

  fun setReleaseToRefreshText(value: String?) {
    releaseToRefreshText = value
    applyMessages()
  }

  fun setRefreshingText(value: String?) {
    refreshingText = value
    applyMessages()
  }

  fun setRefreshCompleteText(value: String?) {
    refreshCompleteText = value
    applyMessages()
  }

  fun setPullUpText(value: String?) {
    pullUpText = value
    applyMessages()
  }

  fun setReleaseToLoadMoreText(value: String?) {
    releaseToLoadMoreText = value
    applyMessages()
  }

  fun setLoadingMoreText(value: String?) {
    loadingMoreText = value
    applyMessages()
  }

  fun setNoMoreDataText(value: String?) {
    noMoreDataText = value
    applyMessages()
  }

  fun beginRefresh(requestId: Int, delayMs: Int) {
    scheduleOperation(Operation(OperationKind.REFRESH, requestId, "programmatic"), delayMs)
  }

  fun finishRefresh(requestId: Int, success: Boolean, delayMs: Int) {
    finishOperation(OperationKind.REFRESH, requestId, delayMs) {
      refreshing = false
      refreshLayout.finishRefresh(0, success, false)
    }
  }

  fun beginLoadMore(requestId: Int, delayMs: Int) {
    scheduleOperation(Operation(OperationKind.LOAD_MORE, requestId, "programmatic"), delayMs)
  }

  fun finishLoadMore(
    requestId: Int,
    success: Boolean,
    noMoreData: Boolean,
    delayMs: Int
  ) {
    finishOperation(OperationKind.LOAD_MORE, requestId, delayMs) {
      loadingMore = false
      this.noMoreData = noMoreData
      disarmAutoLoadMore()
      refreshLayout.finishLoadMore(0, success, noMoreData)
      if (noMoreData) onStateChange?.invoke("no-more-data")
    }
  }

  fun resetNoMoreData() {
    noMoreData = false
    refreshLayout.resetNoMoreData()
    onStateChange?.invoke("idle")
  }

  fun dispose() {
    disposed = true
    delayedCallbacks.toList().forEach(::removeCallbacks)
    delayedCallbacks.clear()
    activeOperation = null
    scheduledOperation = null
  }

  private fun scheduleOperation(operation: Operation, delayMs: Int) {
    if (operation.requestId <= 0 || activeOperation != null || scheduledOperation != null) return
    scheduledOperation = operation
    postDelayedTracked(delayMs) {
      if (scheduledOperation != operation || activeOperation != null) return@postDelayedTracked
      scheduledOperation = null
      activeOperation = operation
      if (operation.kind == OperationKind.REFRESH) {
        disarmAutoLoadMore()
        refreshing = true
        if (!refreshLayout.isRefreshing) refreshLayout.autoRefreshAnimationOnly()
        onRefresh?.invoke(operation.requestId, operation.source)
      } else {
        disarmAutoLoadMore()
        loadingMore = true
        if (!refreshLayout.isLoading) refreshLayout.autoLoadMoreAnimationOnly()
        onLoadMore?.invoke(operation.requestId, operation.source)
      }
    }
  }

  private fun finishOperation(
    kind: OperationKind,
    requestId: Int,
    delayMs: Int,
    completion: () -> Unit
  ) {
    val scheduled = scheduledOperation
    val active = activeOperation
    val matchesScheduled = scheduled?.kind == kind && scheduled.requestId == requestId
    val matchesActive = active?.kind == kind && active.requestId == requestId
    val visualOnly = requestId == 0 && active == null && scheduled == null
    if (!matchesScheduled && !matchesActive && !visualOnly) return

    postDelayedTracked(delayMs) {
      val current = activeOperation
      val currentScheduled = scheduledOperation
      val stillScheduled =
        currentScheduled?.kind == kind && currentScheduled.requestId == requestId
      val stillActive = current?.kind == kind && current.requestId == requestId
      val stillVisualOnly = requestId == 0 && current == null && currentScheduled == null
      if (!stillScheduled && !stillActive && !stillVisualOnly) return@postDelayedTracked

      if (stillScheduled) scheduledOperation = null
      completion()
      if (stillActive) activeOperation = null
    }
  }

  private fun cancelOperation(kind: OperationKind) {
    if (scheduledOperation?.kind == kind) scheduledOperation = null
    if (activeOperation?.kind == kind) activeOperation = null
  }

  private fun beginGestureOperation(kind: OperationKind) {
    if (activeOperation != null || scheduledOperation != null) {
      closeRejectedGesture(kind)
      return
    }
    if (kind == OperationKind.LOAD_MORE && noMoreData) {
      closeRejectedGesture(kind)
      return
    }

    val operation = Operation(kind, allocateGestureRequestId(), "gesture")
    activeOperation = operation
    if (kind == OperationKind.REFRESH) {
      disarmAutoLoadMore()
      refreshing = true
      onRefresh?.invoke(operation.requestId, operation.source)
    } else {
      disarmAutoLoadMore()
      loadingMore = true
      onLoadMore?.invoke(operation.requestId, operation.source)
    }
  }

  private fun closeRejectedGesture(kind: OperationKind) {
    if (kind == OperationKind.REFRESH) refreshLayout.finishRefresh()
    else refreshLayout.finishLoadMore()
  }

  private fun allocateGestureRequestId(): Int {
    val current = nextGestureRequestId
    nextGestureRequestId = if (current == Int.MIN_VALUE) -1 else current - 1
    return current
  }

  private fun postDelayedTracked(delayMs: Int, action: () -> Unit) {
    lateinit var runnable: Runnable
    runnable = Runnable {
      delayedCallbacks.remove(runnable)
      if (!disposed) action()
    }
    delayedCallbacks.add(runnable)
    postDelayed(runnable, delayMs.coerceAtLeast(0).toLong())
  }

  private fun contentExceedsViewport(): Boolean {
    val child = reactChild ?: return false
    return child.canScrollVertically(-1) || child.canScrollVertically(1)
  }

  private fun armAutoLoadMore() {
    if (autoLoadMoreArmed || activeOperation != null || scheduledOperation != null) return
    autoLoadMoreArmed = true
    applyAutoLoadMoreState()
  }

  private fun disarmAutoLoadMore() {
    if (!autoLoadMoreArmed) return
    autoLoadMoreArmed = false
    applyAutoLoadMoreState()
  }

  private fun applyAutoLoadMoreState() {
    refreshLayout.setEnableLoadMoreWhenContentNotFull(!autoLoadMoreEnabled)
    refreshLayout.setEnableAutoLoadMore(
      autoLoadMoreEnabled && autoLoadMoreArmed && loadMoreEnabled && !noMoreData
    )
  }

  private fun applyEnabledState() {
    refreshLayout.setEnableRefresh(refreshEnabled)
    refreshLayout.setEnableLoadMore(loadMoreEnabled)
    applyAutoLoadMoreState()
  }

  private fun rebuildHeader() {
    if (installedHeaderStyle == headerStyle && header != null) return

    val nextHeader: RefreshHeader = if (headerStyle == "material") {
      MaterialHeader(context).also { material ->
        indicatorColor?.let { material.setColorSchemeColors(it) }
      }
    } else {
      ConfiguredClassicsHeader(context).apply {
        setSpinnerStyle(SpinnerStyle.Translate)
      }
    }

    refreshLayout.setEnableHeaderTranslationContent(true)
    refreshLayout.setRefreshHeader(nextHeader)
    header = nextHeader
    installedHeaderStyle = headerStyle
    applyMessages()
    applyColors()
    nextHeader.view.requestLayout()
    nextHeader.view.invalidate()
    refreshLayout.requestLayout()
    refreshLayout.invalidate()
    if (refreshing) post { if (refreshing) refreshLayout.autoRefreshAnimationOnly() }
  }

  private fun requestHeaderRebuild() {
    if (disposed) return
    headerRebuildPending = true
    if (headerRebuildPosted) return

    headerRebuildPosted = true
    post {
      headerRebuildPosted = false
      if (disposed || !headerRebuildPending) return@post
      if (
        refreshLayout.state != RefreshState.None ||
        activeOperation != null ||
        scheduledOperation != null ||
        refreshing ||
        loadingMore
      ) return@post
      headerRebuildPending = false
      rebuildHeader()
    }
  }

  private fun rebuildFooter() {
    footer = ConfiguredClassicsFooter(context)
    refreshLayout.setRefreshFooter(requireNotNull(footer))
    applyMessages()
    applyColors()
    if (noMoreData) {
      refreshLayout.setNoMoreData(true)
    } else if (loadingMore) {
      post { if (loadingMore) refreshLayout.autoLoadMoreAnimationOnly() }
    }
  }

  private fun applyMessages() {
    (header as? ConfiguredClassicsHeader)?.setMessages(
      pullDownText,
      releaseToRefreshText,
      refreshingText,
      refreshCompleteText
    )
    (footer as? ConfiguredClassicsFooter)?.setMessages(
      pullUpText,
      releaseToLoadMoreText,
      loadingMoreText,
      noMoreDataText
    )
  }

  private fun applyColors() {
    val indicator = indicatorColor ?: Color.DKGRAY
    val title = titleColor ?: Color.DKGRAY
    indicatorColor?.let { (header as? MaterialHeader)?.setColorSchemeColors(it) }
    (header as? ConfiguredClassicsHeader)?.setColors(indicator, title)
    (footer as? ConfiguredClassicsFooter)?.setColors(indicator, title)
  }

  private fun installListeners() {
    refreshLayout.setOnMultiListener(object : OnMultiListener {
      override fun onRefresh(refreshLayout: RefreshLayout) {
        beginGestureOperation(OperationKind.REFRESH)
      }

      override fun onLoadMore(refreshLayout: RefreshLayout) {
        beginGestureOperation(OperationKind.LOAD_MORE)
      }

      override fun onStateChanged(
        refreshLayout: RefreshLayout,
        oldState: RefreshState,
        newState: RefreshState
      ) {
        if (
          hapticsEnabled &&
          (newState == RefreshState.ReleaseToRefresh || newState == RefreshState.ReleaseToLoad)
        ) {
          performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
        }
        onStateChange?.invoke(newState.toPublicState())
        if (newState == RefreshState.None && headerRebuildPending) {
          requestHeaderRebuild()
        }
      }

      override fun onHeaderMoving(
        header: RefreshHeader?, isDragging: Boolean, percent: Float, offset: Int,
        headerHeight: Int, maxDragHeight: Int
      ) = Unit

      override fun onFooterMoving(
        footer: RefreshFooter?, isDragging: Boolean, percent: Float, offset: Int,
        footerHeight: Int, maxDragHeight: Int
      ) = Unit

      override fun onHeaderReleased(header: RefreshHeader?, headerHeight: Int, maxDragHeight: Int) = Unit
      override fun onHeaderStartAnimator(header: RefreshHeader?, headerHeight: Int, maxDragHeight: Int) = Unit
      override fun onHeaderFinish(header: RefreshHeader?, success: Boolean) = Unit
      override fun onFooterReleased(footer: RefreshFooter?, footerHeight: Int, maxDragHeight: Int) = Unit
      override fun onFooterStartAnimator(footer: RefreshFooter?, footerHeight: Int, maxDragHeight: Int) = Unit
      override fun onFooterFinish(footer: RefreshFooter?, success: Boolean) = Unit
    })
  }

  private fun RefreshState.toPublicState(): String = when (this) {
    RefreshState.PullDownToRefresh, RefreshState.PullUpToLoad -> "pulling"
    RefreshState.ReleaseToRefresh, RefreshState.ReleaseToLoad -> "ready"
    RefreshState.Refreshing -> "refreshing"
    RefreshState.Loading -> "loading"
    else -> "idle"
  }
}
