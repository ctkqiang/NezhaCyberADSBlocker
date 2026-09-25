package xin.ctkqiang.nezha_cyber.ads_block

import android.app.Application
import android.content.Context

/**
 * 应用入口，唯一职责是持有一个 [AppContainer]。
 *
 * 用 Application 持有容器而不是静态单例：这样服务（由系统实例化、无法注入构造参数）
 * 与界面（由组合根装配）能共享同一份存储与规则索引，同时不引入任何静态可变状态。
 */
class NezhaApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.warmUp()
    }
}

/**
 * 取应用容器。
 *
 * 配置缺失时立刻失败并说明原因，而不是退化成一个空容器：空容器会让界面显示 0 条规则、
 * 让隧道静默不拦任何东西，那种「看起来能用但没有效果」的状态最难排查。
 */
internal fun requireAppContainer(context: Context): AppContainer {
    val application = context.applicationContext as? NezhaApplication
        ?: error("AppContainer 未初始化：请确认 AndroidManifest 的 application 声明了 NezhaApplication")
    return application.container
}
