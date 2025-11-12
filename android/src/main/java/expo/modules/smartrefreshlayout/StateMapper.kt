package expo.modules.smartrefreshlayout

import com.scwang.smart.refresh.layout.constant.RefreshState as NativeRefreshState

/**
 * 状态映射工具类
 * 负责将原生 RefreshState 映射到 TypeScript RefreshState 枚举值
 */
object StateMapper {
    
    /**
     * 将原生状态转换为可读字符串
     */
    fun nativeStateToString(state: NativeRefreshState): String {
        return when (state) {
            NativeRefreshState.None -> "None"
            NativeRefreshState.PullDownToRefresh -> "PullDownToRefresh"
            NativeRefreshState.PullUpToLoad -> "PullUpToLoad"
            NativeRefreshState.PullDownCanceled -> "PullDownCanceled"
            NativeRefreshState.PullUpCanceled -> "PullUpCanceled"
            NativeRefreshState.ReleaseToRefresh -> "ReleaseToRefresh"
            NativeRefreshState.ReleaseToLoad -> "ReleaseToLoad"
            NativeRefreshState.ReleaseToTwoLevel -> "ReleaseToTwoLevel"
            NativeRefreshState.TwoLevelReleased -> "TwoLevelReleased"
            NativeRefreshState.RefreshReleased -> "RefreshReleased"
            NativeRefreshState.LoadReleased -> "LoadReleased"
            NativeRefreshState.Refreshing -> "Refreshing"
            NativeRefreshState.Loading -> "Loading"
            NativeRefreshState.TwoLevel -> "TwoLevel"
            NativeRefreshState.RefreshFinish -> "RefreshFinish"
            NativeRefreshState.LoadFinish -> "LoadFinish"
            NativeRefreshState.TwoLevelFinish -> "TwoLevelFinish"
        }
    }
    
    /**
     * 将原生 RefreshState 映射到 TypeScript RefreshState 枚举值
     * 
     * TypeScript RefreshState 枚举:
     * None = 0, PullDownToRefresh = 1, ReleaseToRefresh = 2, Refreshing = 3,
     * RefreshFinish = 4, PullUpToLoad = 5, ReleaseToLoad = 6, Loading = 7,
     * LoadFinish = 8, NoMoreData = 9
     */
    fun nativeStateToRefreshState(state: NativeRefreshState): Int {
        return when (state) {
            NativeRefreshState.None -> 0                      // 初始状态
            NativeRefreshState.PullDownToRefresh -> 1         // 下拉中
            NativeRefreshState.PullDownCanceled -> 0          // 取消下拉 -> None
            NativeRefreshState.ReleaseToRefresh -> 2          // 释放刷新
            NativeRefreshState.RefreshReleased -> 2           // 刷新释放 -> ReleaseToRefresh
            NativeRefreshState.Refreshing -> 3                // 刷新中
            NativeRefreshState.RefreshFinish -> 4             // 刷新完成
            NativeRefreshState.PullUpToLoad -> 5              // 上拉中
            NativeRefreshState.PullUpCanceled -> 0            // 取消上拉 -> None
            NativeRefreshState.ReleaseToLoad -> 6             // 释放加载
            NativeRefreshState.LoadReleased -> 6              // 加载释放 -> ReleaseToLoad
            NativeRefreshState.Loading -> 7                   // 加载中
            NativeRefreshState.LoadFinish -> 8                // 加载完成
            // 其他状态（如二楼、TwoLevel 等）暂时映射到 None
            else -> 0
        }
    }
}
