package com.campus.autologin.service

import android.content.Context
import com.campus.autologin.data.model.LoginConfig
import com.campus.autologin.data.repository.AuthRepository

/**
 * 后台常驻的统一开关。
 *
 * 任何"配置可能变了"的地方（开机、更新包、打开 App、保存设置、切换开关）
 * 都调这里，保证「前台服务」与「WorkManager 兜底任务」始终与配置一致，
 * 不会出现"服务没起来但兜底也没注册"这种两头落空的状态。
 */
object KeepAlive {

    /**
     * 后台监控是否应当处于开启状态。
     *
     * 两个入口语义不同，必须分开判：
     *  - booting=true（开机广播）：用户关掉「开机自启动后台监控」就不许拉起；
     *  - booting=false（打开 App / 保存设置 / 更新包）：用户主动进来了，按监控开关走。
     */
    fun shouldRun(cfg: LoginConfig, booting: Boolean): Boolean {
        if (!(cfg.autoLogin || cfg.alwaysRunInBackground)) return false
        return !booting || cfg.autoStartOnBoot
    }

    fun sync(ctx: Context, booting: Boolean = false) {
        val app = ctx.applicationContext
        val cfg = runCatching { AuthRepository(app).loadConfig() }.getOrNull()
        // 配置读不出来时按"不开启"处理（与原行为一致），避免残留服务无人回收
        if (cfg != null && shouldRun(cfg, booting)) {
            MonitorWorker.enable(app)
            AutoLoginService.start(app)
        } else {
            MonitorWorker.disable(app)
            AutoLoginService.stop(app)
        }
    }
}
