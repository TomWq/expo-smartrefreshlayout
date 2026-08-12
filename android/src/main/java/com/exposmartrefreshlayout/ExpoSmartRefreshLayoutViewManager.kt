package com.exposmartrefreshlayout

import android.view.View
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.UIManagerHelper
import com.facebook.react.uimanager.ViewGroupManager
import com.facebook.react.uimanager.ViewManagerDelegate
import com.facebook.react.viewmanagers.ExpoSmartRefreshLayoutViewManagerDelegate
import com.facebook.react.viewmanagers.ExpoSmartRefreshLayoutViewManagerInterface

@ReactModule(name = ExpoSmartRefreshLayoutViewManager.NAME)
internal class ExpoSmartRefreshLayoutViewManager :
  ViewGroupManager<ExpoSmartRefreshLayoutView>(),
  ExpoSmartRefreshLayoutViewManagerInterface<ExpoSmartRefreshLayoutView> {

  private val delegate = ExpoSmartRefreshLayoutViewManagerDelegate(this)

  override fun getName(): String = NAME

  override fun getDelegate(): ViewManagerDelegate<ExpoSmartRefreshLayoutView> = delegate

  override fun createViewInstance(reactContext: ThemedReactContext): ExpoSmartRefreshLayoutView =
    ExpoSmartRefreshLayoutView(reactContext)

  override fun addView(parent: ExpoSmartRefreshLayoutView, child: View, index: Int) {
    // Fabric 的通用 addView 不理解 SmartRefreshLayout 的 RefreshContent 登记流程，交给组件专用入口处理。
    parent.addReactChild(child, index)
  }

  override fun getChildCount(parent: ExpoSmartRefreshLayoutView): Int = parent.getReactChildCount()

  override fun getChildAt(parent: ExpoSmartRefreshLayoutView, index: Int): View? =
    parent.getReactChildAt(index)

  override fun removeViewAt(parent: ExpoSmartRefreshLayoutView, index: Int) {
    parent.removeReactChild(index)
  }

  override fun addEventEmitters(
    reactContext: ThemedReactContext,
    view: ExpoSmartRefreshLayoutView
  ) {
    // 使用当前 Surface 和 React Tag 获取 dispatcher，确保事件被派发到正确的 Fabric 树实例。
    fun emit(name: String, payload: com.facebook.react.bridge.WritableMap = Arguments.createMap()) {
      UIManagerHelper.getEventDispatcherForReactTag(reactContext, view.id)?.dispatchEvent(
        SmartRefreshEvent(UIManagerHelper.getSurfaceId(view), view.id, name, payload)
      )
    }

    fun emitRequest(name: String, requestId: Int, source: String) {
      emit(name, Arguments.createMap().apply {
        putInt("requestId", requestId)
        putString("source", source)
      })
    }

    view.onRefresh = { requestId, source ->
      emitRequest("topRefresh", requestId, source)
    }
    view.onLoadMore = { requestId, source ->
      emitRequest("topLoadMore", requestId, source)
    }
    view.onStateChange = { state ->
      emit("topStateChange", Arguments.createMap().apply { putString("state", state) })
    }
    view.onHeaderMoving = { percent, offset, height, maxDragHeight, isDragging ->
      emit("topHeaderMoving", Arguments.createMap().apply {
        putDouble("percent", percent.toDouble())
        putInt("offset", offset)
        putInt("height", height)
        putInt("maxDragHeight", maxDragHeight)
        putBoolean("isDragging", isDragging)
      })
    }
  }

  override fun onDropViewInstance(view: ExpoSmartRefreshLayoutView) {
    // 卸载时先取消延迟命令并断开闭包，避免回调继续持有 ReactContext 或复用后的 view id。
    view.dispose()
    view.onRefresh = null
    view.onLoadMore = null
    view.onStateChange = null
    view.onHeaderMoving = null
    super.onDropViewInstance(view)
  }

  override fun setRefreshEnabled(view: ExpoSmartRefreshLayoutView, value: Boolean) = view.setRefreshEnabled(value)
  override fun setLoadMoreEnabled(view: ExpoSmartRefreshLayoutView, value: Boolean) = view.setLoadMoreEnabled(value)
  override fun setAutoLoadMoreEnabled(view: ExpoSmartRefreshLayoutView, value: Boolean) = view.setAutoLoadMoreEnabled(value)
  override fun setRefreshing(view: ExpoSmartRefreshLayoutView, value: Boolean) = view.setRefreshing(value)
  override fun setLoadingMore(view: ExpoSmartRefreshLayoutView, value: Boolean) = view.setLoadingMore(value)
  override fun setNoMoreData(view: ExpoSmartRefreshLayoutView, value: Boolean) =
    view.setNoMoreDataState(value)
  override fun setHapticsEnabled(view: ExpoSmartRefreshLayoutView, value: Boolean) = view.setHapticsEnabled(value)
  override fun setHeaderStyle(view: ExpoSmartRefreshLayoutView, value: String?) = view.setHeaderStyle(value)
  override fun setPrimaryColor(view: ExpoSmartRefreshLayoutView, value: Int?) = view.setPrimaryColor(value)
  override fun setIndicatorColor(view: ExpoSmartRefreshLayoutView, value: Int?) = view.setIndicatorColor(value)
  override fun setTitleColor(view: ExpoSmartRefreshLayoutView, value: Int?) = view.setTitleColor(value)
  override fun setClassicSpinnerStyle(view: ExpoSmartRefreshLayoutView, value: String?) =
    view.setClassicSpinnerStyle(value)
  override fun setClassicEnableLastTime(view: ExpoSmartRefreshLayoutView, value: Boolean) =
    view.setClassicEnableLastTime(value)
  override fun setMaterialShowBezierWave(view: ExpoSmartRefreshLayoutView, value: Boolean) =
    view.setMaterialShowBezierWave(value)
  override fun setMaterialEnableHeaderTranslationContent(
    view: ExpoSmartRefreshLayoutView,
    value: Boolean
  ) = view.setMaterialEnableHeaderTranslationContent(value)
  override fun setMaterialProgressBackgroundColor(
    view: ExpoSmartRefreshLayoutView,
    value: Int?
  ) = view.setMaterialProgressBackgroundColor(value)
  override fun setPullDownText(view: ExpoSmartRefreshLayoutView, value: String?) = view.setPullDownText(value)
  override fun setReleaseToRefreshText(view: ExpoSmartRefreshLayoutView, value: String?) = view.setReleaseToRefreshText(value)
  override fun setRefreshingText(view: ExpoSmartRefreshLayoutView, value: String?) = view.setRefreshingText(value)
  override fun setRefreshCompleteText(view: ExpoSmartRefreshLayoutView, value: String?) = view.setRefreshCompleteText(value)
  override fun setPullUpText(view: ExpoSmartRefreshLayoutView, value: String?) = view.setPullUpText(value)
  override fun setReleaseToLoadMoreText(view: ExpoSmartRefreshLayoutView, value: String?) = view.setReleaseToLoadMoreText(value)
  override fun setLoadingMoreText(view: ExpoSmartRefreshLayoutView, value: String?) = view.setLoadingMoreText(value)
  override fun setNoMoreDataText(view: ExpoSmartRefreshLayoutView, value: String?) = view.setNoMoreDataText(value)
  override fun beginRefresh(view: ExpoSmartRefreshLayoutView, requestId: Int, delayMs: Int) =
    view.beginRefresh(requestId, delayMs)
  // Fabric Codegen 将组件命令路由到具体 View 实例；requestId 用于拒绝已过期的异步结束命令。
  override fun finishRefresh(
    view: ExpoSmartRefreshLayoutView,
    requestId: Int,
    success: Boolean,
    delayMs: Int
  ) = view.finishRefresh(requestId, success, delayMs)
  override fun beginLoadMore(view: ExpoSmartRefreshLayoutView, requestId: Int, delayMs: Int) =
    view.beginLoadMore(requestId, delayMs)
  override fun finishLoadMore(
    view: ExpoSmartRefreshLayoutView,
    requestId: Int,
    success: Boolean,
    noMoreData: Boolean,
    delayMs: Int
  ) = view.finishLoadMore(requestId, success, noMoreData, delayMs)
  override fun resetNoMoreData(view: ExpoSmartRefreshLayoutView) = view.resetNoMoreDataState()

  override fun getExportedCustomDirectEventTypeConstants(): MutableMap<String, Any> =
    // 原生事件名必须与 Codegen 生成的 top* 名称、JS registrationName 同时对应。
    mutableMapOf(
      "topRefresh" to mutableMapOf("registrationName" to "onRefresh"),
      "topLoadMore" to mutableMapOf("registrationName" to "onLoadMore"),
      "topStateChange" to mutableMapOf("registrationName" to "onStateChange"),
      "topHeaderMoving" to mutableMapOf("registrationName" to "onHeaderMoving")
    )

  companion object {
    const val NAME = "ExpoSmartRefreshLayoutView"
  }
}
