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
import kotlin.math.roundToInt

internal class ExpoSmartRefreshLayoutView(
  context: ThemedReactContext
) : SmartRefreshLayout(context) {
  private enum class OperationKind { REFRESH, LOAD_MORE }

  private data class Operation(
    val kind: OperationKind,
    val requestId: Int,
    val source: String
  )

  // React 内容必须与 SmartRefreshLayout 的 Header/Footer 保持在同一个原生 ViewGroup 中。
  // FixedBehind 会依赖这种兄弟节点关系平移内容并裁剪 Header 的露出区域，因此这里不能再包一层容器。
  private val refreshLayout: SmartRefreshLayout
    get() = this
  private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
  private val delayedCallbacks = mutableSetOf<Runnable>()
  private val reactChildren = mutableListOf<View>()
  private var reactChild: View? = null
  private var headerSlot: ExpoSmartRefreshHeaderSlotView? = null
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
  var onHeaderMoving: ((Float, Int, Int, Int, Boolean) -> Unit)? = null

  init {
    refreshLayout.apply {
      setDragRate(0.5f)
      setEnableOverScrollDrag(true)
      setEnableOverScrollBounce(true)
      setEnableNestedScroll(true)
      setEnableLoadMoreWhenContentNotFull(true)
      setEnableScrollContentWhenRefreshed(true)
      setEnableScrollContentWhenLoaded(true)
      // 与官方 Classics 示例保持一致：FixedBehind 只绘制在已露出的区域，内容则随拖拽让出空间。
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
    // 自动加载不能仅凭“已经滚到底部”触发，否则首屏不足时布局阶段也可能误发请求。
    // 只有用户真实向上滑过系统触摸阈值，且内容确实超过一屏，才临时放开库内的自动加载开关。
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
    // SmartRefreshLayout 默认关闭子节点裁剪，而 React Native 外层 View 不会补回 XML 父布局的视口裁剪。
    // 因此必须在这里裁剪画布，避免被平移的 RefreshContent 绘制到相邻控件上。
    val saveCount = canvas.save()
    canvas.clipRect(0, 0, width, height)
    super.dispatchDraw(canvas)
    canvas.restoreToCount(saveCount)
  }

  fun addReactChild(child: View, index: Int) {
    check(child is ExpoSmartRefreshHeaderSlotView || reactChild == null) {
      "SmartRefreshLayout accepts one refreshHeader slot and one scroll-content child."
    }
    check(index in 0..reactChildren.size) {
      "SmartRefreshLayout received an invalid child index."
    }
    if (reactChildren.contains(child)) return
    reactChildren.add(index, child)

    if (child is ExpoSmartRefreshHeaderSlotView) {
      headerSlot = child
      (child.parent as? ViewGroup)?.removeView(child)
      requestHeaderRebuild()
    } else {
      reactChild = child
      refreshLayout.setRefreshContent(child)
    }
    requestLayout()
  }

  fun getReactChildCount(): Int = reactChildren.size

  fun getReactChildAt(index: Int): View? = reactChildren.getOrNull(index)

  fun removeReactChild(index: Int) {
    val child = reactChildren.getOrNull(index) ?: return
    reactChildren.removeAt(index)
    if (child is ExpoSmartRefreshHeaderSlotView) {
      if (headerSlot === child) {
        if (child.parent is ViewGroup) (child.parent as ViewGroup).removeView(child)
        headerSlot = null
        requestHeaderRebuild()
      }
    } else if (reactChild === child) {
      refreshLayout.removeView(child)
      reactChild = null
    }
    requestLayout()
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
      // Fabric 可能先提交非受控 refreshing=false，随后才送达对应的 finishRefresh 命令。
      // 请求记录必须保留到命令完成，否则 requestId 会被拒绝，Translate Header 也会在展示完成态前关闭。
      if (
        !hasTrackedOperation(OperationKind.REFRESH) &&
        refreshLayout.state != RefreshState.RefreshFinish &&
        wasRefreshActive &&
        !wasLoadMoreActive
      ) {
        // finishRefresh 只处理完全展开的 Refreshing 状态。React 也可能在展开/回弹动画仍处于
        // PullDownToRefresh 时清空属性；closeHeaderOrFooter 能覆盖这些纯视觉状态，确保 Header 收回内容上方。
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
      // 与 refreshing 相同，Fabric 的属性提交和命令到达顺序不固定，加载请求也要保留到 finish 命令处理完。
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
    onHeaderMoving = null
  }

  private fun scheduleOperation(operation: Operation, delayMs: Int) {
    // activeOperation 与 scheduledOperation 共同构成实例级互斥锁，防止刷新和加载更多交叉执行。
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
    // requestId 将结束命令绑定到发起它的那次请求；0 仅用于没有 JS 请求记录的纯视觉同步。
    val scheduled = scheduledOperation
    val active = activeOperation
    val matchesScheduled = scheduled?.kind == kind && scheduled.requestId == requestId
    val matchesActive = active?.kind == kind && active.requestId == requestId
    val visualOnly = requestId == 0 && active == null && scheduled == null
    if (!matchesScheduled && !matchesActive && !visualOnly) return

    postDelayedTracked(delayMs) {
      // 延迟期间可能已经开始新请求，所以执行前必须再次比对 requestId。
      // 旧回调直接失效，不能把后来一次请求的动画和锁一并结束。
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
    // 手势请求与命令请求共用同一把锁，避免用户连续拖拽绕过 JS 层的请求保护。
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
    // 手势使用负数，程序化命令使用正数，两类请求无需共享 JS 侧的递增序列也不会碰撞。
    val current = nextGestureRequestId
    nextGestureRequestId = if (current == Int.MIN_VALUE) -1 else current - 1
    return current
  }

  private fun postDelayedTracked(delayMs: Int, action: () -> Unit) {
    // 集中登记延迟任务，视图从 Fabric 树卸载后可一次取消，避免回调继续访问已销毁实例。
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
    // “已启用”与“本次手势已解锁”必须同时满足；完成请求或反向拖动后会重新上锁。
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

    // 仅切换 Classic spinnerStyle 时会有意复用 Header 实例。ClassicsAbstract 会在首次正常测量时缓存
    // 内容基线，而 Scale 的空闲高度为零，重建实例会丢失这条基线。
    val nextHeader: RefreshHeader = if (headerSlot != null) {
      val slot = requireNotNull(headerSlot)
      if (slot.parent is ViewGroup) (slot.parent as ViewGroup).removeView(slot)
      SlotRefreshHeader(context, slot)
    } else if (headerStyle == "material") {
      MaterialHeader(context)
    } else {
      ConfiguredClassicsHeader(context).apply {
        setSpinnerStyle(resolveClassicSpinnerStyle())
      }
    }

    header = nextHeader
    installedHeaderStyle = headerStyle
    if (nextHeader is SlotRefreshHeader) {
      refreshLayout.setRefreshHeader(nextHeader, 0, customHeaderHeightPx())
    } else {
      refreshLayout.setRefreshHeader(nextHeader)
    }
    applyHeaderConfiguration()
    applyMessages()
    applyColors()
  }

  private fun requestHeaderRebuild() {
    // Header 在拖拽或刷新过程中被替换会破坏库内 spinner 状态，因此配置先标记为待处理，空闲后再应用。
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
    // Footer 重建后需要恢复 no-more-data 或 loading 的原生视觉，否则 React 属性虽然正确，界面会回到空闲态。
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
      is SlotRefreshHeader -> {
        refreshLayout.setEnableHeaderTranslationContent(true)
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

    // SmartRefreshLayout 的 Scale 分支会让空闲 Header 保持零高度，拖拽时再按 spinner 高度测量。
    // 这里保留初始化后的完整宽度，避免 Fabric 下替换出来的 View 从 0xN 尺寸开始布局。
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
    // moveSpinner 仅在 spinner 数值变化时更新 Translate Header。RefreshFinish 会先于下一次位移发布，
    // 若此处把 translationY 清零，完成文案会在整个延迟期间被藏到内容上方。
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

  private fun headerMatchesRequestedConfiguration(): Boolean {
    val currentHeader = header ?: return false
    return if (headerSlot != null) {
      currentHeader is SlotRefreshHeader && currentHeader.slotHost === headerSlot
    } else {
      installedHeaderStyle == headerStyle && currentHeader !is SlotRefreshHeader
    }
  }

  private fun customHeaderHeightPx(): Int =
    (CUSTOM_HEADER_HEIGHT_DP * resources.displayMetrics.density).toInt().coerceAtLeast(1)

  // SmartRefreshLayout reports physical pixels, while React Native layout and
  // the iOS implementation expose logical pixels through onHeaderMoving.
  private fun pxToDp(value: Int): Int =
    (value / resources.displayMetrics.density).roundToInt()

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
        // Classics TextView 改文字时，Fabric 已经完成这个原生根节点的布局。
        // 只重测当前 Header，避免“正在刷新...”等较长状态文案沿用下拉提示的较窄子节点宽度。
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
      ) {
        onHeaderMoving?.invoke(
          percent,
          pxToDp(offset),
          pxToDp(headerHeight),
          pxToDp(maxDragHeight),
          isDragging
        )
      }

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
    const val CUSTOM_HEADER_HEIGHT_DP = 80f
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
