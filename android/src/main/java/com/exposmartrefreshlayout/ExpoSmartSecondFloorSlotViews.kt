package com.exposmartrefreshlayout

import android.content.Context
import com.facebook.react.views.view.ReactViewGroup

/**
 * Distinct host classes make the React children addressable by slot rather
 * than by Fabric mount order. The parent reparents them into SmartRefreshLayout
 * and TwoLevelHeader respectively.
 */
internal class ExpoSmartSecondFloorContentSlotView(context: Context) : ReactViewGroup(context)

internal class ExpoSmartSecondFloorFloorSlotView(context: Context) : ReactViewGroup(context)

internal class ExpoSmartSecondFloorFloorContentSlotView(context: Context) : ReactViewGroup(context)
