package kalves.multimediacenter

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.RemoteException
import android.support.design.widget.BottomNavigationView
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v7.app.AppCompatActivity
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.*
import kotlinx.android.synthetic.main.activity_main.*
import wseemann.media.FFmpegMediaMetadataRetriever


class MainActivity : AppCompatActivity() {

    @SuppressLint("SetTextI18n")
    //Declare Variables
    private var mediaController: MediaController? = null
    private var fullscreenclicked: Boolean = false
    private var isPaused: Boolean = false
    private var playervideoWidth: Int = 0
    private var playervideoHeigth: Int = 0
    private var mediaPlayer: MediaPlayer? = null
    private var selectfilesucess: Boolean = false
    private var selectVideofilesucess: Boolean = false
    private var musicpausedposition: Int = 0
    private val musicSeekBarUpdateHandler = Handler()
    private var songlist: MutableList<Uri?> = mutableListOf()
    private var videolist: MutableList<Uri?> = mutableListOf()
    private var originalsonglist: MutableList<Uri?> = mutableListOf()
    private var shuffledlist: MutableList<Uri?> = mutableListOf()
    private var shuffleclicked: Boolean = false
    private var repeatclicked: Boolean = false
    private var currentIndexSongList = 0
    private var playlistclicked: Boolean = false
    private var mMediaBrowserCompat: MediaBrowserCompat? = null
    private var mMediaControllerCompat: MediaControllerCompat? = null
    private val mUpdateSeekbar = object : Runnable {
        override fun run() {
            if(isPaused){
                musicseekBar?.progress = myService!!.getPausedPosition()
            }
            else {
                musicseekBar?.progress = myService!!.getCurrentPlaybackPosition()
            }
            initialtimeMusic.text = convertMStoMinutes(myService!!.getCurrentPlaybackPosition())
            if (musicseekBar.progress == 0) {
                finaltimeMusic.text = convertMStoMinutes(myService!!.getDuration())
            }
                musicSeekBarUpdateHandler.postDelayed(this, 500)
        }
    }
    var myService: Playback? = null
    var isBound = false

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.nav_music -> {
                if(video_frame.visibility==View.VISIBLE) {
                    video_frame.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right))
                    video_frame.visibility = View.GONE
                    music_frame.visibility = View.VISIBLE
                    music_frame.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left))
                }
                if(home_frame.visibility==View.VISIBLE) {
                    home_frame.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right))
                    home_frame.visibility = View.GONE
                    music_frame.visibility = View.VISIBLE
                    music_frame.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left))
                }

                return@OnNavigationItemSelectedListener true
            }
            R.id.nav_video -> {
                if(music_frame.visibility==View.VISIBLE) {
                    music_frame.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right))
                    music_frame.visibility = View.GONE
                    video_frame.visibility = View.VISIBLE
                    video_frame.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left))
                }
                if(home_frame.visibility==View.VISIBLE) {
                    home_frame.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right))
                    home_frame.visibility = View.GONE
                    video_frame.visibility = View.VISIBLE
                    video_frame.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left))
                }


                return@OnNavigationItemSelectedListener true
            }
            R.id.nav_home -> {
                if(video_frame.visibility==View.VISIBLE) {
                    video_frame.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right))
                    video_frame.visibility = View.GONE
                    home_frame.visibility = View.VISIBLE
                    home_frame.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left))
                }
                if(music_frame.visibility==View.VISIBLE) {
                    music_frame.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right))
                    music_frame.visibility = View.GONE
                    home_frame.visibility = View.VISIBLE
                    home_frame.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left))
                }
                return@OnNavigationItemSelectedListener true
            }
            R.id.nav_settings -> {
                /*val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                if (mBluetoothAdapter != null) {
                    switchBT.isChecked = mBluetoothAdapter.isEnabled
                }
                val wifiManager = this.getSystemService(Context.WIFI_SERVICE) as WifiManager
                switchWifi.isChecked = wifiManager.isWifiEnabled
                TextoWifi.text = "Connected to ${wifiManager.connectionInfo.ssid}"*/
                startActivity(Intent(this@MainActivity, Settings::class.java))
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
        musicseekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser){
                    if (!myService!!.mediaPlayerIsNull())
                    {
                        if (!myService!!.mediaPlayerIsNull()) {
                            if (isPaused) {
                                myService!!.setMusicPausedPosition(progress)
                                musicseekBar.progress = myService!!.getPausedPosition()
                            } else {
                                myService!!.setPositionFromSeekBar(progress)
                                musicseekBar.progress = myService!!.getCurrentPlaybackPosition()
                            }
                        }
                    }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {

            }
        })
        //window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        musicPlaylist.setOnItemClickListener { adapterView, view, i, l ->
            currentIndexSongList = i
                if (myService!!.isMediaPlaying()) {
                    myService!!.releasePlayer()
                    myService!!.setCurrentSong(i)
                    myService!!.setMusicPausedPosition(0)
                    updateMetaData()
                    myService!!.playSongs()
                }
                playMusicButton.visibility = View.GONE
                pauseMusicButton.visibility = View.VISIBLE
                runSeekBar()
                finaltimeMusic.text = convertMStoMinutes(myService!!.getDuration())
                playlistclicked = false

            }
        val myConnection = object : ServiceConnection {
            override fun onServiceConnected(className: ComponentName,
                                            service: IBinder) {
                val binder = service as Playback.MyLocalBinder
                myService = binder.getService()
                isBound = true
            }

            override fun onServiceDisconnected(name: ComponentName) {
                isBound = false
            }
        }
        val intent = Intent(this, Playback::class.java)
        bindService(intent, myConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 111 && resultCode == RESULT_OK && data != null) {
            val clipdata = data.clipData
            if (clipdata != null) {
                for (i in 0 until data.clipData!!.itemCount) {
                    val uri = data.clipData?.getItemAt(i)?.uri
                    songlist.add(uri)

                }
            } else {
                val uri = data.data
                songlist.add(uri)
            }
            selectfilesucess = true
            updatePlaylist()
            if(originalsonglist.isNotEmpty()){
                originalsonglist.clear()
            }
            originalsonglist.addAll(songlist)
            myService!!.createPlaylist(originalsonglist)
            updateMetaData()

        }

        if (requestCode == 112 && resultCode == RESULT_OK && data != null) {
            val clipdata = data.clipData
            if (clipdata != null) {
                for (i in 0 until data.clipData!!.itemCount) {
                    val uri = data.clipData?.getItemAt(i)?.uri
                    videolist.add(uri)

                }
            } else {
                val uri = data.data
                videolist.add(uri)
            }
            selectVideofilesucess = true
            playVideo(videolist[0])
        }
    }


    fun runSeekBar(){
        musicseekBar.max = myService!!.getDuration()
        mUpdateSeekbar.run()
    }


    fun onFileChooserClicked(v: View) {
        val intent = Intent()
                .setType("video_frame/*")
                .setAction(Intent.ACTION_GET_CONTENT)
                .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)

        startActivityForResult(Intent.createChooser(intent, "Select a file"), 112)
    }


    fun onMusicFileChooserClicked(v: View) {
        val intent = Intent()
                .setType("audio/*")
                .setAction(Intent.ACTION_GET_CONTENT)
                .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)

        startActivityForResult(Intent.createChooser(intent, "Select a file"), 111)
    }


    fun onPlayMusicClicked(v: View) {
        if (!myService!!.mediaPlayerIsNull() && songlist.isNotEmpty()) {
            if(isPaused){
                musicseekBar.progress = myService!!.getPausedPosition()
                myService!!.setPausedState(isPaused)
                myService!!.playSongs()
            }
            else{
                myService!!.setPausedState(isPaused)
                myService!!.playSongs()
            }
            playMusicButton.visibility = View.GONE
            pauseMusicButton.visibility = View.VISIBLE
            finaltimeMusic.text = convertMStoMinutes(myService!!.getSeekBarMaximum())
            musicseekBar.max = myService!!.getSeekBarMaximum()
            isPaused = false
            initialtimeMusic.text = convertMStoMinutes(myService!!.getPausedPosition())
            myService!!.showPlayingNotification()
            musicseekBar.progress = 0
            runSeekBar()
        }

    }

    fun onPauseMusicClicked(v: View) {
        isPaused = true
        if (myService!!.isMediaPlaying()) {
            myService!!.pauseMusic()
            myService!!.setPausedState(isPaused)
            var playbutton = findViewById<ImageView>(R.id.playMusicButton)
            var pausebutton = findViewById<ImageView>(R.id.pauseMusicButton)
            playbutton.visibility = View.VISIBLE
            pausebutton.visibility = View.GONE
            musicSeekBarUpdateHandler.removeCallbacks(mUpdateSeekbar)
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            myService!!.showPausedNotification()
        }
    }

    fun onStopMusicClicked(v: View) {
        musicSeekBarUpdateHandler.removeCallbacks(mUpdateSeekbar)
        myService!!.releasePlayer()
        musicseekBar.progress = 0
        MusicAlbumArt.setImageResource(R.drawable.nocoverart)
        MusicData.text = " - "
        initialtimeMusic.text = "00:00"
        finaltimeMusic.text = "00:00"
        var playbutton = findViewById<ImageView>(R.id.playMusicButton)
        var pausebutton = findViewById<ImageView>(R.id.pauseMusicButton)
        playbutton.visibility = View.VISIBLE
        pausebutton.visibility = View.GONE
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    fun onNextMusicClicked(v: View) {
        myService!!.nextSong()
        musicseekBar.max = myService!!.getDuration()
        updateMetaData()
        myService!!.showPlayingNotification()
        musicseekBar.progress = 0
        runSeekBar()
    }

    fun onPreviousMusicClicked(v: View) {
        myService!!.previousSong()
        musicseekBar.max = myService!!.getDuration()
        updateMetaData()
        myService!!.showPlayingNotification()
        musicseekBar.progress = 0
        runSeekBar()
    }

    fun updateMetaData() {
    val metadata = myService!!.getMetaData()
        MusicData.text = "${metadata.second} - ${metadata.first}"
        val image = BitmapFactory.decodeByteArray(metadata.third, 0, metadata.third.size)
        MusicAlbumArt.setImageBitmap(image)
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
                var fullscreenbutton = findViewById<ImageView>(R.id.fullscreenVideoButton)
                fullscreenbutton.setImageResource(R.drawable.exo_controls_fullscreen_enter)
                val metrics = DisplayMetrics()
                windowManager.defaultDisplay.getMetrics(metrics)
                val params = playerVideo.layoutParams as android.widget.RelativeLayout.LayoutParams
                params.width = (playervideoWidth)
                params.height = (playervideoHeigth)
                playerVideo.layoutParams = params
            }
        }
    }


    fun onStopVideoClicked(v: View) {
        if (playerVideo.isPlaying) {
            playerVideo.suspend()
        }
    }

    fun onShuffleMusicClicked(v: View) {
        if(selectfilesucess) {
            if (!shuffleclicked) {
                DrawableCompat.setTint(shuffleMusicButton.drawable, ContextCompat.getColor(applicationContext, R.color.colorAccent));
                shuffleclicked = true
                shuffleMusic()
            } else {
                DrawableCompat.setTint(shuffleMusicButton.drawable, ContextCompat.getColor(applicationContext, R.color.colorPrimary));
                songlist.clear()
                songlist.addAll(originalsonglist)
                updatePlaylist()
                myService!!.setCurrentSong(0)
                updateMetaData()
                if (!myService!!.mediaPlayerIsNull())
                    myService!!.releasePlayer()
                myService!!.playSongs()
                shuffleclicked = false
            }
        }
    }

    fun onRepeatMusicClicked(v: View) {
        if (!repeatclicked) {
            DrawableCompat.setTint(repeatButton.drawable, ContextCompat.getColor(applicationContext, R.color.colorAccent));
            repeatclicked = true
            myService!!.setRepeat(repeatclicked)
        } else {
            DrawableCompat.setTint(repeatButton.drawable, ContextCompat.getColor(applicationContext, R.color.colorPrimary));
            repeatclicked = false
            myService!!.setRepeat(repeatclicked)
        }
    }

    fun onPlaylistMusicClicked(v: View) {
        if (!playlistclicked) {
            MusicAlbumArt.animate().withLayer()
                    .rotationY(90f)
                    .setDuration(500)
                    .withEndAction {
                        run {

                            musicPlaylist.visibility = View.VISIBLE
                            MusicAlbumArt.visibility = View.GONE
                            // second quarter turn
                            MusicAlbumArt.rotationY = -90f
                            MusicAlbumArt.animate().withLayer()
                                    .rotationY(0f)
                                    .setDuration(500)
                                    .start()
                        }
                    }.start()
            playlistclicked = true
        } else {
            musicPlaylist.animate().withLayer()
                    .rotationY(90f)
                    .setDuration(500)
                    .withEndAction {
                        run {
                            MusicAlbumArt.visibility = View.VISIBLE
                            musicPlaylist.visibility = View.GONE

                            // second quarter turn
                            musicPlaylist.rotationY = -90f
                            musicPlaylist.animate().withLayer()
                                    .rotationY(0f)
                                    .setDuration(500)
                                    .start()
                        }
                    }.start()
            playlistclicked = false
        }
    }

    private fun updatePlaylist() {

        var songnames: MutableList<String> = mutableListOf()
            for (i in 0 until songlist.size) {
                val mmr = FFmpegMediaMetadataRetriever()
                mmr.setDataSource(applicationContext, songlist[i])
                var musictitle = mmr.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_TITLE)
                var musicartist = mmr.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_ARTIST)
                songnames.add("\"$musictitle\",\n$musicartist")

            }
            // Create an ArrayAdapter from List
            val arrayAdapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, songnames)

            // DataBind ListView with items from ArrayAdapter
            musicPlaylist.adapter = arrayAdapter
        }

    private fun shuffleMusic(){
        songlist.shuffle()
        myService!!.clearPlaylist()
        myService!!.createPlaylist(songlist)
        if(!myService!!.mediaPlayerIsNull()) {
            myService!!.releasePlayer()
            myService!!.setMusicPausedPosition(0)
            myService!!.setCurrentSong(0)
            myService!!.playSongs()
            updatePlaylist()
        }
    }

    private fun convertMStoMinutes(milliseconds: Int): String {
        val minutes = milliseconds / 1000 / 60
        val seconds = milliseconds / 1000 % 60
        return if (seconds < 10) {
            "0$minutes:0$seconds"
        } else {
            "0$minutes:$seconds"
        }
    }

}

