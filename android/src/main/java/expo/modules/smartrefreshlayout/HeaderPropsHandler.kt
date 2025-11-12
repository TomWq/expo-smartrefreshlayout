package expo.modules.smartrefreshlayout

import android.content.res.Resources
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import com.scwang.smart.refresh.header.ClassicsHeader
import com.scwang.smart.refresh.layout.SmartRefreshLayout
import com.scwang.smart.refresh.layout.constant.RefreshState as NativeRefreshState

/**
 * Header 属性处理器
 * 负责处理经典样式 Header 的所有属性设置
 */
class HeaderPropsHandler(
    private val srl: SmartRefreshLayout,
    private val resources: Resources
) {
    
    /**
     * 应用 Header 属性
     */
    fun applyProps(props: Map<String, Any?>?) {
        android.util.Log.d("SmartRefresh", "HeaderPropsHandler.applyProps 被调用，props = $props")
        props ?: return
        
        val header = srl.refreshHeader
        if (header !is ClassicsHeader) {
            android.util.Log.w("SmartRefresh", "Header 不是 ClassicsHeader 类型，类型 = ${header?.javaClass?.simpleName}")
            return
        }
        
        try {
            android.util.Log.d("SmartRefresh", "开始应用 Header 属性，当前状态: ${srl.state}")
            
            // 设置文字内容
            setTextContent(header, props)
            
            // 强制刷新 Header 显示
            header.onStateChanged(srl, NativeRefreshState.None, srl.state)
            
            // 设置颜色
            setColors(header, props)
            
            // 设置字体大小
            setTextSize(header, props)
            
            // 设置其他属性
            setOtherProps(header, props)
            
            // 设置箭头大小
            setArrowSize(header, props)
            
            // 设置间距
            setMargin(header, props)
            
            // 监听状态变化，确保刷新图片动画运行
            setupStateListener(header, props)
            
            android.util.Log.d("SmartRefresh", "Header 属性已全部更新完成")
        } catch (e: Throwable) {
            android.util.Log.e("SmartRefresh", "设置 Header 属性失败", e)
            e.printStackTrace()
        }
    }
    
    /**
     * 设置状态监听器
     * 在刷新状态时确保刷新图片动画运行
     */
    private fun setupStateListener(header: ClassicsHeader, props: Map<String, Any?>) {
        // 使用 Handler 定期检查并确保刷新图片动画运行
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                if (srl.state == NativeRefreshState.Refreshing) {
                    // 在刷新状态时，持续确保刷新图片动画运行
                    ensureProgressImageAnimating(header, props)
                }
                // 每 100ms 检查一次
                handler.postDelayed(this, 100)
            }
        }
        handler.post(runnable)
    }
    
    /**
     * 确保刷新图片动画运行
     * 注意：不要永久改变视图状态，让 ClassicsHeader 自己管理箭头/图片的切换
     */
    private fun ensureProgressImageAnimating(header: ClassicsHeader, props: Map<String, Any?>) {
        try {
            var searchClass: Class<*>? = ClassicsHeader::class.java
            var progressImageView: View? = null
            
            while (searchClass != null && searchClass != Any::class.java) {
                try {
                    val progressField = searchClass.getDeclaredField("mProgressView")
                    progressField.isAccessible = true
                    progressImageView = progressField.get(header) as? View
                    break
                } catch (_: NoSuchFieldException) {
                    searchClass = searchClass.superclass
                }
            }
            
            // 只确保图片动画在运行，不改变 visibility
            progressImageView?.let { imageView ->
                if (imageView is android.widget.ImageView) {
                    val drawable = imageView.drawable
                    if (drawable is android.graphics.drawable.Animatable) {
                        if (!(drawable as android.graphics.drawable.Animatable).isRunning) {
                            (drawable as android.graphics.drawable.Animatable).start()
                            android.util.Log.d("SmartRefresh", "【刷新中】启动刷新图片动画")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SmartRefresh", "【刷新中】启动刷新图片动画失败", e)
        }
    }
    
    /**
     * 设置文字内容
     */
    private fun setTextContent(header: ClassicsHeader, props: Map<String, Any?>) {
        val textFields = mapOf(
            "REFRESH_HEADER_PULLING" to "mTextPulling",
            "REFRESH_HEADER_REFRESHING" to "mTextRefreshing",
            "REFRESH_HEADER_LOADING" to "mTextLoading",
            "REFRESH_HEADER_RELEASE" to "mTextRelease",
            "REFRESH_HEADER_FINISH" to "mTextFinish",
            "REFRESH_HEADER_FAILED" to "mTextFailed",
            "REFRESH_HEADER_SECONDARY" to "mTextSecondary",
            "REFRESH_HEADER_UPDATE" to "mTextUpdate"
        )
        
        textFields.forEach { (propKey, fieldName) ->
            props[propKey]?.let { value ->
                if (value is String) {
                    try {
                        val field = ClassicsHeader::class.java.getDeclaredField(fieldName)
                        field.isAccessible = true
                        field.set(header, value)
                        android.util.Log.d("SmartRefresh", "设置 $fieldName = $value")
                        
                        // 同时修改静态常量
                        when (propKey) {
                            "REFRESH_HEADER_PULLING" -> 
                                com.scwang.smart.refresh.header.ClassicsHeader.REFRESH_HEADER_PULLING = value
                            "REFRESH_HEADER_REFRESHING" -> 
                                com.scwang.smart.refresh.header.ClassicsHeader.REFRESH_HEADER_REFRESHING = value
                            "REFRESH_HEADER_LOADING" -> 
                                com.scwang.smart.refresh.header.ClassicsHeader.REFRESH_HEADER_LOADING = value
                            "REFRESH_HEADER_RELEASE" -> 
                                com.scwang.smart.refresh.header.ClassicsHeader.REFRESH_HEADER_RELEASE = value
                            "REFRESH_HEADER_FINISH" -> 
                                com.scwang.smart.refresh.header.ClassicsHeader.REFRESH_HEADER_FINISH = value
                            "REFRESH_HEADER_FAILED" -> 
                                com.scwang.smart.refresh.header.ClassicsHeader.REFRESH_HEADER_FAILED = value
                            "REFRESH_HEADER_SECONDARY" -> 
                                com.scwang.smart.refresh.header.ClassicsHeader.REFRESH_HEADER_SECONDARY = value
                            "REFRESH_HEADER_UPDATE" -> 
                                com.scwang.smart.refresh.header.ClassicsHeader.REFRESH_HEADER_UPDATE = value
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("SmartRefresh", "设置 $fieldName 失败", e)
                    }
                }
            }
        }
    }
    
    /**
     * 设置颜色
     */
    private fun setColors(header: ClassicsHeader, props: Map<String, Any?>) {
        props["headerAccentColor"]?.let {
            if (it is String) {
                val colorStr = normalizeColorString(it)
                android.util.Log.d("SmartRefresh", "设置 headerAccentColor = $it -> $colorStr")
                header.setAccentColor(colorStr.toColorInt())
            }
        }
        props["headerPrimaryColor"]?.let {
            if (it is String) {
                val colorStr = normalizeColorString(it)
                android.util.Log.d("SmartRefresh", "设置 headerPrimaryColor = $it -> $colorStr")
                header.setPrimaryColor(colorStr.toColorInt())
            }
        }
    }
    
    /**
     * 标准化颜色字符串
     * 将 3 位十六进制颜色转换为 6 位
     * 例如：#000 -> #000000, #F00 -> #FF0000
     */
    private fun normalizeColorString(color: String): String {
        var result = color.trim()
        
        // 如果是 3 位十六进制颜色（#RGB），转换为 6 位（#RRGGBB）
        if (result.startsWith("#") && result.length == 4) {
            val r = result[1]
            val g = result[2]
            val b = result[3]
            result = "#$r$r$g$g$b$b"
        }
        
        return result
    }
    
    /**
     * 设置字体大小
     */
    private fun setTextSize(header: ClassicsHeader, props: Map<String, Any?>) {
        props["headerTitleTextSize"]?.let { 
            if (it is Number) {
                val spValue = it.toFloat()
                android.util.Log.d("SmartRefresh", "设置 headerTitleTextSize = ${spValue}sp")
                header.setTextSizeTitle(spValue)
            }
        }
        props["headerTimeTextSize"]?.let { 
            if (it is Number) {
                val spValue = it.toFloat()
                android.util.Log.d("SmartRefresh", "设置 headerTimeTextSize = ${spValue}sp")
                header.setTextSizeTime(spValue)
            }
        }
    }
    
    /**
     * 设置其他属性
     */
    private fun setOtherProps(header: ClassicsHeader, props: Map<String, Any?>) {
        props["headerShowTime"]?.let { 
            if (it is Boolean) header.setEnableLastTime(it) 
        }
        props["headerFinishDuration"]?.let { 
            if (it is Number) {
                android.util.Log.d("SmartRefresh", "设置 headerFinishDuration = $it")
                header.setFinishDuration(it.toInt())
            }
        }
        props["headerLastUpdateText"]?.let { 
            if (it is String) header.setLastUpdateText(it) 
        }
    }
    
    /**
     * 设置箭头大小和刷新图片大小
     */
    private fun setArrowSize(header: ClassicsHeader, props: Map<String, Any?>) {
        val headerArrowSize = props["headerDrawableArrowSize"] ?: props["headerDrawableSize"]
        val progressSize = props["headerDrawableProgressSize"]
        
        // 使用 ClassicsHeader 提供的 API 设置箭头大小
        headerArrowSize?.let {
            if (it is Number) {
                val dpValue = it.toFloat()
                android.util.Log.d("SmartRefresh", "设置 Header 箭头大小 = ${dpValue}dp")
                header.setDrawableArrowSize(dpValue)
            }
        }
        
        // 使用 ClassicsHeader 提供的 API 设置刷新图片大小
        progressSize?.let {
            if (it is Number) {
                val dpValue = it.toFloat()
                android.util.Log.d("SmartRefresh", "设置 Header 刷新图片大小 = ${dpValue}dp")
                header.setDrawableProgressSize(dpValue)
            }
        }
        
        // 配置刷新图片的颜色和动画
        configureProgressImage(header, props)
    }

    
    /**
     * 配置刷新图片的颜色和动画
     * 初始化时只配置，不改变显示状态，让 ClassicsHeader 自己控制箭头/图片的切换
     */
    private fun configureProgressImage(header: ClassicsHeader, props: Map<String, Any?>) {
        try {
            var searchClass: Class<*>? = ClassicsHeader::class.java
            var progressImageView: View? = null
            
            // 查找刷新图片视图
            while (searchClass != null && searchClass != Any::class.java) {
                try {
                    val progressField = searchClass.getDeclaredField("mProgressView")
                    progressField.isAccessible = true
                    progressImageView = progressField.get(header) as? View
                    break
                } catch (_: NoSuchFieldException) {
                    searchClass = searchClass.superclass
                }
            }
            
            android.util.Log.d("SmartRefresh", "找到刷新图片视图: ${progressImageView != null}")
            
            // 配置刷新图片（但不改变显示状态，让 ClassicsHeader 自己控制）
            progressImageView?.let { imageView ->
                // 如果是 ImageView（包括 AppCompatImageView），配置动画和颜色
                if (imageView is android.widget.ImageView) {
                    android.util.Log.d("SmartRefresh", "刷新图片是 ImageView/AppCompatImageView")
                    
                    // 获取 drawable
                    val drawable = imageView.drawable
                    android.util.Log.d("SmartRefresh", "Drawable 类型: ${drawable?.javaClass?.simpleName}")
                    
                    // 启动动画
                    drawable?.let { d ->
                        if (d is android.graphics.drawable.Animatable) {
                            android.util.Log.d("SmartRefresh", "Drawable 是 Animatable，启动动画")
                            (d as android.graphics.drawable.Animatable).start()
                            android.util.Log.d("SmartRefresh", "已启动刷新图片动画")
                        }
                    }
                    
                    // 如果设置了强调色，应用到刷新图片
                    props["headerAccentColor"]?.let { color ->
                        if (color is String) {
                            try {
                                imageView.setColorFilter(normalizeColorString(color).toColorInt())
                                android.util.Log.d("SmartRefresh", "已设置 Header 刷新图片颜色: $color")
                            } catch (e: Exception) {
                                android.util.Log.w("SmartRefresh", "设置刷新图片颜色失败", e)
                            }
                        }
                    }
                }
                
                android.util.Log.d("SmartRefresh", "Header 刷新图片配置完成 - visibility=${imageView.visibility}, alpha=${imageView.alpha}, 类型=${imageView.javaClass.simpleName}")
            } ?: android.util.Log.w("SmartRefresh", "未找到刷新图片视图！")
            
        } catch (e: Exception) {
            android.util.Log.e("SmartRefresh", "配置 Header 刷新图片失败", e)
            e.printStackTrace()
        }
    }
    
    /**
     * 设置间距
     */
    private fun setMargin(header: ClassicsHeader, props: Map<String, Any?>) {
        props["headerDrawableMarginRight"]?.let { 
            if (it is Number) {
                android.util.Log.d("SmartRefresh", "设置 headerDrawableMarginRight = $it")
                header.setDrawableMarginRight(dpToPx(it.toDouble()))
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
