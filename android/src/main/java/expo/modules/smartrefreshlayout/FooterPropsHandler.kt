package expo.modules.smartrefreshlayout

import android.content.res.Resources
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.layout.SmartRefreshLayout
import com.scwang.smart.refresh.layout.constant.RefreshState as NativeRefreshState

/**
 * Footer 属性处理器
 * 负责处理经典样式 Footer 的所有属性设置
 */
class FooterPropsHandler(
    private val srl: SmartRefreshLayout,
    private val resources: Resources
) {
    
    /**
     * 应用 Footer 属性
     */
    fun applyProps(props: Map<String, Any?>?) {
        android.util.Log.d("SmartRefresh", "FooterPropsHandler.applyProps 被调用，props = $props")
        props ?: return
        
        val footer = srl.refreshFooter
        if (footer !is ClassicsFooter) {
            android.util.Log.w("SmartRefresh", "Footer 不是 ClassicsFooter 类型，类型 = ${footer?.javaClass?.simpleName}")
            return
        }
        
        try {
            android.util.Log.d("SmartRefresh", "开始应用 Footer 属性，当前状态: ${srl.state}")
            
            // 设置文字内容
            setTextContent(footer, props)
            
            // 强制刷新 Footer 显示
            footer.onStateChanged(srl, NativeRefreshState.None, srl.state)
            
            // 调整 TextView 宽度
            adjustTextViewWidth(footer)
            
            // 设置颜色
            setColors(footer, props)
            
            // 设置字体大小
            setTextSize(footer, props)
            
            // 设置其他属性
            setOtherProps(footer, props)
            
            // 设置箭头大小
            setArrowSize(footer, props)
            
            // 设置间距
            setMargin(footer, props)
            
            android.util.Log.d("SmartRefresh", "Footer 属性已全部更新完成")
        } catch (e: Throwable) {
            android.util.Log.e("SmartRefresh", "设置 Footer 属性失败", e)
            e.printStackTrace()
        }
    }
    
    /**
     * 设置文字内容
     */
    private fun setTextContent(footer: ClassicsFooter, props: Map<String, Any?>) {
        val textFields = mapOf(
            "REFRESH_FOOTER_PULLING" to "mTextPulling",
            "REFRESH_FOOTER_LOADING" to "mTextLoading",
            "REFRESH_FOOTER_REFRESHING" to "mTextRefreshing",
            "REFRESH_FOOTER_RELEASE" to "mTextRelease",
            "REFRESH_FOOTER_FINISH" to "mTextFinish",
            "REFRESH_FOOTER_FAILED" to "mTextFailed",
            "REFRESH_FOOTER_NOTHING" to "mTextNothing"
        )
        
        textFields.forEach { (propKey, fieldName) ->
            props[propKey]?.let { value ->
                if (value is String) {
                    try {
                        val field = ClassicsFooter::class.java.getDeclaredField(fieldName)
                        field.isAccessible = true
                        field.set(footer, value)
                        android.util.Log.d("SmartRefresh", "设置 Footer $fieldName = $value")
                        
                        // 同时修改静态常量
                        when (propKey) {
                            "REFRESH_FOOTER_PULLING" -> 
                                com.scwang.smart.refresh.footer.ClassicsFooter.REFRESH_FOOTER_PULLING = value
                            "REFRESH_FOOTER_LOADING" -> 
                                com.scwang.smart.refresh.footer.ClassicsFooter.REFRESH_FOOTER_LOADING = value
                            "REFRESH_FOOTER_REFRESHING" -> 
                                com.scwang.smart.refresh.footer.ClassicsFooter.REFRESH_FOOTER_REFRESHING = value
                            "REFRESH_FOOTER_RELEASE" -> 
                                com.scwang.smart.refresh.footer.ClassicsFooter.REFRESH_FOOTER_RELEASE = value
                            "REFRESH_FOOTER_FINISH" -> 
                                com.scwang.smart.refresh.footer.ClassicsFooter.REFRESH_FOOTER_FINISH = value
                            "REFRESH_FOOTER_FAILED" -> 
                                com.scwang.smart.refresh.footer.ClassicsFooter.REFRESH_FOOTER_FAILED = value
                            "REFRESH_FOOTER_NOTHING" -> 
                                com.scwang.smart.refresh.footer.ClassicsFooter.REFRESH_FOOTER_NOTHING = value
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("SmartRefresh", "设置 Footer $fieldName 失败", e)
                    }
                }
            }
        }
    }
    
    /**
     * 调整 TextView 宽度，确保文字完整显示
     */
    private fun adjustTextViewWidth(footer: ClassicsFooter) {
        try {
            val possibleNames = listOf("mTitleText", "mTextTitle", "titleText", "mTitle")
            var titleTextView: android.widget.TextView? = null
            
            // 遍历类层次结构查找 TextView
            var searchClass: Class<*>? = ClassicsFooter::class.java
            outerLoop@ while (searchClass != null && searchClass != Any::class.java) {
                for (fieldName in possibleNames) {
                    try {
                        val field = searchClass.getDeclaredField(fieldName)
                        field.isAccessible = true
                        val view = field.get(footer)
                        if (view is android.widget.TextView) {
                            titleTextView = view
                            android.util.Log.d("SmartRefresh", "找到 Footer TextView 字段: $fieldName")
                            break@outerLoop
                        }
                    } catch (_: NoSuchFieldException) {}
                }
                searchClass = searchClass.superclass
            }
            
            if (titleTextView != null) {
                // 调整父容器宽度
                val parent = titleTextView.parent
                if (parent is ViewGroup) {
                    val parentParams = parent.layoutParams
                    if (parentParams != null) {
                        parentParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                        parent.layoutParams = parentParams
                    }
                }
                
                // 设置 TextView 为 WRAP_CONTENT
                val params = titleTextView.layoutParams
                if (params != null) {
                    params.width = ViewGroup.LayoutParams.WRAP_CONTENT
                    titleTextView.layoutParams = params
                }
                
                // 禁用 ellipsize，让文字完整显示
                titleTextView.ellipsize = null
                titleTextView.setSingleLine(true)
                titleTextView.maxLines = 1
                titleTextView.maxWidth = Int.MAX_VALUE
                titleTextView.maxEms = Int.MAX_VALUE
                titleTextView.requestLayout()
                
                android.util.Log.d("SmartRefresh", "Footer TextView 宽度已调整")
            } else {
                // 通过遍历 View 树查找
                titleTextView = findTextViewInTree(footer.view)
                if (titleTextView != null) {
                    val params = titleTextView.layoutParams
                    if (params != null) {
                        params.width = ViewGroup.LayoutParams.WRAP_CONTENT
                        titleTextView.layoutParams = params
                    }
                    titleTextView.setSingleLine(true)
                    titleTextView.maxLines = 1
                    titleTextView.maxWidth = Int.MAX_VALUE
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SmartRefresh", "调整 Footer TextView 失败", e)
        }
    }
    
    /**
     * 在 View 树中查找 TextView
     */
    private fun findTextViewInTree(view: View): android.widget.TextView? {
        if (view is android.widget.TextView && view.text.toString().isNotEmpty()) {
            return view
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val found = findTextViewInTree(view.getChildAt(i))
                if (found != null) return found
            }
        }
        return null
    }
    
    /**
     * 设置颜色
     */
    private fun setColors(footer: ClassicsFooter, props: Map<String, Any?>) {
        props["footerAccentColor"]?.let { 
            if (it is String) {
                android.util.Log.d("SmartRefresh", "设置 footerAccentColor = $it")
                footer.setAccentColor(it.toColorInt())
            }
        }
        props["footerPrimaryColor"]?.let { 
            if (it is String) {
                android.util.Log.d("SmartRefresh", "设置 footerPrimaryColor = $it")
                footer.setPrimaryColor(it.toColorInt())
            }
        }
    }
    
    /**
     * 设置字体大小
     */
    private fun setTextSize(footer: ClassicsFooter, props: Map<String, Any?>) {
        props["footerTitleTextSize"]?.let { 
            if (it is Number) {
                val spValue = it.toFloat()
                android.util.Log.d("SmartRefresh", "设置 footerTitleTextSize = ${spValue}sp")
                footer.setTextSizeTitle(spValue)
            }
        }
    }
    
    /**
     * 设置其他属性
     */
    private fun setOtherProps(footer: ClassicsFooter, props: Map<String, Any?>) {
        props["footerFinishDuration"]?.let { 
            if (it is Number) {
                android.util.Log.d("SmartRefresh", "设置 footerFinishDuration = $it")
                footer.setFinishDuration(it.toInt())
            }
        }
    }
    
    /**
     * 设置箭头大小
     */
    private fun setArrowSize(footer: ClassicsFooter, props: Map<String, Any?>) {
        val arrowSize = props["footerDrawableArrowSize"] ?: props["footerDrawableSize"]
        arrowSize?.let { 
            if (it is Number) {
                val dpValue = it.toDouble()
                val pxValue = dpToPx(dpValue).toInt()
                android.util.Log.d("SmartRefresh", "设置 Footer 箭头大小 = ${dpValue}dp (${pxValue}px)")
                
                if (dpValue == 0.0) {
                    // 隐藏箭头
                    hideArrows(footer)
                } else {
                    // 设置箭头大小
                    resizeArrows(footer, pxValue)
                }
            }
        }
    }
    
    /**
     * 隐藏箭头
     */
    private fun hideArrows(footer: ClassicsFooter) {
        try {
            val arrowFieldNames = listOf("mArrowView", "mProgressView")
            var searchClass: Class<*>? = ClassicsFooter::class.java
            while (searchClass != null && searchClass != Any::class.java) {
                for (fieldName in arrowFieldNames) {
                    try {
                        val field = searchClass.getDeclaredField(fieldName)
                        field.isAccessible = true
                        val view = field.get(footer)
                        if (view is View) {
                            view.visibility = View.GONE
                            android.util.Log.d("SmartRefresh", "已隐藏 Footer 箭头: $fieldName")
                        }
                    } catch (_: NoSuchFieldException) {}
                }
                searchClass = searchClass.superclass
            }
        } catch (e: Exception) {
            android.util.Log.e("SmartRefresh", "隐藏 Footer 箭头失败", e)
        }
    }
    
    /**
     * 调整箭头大小
     */
    private fun resizeArrows(footer: ClassicsFooter, pxValue: Int) {
        try {
            val arrowFieldNames = listOf("mArrowView", "mProgressView")
            var searchClass: Class<*>? = ClassicsFooter::class.java
            while (searchClass != null && searchClass != Any::class.java) {
                for (fieldName in arrowFieldNames) {
                    try {
                        val field = searchClass.getDeclaredField(fieldName)
                        field.isAccessible = true
                        val view = field.get(footer)
                        if (view is View) {
                            val layoutParams = view.layoutParams
                            layoutParams.width = pxValue
                            layoutParams.height = pxValue
                            view.layoutParams = layoutParams
                            android.util.Log.d("SmartRefresh", "已设置 Footer $fieldName 大小为 ${pxValue}px")
                        }
                    } catch (_: NoSuchFieldException) {}
                }
                searchClass = searchClass.superclass
            }
        } catch (e: Exception) {
            android.util.Log.e("SmartRefresh", "设置 Footer 图标大小失败", e)
        }
    }
    
    /**
     * 设置间距
     */
    private fun setMargin(footer: ClassicsFooter, props: Map<String, Any?>) {
        props["footerDrawableMarginRight"]?.let { 
            if (it is Number) {
                android.util.Log.d("SmartRefresh", "设置 footerDrawableMarginRight = $it")
                footer.setDrawableMarginRight(dpToPx(it.toDouble()))
            }
        }
    }
    
    /**
     * dp 转 px
     * 
     * 将密度无关像素（dp）转换为实际像素（px）
     * 
     * @param dp 密度无关像素值
     * @return 对应的像素值
     */
    private fun dpToPx(dp: Double): Float {
        val density = resources.displayMetrics.density
        return (dp * density).toFloat()
    }
}
