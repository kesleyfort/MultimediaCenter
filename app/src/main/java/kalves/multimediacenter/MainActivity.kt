package kalves.multimediacenter

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.support.design.widget.BottomNavigationView
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.ArrayAdapter
import android.widget.SeekBar
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.util.RepeatModeUtil.REPEAT_TOGGLE_MODE_ALL
import com.google.android.exoplayer2.util.RepeatModeUtil.REPEAT_TOGGLE_MODE_ONE
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.exo_playback_control_view.*
import wseemann.media.FFmpegMediaMetadataRetriever


class MainActivity : AppCompatActivity() {
    @SuppressLint("SetTextI18n")
    //Declare Variables
    var currentindex = 0
    private var selectfilesucess: Boolean = false
    private var selectVideofilesucess: Boolean = false
    private val musicSeekBarUpdateHandler = Handler()
    var songlist: MutableList<Uri?> = mutableListOf()
    var videolist: MutableList<Uri?> = mutableListOf()
    var playlistclicked: Boolean = false
    val mUpdateSeekbar = object : Runnable {
        override fun run() {
            seekBar?.progress = myService!!.mediaPlayer?.currentPosition!!.toInt()
            musicSeekBarUpdateHandler.postDelayed(this, 500)
        }
    }
    var isBound = false
    private var mediaplaylistcreated: Boolean = false
    var myService: Playback? = null
    private var serviceCreated: Boolean = false
    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.nav_music -> {
                if (video_frame.visibility == View.VISIBLE) {
                    video_frame.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right))
                    video_frame.visibility = View.GONE
                    music_frame.visibility = View.VISIBLE
                    music_frame.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left))
                }
                if (home_frame.visibility == View.VISIBLE) {
                    home_frame.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right))
                    home_frame.visibility = View.GONE
                    music_frame.visibility = View.VISIBLE
                    music_frame.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left))
                }

                return@OnNavigationItemSelectedListener true
            }
            R.id.nav_video -> {
                if (music_frame.visibility == View.VISIBLE) {
                    music_frame.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right))
                    music_frame.visibility = View.GONE
                    video_frame.visibility = View.VISIBLE
                    video_frame.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left))
                }
                if (home_frame.visibility == View.VISIBLE) {
                    home_frame.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right))
                    home_frame.visibility = View.GONE
                    video_frame.visibility = View.VISIBLE
                    video_frame.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left))
                }


                return@OnNavigationItemSelectedListener true
            }
            R.id.nav_home -> {
                if (video_frame.visibility == View.VISIBLE) {
                    video_frame.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right))
                    video_frame.visibility = View.GONE
                    home_frame.visibility = View.VISIBLE
                    home_frame.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left))
                }
                if (music_frame.visibility == View.VISIBLE) {
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
        startService(intent)
        bindService(intent, myConnection, Context.BIND_AUTO_CREATE)
        serviceCreated = true
        seekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    myService?.mediaPlayer?.seekTo(progress.toLong())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {

            }
        })
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        musicPlaylist.setOnItemClickListener { _, _, i, l ->
            myService?.mediaPlayer?.seekTo(i, 0)
        }
        PlayerControl.show()
        PlayerControl.showShuffleButton = false
        PlayerControl.showTimeoutMs = 30000000
        PlayerControl.repeatToggleModes = REPEAT_TOGGLE_MODE_ONE or REPEAT_TOGGLE_MODE_ALL

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
            updateMetaData()
            selectfilesucess = true
            updatePlaylist()
            myService?.createMediaSourcePlaylist(songlist)
            if (!mediaplaylistcreated) {
                myService?.playMusic()
                bindPlayer()
                mediaplaylistcreated = true
            }


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
        }
    }

    fun runSeekBar() {
        seekBar.max = myService?.mediaPlayer!!.duration.toInt()
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


    fun updateMetaData() {
        try {
            val mmr = FFmpegMediaMetadataRetriever()
            mmr.setDataSource(applicationContext, songlist[currentindex])
            var musictitle = mmr.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_TITLE)
            var musicartist = mmr.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_ARTIST)
            var artwork = mmr.embeddedPicture
            mmr.release()
            musicData.text = "$musictitle\n$musicartist"
            val image = BitmapFactory.decodeByteArray(artwork, 0, artwork.size)
            MusicCoverArtView.setImageBitmap(image)
            runSeekBar()
        } catch (e: Exception) {
            e.localizedMessage
        }
    }

    fun bindPlayer(){
        var player = myService?.mediaPlayer
        PlayerControl.player = player
        if (player != null) player.playWhenReady = false
        player?.addListener(object : Player.DefaultEventListener() {
            override fun onPlayerStateChanged(
                    playWhenReady: Boolean, playbackState: Int) {
                when (playbackState) {
                    Player.STATE_IDLE -> {
                        updateMetaData()
                    }
                    Player.STATE_BUFFERING -> {
                    }
                    Player.STATE_READY -> {
                        runSeekBar()
                    }
                    Player.STATE_ENDED -> {
                    }

                }
            }

            override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {
                super.onTracksChanged(trackGroups, trackSelections)
                currentindex = player!!.currentWindowIndex
                updateMetaData()
            }

            override fun onPositionDiscontinuity(reason: Int) {
                super.onPositionDiscontinuity(reason)
                if(reason==Player.DISCONTINUITY_REASON_PERIOD_TRANSITION) {
                    Log.i("DISCONTINUITY", "DISCONTINUITY_REASON_PERIOD_TRANSITION")
                    currentindex = player!!.currentPeriodIndex
                    updateMetaData()
                }
            }
        })

    }
    fun onPlaylistMusicClicked(v: View) {
        if (!playlistclicked) {
            MusicCoverArtView.animate().withLayer()
                    .rotationY(90f)
                    .setDuration(500)
                    .withEndAction {
                        run {

                            musicPlaylist.visibility = View.VISIBLE
                            MusicCoverArtView.visibility = View.GONE
                            // second quarter turn
                            MusicCoverArtView.rotationY = -90f
                            MusicCoverArtView.animate().withLayer()
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
                            MusicCoverArtView.visibility = View.VISIBLE
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

        val songnames: MutableList<String> = mutableListOf()
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

}

