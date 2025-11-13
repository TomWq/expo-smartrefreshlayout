package expo.modules.smartrefreshlayout

import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition

class ExpoSmartrefreshlayoutModule : Module() {
    override fun definition() = ModuleDefinition {

        Name("ExpoSmartrefreshlayout")


        // 注册 Expo View
        View(ExpoSmartrefreshlayoutView::class) {

            // ------------------- Props -------------------
            // Header/Footer 高度、拖拽、触发比率
            Prop<Double>("dragRate") { view, value -> view.setDragRate(value) }
            Prop<Double>("headerHeight") { view, value -> view.setHeaderHeight(value) }
            Prop<Double>("footerHeight") { view, value -> view.setFooterHeight(value) }
            Prop<Double>("headerInsetStart") { view, value -> view.setHeaderInsetStart(value) }
            Prop<Double>("footerInsetStart") { view, value -> view.setFooterInsetStart(value) }
            Prop<Double>("headerMaxDragRate") { view, value -> view.setHeaderMaxDragRate(value) }
            Prop<Double>("footerMaxDragRate") { view, value -> view.setFooterMaxDragRate(value) }
            Prop<Double>("headerTriggerRate") { view, value -> view.setHeaderTriggerRate(value) }
            Prop<Double>("footerTriggerRate") { view, value -> view.setFooterTriggerRate(value) }
            Prop<Int>("reboundDuration") { view, value -> view.setReboundDuration(value) }

            // Header 类型
            Prop<String>("headerType") { view, value -> view.setHeaderType(value) }

            // 启用/禁用各种功能
            Prop<Boolean>("enableRefresh") { view, value -> view.setEnableRefresh(value) }
            Prop<Boolean>("enableLoadMore") { view, value -> view.setEnableLoadMore(value) }
            Prop<Boolean>("enableAutoLoadMore") { view, value -> view.setEnableAutoLoadMore(value) }
            Prop<Boolean>("enablePureScrollMode") { view, value -> view.setEnablePureScrollMode(value) }
            Prop<Boolean>("enableScrollContentWhenLoaded") { view, value -> view.setEnableScrollContentWhenLoaded(value) }
            Prop<Boolean>("enableScrollContentWhenRefreshed") { view, value -> view.setEnableScrollContentWhenRefreshed(value) }
            Prop<Boolean>("enableOverScrollDrag") { view, value -> view.setEnableOverScrollDrag(value) }
            Prop<Boolean>("enableOverScrollBounce") { view, value -> view.setEnableOverScrollBounce(value) }
            Prop<Boolean>("enableNestedScroll") { view, value -> view.setEnableNestedScroll(value) }
            Prop<Boolean>("enableHeaderTranslationContent") { view, value -> view.setEnableHeaderTranslationContent(value) }
            Prop<Boolean>("enableFooterTranslationContent") { view, value -> view.setEnableFooterTranslationContent(value) }
            Prop<Boolean>("enableLoadMoreWhenContentNotFull") { view, value -> view.setEnableLoadMoreWhenContentNotFull(value) }

            // Classic Header/Footer props
            Prop("classicRefreshHeaderProps") { view: ExpoSmartrefreshlayoutView, value: Map<String, Any?> -> 
                view.setClassicRefreshHeaderProps(value)
            }
            Prop("classicLoadMoreFooterProps") { view: ExpoSmartrefreshlayoutView, value: Map<String, Any?> -> 
                view.setClassicLoadMoreFooterProps(value)
            }
            
            // Custom Header/Footer flags
            Prop<Boolean>("hasCustomHeader") { view, value -> view.setHasCustomHeader(value) }
            Prop<Boolean>("hasCustomFooter") { view, value -> view.setHasCustomFooter(value) }
            
            // Haptic Feedback
            Prop<Boolean>("enableHapticFeedback") { view, value -> view.setEnableHapticFeedback(value) }


            // ------------------- Events -------------------
            Events(
                "onRefresh",
                "onLoadMore",
                "onStateChanged",
                "onHeaderMoving",
                "onFooterMoving"
            )

            // ------------------- Commands -------------------

            AsyncFunction("finishRefresh") { view: ExpoSmartrefreshlayoutView, success: Boolean, delay: Int ->
                view.finishRefresh(success, delay)
            }

            AsyncFunction("finishLoadMore") { view: ExpoSmartrefreshlayoutView, success: Boolean, delay: Int, noMoreData: Boolean ->
                view.finishLoadMore(success, delay, noMoreData)
            }

            AsyncFunction("autoRefresh") { view: ExpoSmartrefreshlayoutView, delay: Int ->
                view.autoRefresh(delay)
            }

            AsyncFunction("autoLoadMore") { view: ExpoSmartrefreshlayoutView, delay: Int ->
                view.autoLoadMore(delay)
            }

            AsyncFunction("setNoMoreData") { view: ExpoSmartrefreshlayoutView, noMore: Boolean ->
                view.setNoMoreData(noMore)
            }
        }
    }
}
