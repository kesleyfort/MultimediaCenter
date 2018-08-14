package kalves.multimediacenter

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.preference.Preference
import android.preference.SwitchPreference
import android.provider.Settings.ACTION_BLUETOOTH_SETTINGS
import android.provider.Settings.ACTION_WIFI_SETTINGS
import android.support.design.widget.BottomNavigationView
import android.support.v7.app.AppCompatActivity
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.*
import kotlinx.android.synthetic.main.activity_main.*
import wseemann.media.FFmpegMediaMetadataRetriever
import android.widget.SeekBar




class MainActivity : AppCompatActivity() {
    @SuppressLint("SetTextI18n")
    //Declare Variables
    private var mediaController: MediaController? = null
    private var fullscreenclicked: Boolean = false
    private var playervideoWidth: Int = 0
    private var playervideoHeigth: Int = 0
    private var mediaPlayer: MediaPlayer? = null
    private var selectfilesucess: Boolean = false
    private var musicpausedposition: Int = 0
    private val musicSeekBarUpdateHandler = Handler()
    private var songlist: MutableList<Uri?> = mutableListOf<Uri?>()
    private var currentIndexSongList = 0
    val mUpdateSeekbar = object : Runnable {
        override fun run() {
            musicseekBar.progress = mediaPlayer!!.getCurrentPosition()
            musicSeekBarUpdateHandler.postDelayed(this, 500)
            val currentposition = findViewById<TextView>(R.id.initialtimeMusic)
            currentposition.text = ConvertMStoMinutes(mediaPlayer!!.currentPosition)
        }
    }


    //End Declare Variables

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.nav_music -> {
                //Make the views go away
                val music = findViewById<FrameLayout>(R.id.music_frame)
                val video = findViewById<FrameLayout>(R.id.video_frame)
                music.visibility = View.VISIBLE
                video.visibility = View.GONE
                //end view

                //Music Player

                return@OnNavigationItemSelectedListener true
            }
            R.id.nav_video -> {
                val music = findViewById<FrameLayout>(R.id.music_frame)
                val video = findViewById<FrameLayout>(R.id.video_frame)
                music.visibility = View.GONE
                video.visibility = View.VISIBLE
                return@OnNavigationItemSelectedListener true
            }
            R.id.nav_home -> {
                val music = findViewById<FrameLayout>(R.id.music_frame)
                val video = findViewById<FrameLayout>(R.id.video_frame)
                music.visibility = View.GONE
                video.visibility = View.GONE
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
        var playMusicbutton = findViewById<ImageView>(R.id.playMusicButton)
        playMusicbutton.isClickable = false
        musicseekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser)
                    if(mediaPlayer !=null)
                    mediaPlayer!!.seekTo(progress)
            }

           override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {

            }
        })

    }

    fun bluetoothSwitchOff() {
        var REQUEST_ENABLE_BT = 1
        val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (mBluetoothAdapter != null) {
            if (mBluetoothAdapter.isEnabled) {
                mBluetoothAdapter.disable()
            }
        }
    }

    fun bluetoothSwitch() {
        var REQUEST_ENABLE_BT = 1
        val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (mBluetoothAdapter != null) {
            if (!mBluetoothAdapter.isEnabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            }
        }
    }

    fun wifiSwitch() {
        val wifiManager = this.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiManager.isWifiEnabled = true
    }

    fun wifiSwitchOff() {
        val wifiManager = this.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiManager.isWifiEnabled = false
    }

    fun onSwitchClicked(preference: Preference) {
        //Is the switch on?
        val on = (preference as SwitchPreference).isChecked

        if (on) {
            bluetoothSwitch()
        } else {
            bluetoothSwitchOff()
        }
    }

    fun onSearchDevicesClicked(v: View) {
        val intent = Intent(ACTION_BLUETOOTH_SETTINGS)
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        }
    }

    fun onSearchWifiClicked(v: View) {
        val intent = Intent(ACTION_WIFI_SETTINGS)
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        }
    }

    fun onSwitchWifiClicked(v: View) {
        val on = (v as SwitchPreference).isChecked

        if (on) {
            wifiSwitch()
        } else {
            wifiSwitchOff()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 111 && resultCode == RESULT_OK) {
            for (i in 0 until data?.clipData!!.itemCount) {
                val uri = data?.clipData?.getItemAt(i)?.uri
                songlist.add(uri)

            }

            selectfilesucess = true
            var playMusicbutton = findViewById<ImageView>(R.id.playMusicButton)
            playMusicbutton.isClickable = true
            updateMetaData()
        }
    }

    fun onFileChooserClicked(v: View) {
        val intent = Intent()
                .setType("video/*")
                .setAction(Intent.ACTION_GET_CONTENT)
                .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)

        startActivityForResult(Intent.createChooser(intent, "Select a file"), 111)
    }

    fun onMusicFileChooserClicked(v: View) {
        val intent = Intent()
                .setType("audio/*")
                .setAction(Intent.ACTION_GET_CONTENT)
                .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)

        startActivityForResult(Intent.createChooser(intent, "Select a file"), 111)
    }

    fun playMusic() {
        if (selectfilesucess && musicpausedposition == 0) {
            mediaPlayer = MediaPlayer().apply {
                setAudioStreamType(AudioManager.STREAM_MUSIC)
                setDataSource(applicationContext, songlist[currentIndexSongList])
                prepare()
                start()
            }
            var playbutton = findViewById<ImageView>(R.id.playMusicButton)
            var pausebutton = findViewById<ImageView>(R.id.pauseMusicButton)
            var duration = findViewById<TextView>(R.id.finaltimeMusic)
            playbutton.visibility = View.GONE
            pausebutton.visibility = View.VISIBLE
            duration.text = ConvertMStoMinutes(mediaPlayer!!.duration)
            musicseekBar.max = mediaPlayer!!.duration

        } else if (selectfilesucess && musicpausedposition != 0) {
            mediaPlayer = MediaPlayer().apply {
                setAudioStreamType(AudioManager.STREAM_MUSIC)
                setDataSource(applicationContext, songlist[currentIndexSongList])
                prepare()
                seekTo(musicpausedposition)
                start()
            }
            musicseekBar.max = mediaPlayer!!.duration
            val musicDuration = findViewById<TextView>(R.id.finaltimeMusic)
            musicDuration.text = ConvertMStoMinutes(mediaPlayer!!.duration)
            var playbutton = findViewById<ImageView>(R.id.playMusicButton)
            var pausebutton = findViewById<ImageView>(R.id.pauseMusicButton)
            playbutton.visibility = View.GONE
            pausebutton.visibility = View.VISIBLE
        }
        updateMetaData()


        musicSeekBarUpdateHandler.postDelayed(mUpdateSeekbar, 0);
        mediaPlayer!!.setOnCompletionListener {
            if (currentIndexSongList < (songlist.size - 1)) {
                currentIndexSongList += 1
                playMusic()
                //updateMetaData()
                val musicdata = findViewById<TextView>(R.id.MusicData)
                musicdata.text = "Index: $currentIndexSongList && List Size: ${songlist.size}"
            }
            else if (currentIndexSongList==(songlist.size - 1)) {
                currentIndexSongList = 0
                updateMetaData()
                var playbutton = findViewById<ImageView>(R.id.playMusicButton)
                var pausebutton = findViewById<ImageView>(R.id.pauseMusicButton)
                playbutton.visibility = View.VISIBLE
                pausebutton.visibility = View.GONE

            }
        }

    }

    fun onPlayMusicClicked(v: View) {
        playMusic()
    }

    fun onPauseMusicClicked(v: View) {
        if(mediaPlayer!!.isPlaying) {
            mediaPlayer!!.pause()
            var playbutton = findViewById<ImageView>(R.id.playMusicButton)
            var pausebutton = findViewById<ImageView>(R.id.pauseMusicButton)
            playbutton.visibility = View.VISIBLE
            pausebutton.visibility = View.GONE
            musicpausedposition = mediaPlayer!!.currentPosition

            musicSeekBarUpdateHandler.removeCallbacks(mUpdateSeekbar);

            //musicSeekBarUpdateHandler.removeCallbacks();
        }
    }

    fun onStopMusicClicked(v: View) {
        if (mediaPlayer!=null) {
            if (mediaPlayer!!.isPlaying) {
                mediaPlayer!!.release()
                musicpausedposition = 0
                currentIndexSongList = 0
                var playbutton = findViewById<ImageView>(R.id.playMusicButton)
                var pausebutton = findViewById<ImageView>(R.id.pauseMusicButton)
                playbutton.visibility = View.VISIBLE
                pausebutton.visibility = View.GONE
            }
        }
    }

    fun onNextMusicClicked(v: View) {
        if (currentIndexSongList < (songlist.size - 1)) {
            currentIndexSongList += 1
            mediaPlayer!!.release()
            musicpausedposition = 0
            updateMetaData()
            playMusic()
        }
    }

    fun onPreviousMusicClicked(v: View) {
        if (currentIndexSongList < songlist.size && currentIndexSongList > 0) {
            currentIndexSongList -= 1
            mediaPlayer!!.release()
            musicpausedposition = 0
            updateMetaData()
            playMusic()
        }
    }

    fun updateMetaData() {
        if (selectfilesucess && musicpausedposition == 0) {
            try {
                val mmr = FFmpegMediaMetadataRetriever()
                mmr.setDataSource(applicationContext, songlist[currentIndexSongList])
                var musictitle = mmr.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_TITLE)
                var musicartist = mmr.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_ARTIST)
                val b = mmr.getFrameAtTime(2000000, FFmpegMediaMetadataRetriever.OPTION_CLOSEST) // frame at 2 seconds
                val artwork = mmr.embeddedPicture
                val musicdata = findViewById<TextView>(R.id.MusicData)
                musicdata.text = "$musictitle - $musicartist"
                val image = BitmapFactory.decodeByteArray(artwork, 0, artwork.size)
                val musicartwork = findViewById<ImageView>(R.id.MusicAlbumArt)
                musicartwork.setImageBitmap(image)
                mmr.release()
            } catch (e: Exception) {
                e.localizedMessage
            }
        }
    }

    private fun playVideo(path: Uri?) {

        try {
            playerVideo.setVideoURI(path)
        } catch (e: Exception) {
            Log.e("Error", e.localizedMessage)
            e.printStackTrace()
        }
        playerVideo.requestFocus()
        playerVideo.setOnPreparedListener {
            playerVideo.seekTo(0)
            mediaController = MediaController(this)
            mediaController?.setAnchorView(playerVideo)
            playerVideo.setMediaController(mediaController)
            playerVideo.start()
            playervideoWidth = playerVideo.width
            playervideoHeigth = playerVideo.height
        }
    }

    fun onFullScreenClicked(v: View) {
        if (playerVideo.isPlaying) {
            if (!fullscreenclicked) {
                fullscreenclicked = true
                var fullscreenbutton = findViewById<ImageView>(R.id.fullscreenVideoButton)
                fullscreenbutton.setImageResource(R.drawable.exo_controls_fullscreen_exit)
                val metrics = DisplayMetrics()
                windowManager.defaultDisplay.getMetrics(metrics)
                val params = playerVideo.layoutParams as RelativeLayout.LayoutParams
                params.width = metrics.widthPixels
                params.height = metrics.heightPixels
                params.leftMargin = 0
                playerVideo.layoutParams = params
            } else {
                fullscreenclicked = false
                var fullscreenbutton = findViewById<ImageView>(R.id.fullScreenMusicButton)
                fullscreenbutton.setImageResource(R.drawable.exo_controls_fullscreen_enter)
                val metrics = DisplayMetrics()
                windowManager.defaultDisplay.getMetrics(metrics)
                val params = playerVideo.layoutParams as android.widget.RelativeLayout.LayoutParams
                params.width = (playervideoWidth).toInt()
                params.height = (playervideoHeigth).toInt()
                playerVideo.layoutParams = params
            }
        }
    }


    fun onStopVideoClicked(v: View) {
        if (playerVideo.isPlaying) {
            playerVideo.suspend()
        }
    }

    fun ConvertMStoMinutes(milliseconds: Int): String {
        val minutes = milliseconds / 1000 / 60
        val seconds = milliseconds / 1000 % 60
        return if(seconds<10){
            "0$minutes:0$seconds"
        }
        else {
            "0$minutes:$seconds"
        }
    }



}

