package com.example.persona3reloadscreensaver

import android.animation.AnimatorSet
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextClock
import android.widget.TextView
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.persona3reloadscreensaver.ui.theme.Persona3ReloadScreensaverTheme
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.sdk.android.auth.AuthorizationResponse
import kotlin.math.roundToInt


class MainActivity : ComponentActivity() {
    private lateinit var batteryTxt: TextView
    private lateinit var lockIcon: ImageView
    private lateinit var clock: TextClock
    private lateinit var video: VideoView
    private lateinit var spotifyAPI: SpotifyAPI

    private lateinit var spotifyAppRemote: SpotifyAppRemote


    private val clockAnimator = AnimatorSet()

    private fun clockAnimation() {

        clockAnimator.playSequentially()
    }

    private val OnPowerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            val i = Intent(context, MainActivity::class.java)
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(i)
        }
    }

    private val mBatInfoReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctxt: Context, intent: Intent) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val batteryPct = (level * 100 / scale.toFloat()).roundToInt()
            batteryTxt.setText("$batteryPct%")
        }
    }

    private val screenLocked: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val strAction = intent?.action

            val myKM: KeyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as (KeyguardManager)
            if(strAction.equals(Intent.ACTION_USER_PRESENT) || strAction.equals(Intent.ACTION_SCREEN_OFF) || strAction.equals(Intent.ACTION_SCREEN_ON)  )
                if( myKM.inKeyguardRestrictedInputMode())
                {
                    lockIcon.alpha = 1f
                } else
                {
                    lockIcon.alpha = 0f
                }
        }
    }

    private fun handleIntent() {
        val action: String? = intent?.action
        val uri: Uri? = intent?.data

        if (uri == null) {
            spotifyAPI.nativeGetAccessToken()
        } else {
            val response = AuthorizationResponse.fromUri(uri)
            if (response.type == AuthorizationResponse.Type.TOKEN) {
                Log.d("Access Token", response.accessToken)
                spotifyAPI.accessToken = response.accessToken
                spotifyAPI.getPlaybackState()
            }
        }
    }

    private val spotifyRunnable: Runnable = Runnable {
        while (true) {
            spotifyAPI.getPlaybackState()
            Thread.sleep(2000)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O_MR1)
    override fun onCreate(savedInstanceState: Bundle?) {
        super<ComponentActivity>.onCreate(savedInstanceState)
        setContentView(R.layout.layout)

        val playingText: TextView = findViewById(R.id.playing)
        playingText.isSelected = true

        spotifyAPI = SpotifyAPI(this, applicationContext, "b3b89dbe7ed04fb8a2df6741a03c2292", "21ade5c8e8404b6485c356a5f2c7863a", playingText)
        handleIntent()

        val thread: Thread = Thread(spotifyRunnable)
        thread.start()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            var keyguardManager: KeyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
//            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            this.window.addFlags(
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }

        val intentFilter = IntentFilter()
        intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED)

        val theFilter = IntentFilter()
        theFilter.addAction(Intent.ACTION_SCREEN_ON)
        theFilter.addAction(Intent.ACTION_SCREEN_OFF)
        theFilter.addAction(Intent.ACTION_USER_PRESENT)



        val color: Int =
            255 and 0xff shl 24 or (255 and 0xff shl 16) or (254 and 0xff shl 8) or (253 and 0xff)

        val bitmap = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(color)
        val bitmapDrawable = BitmapDrawable(resources, bitmap)
        window.setBackgroundDrawable(bitmapDrawable)


        window.decorView.apply { systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN }

        batteryTxt = findViewById(R.id.batteryLevel)
        clock = findViewById(R.id.textClock)
        video = findViewById(R.id.videoview)
        val vidPath = "android.resource://$packageName/raw/p3r"


        val batteryAnim = AnimationUtils.loadAnimation(applicationContext, R.anim.battery_anim)
        batteryAnim.repeatCount = Animation.INFINITE
        batteryAnim.repeatMode = Animation.REVERSE
        batteryTxt.startAnimation(batteryAnim)

        val clockAnim = AnimationUtils.loadAnimation(applicationContext, R.anim.clock_anim)
        clockAnim.repeatCount = Animation.INFINITE
        clockAnim.repeatMode = Animation.REVERSE
        clock.startAnimation(clockAnim)


        registerReceiver(this.mBatInfoReceiver, intentFilter)
        registerReceiver(this.screenLocked, theFilter)

        video.setOnPreparedListener { it.isLooping = true }
        video.setVideoPath(vidPath)
        video.start()
    }

    override fun onResume() {
        super.onResume()
        video.start()
    }

}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    Persona3ReloadScreensaverTheme {
        Greeting("Shahid!!!")
    }
}