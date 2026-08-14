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
 * Android 二楼交互的原生实现。React 只负责声明内容、背景和二楼内容三个槽位，
 * 槽位最终的原生层级与位置由 SmartRefreshLayout 管理。
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
  private var requestedTitleTextSize = DEFAULT_TITLE_TEXT_SIZE
  private var classicEnableLastTime = true
  private var pullDownText: String? = null
  private var releaseToRefreshText: String? = null
  private var refreshingText: String? = null
  private var refreshCompleteText: String? = null
  // TwoLevelHeader renders its own native hints on Android. Keep the public
  // strings here and render them through the wrapped ClassicsHeader because
  // this library version does not expose setters for TwoLevelHeader itself.
  private var pullToSecondFloorText: String? = null
  private var releaseToSecondFloorText: String? = null

  var onRefresh: ((Int, String) -> Unit)? = null
  var onStateChange: ((String) -> Unit)? = null
  var onSecondFloorOpen: (() -> Unit)? = null
  var onSecondFloorClose: (() -> Unit)? = null

  init {
    refreshLayout.apply {
      // TwoLevelHeader 会主动把全屏背景放到折叠 Header 上方。这里保留容器裁剪，
      // 防止背景越过当前 React 组件边界，覆盖页面标签等兄弟控件。
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

    classicHeader = ConfiguredClassicsHeader(
      context,
      { onClassicHeaderInitialized(it) },
      { twoLevelHeader.requestHeaderRemeasure() },
    )
    insetRefreshHeader = InsetRefreshHeader(context, classicHeader)
    twoLevelHeader.setRefreshHeader(insetRefreshHeader)
    twoLevelHeader.setOnTwoLevelListener {
      // TwoLevelHeader 在发布 ReleaseToTwoLevel 后才询问监听器，这是同一次手势的正常交接，
      // 不能按“重复打开”拒绝，所以允许 RELEASE 生命周期继续进入二楼。
      canOpenSecondFloor(allowReleasedGesture = true)
    }
    twoLevelHeader.onPullProgressChanged = { isDragging, percent ->
      updateSecondFloorHint(isDragging, percent)
    }
    refreshLayout.setRefreshHeader(twoLevelHeader)
    refreshLayout.setRefreshContent(emptyContent)
    applyTwoLevelConfiguration()
    applyClassicConfiguration()
    applyEnabledState()
    installListeners()
  }

  fun addReactChild(child: View, index: Int) {
    // Fabric 只知道这些槽位都是 React 子节点，真正的原生父节点却不同：普通内容属于刷新容器，
    // 背景和二楼内容属于 TwoLevelHeader。这里按槽位类型重新挂载，不能直接 addView。
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
    // React 卸载槽位时同步恢复 SmartRefreshLayout 的合法结构；尤其内容槽位必须换回占位 View，
    // 否则库内部仍可能持有已经从 Fabric 树移除的 RefreshContent。
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
      // Fabric 可能先提交非受控 refreshing=false，再送达对应 finishRefresh 命令。
      // 原生 requestId 必须保留到命令完成，避免正常的刷新完成反馈被跳过。
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
    // JS 传入的是 dp；先限制极端值再换算像素，并清除 Header 高度缓存，
    // 因为 inset 同时参与普通刷新与二楼阈值计算。
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

  fun setTitleTextSize(value: Float) {
    requestedTitleTextSize = clampFinite(
      value,
      DEFAULT_TITLE_TEXT_SIZE,
      MIN_TITLE_TEXT_SIZE,
      MAX_TITLE_TEXT_SIZE,
    )
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

  fun setPullToSecondFloorText(value: String?) {
    pullToSecondFloorText = value
    applySecondFloorMessages()
    refreshSecondFloorHint()
  }

  fun setReleaseToSecondFloorText(value: String?) {
    releaseToSecondFloorText = value
    applySecondFloorMessages()
    refreshSecondFloorHint()
  }

  fun beginRefresh(requestId: Int, delayMs: Int) {
    if (disposed || isSecondFloorActive()) return
    scheduleRefresh(RefreshOperation(requestId, "programmatic"), delayMs)
  }

  fun finishRefresh(requestId: Int, success: Boolean, delayMs: Int) {
    if (disposed || isSecondFloorActive()) return
    // 结束命令只允许命中同一个 requestId；0 保留给仅同步原生视觉、没有业务请求的场景。
    val active = activeRefresh
    val scheduled = scheduledRefresh
    val matchesActive = active?.requestId == requestId
    val matchesScheduled = scheduled?.requestId == requestId
    val visualOnly = requestId == 0 && active == null && scheduled == null
    if (!matchesActive && !matchesScheduled && !visualOnly) return

    postDelayedTracked(delayMs) {
      // 延迟结束期间可能已有新请求接管，执行前再校验一次，防止旧命令关闭新动画。
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
    // 命令式打开直接启动内核动画，不会经过手势路径的 TwoLevelReleased，
    // 所以此处必须主动补上 Header 与二楼内容的交叉淡入淡出。
    animateInsetRefreshHeader(0f, configuredFloorDuration() / 2)
    setFloorContentAlpha(0f)
    animateFloorContent(1f, configuredFloorDuration() * 2)
    onStateChange?.invoke("second-floor-opening")
    // 上方已经完成打开条件检查，传 false 避免命令式打开重复调用监听器，同时保留内核原生动画。
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
    // 槽位可能先被 Fabric 临时挂到别处，迁移前先从旧父节点脱离，避免 Android 重复父节点异常。
    if (slot.parent === refreshLayout && contentSlot === slot) return
    (slot.parent as? ViewGroup)?.removeView(slot)
    refreshLayout.setRefreshContent(slot)
  }

  private fun attachFloorSlot(slot: ExpoSmartSecondFloorFloorSlotView) {
    // 背景放在 TwoLevelHeader 最底层；刷新指示器最后置顶，保证下拉阶段仍可读且可交互。
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
    // 正式二楼内容位于背景与刷新指示器之间，打开完成前用透明度隐藏，避免折叠态提前透出。
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
    // 即使普通刷新关闭，父布局也必须保持可用，TwoLevelHeader 才能继续收到原生关闭手势。
    refreshLayout.setEnableRefresh(headerReady && hasContent && (refreshEnabled || canOpenFloor))
  }

  private fun applyTwoLevelConfiguration() {
    // 三个阈值必须满足 refreshRate < floorRate < maxRate。分别归一化并保留最小间隔，
    // 可防止 NaN、无穷大或错误顺序让 SmartRefreshLayout 进入无法释放的状态。
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
    // TwoLevelHeader 自己的 refreshRate 参与从二楼阈值退回的转换；父布局 trigger 才控制普通刷新。
    refreshLayout.setHeaderTriggerRate(refreshRate)
  }

  private fun applyClassicConfiguration() {
    if (!::classicHeader.isInitialized) return
    val primary = primaryColor ?: Color.TRANSPARENT
    val indicator = indicatorColor ?: Color.DKGRAY
    val title = titleColor ?: Color.DKGRAY
    classicHeader.setEnableLastTime(classicEnableLastTime)
    classicHeader.setColors(primary, indicator, title)
    classicHeader.setTitleTextSizeSp(requestedTitleTextSize)
    classicHeader.setMessages(
      pullDownText,
      releaseToRefreshText,
      refreshingText,
      refreshCompleteText,
      refreshLayout,
      refreshLayout.state,
    )
    applySecondFloorMessages()
    refreshSecondFloorHint()
  }

  private fun applySecondFloorMessages() {
    if (!::classicHeader.isInitialized) return
    classicHeader.setSecondFloorMessages(
      pullToSecondFloorText,
      releaseToSecondFloorText,
      refreshLayout,
      refreshLayout.state,
    )
  }

  private fun refreshSecondFloorHint() {
    updateSecondFloorHint(
      twoLevelHeader.isPullDragging,
      twoLevelHeader.pullPercent,
    )
  }

  private fun updateSecondFloorHint(isDragging: Boolean, percent: Float) {
    if (!::classicHeader.isInitialized) return

    val refreshThreshold = twoLevelHeader.refreshRate
    val floorThreshold = twoLevelHeader.floorRate
    val secondFloorHintThreshold = (refreshThreshold + floorThreshold) / 2f
    val title = when {
      !isDragging || !secondFloorEnabled || floorSlot == null -> null
      // 到达 floorRate 后 TwoLevelHeader 会发布 ReleaseToTwoLevel，官方
      // ClassicsHeader 自动渲染 mTextSecondary，不能再覆盖该状态文案。
      percent >= floorThreshold -> null
      percent > secondFloorHintThreshold -> pullToSecondFloorText
      else -> null
    }
    classicHeader.setTransientTitle(title, refreshLayout, refreshLayout.state)
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
    // scheduledRefresh 与 activeRefresh 是实例级请求锁；二楼活动期间也不能并发开始普通刷新。
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
    // 手势请求使用负数，与 JS 命令使用的正数 requestId 隔离，便于精确匹配结束命令。
    val current = nextGestureRequestId
    nextGestureRequestId = if (current == Int.MIN_VALUE) -1 else current - 1
    return current
  }

  private fun postDelayedTracked(delayMs: Int, action: () -> Unit) {
    // 记录所有延迟任务，Fabric 卸载视图时统一取消，避免回调触碰已销毁的原生层级。
    lateinit var runnable: Runnable
    runnable = Runnable {
      delayedCallbacks.remove(runnable)
      if (!disposed) action()
    }
    delayedCallbacks.add(runnable)
    postDelayed(runnable, delayMs.coerceAtLeast(0).toLong())
  }

  private fun reconcileChildOrder() {
    // TwoLevelHeader 挂载后会变成 MatchLayout。React 内容保持在前方，才能在内核随打开手势
    // 平移内容之前遮住全屏二楼背景。
    contentSlot?.let(refreshLayout::bringChildToFront)
    twoLevelHeader.bringChildToFront(insetRefreshHeader)
  }

  private fun positionFloorBackground(offset: Int) {
    val floor = floorSlot ?: return
    if (floor.height == 0 || height == 0) return
    currentHeaderOffset = offset.coerceAtLeast(0)
    // 让露出的背景与悬浮工具栏下方的内容对齐；header inset 本身就是可见下拉范围的一部分。
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

      // 二楼布局虽禁用加载更多，但 OnMultiListener 接口继承了加载监听，因此仍需提供空实现。
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
        refreshSecondFloorHint()
        // 官方效果会在二楼展开时同步开始内容淡入，默认 1000ms 展开对应 2000ms 淡入；
        // 自定义时长也保持这一比例，避免内容突兀出现。
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
        refreshSecondFloorHint()
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
    // coerceIn 无法修复 NaN，需先回退默认值，再进行上下界约束。
    val finiteValue = if (value.isFinite()) value else fallback
    return finiteValue.coerceIn(minimum, maximum)
  }

  private class ConfiguredTwoLevelHeader(context: Context) : TwoLevelHeader(context) {
    var onPullProgressChanged: ((isDragging: Boolean, percent: Float) -> Unit)? = null
    var isPullDragging = false
      private set
    var pullPercent = 0f
      private set

    val refreshRate: Float
      get() = mRefreshRate
    val floorRate: Float
      get() = mFloorRate

    override fun onMoving(
      isDragging: Boolean,
      percent: Float,
      offset: Int,
      height: Int,
      maxDragHeight: Int,
    ) {
      super.onMoving(isDragging, percent, offset, height, maxDragHeight)
      isPullDragging = isDragging
      pullPercent = percent
      onPullProgressChanged?.invoke(isDragging, percent)
    }

    fun requestHeaderRemeasure() {
      // SmartRefreshLayout 会缓存测量后的 Header 高度；同时清空 TwoLevelHeader 的副本，
      // 才能在 inset 变化后重新计算拖拽阈值。
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
          // 库首次初始化时会从该 margin 中减去 Header 高度，而下方显式布局已经负责 Translate 位置。
          // 因此 Fabric 重测时始终恢复零 margin，避免偏移累计或丢失。
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

      // TwoLevelHeader 会对该子节点执行 moveSpinner(offset)，所以初始位置必须恰好在容器上方一个
      // 有效 Header 高度：offset=0 时隐藏 inset 和 Classic 内容，到刷新阈值时才露出固定工具栏下方。
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
    const val DEFAULT_TITLE_TEXT_SIZE = 15f
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
    const val MIN_TITLE_TEXT_SIZE = 8f
    const val MAX_TITLE_TEXT_SIZE = 40f
  }
}
