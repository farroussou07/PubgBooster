package com.pubg.booster

import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.pubg.booster.databinding.ActivityMainBinding
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val mainHandler = Handler(Looper.getMainLooper())
    private val SHIZUKU_PERMISSION_REQUEST_CODE = 1001

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        checkShizukuState()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        checkShizukuState()
    }

    private val requestPermissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == SHIZUKU_PERMISSION_REQUEST_CODE) {
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    appendLog("✓ تم منح صلاحية Shizuku بنجاح!")
                    updateStatus(true, "متصل ومُفعل")
                } else {
                    appendLog("✗ تم رفض صلاحية Shizuku.")
                    updateStatus(false, "تم رفض الصلاحية")
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(requestPermissionResultListener)

        setupButtons()
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
        Shizuku.removeRequestPermissionResultListener(requestPermissionResultListener)
    }

    override fun onResume() {
        super.onResume()
        checkShizukuState()
    }

    private fun checkShizukuState() {
        if (!Shizuku.pingBinder()) {
            updateStatus(false, "خدمة Shizuku متوقفة")
            return
        }

        if (Shizuku.isPreV11()) {
            updateStatus(false, "إصدار Shizuku قديم جداً")
            return
        }

        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            updateStatus(true, "متصل ومُفعل ✓")
        } else {
            updateStatus(false, "في انتظار منح الصلاحية")
        }
    }

    private fun updateStatus(isReady: Boolean, statusText: String) {
        mainHandler.post {
            binding.tvShizukuStatus.text = statusText
            val colorRes = if (isReady) R.color.status_green else R.color.status_red
            binding.tvShizukuStatus.setTextColor(ContextCompat.getColor(this, colorRes))
            binding.btnRequestPermission.isEnabled = !isReady && Shizuku.pingBinder()
        }
    }

    private fun setupButtons() {
        binding.btnRequestPermission.setOnClickListener {
            if (Shizuku.pingBinder()) {
                if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                    Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE)
                } else {
                    Toast.makeText(this, "الصلاحية ممنوحة بالفعل!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "تأكد من تشغيل تطبيق Shizuku أولاً!", Toast.LENGTH_LONG).show()
            }
        }

        binding.btnBoostAll.setOnClickListener {
            runOptimization(
                listOf(
                    "settings put system min_refresh_rate 144.0",
                    "settings put system peak_refresh_rate 144.0",
                    "settings put system user_refresh_rate 144",
                    "cmd game set --mode 2 --fps 120 com.tencent.ig",
                    "settings put global wifi_sleep_policy 2",
                    "settings put global wifi_scan_always_enabled 0",
                    "settings put global private_dns_mode hostname",
                    "settings put global private_dns_specifier one.one.one.one",
                    "pm clear com.miui.powerkeeper"
                ),
                "⚡ تطبيق التعزيز الشامل (144Hz + 120FPS + Gaming DNS + Powerkeeper Clear)"
            )
        }

        binding.btnBoostFps.setOnClickListener {
            runOptimization(
                listOf(
                    "settings put system min_refresh_rate 144.0",
                    "settings put system peak_refresh_rate 144.0",
                    "settings put system user_refresh_rate 144",
                    "cmd game set --mode 2 --fps 120 com.tencent.ig"
                ),
                "🎮 تفعيل 144Hz وتجاوز الفريمات إلى 120 FPS"
            )
        }

        binding.btnBoostNetwork.setOnClickListener {
            runOptimization(
                listOf(
                    "settings put global wifi_sleep_policy 2",
                    "settings put global wifi_scan_always_enabled 0",
                    "settings put global private_dns_mode hostname",
                    "settings put global private_dns_specifier one.one.one.one"
                ),
                "🌐 ضبط أداء الواي فاي و Cloudflare DNS"
            )
        }

        binding.btnClearPowerkeeper.setOnClickListener {
            runOptimization(
                listOf("pm clear com.miui.powerkeeper"),
                "🛡️ مسح قيود وتخفيض حرارة Joyose/Powerkeeper"
            )
        }
    }

    private fun runOptimization(commands: List<String>, title: String) {
        if (!Shizuku.pingBinder() || Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "يجب منح صلاحية Shizuku أولاً!", Toast.LENGTH_SHORT).show()
            appendLog("✗ فشل: Shizuku غير متصل أو الصلاحية غير ممنوحة.")
            return
        }

        appendLog("\n>>> بدء: $title")

        Thread {
            try {
                val newProcessMethod = Shizuku::class.java.getDeclaredMethod(
                    "newProcess",
                    Array<String>::class.java,
                    Array<String>::class.java,
                    String::class.java
                )
                newProcessMethod.isAccessible = true
                val process = newProcessMethod.invoke(null, arrayOf("sh"), null, null) as java.lang.Process
                val writer = process.outputStream.bufferedWriter()
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val errorReader = BufferedReader(InputStreamReader(process.errorStream))

                for (cmd in commands) {
                    writer.write(cmd + "\n")
                }
                writer.write("exit\n")
                writer.flush()

                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    line?.let { if (it.isNotBlank()) appendLog("  $it") }
                }
                while (errorReader.readLine().also { line = it } != null) {
                    line?.let { if (it.isNotBlank()) appendLog("  [err] $it") }
                }

                process.waitFor()
                appendLog("✓ تم إكمال جميع الأوامر بنجاح!")
                mainHandler.post {
                    Toast.makeText(this, "تم تطبيق التعديلات بنجاح!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                appendLog("✗ حدث خطأ: ${e.message}")
            }
        }.start()
    }

    private fun appendLog(msg: String) {
        mainHandler.post {
            binding.tvLogs.append("$msg\n")
        }
    }
}
