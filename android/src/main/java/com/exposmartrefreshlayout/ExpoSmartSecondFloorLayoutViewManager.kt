package com.exposmartrefreshlayout

import android.view.View
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.UIManagerHelper
import com.facebook.react.uimanager.ViewGroupManager
import com.facebook.react.uimanager.ViewManagerDelegate
import com.facebook.react.viewmanagers.ExpoSmartSecondFloorLayoutViewManagerDelegate
import com.facebook.react.viewmanagers.ExpoSmartSecondFloorLayoutViewManagerInterface

@ReactModule(name = ExpoSmartSecondFloorLayoutViewManager.NAME)
internal class ExpoSmartSecondFloorLayoutViewManager :
  ViewGroupManager<ExpoSmartSecondFloorLayoutView>(),
  ExpoSmartSecondFloorLayoutViewManagerInterface<ExpoSmartSecondFloorLayoutView> {

  private val delegate = ExpoSmartSecondFloorLayoutViewManagerDelegate(this)

  override fun getName(): String = NAME

  override fun getDelegate(): ViewManagerDelegate<ExpoSmartSecondFloorLayoutView> = delegate

  override fun createViewInstance(
    reactContext: ThemedReactContext
  ): ExpoSmartSecondFloorLayoutView = ExpoSmartSecondFloorLayoutView(reactContext)

  override fun addView(parent: ExpoSmartSecondFloorLayoutView, child: View, index: Int) {
    // 三种 Fabric 槽位需要被重新挂到不同原生父节点，由组件按类型完成真实挂载。
    parent.addReactChild(child, index)
  }

  override fun getChildCount(parent: ExpoSmartSecondFloorLayoutView): Int = parent.getReactChildCount()

  override fun getChildAt(parent: ExpoSmartSecondFloorLayoutView, index: Int): View? =
    parent.getReactChildAt(index)

  override fun removeViewAt(parent: ExpoSmartSecondFloorLayoutView, index: Int) {
    parent.removeReactChild(index)
  }

  override fun addEventEmitters(
    reactContext: ThemedReactContext,
    view: ExpoSmartSecondFloorLayoutView,
  ) {
    // 事件携带当前 SurfaceId，避免同一页面存在多个 Fabric Root 时被投递到错误根节点。
    fun emit(name: String, payload: com.facebook.react.bridge.WritableMap = Arguments.createMap()) {
      UIManagerHelper.getEventDispatcherForReactTag(reactContext, view.id)?.dispatchEvent(
        SmartRefreshEvent(UIManagerHelper.getSurfaceId(view), view.id, name, payload),
      )
    }

    view.onRefresh = { requestId, source ->
      emit("topRefresh", Arguments.createMap().apply {
        putInt("requestId", requestId)
        putString("source", source)
      })
    }
    view.onStateChange = { state ->
      emit("topStateChange", Arguments.createMap().apply { putString("state", state) })
    }
    view.onSecondFloorOpen = { emit("topSecondFloorOpen") }
    view.onSecondFloorClose = { emit("topSecondFloorClose") }
  }

  override fun onDropViewInstance(view: ExpoSmartSecondFloorLayoutView) {
    // dispose 会取消延迟刷新并清空事件闭包，必须先于基类释放原生 View。
    view.dispose()
    super.onDropViewInstance(view)
  }

  override fun setRefreshEnabled(view: ExpoSmartSecondFloorLayoutView, value: Boolean) =
    view.setRefreshEnabled(value)

  override fun setRefreshing(view: ExpoSmartSecondFloorLayoutView, value: Boolean) =
    view.setRefreshing(value)

  override fun setHapticsEnabled(view: ExpoSmartSecondFloorLayoutView, value: Boolean) =
    view.setHapticsEnabled(value)

  override fun setSecondFloorEnabled(view: ExpoSmartSecondFloorLayoutView, value: Boolean) =
    view.setSecondFloorEnabled(value)

  override fun setHeaderInset(view: ExpoSmartSecondFloorLayoutView, value: Int) =
    view.setHeaderInset(value)

  override fun setFloorRate(view: ExpoSmartSecondFloorLayoutView, value: Float) = view.setFloorRate(value)

  override fun setMaxRate(view: ExpoSmartSecondFloorLayoutView, value: Float) = view.setMaxRate(value)

  override fun setRefreshRate(view: ExpoSmartSecondFloorLayoutView, value: Float) = view.setRefreshRate(value)

  override fun setFloorDuration(view: ExpoSmartSecondFloorLayoutView, value: Int) =
    view.setFloorDuration(value)

  override fun setPullToCloseEnabled(view: ExpoSmartSecondFloorLayoutView, value: Boolean) =
    view.setPullToCloseEnabled(value)

  override fun setBottomPullUpToCloseRate(
    view: ExpoSmartSecondFloorLayoutView,
    value: Float,
  ) = view.setBottomPullUpToCloseRate(value)

  override fun setPrimaryColor(view: ExpoSmartSecondFloorLayoutView, value: Int?) =
    view.setPrimaryColor(value)

  override fun setIndicatorColor(view: ExpoSmartSecondFloorLayoutView, value: Int?) =
    view.setIndicatorColor(value)

  override fun setTitleColor(view: ExpoSmartSecondFloorLayoutView, value: Int?) =
    view.setTitleColor(value)

  override fun setClassicEnableLastTime(view: ExpoSmartSecondFloorLayoutView, value: Boolean) =
    view.setClassicEnableLastTime(value)

  override fun setPullDownText(view: ExpoSmartSecondFloorLayoutView, value: String?) =
    view.setPullDownText(value)

  override fun setReleaseToRefreshText(view: ExpoSmartSecondFloorLayoutView, value: String?) =
    view.setReleaseToRefreshText(value)

  override fun setRefreshingText(view: ExpoSmartSecondFloorLayoutView, value: String?) =
    view.setRefreshingText(value)

  override fun setRefreshCompleteText(view: ExpoSmartSecondFloorLayoutView, value: String?) =
    view.setRefreshCompleteText(value)

  override fun beginRefresh(view: ExpoSmartSecondFloorLayoutView, requestId: Int, delayMs: Int) =
    view.beginRefresh(requestId, delayMs)

  // 所有命令都由 Codegen 定位到组件实例；刷新结束仍依靠 requestId 防止旧命令覆盖新请求。
  override fun finishRefresh(
    view: ExpoSmartSecondFloorLayoutView,
    requestId: Int,
    success: Boolean,
    delayMs: Int,
  ) = view.finishRefresh(requestId, success, delayMs)

  override fun openSecondFloor(view: ExpoSmartSecondFloorLayoutView) = view.openSecondFloor()

  override fun closeSecondFloor(view: ExpoSmartSecondFloorLayoutView) = view.closeSecondFloor()

  override fun getExportedCustomDirectEventTypeConstants(): MutableMap<String, Any> =
    // top* 是原生直接事件名，registrationName 是 JS Props 上公开的回调名，两侧必须成对维护。
    mutableMapOf(
      "topRefresh" to mutableMapOf("registrationName" to "onRefresh"),
      "topStateChange" to mutableMapOf("registrationName" to "onStateChange"),
      "topSecondFloorOpen" to mutableMapOf("registrationName" to "onSecondFloorOpen"),
      "topSecondFloorClose" to mutableMapOf("registrationName" to "onSecondFloorClose"),
    )

  companion object {
    const val NAME = "ExpoSmartSecondFloorLayoutView"
  }
}
