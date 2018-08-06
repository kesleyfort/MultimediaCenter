package kalves.multimediacenter

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Bundle
import android.preference.Preference
import android.preference.SwitchPreference
import android.provider.Settings.ACTION_BLUETOOTH_SETTINGS
import android.provider.Settings.ACTION_WIFI_SETTINGS
import android.support.design.widget.BottomNavigationView
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.FrameLayout
import android.widget.Switch
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    @SuppressLint("SetTextI18n")
    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.nav_music -> {

                /*video.visibility = View.GONE
                spotify.visibility = View.GONE
                deezer.visibility = View.GONE
                settings.visibility = View.GONE
                music.visibility = View.VISIBLE*/
                return@OnNavigationItemSelectedListener true
            }
            R.id.nav_video -> {
                /* music.visibility = View.GONE
                spotify.visibility = View.GONE
                deezer.visibility = View.GONE
                settings.visibility = View.GONE
                video.visibility = View.VISIBLE*/
                return@OnNavigationItemSelectedListener true
            }
            R.id.nav_home -> {
                val music = findViewById<FrameLayout>(R.id.music_frame)
                val video = findViewById<FrameLayout>(R.id.video_frame)
                val spotify = findViewById<FrameLayout>(R.id.spotify_frame)
                val deezer = findViewById<FrameLayout>(R.id.deezer_frame)
                music.visibility = View.GONE
                video.visibility = View.GONE
                deezer.visibility = View.GONE
                spotify.visibility = View.VISIBLE
                return@OnNavigationItemSelectedListener true
            }
            R.id.nav_deezer -> {
                val music = findViewById<FrameLayout>(R.id.music_frame)
                val video = findViewById<FrameLayout>(R.id.video_frame)
                val spotify = findViewById<FrameLayout>(R.id.spotify_frame)
                val deezer = findViewById<FrameLayout>(R.id.deezer_frame)
                music.visibility = View.GONE
                video.visibility = View.GONE
                spotify.visibility = View.GONE
                deezer.visibility = View.VISIBLE
                return@OnNavigationItemSelectedListener true
            }
            R.id.nav_settings -> {
/*                val settings = findViewById<FrameLayout>(R.id.settings_frame)
                val music = findViewById<FrameLayout>(R.id.music_frame)
                val video = findViewById<FrameLayout>(R.id.video_frame)
                val spotify = findViewById<FrameLayout>(R.id.spotify_frame)
                val deezer = findViewById<FrameLayout>(R.id.deezer_frame)
                val switchBT = findViewById<Switch>(R.id.btswitch)
                val switchWifi = findViewById<Switch>(R.id.wifiswitch)
                val TextoWifi = findViewById<TextView>(R.id.wificonnectiontext)
                val TextoBT = findViewById<TextView>(R.id.btconnectiontext)
                val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                if (mBluetoothAdapter != null) {
                    switchBT.isChecked = mBluetoothAdapter.isEnabled
                }
                val wifiManager = this.getSystemService(Context.WIFI_SERVICE) as WifiManager
                switchWifi.isChecked = wifiManager.isWifiEnabled
                TextoWifi.text = "Connected to ${wifiManager.connectionInfo.ssid}"

                music.visibility = View.GONE
                video.visibility = View.GONE
                spotify.visibility = View.GONE
                deezer.visibility = View.GONE
                settings.visibility = View.VISIBLE*/
                startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)




    }




}
