package com.exposmartrefreshlayout

import com.facebook.react.bridge.WritableMap
import com.facebook.react.uimanager.events.Event

internal class SmartRefreshEvent(
  surfaceId: Int,
  viewId: Int,
  private val name: String,
  private val payload: WritableMap
) : Event<SmartRefreshEvent>(surfaceId, viewId) {
  override fun getEventName(): String = name

  override fun getEventData(): WritableMap = payload

  override fun canCoalesce(): Boolean = false
}
