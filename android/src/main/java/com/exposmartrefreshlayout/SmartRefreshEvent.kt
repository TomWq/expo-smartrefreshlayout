package com.exposmartrefreshlayout

import com.facebook.react.bridge.WritableMap
import com.facebook.react.uimanager.events.Event

internal class SmartRefreshEvent(
  surfaceId: Int,
  viewId: Int,
  private val name: String,
  private val payload: WritableMap
) : Event<SmartRefreshEvent>(surfaceId, viewId) {
  // name 由各 Manager 传入，使刷新、加载和状态变化共用同一套 Fabric 事件封装。
  override fun getEventName(): String = name

  override fun getEventData(): WritableMap = payload

  // 请求与状态事件都具有顺序语义；禁止合并，避免中间状态或某个 requestId 被 Fabric 丢弃。
  override fun canCoalesce(): Boolean = false
}
