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
        props ?: return
        
        val footer = srl.refreshFooter
        if (footer !is ClassicsFooter) {
            return
        }
        
        try {
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
        } catch (_: Throwable) {
            // 静默处理异常
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
                    } catch (_: Exception) {
                        // 静默处理异常
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
        } catch (_: Exception) {
            // 静默处理异常
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
                footer.setAccentColor(it.toColorInt())
            }
        }
        props["footerPrimaryColor"]?.let { 
            if (it is String) {
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
                        }
                    } catch (_: NoSuchFieldException) {}
                }
                searchClass = searchClass.superclass
            }
        } catch (_: Exception) {
            // 静默处理异常
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
                        }
                    } catch (_: NoSuchFieldException) {}
                }
                searchClass = searchClass.superclass
            }
        } catch (_: Exception) {
            // 静默处理异常
        }
    }
    
    /**
     * 设置间距
     */
    private fun setMargin(footer: ClassicsFooter, props: Map<String, Any?>) {
        props["footerDrawableMarginRight"]?.let { 
            if (it is Number) {
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
