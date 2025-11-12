import Foundation

/**
 * 状态映射工具类
 * 
 * 负责将 iOS 端的状态字符串映射到 TypeScript 端的 RefreshState 枚举值
 * 
 * 映射规则：
 * - None(0): 初始状态
 * - PullDownToRefresh(1): 下拉中
 * - ReleaseToRefresh(2): 释放即可刷新
 * - Refreshing(3): 刷新中
 * - RefreshFinish(4): 刷新完成
 * - PullUpToLoad(5): 上拉中
 * - ReleaseToLoad(6): 释放即可加载
 * - Loading(7): 加载中
 * - LoadFinish(8): 加载完成
 * - NoMoreData(9): 没有更多数据
 */
class StateMapper {
    
    /**
     * 将状态字符串映射到 TypeScript RefreshState 枚举值
     * 
     * @param state 状态字符串
     * @return TypeScript RefreshState 枚举对应的整数值
     */
    static func stateStringToRefreshState(_ state: String) -> Int {
        switch state {
        case "None":
            return 0  // None
        case "PullDownToRefresh":
            return 1  // PullDownToRefresh
        case "ReleaseToRefresh":
            return 2  // ReleaseToRefresh
        case "Refreshing":
            return 3  // Refreshing
        case "RefreshFinish":
            return 4  // RefreshFinish
        case "PullUpToLoad":
            return 5  // PullUpToLoad
        case "ReleaseToLoad":
            return 6  // ReleaseToLoad
        case "Loading":
            return 7  // Loading
        case "LoadFinish":
            return 8  // LoadFinish
        case "NoMoreData":
            return 9  // NoMoreData
        default:
            return 0  // 默认返回 None
        }
    }
}
