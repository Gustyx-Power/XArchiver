package id.xms.xarchiver.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import id.xms.xarchiver.MainActivity
import androidx.activity.compose.setContent
import id.xms.xarchiver.ui.theme.XArchiverTheme

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            XArchiverTheme {
                SplashScreen {
                    startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                    finish()
                }
            }
        }
    }
}
