package com.exposmartrefreshlayout

import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.ViewGroupManager
import com.facebook.react.uimanager.ViewManagerDelegate
import com.facebook.react.viewmanagers.ExpoSmartSecondFloorContentSlotManagerDelegate
import com.facebook.react.viewmanagers.ExpoSmartSecondFloorContentSlotManagerInterface
import com.facebook.react.viewmanagers.ExpoSmartSecondFloorFloorSlotManagerDelegate
import com.facebook.react.viewmanagers.ExpoSmartSecondFloorFloorSlotManagerInterface
import com.facebook.react.viewmanagers.ExpoSmartSecondFloorFloorContentSlotManagerDelegate
import com.facebook.react.viewmanagers.ExpoSmartSecondFloorFloorContentSlotManagerInterface

@ReactModule(name = ExpoSmartSecondFloorContentSlotViewManager.NAME)
// 槽位 Manager 本身不做布局；它只让 Fabric 创建可识别的宿主，真实挂载由二楼根组件接管。
internal class ExpoSmartSecondFloorContentSlotViewManager :
  ViewGroupManager<ExpoSmartSecondFloorContentSlotView>(),
  ExpoSmartSecondFloorContentSlotManagerInterface<ExpoSmartSecondFloorContentSlotView> {

  private val delegate = ExpoSmartSecondFloorContentSlotManagerDelegate(this)

  override fun getName(): String = NAME

  override fun getDelegate(): ViewManagerDelegate<ExpoSmartSecondFloorContentSlotView> = delegate

  override fun createViewInstance(
    reactContext: ThemedReactContext
  ): ExpoSmartSecondFloorContentSlotView = ExpoSmartSecondFloorContentSlotView(reactContext)

  companion object {
    const val NAME = "ExpoSmartSecondFloorContentSlot"
  }
}

@ReactModule(name = ExpoSmartSecondFloorFloorSlotViewManager.NAME)
// 背景槽位会被根组件放到 TwoLevelHeader 底层，用于下拉阶段逐步揭露。
internal class ExpoSmartSecondFloorFloorSlotViewManager :
  ViewGroupManager<ExpoSmartSecondFloorFloorSlotView>(),
  ExpoSmartSecondFloorFloorSlotManagerInterface<ExpoSmartSecondFloorFloorSlotView> {

  private val delegate = ExpoSmartSecondFloorFloorSlotManagerDelegate(this)

  override fun getName(): String = NAME

  override fun getDelegate(): ViewManagerDelegate<ExpoSmartSecondFloorFloorSlotView> = delegate

  override fun createViewInstance(
    reactContext: ThemedReactContext
  ): ExpoSmartSecondFloorFloorSlotView = ExpoSmartSecondFloorFloorSlotView(reactContext)

  companion object {
    const val NAME = "ExpoSmartSecondFloorFloorSlot"
  }
}

@ReactModule(name = ExpoSmartSecondFloorFloorContentSlotViewManager.NAME)
// 二楼内容槽位独立于背景，便于原生开合生命周期单独控制其透明度。
internal class ExpoSmartSecondFloorFloorContentSlotViewManager :
  ViewGroupManager<ExpoSmartSecondFloorFloorContentSlotView>(),
  ExpoSmartSecondFloorFloorContentSlotManagerInterface<ExpoSmartSecondFloorFloorContentSlotView> {

  private val delegate = ExpoSmartSecondFloorFloorContentSlotManagerDelegate(this)

  override fun getName(): String = NAME

  override fun getDelegate(): ViewManagerDelegate<ExpoSmartSecondFloorFloorContentSlotView> = delegate

  override fun createViewInstance(
    reactContext: ThemedReactContext
  ): ExpoSmartSecondFloorFloorContentSlotView =
    ExpoSmartSecondFloorFloorContentSlotView(reactContext)

  companion object {
    const val NAME = "ExpoSmartSecondFloorFloorContentSlot"
  }
}
