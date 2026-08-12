package com.exposmartrefreshlayout

import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.ViewGroupManager
import com.facebook.react.uimanager.ViewManagerDelegate
import com.facebook.react.viewmanagers.ExpoSmartRefreshHeaderSlotManagerDelegate
import com.facebook.react.viewmanagers.ExpoSmartRefreshHeaderSlotManagerInterface

@ReactModule(name = ExpoSmartRefreshHeaderSlotViewManager.NAME)
internal class ExpoSmartRefreshHeaderSlotViewManager :
  ViewGroupManager<ExpoSmartRefreshHeaderSlotView>(),
  ExpoSmartRefreshHeaderSlotManagerInterface<ExpoSmartRefreshHeaderSlotView> {

  private val delegate = ExpoSmartRefreshHeaderSlotManagerDelegate(this)

  override fun getName(): String = NAME

  override fun getDelegate(): ViewManagerDelegate<ExpoSmartRefreshHeaderSlotView> = delegate

  override fun createViewInstance(
    reactContext: ThemedReactContext
  ): ExpoSmartRefreshHeaderSlotView = ExpoSmartRefreshHeaderSlotView(reactContext)

  companion object {
    const val NAME = "ExpoSmartRefreshHeaderSlot"
  }
}
