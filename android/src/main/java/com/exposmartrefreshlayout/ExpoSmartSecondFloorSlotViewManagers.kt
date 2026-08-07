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
