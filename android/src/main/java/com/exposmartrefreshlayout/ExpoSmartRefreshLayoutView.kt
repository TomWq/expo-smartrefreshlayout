package com.exposmartrefreshlayout

import android.graphics.Color
import android.graphics.Canvas
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
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
) : SmartRefreshLayout(context) {
  private enum class OperationKind { REFRESH, LOAD_MORE }

  private data class Operation(
    val kind: OperationKind,
    val requestId: Int,
    val source: String
  )

  // Keep the React content and SmartRefreshLayout's Header/Footer in one
  // native ViewGroup. FixedBehind relies on that exact sibling relationship
  // when it translates the content and clips the exposed Header region.
  private val refreshLayout: SmartRefreshLayout
    get() = this
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
  private var primaryColor: Int? = null
  private var indicatorColor: Int? = null
  private var titleColor: Int? = null
  private var classicSpinnerStyle = "translate"
  private var classicEnableLastTime = true
  private var materialShowBezierWave = false
  private var materialEnableHeaderTranslationContent = false
  private var materialProgressBackgroundColor: Int? = null
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
    refreshLayout.apply {
      setDragRate(0.5f)
      setEnableOverScrollDrag(true)
      setEnableOverScrollBounce(true)
      setEnableNestedScroll(true)
      setEnableLoadMoreWhenContentNotFull(true)
      setEnableScrollContentWhenRefreshed(true)
      setEnableScrollContentWhenLoaded(true)
      // Match the official Classics sample: FixedBehind is clipped to the
      // revealed area while RefreshContent is translated out of its way.
      setEnableClipHeaderWhenFixedBehind(true)
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

  override fun dispatchDraw(canvas: Canvas) {
    // SmartRefreshLayout disables child clipping by default. React Native's
    // surrounding Views do not restore the XML parent's viewport clip, so a
    // translated RefreshContent can otherwise paint over sibling controls.
    val saveCount = canvas.save()
    canvas.clipRect(0, 0, width, height)
    super.dispatchDraw(canvas)
    canvas.restoreToCount(saveCount)
  }

  fun addReactChild(child: View, index: Int) {
    check(reactChild == null) {
      "SmartRefreshLayout accepts exactly one child. Wrap multiple views in a React Native View."
    }
    check(index == 0) { "SmartRefreshLayout only supports a child at index 0." }

    reactChild = child
    // Do not use ViewGroup.addView here. SmartRefreshLayout needs this API to
    // register mRefreshContent, remove an empty-content placeholder, and wire
    // content translation/clipping for FixedBehind and Scale headers.
    refreshLayout.setRefreshContent(child)
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
    val wasRefreshActive =
      refreshing ||
        activeOperation?.kind == OperationKind.REFRESH ||
        refreshLayout.isRefreshing
    val wasLoadMoreActive =
      loadingMore ||
        activeOperation?.kind == OperationKind.LOAD_MORE ||
        refreshLayout.isLoading
    refreshing = value
    if (value) {
      if (activeOperation?.kind != OperationKind.LOAD_MORE && !refreshLayout.isRefreshing) {
        refreshLayout.autoRefreshAnimationOnly()
      }
    } else {
      // React can commit the uncontrolled prop update before the matching
      // finishRefresh command reaches Fabric. Keep the tracked operation until
      // that command completes, otherwise its request id is rejected and a
      // Translate header is closed before RefreshFinish can be displayed.
      if (
        !hasTrackedOperation(OperationKind.REFRESH) &&
        refreshLayout.state != RefreshState.RefreshFinish &&
        wasRefreshActive &&
        !wasLoadMoreActive
      ) {
        // finishRefresh only handles the fully-open Refreshing state. React
        // can clear the prop while the opening/rebound animator is still in
        // PullDownToRefresh; closeHeaderOrFooter covers those visual-only
        // states and guarantees the Translate header returns above content.
        refreshLayout.closeHeaderOrFooter()
      }
    }
  }

  fun setLoadingMore(value: Boolean) {
    val wasRefreshActive =
      refreshing ||
        activeOperation?.kind == OperationKind.REFRESH ||
        refreshLayout.isRefreshing
    val wasLoadMoreActive =
      loadingMore ||
        activeOperation?.kind == OperationKind.LOAD_MORE ||
        refreshLayout.isLoading
    loadingMore = value
    if (value) {
      if (activeOperation?.kind != OperationKind.REFRESH && !refreshLayout.isLoading) {
        refreshLayout.autoLoadMoreAnimationOnly()
      }
    } else {
      // Keep the load-more request for its finishLoadMore command for the
      // same Fabric prop/command ordering reason as refreshing above.
      if (
        !hasTrackedOperation(OperationKind.LOAD_MORE) &&
        refreshLayout.state != RefreshState.LoadFinish &&
        wasLoadMoreActive &&
        !wasRefreshActive
      ) {
        refreshLayout.closeHeaderOrFooter()
      }
    }
  }

  fun setNoMoreDataState(value: Boolean) {
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
    if (headerStyle == nextValue) return
    headerStyle = nextValue
    requestHeaderRebuild()
  }

  fun setPrimaryColor(value: Int?) {
    primaryColor = value
    applyColors()
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

  fun setClassicSpinnerStyle(value: String?) {
    val nextValue = when (value?.lowercase(Locale.ROOT)) {
      "scale" -> "scale"
      "translate" -> "translate"
      "fixed-behind" -> "fixed-behind"
      else -> "translate"
    }
    if (classicSpinnerStyle == nextValue) return
    classicSpinnerStyle = nextValue
    if (headerStyle == "classic") requestHeaderRebuild()
  }

  fun setClassicEnableLastTime(value: Boolean) {
    classicEnableLastTime = value
    (header as? ConfiguredClassicsHeader)?.let { classic ->
      classic.setEnableLastTime(value)
      classic.onStateChanged(refreshLayout, refreshLayout.state, refreshLayout.state)
    }
  }

  fun setMaterialShowBezierWave(value: Boolean) {
    materialShowBezierWave = value
    applyHeaderConfiguration()
  }

  fun setMaterialEnableHeaderTranslationContent(value: Boolean) {
    materialEnableHeaderTranslationContent = value
    applyHeaderConfiguration()
  }

  fun setMaterialProgressBackgroundColor(value: Int?) {
    materialProgressBackgroundColor = value
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

  fun resetNoMoreDataState() {
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
      startScheduledOperation(operation)
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

      if (stillScheduled) {
        scheduledOperation = null
      }
      completion()
      if (stillActive) activeOperation = null
      if (headerRebuildPending) requestHeaderRebuild()
    }
  }

  private fun startScheduledOperation(operation: Operation) {
    if (scheduledOperation != operation || activeOperation != null) return

    scheduledOperation = null
    activeOperation = operation
    disarmAutoLoadMore()
    if (operation.kind == OperationKind.REFRESH) {
      refreshing = true
      if (!refreshLayout.isRefreshing) refreshLayout.autoRefreshAnimationOnly()
      onRefresh?.invoke(operation.requestId, operation.source)
    } else {
      loadingMore = true
      if (!refreshLayout.isLoading) refreshLayout.autoLoadMoreAnimationOnly()
      onLoadMore?.invoke(operation.requestId, operation.source)
    }
  }

  private fun hasTrackedOperation(kind: OperationKind): Boolean =
    scheduledOperation?.kind == kind || activeOperation?.kind == kind

  private fun beginGestureOperation(kind: OperationKind) {
    if (activeOperation != null || scheduledOperation != null) {
      closeRejectedGesture()
      return
    }
    if (kind == OperationKind.LOAD_MORE && noMoreData) {
      closeRejectedGesture()
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

  private fun closeRejectedGesture() {
    refreshLayout.closeHeaderOrFooter()
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
    if (headerMatchesRequestedConfiguration()) return

    // A Classic spinner-style change intentionally keeps this component
    // instance. ClassicsAbstract caches its content baseline during the first
    // normal measure, which Scale needs when its idle height is zero.
    val nextHeader: RefreshHeader = if (headerStyle == "material") {
      MaterialHeader(context)
    } else {
      ConfiguredClassicsHeader(context).apply {
        setSpinnerStyle(resolveClassicSpinnerStyle())
      }
    }

    header = nextHeader
    installedHeaderStyle = headerStyle
    refreshLayout.setRefreshHeader(nextHeader)
    applyHeaderConfiguration()
    applyMessages()
    applyColors()
  }

  private fun requestHeaderRebuild() {
    if (disposed) return
    headerRebuildPending = true
    if (!canReconfigureHeaderNow()) return

    headerRebuildPending = false
    if (headerMatchesRequestedConfiguration()) {
      applyClassicSpinnerStyle()
    } else {
      rebuildHeader()
    }
  }

  private fun postHeaderRebuildIfIdle() {
    if (disposed || !headerRebuildPending || headerRebuildPosted) return
    headerRebuildPosted = true
    post {
      headerRebuildPosted = false
      if (disposed || !headerRebuildPending) return@post
      requestHeaderRebuild()
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
      refreshCompleteText,
      refreshLayout,
      refreshLayout.state
    )
    (footer as? ConfiguredClassicsFooter)?.setMessages(
      pullUpText,
      releaseToLoadMoreText,
      loadingMoreText,
      noMoreDataText
    )
  }

  private fun applyHeaderConfiguration() {
    when (val currentHeader = header) {
      is ConfiguredClassicsHeader -> {
        currentHeader.setEnableLastTime(classicEnableLastTime)
        refreshLayout.setEnableHeaderTranslationContent(true)
        refreshLayout.setEnableClipHeaderWhenFixedBehind(true)
        currentHeader.onStateChanged(refreshLayout, refreshLayout.state, refreshLayout.state)
      }
      is MaterialHeader -> {
        currentHeader.setShowBezierWave(materialShowBezierWave)
        refreshLayout.setEnableHeaderTranslationContent(
          materialEnableHeaderTranslationContent
        )
      }
    }
    header?.view?.invalidate()
  }

  private fun applyColors() {
    val primary = primaryColor ?: if (header is MaterialHeader) {
      MATERIAL_DEFAULT_PRIMARY_COLOR
    } else {
      Color.TRANSPARENT
    }
    val indicator = indicatorColor ?: Color.DKGRAY
    val title = titleColor ?: Color.DKGRAY
    refreshLayout.setPrimaryColors(primary, indicator)
    (header as? MaterialHeader)?.apply {
      setProgressBackgroundColorSchemeColor(
        materialProgressBackgroundColor ?: MATERIAL_DEFAULT_PROGRESS_BACKGROUND_COLOR
      )
      if (indicatorColor == null) {
        setColorSchemeColors(*MATERIAL_DEFAULT_PROGRESS_COLORS)
      } else {
        setColorSchemeColors(indicator)
      }
    }
    (header as? ConfiguredClassicsHeader)?.setColors(primary, indicator, title)
    (footer as? ConfiguredClassicsFooter)?.setColors(primary, indicator, title)
  }

  private fun resolveClassicSpinnerStyle(): SpinnerStyle = when (classicSpinnerStyle) {
    "scale" -> SpinnerStyle.Scale
    "fixed-behind" -> SpinnerStyle.FixedBehind
    else -> SpinnerStyle.Translate
  }

  private fun applyClassicSpinnerStyle() {
    val classic = header as? ConfiguredClassicsHeader ?: return
    val spinnerStyle = resolveClassicSpinnerStyle()
    classic.setSpinnerStyle(spinnerStyle)
    restoreClassicHeaderLayout(classic, spinnerStyle)
    restoreRefreshChildOrder()
  }

  private fun restoreClassicHeaderLayout(
    classic: ConfiguredClassicsHeader,
    spinnerStyle: SpinnerStyle
  ) {
    val headerView = classic.view
    val layoutParams = headerView.layoutParams
    val margins = layoutParams as? ViewGroup.MarginLayoutParams
    val leftMargin = margins?.leftMargin ?: 0
    val rightMargin = margins?.rightMargin ?: 0
    val topMargin = margins?.topMargin ?: 0
    val bottomMargin = margins?.bottomMargin ?: 0
    if (width == 0) return
    val headerWidth = (width - leftMargin - rightMargin).coerceAtLeast(0)

    // SmartRefreshLayout's Scale branch deliberately keeps an idle header at
    // zero height, then remeasures it at the spinner height while dragging.
    // Preserve its previously initialized full width so that branch never
    // starts from a 0xN replacement view under Fabric.
    val headerHeight = if (spinnerStyle.scale) {
      mSpinner.coerceAtLeast(0)
    } else {
      (mHeaderHeight - topMargin - bottomMargin).coerceAtLeast(0)
    }
    headerView.measure(
      View.MeasureSpec.makeMeasureSpec(headerWidth, View.MeasureSpec.EXACTLY),
      View.MeasureSpec.makeMeasureSpec(headerHeight, View.MeasureSpec.EXACTLY)
    )

    val top = topMargin + mHeaderInsetStart - if (spinnerStyle == SpinnerStyle.Translate) {
      mHeaderHeight
    } else {
      0
    }
    headerView.layout(
      leftMargin,
      top,
      leftMargin + headerView.measuredWidth,
      top + headerView.measuredHeight
    )
    // moveSpinner only updates a Translate header when its spinner value
    // changes. RefreshFinish publishes its state before that next movement,
    // so resetting this to zero here would hide the completion label above
    // the content for the whole finish delay.
    headerView.translationY = if (spinnerStyle == SpinnerStyle.Translate) {
      mSpinner.toFloat()
    } else {
      0f
    }
  }

  private fun restoreRefreshChildOrder() {
    val layout = refreshLayout.layout
    reactChild?.let(layout::bringChildToFront)
    header?.takeIf { it.spinnerStyle.front }?.let { layout.bringChildToFront(it.view) }
    footer?.takeIf { it.spinnerStyle.front }?.let { layout.bringChildToFront(it.view) }
  }

  private fun headerMatchesRequestedConfiguration(): Boolean =
    header != null &&
      installedHeaderStyle == headerStyle

  private fun canReconfigureHeaderNow(): Boolean =
    refreshLayout.state == RefreshState.None &&
      !refreshLayout.isRefreshing &&
      !refreshLayout.isLoading &&
      activeOperation == null

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
        // A Classics TextView changes its text after Fabric has already laid
        // out this native root. Remeasure only the current header so longer
        // state strings such as "正在刷新..." do not retain the pulling
        // label's narrower child width.
        (header as? ConfiguredClassicsHeader)?.let { classic ->
          restoreClassicHeaderLayout(classic, classic.spinnerStyle)
        }
        if (
          hapticsEnabled &&
          (newState == RefreshState.ReleaseToRefresh || newState == RefreshState.ReleaseToLoad)
        ) {
          performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
        }
        onStateChange?.invoke(newState.toPublicState())
        if (newState == RefreshState.None) postHeaderRebuildIfIdle()
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

  private companion object {
    const val MATERIAL_DEFAULT_PRIMARY_COLOR = 0xff11bbff.toInt()
    const val MATERIAL_DEFAULT_PROGRESS_BACKGROUND_COLOR = 0xfffafafa.toInt()
    val MATERIAL_DEFAULT_PROGRESS_COLORS = intArrayOf(
      0xff0099cc.toInt(),
      0xffff4444.toInt(),
      0xff669900.toInt(),
      0xffaa66cc.toInt(),
      0xffff8800.toInt()
    )
  }
}
