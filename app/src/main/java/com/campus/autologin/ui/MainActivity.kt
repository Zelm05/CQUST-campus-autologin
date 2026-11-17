package com.campus.autologin.ui

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.campus.autologin.R
import com.campus.autologin.service.KeepAlive
import com.campus.autologin.ui.navigation.AppRoot
import com.campus.autologin.ui.theme.CampusTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPostNotifications()

        setContent {
            CampusTheme {
                AppRoot()
            }
        }

        // 每次打开 App 都重新对齐后台状态：
        // 前台服务 + WorkManager 兜底任务一起按最新配置同步，
        // 顺带把"服务被杀了但用户又打开了 App"这种情况自动修复回来。
        KeepAlive.sync(this)
    }

    private fun requestPostNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_POST_NOTIFICATIONS
                )
            }
        }
    }

    /**
     * 通知权限被拒后给出引导。
     *
     * Android 13+ 即使没有通知权限，startForeground 也会成功，只是通知不可见——
     * 用户会以为「在监控」却看不到任何提示，所以被拒时必须主动指路。
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQUEST_POST_NOTIFICATIONS) return
        if (!needsNotificationPermission()) return
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.notification_permission_denied_title))
            .setMessage(getString(R.string.notification_permission_denied_message))
            .setPositiveButton(R.string.notification_permission_denied_action) { _, _ ->
                runCatching {
                    startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                    )
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    /** 是否仍然没有通知权限（仅 Android 13+ 存在该权限）。 */
    private fun needsNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED

    companion object {
        private const val REQUEST_POST_NOTIFICATIONS = 1
    }
}
