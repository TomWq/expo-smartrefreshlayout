package com.exposmartrefreshlayout

import android.content.Context
import android.graphics.Color
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import com.facebook.react.uimanager.ThemedReactContext
import com.scwang.smart.refresh.header.TwoLevelHeader
import com.scwang.smart.refresh.layout.SmartRefreshLayout
import com.scwang.smart.refresh.layout.api.RefreshHeader
import com.scwang.smart.refresh.layout.api.RefreshLayout
import com.scwang.smart.refresh.layout.constant.RefreshState
import com.scwang.smart.refresh.layout.constant.SpinnerStyle
import com.scwang.smart.refresh.layout.listener.OnMultiListener
import kotlin.math.roundToInt

/**
 * Android implementation of the Taobao-style second floor. React owns two
 * explicit slot hosts; SmartRefreshLayout owns their actual native placement.
 */
internal class ExpoSmartSecondFloorLayoutView(
  context: ThemedReactContext
) : SmartRefreshLayout(context) {
  private enum class SecondFloorLifecycle {
    IDLE,
    RELEASE,
    OPENING,
    OPEN,
    CLOSING,
  }

  private data class RefreshOperation(
    val requestId: Int,
    val source: String,
  )

  private val refreshLayout: SmartRefreshLayout
    get() = this
  private val delayedCallbacks = mutableSetOf<Runnable>()
  private val reactChildren = mutableListOf<View>()
  private val emptyContent = View(context).apply { visibility = GONE }
  private val twoLevelHeader = ConfiguredTwoLevelHeader(context)
  private lateinit var classicHeader: ConfiguredClassicsHeader
  private lateinit var insetRefreshHeader: InsetRefreshHeader

  private var contentSlot: ExpoSmartSecondFloorContentSlotView? = null
  private var floorSlot: ExpoSmartSecondFloorFloorSlotView? = null
  private var floorContentSlot: ExpoSmartSecondFloorFloorContentSlotView? = null
  private var headerReady = false
  private var disposed = false
  private var activeRefresh: RefreshOperation? = null
  private var scheduledRefresh: RefreshOperation? = null
  private var nextGestureRequestId = -1
  private var lifecycle = SecondFloorLifecycle.IDLE
  private var secondFloorOpenedInCurrentCycle = false
  private var currentHeaderOffset = 0

  private var refreshEnabled = true
  private var refreshingIntent = false
  private var hapticsEnabled = true
  private var secondFloorEnabled = true
  private var requestedHeaderInsetDp = 0
  private var requestedMaxRate = DEFAULT_MAX_RATE
  private var requestedFloorRate = DEFAULT_FLOOR_RATE
  private var requestedRefreshRate = DEFAULT_REFRESH_RATE
  private var requestedFloorDuration = DEFAULT_FLOOR_DURATION
  private var pullToCloseEnabled = true
  private var requestedBottomPullUpToCloseRate = DEFAULT_BOTTOM_PULL_UP_TO_CLOSE_RATE
  private var primaryColor: Int? = null
  private var indicatorColor: Int? = null
  private var titleColor: Int? = null
  private var classicEnableLastTime = true
  private var pullDownText: String? = null
  private var releaseToRefreshText: String? = null
  private var refreshingText: String? = null
  private var refreshCompleteText: String? = null

  var onRefresh: ((Int, String) -> Unit)? = null
  var onStateChange: ((String) -> Unit)? = null
  var onSecondFloorOpen: (() -> Unit)? = null
  var onSecondFloorClose: (() -> Unit)? = null

  init {
    refreshLayout.apply {
      // TwoLevelHeader intentionally positions its full-screen backdrop above
      // the collapsed header. Keep that drawing inside this React container so
      // it cannot paint over sibling UI such as the example's page tabs.
      clipChildren = true
      clipToPadding = true
      setDragRate(0.5f)
      setEnableOverScrollDrag(true)
      setEnableOverScrollBounce(true)
      setEnableNestedScroll(true)
      setEnableLoadMore(false)
      setEnableAutoLoadMore(false)
      setEnableLoadMoreWhenContentNotFull(false)
      setEnableScrollContentWhenRefreshed(true)
      setEnableHeaderTranslationContent(true)
      setReboundDuration(300)
    }

    classicHeader = ConfiguredClassicsHeader(context) { onClassicHeaderInitialized(it) }
    insetRefreshHeader = InsetRefreshHeader(context, classicHeader)
    twoLevelHeader.setRefreshHeader(insetRefreshHeader)
    twoLevelHeader.setOnTwoLevelListener {
      // TwoLevelHeader asks this listener after ReleaseToTwoLevel has already
      // been published. That is a valid handoff, not a competing open command.
      canOpenSecondFloor(allowReleasedGesture = true)
    }
    refreshLayout.setRefreshHeader(twoLevelHeader)
    refreshLayout.setRefreshContent(emptyContent)
    applyTwoLevelConfiguration()
    applyClassicConfiguration()
    applyEnabledState()
    installListeners()
  }

  fun addReactChild(child: View, index: Int) {
    if (disposed) return
    require(
      child is ExpoSmartSecondFloorContentSlotView ||
        child is ExpoSmartSecondFloorFloorSlotView ||
        child is ExpoSmartSecondFloorFloorContentSlotView
    ) {
      "SmartSecondFloorLayout accepts only its content, floor background, and floor content slot hosts."
    }
    if (!reactChildren.contains(child)) {
      reactChildren.add(index.coerceIn(0, reactChildren.size), child)
    }

    when (child) {
      is ExpoSmartSecondFloorContentSlotView -> {
        contentSlot = child
        attachContentSlot(child)
      }
      is ExpoSmartSecondFloorFloorSlotView -> {
        floorSlot = child
        attachFloorSlot(child)
      }
      is ExpoSmartSecondFloorFloorContentSlotView -> {
        floorContentSlot = child
        attachFloorContentSlot(child)
      }
    }
    applyEnabledState()
    reconcileChildOrder()
    requestLayout()
  }

  fun getReactChildCount(): Int = reactChildren.size

  fun getReactChildAt(index: Int): View? = reactChildren.getOrNull(index)

  fun removeReactChild(index: Int) {
    val child = reactChildren.getOrNull(index) ?: return
    reactChildren.removeAt(index)
    when (child) {
      is ExpoSmartSecondFloorContentSlotView -> {
        if (contentSlot === child) {
          contentSlot = null
          refreshLayout.setRefreshContent(emptyContent)
        }
      }
      is ExpoSmartSecondFloorFloorSlotView -> {
        if (floorSlot === child) {
          if (lifecycle != SecondFloorLifecycle.IDLE) closeSecondFloorInternal()
          if (child.parent === twoLevelHeader) twoLevelHeader.removeView(child)
          floorSlot = null
        }
      }
      is ExpoSmartSecondFloorFloorContentSlotView -> {
        if (floorContentSlot === child) {
          if (child.parent === twoLevelHeader) twoLevelHeader.removeView(child)
          floorContentSlot = null
        }
      }
    }
    applyEnabledState()
    reconcileChildOrder()
  }

  fun setRefreshEnabled(value: Boolean) {
    refreshEnabled = value
    applyEnabledState()
  }

  fun setRefreshing(value: Boolean) {
    if (disposed) return
    val wasVisualRefresh =
      refreshingIntent || activeRefresh != null || refreshLayout.isRefreshing
    refreshingIntent = value
    if (value) {
      if (canStartRefresh() && !refreshLayout.isRefreshing) {
        refreshLayout.autoRefreshAnimationOnly()
      }
    } else {
      // Fabric may deliver the uncontrolled prop update before the matching
      // finishRefresh command. Keep its native request id until that command
      // completes so normal refresh feedback is not skipped.
      if (
        !hasTrackedRefresh() &&
        refreshLayout.state != RefreshState.RefreshFinish &&
        wasVisualRefresh &&
        !isSecondFloorActive()
      ) {
        refreshLayout.closeHeaderOrFooter()
      }
    }
  }

  fun setHapticsEnabled(value: Boolean) {
    hapticsEnabled = value
  }

  fun setSecondFloorEnabled(value: Boolean) {
    if (!value && lifecycle != SecondFloorLifecycle.IDLE) {
      closeSecondFloorInternal()
    }
    secondFloorEnabled = value
    applyEnabledState()
  }

  fun setHeaderInset(value: Int) {
    val normalizedValue = value.coerceIn(0, MAX_HEADER_INSET_DP)
    if (requestedHeaderInsetDp == normalizedValue) return

    requestedHeaderInsetDp = normalizedValue
    if (insetRefreshHeader.setInsetPx(dpToPx(normalizedValue))) {
      twoLevelHeader.requestHeaderRemeasure()
      positionFloorBackground(currentHeaderOffset)
      requestLayout()
    }
  }

  fun setFloorRate(value: Float) {
    requestedFloorRate = value
    applyTwoLevelConfiguration()
  }

  fun setMaxRate(value: Float) {
    requestedMaxRate = value
    applyTwoLevelConfiguration()
  }

  fun setRefreshRate(value: Float) {
    requestedRefreshRate = value
    applyTwoLevelConfiguration()
  }

  fun setFloorDuration(value: Int) {
    requestedFloorDuration = value
    applyTwoLevelConfiguration()
  }

  fun setPullToCloseEnabled(value: Boolean) {
    pullToCloseEnabled = value
    applyTwoLevelConfiguration()
  }

  fun setBottomPullUpToCloseRate(value: Float) {
    requestedBottomPullUpToCloseRate = value
    applyTwoLevelConfiguration()
  }

  fun setPrimaryColor(value: Int?) {
    primaryColor = value
    applyClassicConfiguration()
  }

  fun setIndicatorColor(value: Int?) {
    indicatorColor = value
    applyClassicConfiguration()
  }

  fun setTitleColor(value: Int?) {
    titleColor = value
    applyClassicConfiguration()
  }

  fun setClassicEnableLastTime(value: Boolean) {
    classicEnableLastTime = value
    applyClassicConfiguration()
  }

  fun setPullDownText(value: String?) {
    pullDownText = value
    applyClassicConfiguration()
  }

  fun setReleaseToRefreshText(value: String?) {
    releaseToRefreshText = value
    applyClassicConfiguration()
  }

  fun setRefreshingText(value: String?) {
    refreshingText = value
    applyClassicConfiguration()
  }

  fun setRefreshCompleteText(value: String?) {
    refreshCompleteText = value
    applyClassicConfiguration()
  }

  fun beginRefresh(requestId: Int, delayMs: Int) {
    if (disposed || isSecondFloorActive()) return
    scheduleRefresh(RefreshOperation(requestId, "programmatic"), delayMs)
  }

  fun finishRefresh(requestId: Int, success: Boolean, delayMs: Int) {
    if (disposed || isSecondFloorActive()) return
    val active = activeRefresh
    val scheduled = scheduledRefresh
    val matchesActive = active?.requestId == requestId
    val matchesScheduled = scheduled?.requestId == requestId
    val visualOnly = requestId == 0 && active == null && scheduled == null
    if (!matchesActive && !matchesScheduled && !visualOnly) return

    postDelayedTracked(delayMs) {
      val currentActive = activeRefresh
      val currentScheduled = scheduledRefresh
      val stillActive = currentActive?.requestId == requestId
      val stillScheduled = currentScheduled?.requestId == requestId
      val stillVisualOnly = requestId == 0 && currentActive == null && currentScheduled == null
      if (!stillActive && !stillScheduled && !stillVisualOnly) return@postDelayedTracked

      if (stillScheduled) scheduledRefresh = null
      refreshingIntent = false
      super.finishRefresh(0, success, false)
      if (stillActive) activeRefresh = null
    }
  }

  fun openSecondFloor() {
    if (disposed || !canOpenSecondFloor() || refreshLayout.state != RefreshState.None) return
    lifecycle = SecondFloorLifecycle.OPENING
    // The imperative path starts the kernel's floor animation directly, so
    // it bypasses TwoLevelReleased where gesture opens begin this fade.
    animateInsetRefreshHeader(0f, configuredFloorDuration() / 2)
    setFloorContentAlpha(0f)
    animateFloorContent(1f, configuredFloorDuration() * 2)
    onStateChange?.invoke("second-floor-opening")
    // The listener was already checked above. Avoid calling it twice for an
    // imperative command while preserving the kernel's native animation.
    twoLevelHeader.openTwoLevel(false)
  }

  fun closeSecondFloor() {
    if (disposed) return
    closeSecondFloorInternal()
  }

  fun dispose() {
    if (disposed) return
    disposed = true
    delayedCallbacks.toList().forEach(::removeCallbacks)
    delayedCallbacks.clear()
    activeRefresh = null
    scheduledRefresh = null
    onRefresh = null
    onStateChange = null
    onSecondFloorOpen = null
    onSecondFloorClose = null
  }

  private fun attachContentSlot(slot: ExpoSmartSecondFloorContentSlotView) {
    if (slot.parent === refreshLayout && contentSlot === slot) return
    (slot.parent as? ViewGroup)?.removeView(slot)
    refreshLayout.setRefreshContent(slot)
  }

  private fun attachFloorSlot(slot: ExpoSmartSecondFloorFloorSlotView) {
    if (slot.parent === twoLevelHeader && floorSlot === slot) return
    (slot.parent as? ViewGroup)?.removeView(slot)
    twoLevelHeader.addView(
      slot,
      0,
      RelativeLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT,
      ),
    )
    twoLevelHeader.bringChildToFront(insetRefreshHeader)
    slot.post { positionFloorBackground(0) }
  }

  private fun attachFloorContentSlot(slot: ExpoSmartSecondFloorFloorContentSlotView) {
    if (slot.parent === twoLevelHeader && floorContentSlot === slot) return
    (slot.parent as? ViewGroup)?.removeView(slot)
    val classicIndex = twoLevelHeader.indexOfChild(insetRefreshHeader)
    twoLevelHeader.addView(
      slot,
      if (classicIndex >= 0) classicIndex else twoLevelHeader.childCount,
      RelativeLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT,
      ),
    )
    twoLevelHeader.bringChildToFront(insetRefreshHeader)
    setFloorContentAlpha(if (lifecycle == SecondFloorLifecycle.OPEN) 1f else 0f)
  }

  private fun applyEnabledState() {
    val hasContent = contentSlot != null
    val canOpenFloor = secondFloorEnabled && floorSlot != null
    twoLevelHeader.setEnableRefresh(refreshEnabled)
    twoLevelHeader.setEnableTwoLevel(canOpenFloor)
    // The parent must remain enabled for TwoLevelHeader to receive its native
    // close gestures even when ordinary refresh has been disabled.
    refreshLayout.setEnableRefresh(headerReady && hasContent && (refreshEnabled || canOpenFloor))
  }

  private fun applyTwoLevelConfiguration() {
    val maxRate = clampFinite(requestedMaxRate, DEFAULT_MAX_RATE, MIN_MAX_RATE, MAX_MAX_RATE)
    val floorRate = clampFinite(
      requestedFloorRate,
      DEFAULT_FLOOR_RATE,
      MIN_FLOOR_RATE,
      maxRate - RATE_GAP,
    )
    val refreshRate = clampFinite(
      requestedRefreshRate,
      DEFAULT_REFRESH_RATE,
      MIN_REFRESH_RATE,
      floorRate - RATE_GAP,
    )
    val floorDuration = requestedFloorDuration.coerceIn(0, MAX_FLOOR_DURATION)
    val bottomPullUpToCloseRate = clampFinite(
      requestedBottomPullUpToCloseRate,
      DEFAULT_BOTTOM_PULL_UP_TO_CLOSE_RATE,
      MIN_BOTTOM_PULL_UP_TO_CLOSE_RATE,
      MAX_BOTTOM_PULL_UP_TO_CLOSE_RATE,
    )

    twoLevelHeader
      .setMaxRate(maxRate)
      .setFloorRate(floorRate)
      .setRefreshRate(refreshRate)
      .setFloorDuration(floorDuration)
      .setEnablePullToCloseTwoLevel(pullToCloseEnabled)
      .setBottomPullUpToCloseRate(bottomPullUpToCloseRate)
    // TwoLevelHeader's own refreshRate participates in the transition back
    // from the floor threshold. The layout trigger controls ordinary refresh.
    refreshLayout.setHeaderTriggerRate(refreshRate)
  }

  private fun applyClassicConfiguration() {
    if (!::classicHeader.isInitialized) return
    val primary = primaryColor ?: Color.TRANSPARENT
    val indicator = indicatorColor ?: Color.DKGRAY
    val title = titleColor ?: Color.DKGRAY
    classicHeader.setEnableLastTime(classicEnableLastTime)
    classicHeader.setColors(primary, indicator, title)
    classicHeader.setMessages(
      pullDownText,
      releaseToRefreshText,
      refreshingText,
      refreshCompleteText,
      refreshLayout,
      refreshLayout.state,
    )
  }

  private fun onClassicHeaderInitialized(initializedHeader: RefreshHeader) {
    if (disposed || initializedHeader !== classicHeader) return
    headerReady = true
    applyEnabledState()
    applyClassicConfiguration()
    requestLayout()
    if (refreshingIntent && !isSecondFloorActive() && !refreshLayout.isRefreshing) {
      refreshLayout.autoRefreshAnimationOnly()
    }
    scheduledRefresh?.let(::startScheduledRefresh)
  }

  private fun scheduleRefresh(operation: RefreshOperation, delayMs: Int) {
    if (
      operation.requestId <= 0 ||
      activeRefresh != null ||
      scheduledRefresh != null ||
      isSecondFloorActive()
    ) return
    scheduledRefresh = operation
    postDelayedTracked(delayMs) { startScheduledRefresh(operation) }
  }

  private fun startScheduledRefresh(operation: RefreshOperation) {
    if (
      disposed ||
      scheduledRefresh != operation ||
      activeRefresh != null ||
      !canStartRefresh()
    ) return
    scheduledRefresh = null
    activeRefresh = operation
    refreshingIntent = true
    if (!refreshLayout.isRefreshing) refreshLayout.autoRefreshAnimationOnly()
    onRefresh?.invoke(operation.requestId, operation.source)
  }

  private fun beginGestureRefresh() {
    if (!canStartRefresh() || activeRefresh != null || scheduledRefresh != null) {
      refreshLayout.closeHeaderOrFooter()
      return
    }
    val operation = RefreshOperation(allocateGestureRequestId(), "gesture")
    activeRefresh = operation
    refreshingIntent = true
    onRefresh?.invoke(operation.requestId, operation.source)
  }

  private fun hasTrackedRefresh(): Boolean = activeRefresh != null || scheduledRefresh != null

  private fun canStartRefresh(): Boolean =
    !disposed &&
      refreshEnabled &&
      headerReady &&
      contentSlot != null &&
      !isSecondFloorActive()

  private fun canOpenSecondFloor(allowReleasedGesture: Boolean = false): Boolean =
    !disposed &&
      secondFloorEnabled &&
      floorSlot != null &&
      contentSlot != null &&
      activeRefresh == null &&
      scheduledRefresh == null &&
      !refreshingIntent &&
      !refreshLayout.isRefreshing &&
      (
        lifecycle == SecondFloorLifecycle.IDLE ||
          (allowReleasedGesture && lifecycle == SecondFloorLifecycle.RELEASE)
        )

  private fun isSecondFloorActive(): Boolean = lifecycle != SecondFloorLifecycle.IDLE

  private fun closeSecondFloorInternal() {
    when (refreshLayout.state) {
      RefreshState.TwoLevel -> twoLevelHeader.finishTwoLevel()
      RefreshState.ReleaseToTwoLevel,
      RefreshState.TwoLevelReleased -> refreshLayout.closeHeaderOrFooter()
      else -> {
        if (lifecycle == SecondFloorLifecycle.OPENING || lifecycle == SecondFloorLifecycle.RELEASE) {
          lifecycle = SecondFloorLifecycle.CLOSING
          onStateChange?.invoke("second-floor-closing")
          refreshLayout.closeHeaderOrFooter()
        }
      }
    }
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

  private fun reconcileChildOrder() {
    // TwoLevelHeader changes to MatchLayout after attachment. Keeping React
    // content in front is what hides the full-screen floor until the native
    // kernel translates that content during the opening gesture.
    contentSlot?.let(refreshLayout::bringChildToFront)
    twoLevelHeader.bringChildToFront(insetRefreshHeader)
  }

  private fun positionFloorBackground(offset: Int) {
    val floor = floorSlot ?: return
    if (floor.height == 0 || height == 0) return
    currentHeaderOffset = offset.coerceAtLeast(0)
    // Keep the revealed backdrop aligned with the content below an overlay
    // toolbar. The inset is part of the header's visible pull range.
    val top = minOf(
      currentHeaderOffset - floor.height + dpToPx(requestedHeaderInsetDp),
      height - floor.height,
    )
    floor.translationY = top.toFloat()
  }

  private fun setFloorContentAlpha(alpha: Float) {
    floorContentSlot?.let { slot ->
      slot.animate().cancel()
      slot.alpha = alpha
    }
  }

  private fun animateFloorContent(alpha: Float, durationMs: Int) {
    floorContentSlot?.let { slot ->
      slot.animate().cancel()
      if (durationMs == 0) {
        slot.alpha = alpha
      } else {
        slot.animate().alpha(alpha).setDuration(durationMs.toLong()).start()
      }
    }
  }

  private fun animateInsetRefreshHeader(alpha: Float, durationMs: Int) {
    insetRefreshHeader.animate().cancel()
    if (durationMs == 0) {
      insetRefreshHeader.alpha = alpha
    } else {
      insetRefreshHeader.animate().alpha(alpha).setDuration(durationMs.toLong()).start()
    }
  }

  private fun configuredFloorDuration(): Int =
    requestedFloorDuration.coerceIn(0, MAX_FLOOR_DURATION)

  private fun dpToPx(value: Int): Int =
    (value * resources.displayMetrics.density).roundToInt()

  private fun installListeners() {
    refreshLayout.setOnMultiListener(object : OnMultiListener {
      override fun onRefresh(refreshLayout: RefreshLayout) {
        beginGestureRefresh()
      }

      // OnMultiListener extends OnRefreshLoadMoreListener even when loading
      // more is disabled for the second-floor layout.
      override fun onLoadMore(refreshLayout: RefreshLayout) = Unit

      override fun onStateChanged(
        refreshLayout: RefreshLayout,
        oldState: RefreshState,
        newState: RefreshState,
      ) {
        if (
          hapticsEnabled &&
          (newState == RefreshState.ReleaseToRefresh || newState == RefreshState.ReleaseToTwoLevel)
        ) {
          performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
        }
        publishState(newState)
      }

      override fun onHeaderMoving(
        header: RefreshHeader?,
        isDragging: Boolean,
        percent: Float,
        offset: Int,
        headerHeight: Int,
        maxDragHeight: Int,
      ) {
        positionFloorBackground(offset)
      }

      override fun onFooterMoving(
        footer: com.scwang.smart.refresh.layout.api.RefreshFooter?,
        isDragging: Boolean,
        percent: Float,
        offset: Int,
        footerHeight: Int,
        maxDragHeight: Int,
      ) = Unit

      override fun onHeaderReleased(
        header: RefreshHeader?,
        headerHeight: Int,
        maxDragHeight: Int,
      ) = Unit

      override fun onHeaderStartAnimator(
        header: RefreshHeader?,
        headerHeight: Int,
        maxDragHeight: Int,
      ) = Unit

      override fun onHeaderFinish(header: RefreshHeader?, success: Boolean) = Unit

      override fun onFooterReleased(
        footer: com.scwang.smart.refresh.layout.api.RefreshFooter?,
        footerHeight: Int,
        maxDragHeight: Int,
      ) = Unit

      override fun onFooterStartAnimator(
        footer: com.scwang.smart.refresh.layout.api.RefreshFooter?,
        footerHeight: Int,
        maxDragHeight: Int,
      ) = Unit

      override fun onFooterFinish(
        footer: com.scwang.smart.refresh.layout.api.RefreshFooter?,
        success: Boolean,
      ) = Unit
    })
  }

  private fun publishState(state: RefreshState) {
    when (state) {
      RefreshState.ReleaseToTwoLevel -> {
        lifecycle = SecondFloorLifecycle.RELEASE
        setFloorContentAlpha(0f)
      }
      RefreshState.TwoLevelReleased -> {
        lifecycle = SecondFloorLifecycle.OPENING
        // The official demo starts the content cross-fade while the floor
        // expansion runs. Its default 1000ms floor duration uses a 2000ms
        // content fade, so retain that relationship for custom durations.
        animateFloorContent(1f, configuredFloorDuration() * 2)
      }
      RefreshState.TwoLevel -> {
        lifecycle = SecondFloorLifecycle.OPEN
        if (!secondFloorOpenedInCurrentCycle) {
          secondFloorOpenedInCurrentCycle = true
          onSecondFloorOpen?.invoke()
        }
      }
      RefreshState.TwoLevelFinish -> {
        lifecycle = SecondFloorLifecycle.CLOSING
        animateInsetRefreshHeader(1f, configuredFloorDuration() / 2)
        animateFloorContent(0f, configuredFloorDuration())
      }
      RefreshState.None -> {
        val wasOpen = secondFloorOpenedInCurrentCycle
        lifecycle = SecondFloorLifecycle.IDLE
        secondFloorOpenedInCurrentCycle = false
        animateInsetRefreshHeader(1f, 0)
        setFloorContentAlpha(0f)
        positionFloorBackground(0)
        onStateChange?.invoke("idle")
        if (wasOpen) onSecondFloorClose?.invoke()
        return
      }
      else -> {
        if (
          lifecycle == SecondFloorLifecycle.RELEASE ||
          lifecycle == SecondFloorLifecycle.OPENING ||
          lifecycle == SecondFloorLifecycle.CLOSING
        ) {
          lifecycle = SecondFloorLifecycle.IDLE
          animateInsetRefreshHeader(1f, 0)
          setFloorContentAlpha(0f)
        }
      }
    }
    onStateChange?.invoke(state.toPublicState())
  }

  private fun RefreshState.toPublicState(): String = when (this) {
    RefreshState.PullDownToRefresh -> "pulling"
    RefreshState.ReleaseToRefresh, RefreshState.RefreshReleased -> "ready"
    RefreshState.Refreshing, RefreshState.RefreshFinish -> "refreshing"
    RefreshState.ReleaseToTwoLevel -> "release-to-second-floor"
    RefreshState.TwoLevelReleased -> "second-floor-opening"
    RefreshState.TwoLevel -> "second-floor"
    RefreshState.TwoLevelFinish -> "second-floor-closing"
    else -> "idle"
  }

  private fun clampFinite(value: Float, fallback: Float, minimum: Float, maximum: Float): Float {
    val finiteValue = if (value.isFinite()) value else fallback
    return finiteValue.coerceIn(minimum, maximum)
  }

  private class ConfiguredTwoLevelHeader(context: Context) : TwoLevelHeader(context) {
    fun requestHeaderRemeasure() {
      // SmartRefreshLayout caches its measured header height. Reset the
      // TwoLevelHeader copy too so an inset update recomputes drag thresholds.
      mHeaderHeight = 0
      mRefreshKernel?.requestRemeasureHeightFor(this)
      requestLayout()
    }

    override fun onInitialized(
      kernel: com.scwang.smart.refresh.layout.api.RefreshKernel,
      height: Int,
      maxDragHeight: Int,
    ) {
      val wrappedHeader = mRefreshHeader
      super.onInitialized(kernel, height, maxDragHeight)
      if (wrappedHeader?.spinnerStyle == SpinnerStyle.Translate) {
        val params = wrappedHeader.view.layoutParams as? ViewGroup.MarginLayoutParams
        if (params != null) {
          // The library subtracts the header height from this margin during
          // its first initialization. The explicit layout below owns that
          // translate position, so retain a neutral margin across Fabric
          // remeasures instead of allowing it to accumulate or be discarded.
          params.topMargin = 0
          wrappedHeader.view.layoutParams = params
        }
      }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
      super.onLayout(changed, left, top, right, bottom)

      val wrappedHeader = mRefreshHeader ?: return
      if (wrappedHeader.spinnerStyle != SpinnerStyle.Translate) return

      val headerView = wrappedHeader.view
      val effectiveHeight = if (mHeaderHeight > 0) mHeaderHeight else headerView.measuredHeight
      if (effectiveHeight <= 0) return

      // TwoLevelHeader applies moveSpinner(offset) to this child. It must
      // therefore start exactly one effective header height above the host:
      // at offset=0 the inset and Classic content are hidden; at the refresh
      // threshold the Classic content is exposed below the fixed toolbar.
      val params = headerView.layoutParams as? ViewGroup.MarginLayoutParams
      val childLeft = params?.leftMargin ?: 0
      headerView.layout(
        childLeft,
        -effectiveHeight,
        childLeft + headerView.measuredWidth,
        -effectiveHeight + headerView.measuredHeight,
      )
    }

    override fun setFloorDuration(duration: Int): TwoLevelHeader {
      super.setFloorDuration(duration)
      mRefreshKernel?.requestFloorDuration(duration)
      return this
    }

    override fun setBottomPullUpToCloseRate(rate: Float): TwoLevelHeader {
      super.setBottomPullUpToCloseRate(rate)
      mRefreshKernel?.requestFloorBottomPullUpToCloseRate(rate)
      return this
    }
  }

  private companion object {
    const val DEFAULT_MAX_RATE = 2.5f
    const val DEFAULT_FLOOR_RATE = 1.9f
    const val DEFAULT_REFRESH_RATE = 1f
    const val DEFAULT_FLOOR_DURATION = 1000
    const val DEFAULT_BOTTOM_PULL_UP_TO_CLOSE_RATE = 1f / 6f
    const val MIN_MAX_RATE = 1.2f
    const val MAX_MAX_RATE = 5f
    const val MIN_FLOOR_RATE = 1.1f
    const val MIN_REFRESH_RATE = 0.25f
    const val RATE_GAP = 0.05f
    const val MIN_BOTTOM_PULL_UP_TO_CLOSE_RATE = 0.01f
    const val MAX_BOTTOM_PULL_UP_TO_CLOSE_RATE = 0.5f
    const val MAX_FLOOR_DURATION = 10_000
    const val MAX_HEADER_INSET_DP = 10_000
  }
}
