package kalves.multimediacenter

import android.annotation.TargetApi
import android.app.*
import android.content.*
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.service.media.MediaBrowserService
import android.support.annotation.Nullable
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaButtonReceiver
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.TextUtils
import wseemann.media.FFmpegMediaMetadataRetriever
import android.support.v4.os.HandlerCompat.postDelayed
import android.content.Intent
import android.graphics.Color
import android.os.*
import android.support.v4.app.NotificationCompat
import android.view.View
import android.widget.ImageView
import br.com.goncalves.pugnotification.notification.PugNotification


class Playback : Service(), AudioManager.OnAudioFocusChangeListener {
    //Variables
    private var musicpausedposition: Int = 0
    private var shuffleclicked: Boolean = false
    private var repeatclicked: Boolean = false
    private var currentIndexSongList = 0
    private var playlistclicked: Boolean = false
    private var mMediaPlayer: MediaPlayer? = null
    private var songlist: MutableList<Uri?> = mutableListOf<Uri?>()
    private var videolist: MutableList<Uri?> = mutableListOf()
    private var originalsonglist: MutableList<Uri?> = mutableListOf<Uri?>()
    private var shuffledlist: MutableList<Uri?> = mutableListOf<Uri?>()
    var musictitle: String = ""
    var musicartist: String = ""
    var artwork: ByteArray = byteArrayOf(0)

    private var mMediaSessionCompat: MediaSessionCompat? = null
    private val mNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (mMediaPlayer != null && mMediaPlayer!!.isPlaying) {
                mMediaPlayer!!.pause()
            }
        }
    }
    private var notificationManager: NotificationManager? = null
    //end of variables
    //Initiate Binder to bind the service to the app
    private val myBinder = MyLocalBinder()

    override fun onBind(intent: Intent): IBinder {
        return myBinder
    }

    inner class MyLocalBinder : Binder() {
        fun getService(): Playback {
            return this@Playback
        }
    }
    //end of Binder


    fun createPlaylist(playlist: MutableList<Uri?>) {
        songlist.addAll(playlist)
    }

    fun playSongs() {

        if (musicpausedposition == 0) {
            mMediaPlayer = MediaPlayer().apply {
                setAudioStreamType(AudioManager.STREAM_MUSIC)
                setDataSource(applicationContext, songlist[currentIndexSongList])
                prepare()
                start()
            }
        } else {
            mMediaPlayer = MediaPlayer().apply {
                setAudioStreamType(AudioManager.STREAM_MUSIC)
                setDataSource(applicationContext, songlist[currentIndexSongList])
                prepare()
                seekTo(musicpausedposition)
                start()
            }

        }
        mMediaPlayer!!.setOnCompletionListener {
            if (currentIndexSongList < (songlist.size - 1)) {
                currentIndexSongList += 1
                playSongs()
            } else if (currentIndexSongList == (songlist.size - 1)) {
                if (repeatclicked) {
                    currentIndexSongList = 0
                    playSongs()
                } else {
                    currentIndexSongList = 0
                }

            }
        }
        mMediaSessionCompat!!.isActive = true
        setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING)
    }

    private val mMediaSessionCallback = object : MediaSessionCompat.Callback() {

        override fun onPlay() {
            super.onPlay()
            if (successfullyRetrievedAudioFocus()) {
                mMediaSessionCompat!!.isActive = true;
                setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING)
                mMediaPlayer!!.start()
            }
        }

        override fun onPause() {
            super.onPause()
            if (mMediaPlayer!!.isPlaying) {
                mMediaPlayer!!.pause()
                setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED)
            }
        }

        override fun onSkipToNext() {
            super.onSkipToNext()
            if (currentIndexSongList < (songlist.size - 1)) {
                currentIndexSongList += 1
                mMediaPlayer!!.release()
                musicpausedposition = 0
                playSongs()
            }

        }

        override fun onSkipToPrevious() {
            super.onSkipToPrevious()
            if (currentIndexSongList < songlist.size && currentIndexSongList > 0) {
                currentIndexSongList -= 1
                mMediaPlayer!!.release()
                musicpausedposition = 0
                playSongs()
            }
        }

        override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
            super.onPlayFromMediaId(mediaId, extras)
        }
    }

    @Nullable
    fun onGetRoot(clientPackageName: String, clientUid: Int, @Nullable rootHints: Bundle): MediaBrowserService.BrowserRoot? {
        return if (TextUtils.equals(clientPackageName, packageName)) {
            MediaBrowserService.BrowserRoot(getString(R.string.app_name), null)
        } else null

    }

    //Not important for general audio service, required for class
    fun onLoadChildren(parentId: String, result: MediaBrowserService.Result<List<MediaBrowserCompat.MediaItem>>) {
        result.sendResult(null)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        initMediaPlayer()
        initMediaSession()
        initNoisyReceiver()
        MediaButtonReceiver.handleIntent(mMediaSessionCompat, intent)
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCreate() {
        super.onCreate()

        initMediaPlayer()
        initMediaSession()
        initNoisyReceiver()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {

        PugNotification.with(this)
                .load()
                .title("Notification")
                .message("Mensagem")
                .bigTextStyle("Big Text")
                .smallIcon(R.drawable.pugnotification_ic_launcher)
                .largeIcon(R.drawable.pugnotification_ic_launcher)
                .flags(Notification.FLAG_FOREGROUND_SERVICE)
                .simple()
                .build()
    }


    private fun initMediaPlayer() {
        mMediaPlayer = MediaPlayer()
        mMediaPlayer!!.setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
        mMediaPlayer!!.setAudioStreamType(AudioManager.STREAM_MUSIC)
        mMediaPlayer!!.setVolume(1.0f, 1.0f)
    }

    private fun initMediaSession() {
        val mediaButtonReceiver = ComponentName(applicationContext, MediaButtonReceiver::class.java)
        mMediaSessionCompat = MediaSessionCompat(applicationContext, "Tag", mediaButtonReceiver, null)

        mMediaSessionCompat!!.setCallback(mMediaSessionCallback)
        mMediaSessionCompat!!.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)

        val mediaButtonIntent = Intent(Intent.ACTION_MEDIA_BUTTON)
        mediaButtonIntent.setClass(this, MediaButtonReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent, 0)
        mMediaSessionCompat!!.setMediaButtonReceiver(pendingIntent)

    }

    private fun initNoisyReceiver() {
        //Handles headphones coming unplugged. cannot be done through a manifest receiver
        val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        registerReceiver(mNoisyReceiver, filter)
    }

    private fun successfullyRetrievedAudioFocus(): Boolean {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val result = audioManager.requestAudioFocus(this,
                AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)

        return result == AudioManager.AUDIOFOCUS_GAIN
    }

    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                if (mMediaPlayer!!.isPlaying()) {
                    mMediaPlayer!!.stop()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                mMediaPlayer!!.pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                if (mMediaPlayer != null) {
                    mMediaPlayer!!.setVolume(0.3f, 0.3f)
                }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (mMediaPlayer != null) {
                    if (!mMediaPlayer!!.isPlaying()) {
                        mMediaPlayer!!.start()
                    }
                    mMediaPlayer!!.setVolume(1.0f, 1.0f)
                }
            }
        }
    }

    private fun setMediaPlaybackState(state: Int) {
        val playbackstateBuilder = PlaybackStateCompat.Builder()
        if (state == PlaybackStateCompat.STATE_PLAYING) {
            playbackstateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_PAUSE)
        } else {
            playbackstateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_PLAY)
        }
        if (state == PlaybackStateCompat.STATE_SKIPPING_TO_NEXT) {
            playbackstateBuilder.setActions(PlaybackStateCompat.ACTION_SKIP_TO_NEXT)
        }
        if (state == PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS) {
            playbackstateBuilder.setActions(PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
        }
        playbackstateBuilder.setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 0f)
        mMediaSessionCompat!!.setPlaybackState(playbackstateBuilder.build())
    }

    private fun showPlayingNotification() {

    }

    private fun showPausedNotification() {

    }

    override fun onDestroy() {
        super.onDestroy()
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.abandonAudioFocus(this)
        unregisterReceiver(mNoisyReceiver)
        mMediaSessionCompat!!.release()
        NotificationManagerCompat.from(this).cancel(1)
    }

    fun getDuration(): Int {
        return mMediaPlayer!!.duration
    }

    fun getPausedPosition(): Int {
        return musicpausedposition
    }

    fun getCurrentSong(): Int {
        return currentIndexSongList
    }

    fun getCurrentPlaybackPosition(): Int {
        return mMediaPlayer!!.currentPosition
    }

    fun setPositionFromSeekBar(position: Int) {
        if (mMediaPlayer != null && mMediaPlayer!!.isPlaying) {
            mMediaPlayer!!.seekTo(position)
        }
    }

    fun mediaPlayerIsNull(): Boolean {
        return mMediaPlayer == null
    }

    fun getSeekBarMaximum(): Int {
        return mMediaPlayer!!.duration
    }

    fun isMediaPlaying(): Boolean {
        return mMediaPlayer!!.isPlaying
    }

    fun pauseMusic() {
        mMediaPlayer!!.pause()
        musicpausedposition = mMediaPlayer!!.currentPosition
    }

    fun setCurrentSong(currentSong: Int) {
        currentIndexSongList = currentSong
    }

    fun releasePlayer() {
        mMediaPlayer!!.stop()
        mMediaPlayer!!.release()
    }

    fun setMusicPausedPosition(position: Int) {
        musicpausedposition = position
    }

    fun setRepeat(clicked: Boolean) {
        repeatclicked = clicked
    }

    fun getMetaData(): Triple<String, String, ByteArray> {
        if (musicpausedposition == 0) {
            try {
                val mmr = FFmpegMediaMetadataRetriever()
                mmr.setDataSource(applicationContext, songlist[currentIndexSongList])
                musictitle = mmr.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_TITLE)
                musicartist = mmr.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_ARTIST)
                artwork = mmr.embeddedPicture
                mmr.release()

            } catch (e: Exception) {
                e.localizedMessage
            }
        }
        return Triple(musicartist, musictitle, artwork)
    }

    fun getMediaPlayerInstance(): MediaPlayer? {
        return mMediaPlayer
    }

}
