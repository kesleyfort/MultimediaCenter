package kalves.multimediacenter

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.support.design.widget.BottomNavigationView
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v7.app.AppCompatActivity
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.*
import kotlinx.android.synthetic.main.activity_main.*
import wseemann.media.FFmpegMediaMetadataRetriever
import android.widget.ArrayAdapter
import java.util.*
import java.util.Arrays.asList
import kotlin.collections.ArrayList
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.opengl.ETC1.getHeight
import android.support.v4.view.ViewCompat.animate
import android.R.attr.translationY
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.*
import android.view.animation.AnimationUtils
import io.reactivex.*;


open class MainActivity : AppCompatActivity() {

    @SuppressLint("SetTextI18n")
    //Declare Variables
    private var mediaController: MediaController? = null
    private var fullscreenclicked: Boolean = false
    private var playervideoWidth: Int = 0
    private var playervideoHeigth: Int = 0
    private var mediaPlayer: MediaPlayer? = null
    private var selectfilesucess: Boolean = false
    private var selectVideofilesucess: Boolean = false
    private var musicpausedposition: Int = 0
    private val musicSeekBarUpdateHandler = Handler()
    private var songlist: MutableList<Uri?> = mutableListOf<Uri?>()
    private var videolist: MutableList<Uri?> = mutableListOf()
    private var originalsonglist: MutableList<Uri?> = mutableListOf<Uri?>()
    private var shuffledlist: MutableList<Uri?> = mutableListOf<Uri?>()
    private var shuffleclicked: Boolean = false
    private var repeatclicked: Boolean = false
    private var currentIndexSongList = 0
    private var playlistclicked: Boolean = false
    val mUpdateSeekbar = object : Runnable {
        override fun run() {
            musicseekBar?.progress = myService!!.getCurrentPlaybackPosition()
            musicSeekBarUpdateHandler.postDelayed(this, 500)
            initialtimeMusic.text = convertMStoMinutes(myService!!.getCurrentPlaybackPosition())
            if(musicseekBar.progress<500) {
                updateMetaData()
                finaltimeMusic.text = convertMStoMinutes(myService!!.getDuration())
            }

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
                        if (!myService!!.mediaPlayerIsNull())
                            myService!!.setPositionFromSeekBar(progress)
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
            duration.text = convertMStoMinutes(mediaPlayer!!.duration)
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
            musicDuration.text = convertMStoMinutes(mediaPlayer!!.duration)
            var playbutton = findViewById<ImageView>(R.id.playMusicButton)
            var pausebutton = findViewById<ImageView>(R.id.pauseMusicButton)
            playbutton.visibility = View.GONE
            pausebutton.visibility = View.VISIBLE
        }
        updateMetaData()

        if (selectfilesucess) {
            musicSeekBarUpdateHandler.postDelayed(mUpdateSeekbar, 0);

        }


    }

    fun onPlayMusicClicked(v: View) {
        if (!myService!!.mediaPlayerIsNull()) {
            myService!!.playSongs()
            var playbutton = findViewById<ImageView>(R.id.playMusicButton)
            var pausebutton = findViewById<ImageView>(R.id.pauseMusicButton)
            var duration = findViewById<TextView>(R.id.finaltimeMusic)
            playbutton.visibility = View.GONE
            pausebutton.visibility = View.VISIBLE
            duration.text = convertMStoMinutes(myService!!.getSeekBarMaximum())
            musicseekBar.max = myService!!.getSeekBarMaximum()
            mUpdateSeekbar.run()
        } else if (selectfilesucess && myService!!.getPausedPosition() != 0) {

        }

    }

    fun onPauseMusicClicked(v: View) {
        if (myService!!.isMediaPlaying()) {
            myService!!.pauseMusic()
            var playbutton = findViewById<ImageView>(R.id.playMusicButton)
            var pausebutton = findViewById<ImageView>(R.id.pauseMusicButton)
            playbutton.visibility = View.VISIBLE
            pausebutton.visibility = View.GONE
            musicSeekBarUpdateHandler.removeCallbacks(mUpdateSeekbar)
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    fun onStopMusicClicked(v: View) {
        if (mediaPlayer != null) {
            if (mediaPlayer!!.isPlaying) {
                mediaPlayer!!.release()
                musicpausedposition = 0
                currentIndexSongList = 0
                var playbutton = findViewById<ImageView>(R.id.playMusicButton)
                var pausebutton = findViewById<ImageView>(R.id.pauseMusicButton)
                playbutton.visibility = View.VISIBLE
                pausebutton.visibility = View.GONE
                musicSeekBarUpdateHandler.removeCallbacks(mUpdateSeekbar)
                musicseekBar.progress = 0
                initialtimeMusic.text = "00:00"
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
            mediaPlayer = null
        }
    }

    fun onNextMusicClicked(v: View) {
        currentIndexSongList = myService!!.getCurrentSong()
        if (currentIndexSongList < (songlist.size - 1)) {
            currentIndexSongList += 1
            myService!!.releasePlayer()
            myService!!.setCurrentSong(currentIndexSongList)
            myService!!.setMusicPausedPosition(0)
            updateMetaData()
            myService!!.playSongs()
        }
    }

    fun onPreviousMusicClicked(v: View) {
        currentIndexSongList = myService!!.getCurrentSong()
        if (currentIndexSongList < songlist.size && currentIndexSongList > 0) {
            currentIndexSongList -= 1
            myService!!.releasePlayer()
            myService!!.setCurrentSong(currentIndexSongList)
            myService!!.setMusicPausedPosition(0)
            updateMetaData()
            myService!!.playSongs()
        }
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
            if(!myService!!.mediaPlayerIsNull())
                myService!!.releasePlayer()
                myService!!.playSongs()
            shuffleclicked = false
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
                songnames.add("$musictitle - $musicartist")

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

