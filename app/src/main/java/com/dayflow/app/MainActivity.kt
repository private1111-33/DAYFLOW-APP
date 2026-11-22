package com.dayflow.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import com.dayflow.app.data.AlarmScheduler
import com.dayflow.app.ui.DayflowApp
import com.dayflow.app.ui.theme.DayflowTheme
import com.dayflow.app.updates.AppUpdate
import com.dayflow.app.updates.UpdateManager
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            val notificationPermission = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { }
            var availableUpdate by remember { mutableStateOf<AppUpdate?>(null) }
            var downloading by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= 33 &&
                    ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                availableUpdate = runCatching {
                    UpdateManager.checkForUpdate(this@MainActivity)
                }.getOrNull()
            }

            DayflowTheme {
                DayflowApp()

                availableUpdate?.let { update ->
                    AlertDialog(
                        onDismissRequest = { if (!downloading) availableUpdate = null },
                        title = {
                            Text("تحديث جديد متاح", fontWeight = FontWeight.Bold)
                        },
                        text = {
                            Text(
                                "يتوفر DAYFLOW إصدار ${update.versionName}. " +
                                    "حدّث التطبيق للحصول على أحدث المميزات والتحسينات."
                            )
                        },
                        confirmButton = {
                            Button(
                                enabled = !downloading,
                                onClick = {
                                    scope.launch {
                                        downloading = true
                                        try {
                                            UpdateManager.downloadAndInstall(this@MainActivity, update)
                                        } catch (error: Exception) {
                                            Toast.makeText(
                                                this@MainActivity,
                                                error.message ?: "فشل تحميل التحديث",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        } finally {
                                            downloading = false
                                        }
                                    }
                                }
                            ) {
                                Text(if (downloading) "جارٍ التحميل..." else "تحميل وتثبيت")
                            }
                        },
                        dismissButton = {
                            TextButton(
                                enabled = !downloading,
                                onClick = { availableUpdate = null }
                            ) {
                                Text("لاحقًا")
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AlarmScheduler.rescheduleAll(this)
    }
}
