package com.exposmartrefreshlayout

import android.content.Context
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.ClassicsHeader

internal class ConfiguredClassicsHeader(context: Context) : ClassicsHeader(context) {
  fun setMessages(
    pulling: String?,
    release: String?,
    refreshing: String?,
    complete: String?
  ) {
    pulling?.let { mTextPulling = it }
    release?.let { mTextRelease = it }
    refreshing?.let { mTextRefreshing = it }
    complete?.let { mTextFinish = it }
  }

  fun setColors(indicatorColor: Int, titleColor: Int) {
    setAccentColor(indicatorColor)
    mTitleText.setTextColor(titleColor)
    mLastUpdateText.setTextColor(titleColor)
  }
}

internal class ConfiguredClassicsFooter(context: Context) : ClassicsFooter(context) {
  fun setMessages(
    pulling: String?,
    release: String?,
    loading: String?,
    noMoreData: String?
  ) {
    pulling?.let { mTextPulling = it }
    release?.let { mTextRelease = it }
    loading?.let {
      mTextLoading = it
      mTextRefreshing = it
    }
    noMoreData?.let { mTextNothing = it }
  }

  fun setColors(indicatorColor: Int, titleColor: Int) {
    setAccentColor(indicatorColor)
    mTitleText.setTextColor(titleColor)
  }
}
