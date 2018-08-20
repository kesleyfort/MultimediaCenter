package kalves.multimediacenter

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.*
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.service.media.MediaBrowserService
import android.support.annotation.Nullable
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserServiceCompat
import android.support.v4.media.session.MediaButtonReceiver
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.TextUtils
import android.util.Log
import wseemann.media.FFmpegMediaMetadataRetriever


class Playback :  MediaBrowserServiceCompat(), AudioManager.OnAudioFocusChangeListener {
    override fun onLoadChildren(p0: String, p1: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onGetRoot(p0: String, p1: Int, p2: Bundle?): BrowserRoot? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    //Variables
    private var musicpausedposition: Int = 0
    private var repeatclicked: Boolean = false
    private var isPaused: Boolean = false
    private var currentIndexSongList = 0
    private var mMediaPlayer: MediaPlayer? = null
    private var songlist: MutableList<Uri?> = mutableListOf()
    private var musictitle: String = ""
    private var musicartist: String = ""
    private var artwork: ByteArray = byteArrayOf(0)

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
    private val mMediaSessionCallback = object : MediaSessionCompat.Callback() {

        override fun onPlay() {
            super.onPlay()
            if (!successfullyRetrievedAudioFocus()) {
                return
            }
            Log.i("MediaSessionCalback", "ONPLAY")
            mMediaSessionCompat!!.isActive = true
            setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING)

            showPlayingNotification()
            mMediaPlayer!!.start()
        }

        override fun onPause() {
            super.onPause()
            Log.i("MediaSessionCalback", "ONPAUSE")
            mMediaPlayer!!.pause()
            setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED)
            showPausedNotification()
        }

        override fun onSkipToNext() {
            super.onSkipToNext()
            nextSong()
            showPlayingNotification()

        }

        override fun onSkipToPrevious() {
            super.onSkipToPrevious()
            previousSong()
            showPlayingNotification()
        }

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        MediaButtonReceiver.handleIntent(mMediaSessionCompat, intent)
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCreate() {
        super.onCreate()
        Log.i("On Create", "ON CREATE RUNNED")
        initMediaPlayer()
        initMediaSession()
        initNoisyReceiver()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    }


    private fun initMediaPlayer() {
        mMediaPlayer = MediaPlayer()
        mMediaPlayer!!.setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
        mMediaPlayer!!.setAudioStreamType(AudioManager.STREAM_MUSIC)
        mMediaPlayer!!.setVolume(1.0f, 1.0f)
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
        sessionToken = mMediaSessionCompat!!.sessionToken


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
            playbackstateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_PAUSE or PlaybackStateCompat.ACTION_SKIP_TO_NEXT or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
        } else {
            playbackstateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_SKIP_TO_NEXT or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
        }
        playbackstateBuilder.setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 0f)
        mMediaSessionCompat!!.setPlaybackState(playbackstateBuilder.build())
    }
    fun createPlaylist(playlist: MutableList<Uri?>) {
        songlist.addAll(playlist)
    }

    fun playSongs() {
        if(mMediaPlayer==null){
            initMediaPlayer()
        }
        if(isPaused) {
            mMediaPlayer!!.start()
            setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING)
            showPausedNotification()
        }
        if (songlist.isNotEmpty()) {
            if (!isPaused) {
                mMediaPlayer = MediaPlayer().apply {
                    setAudioStreamType(AudioManager.STREAM_MUSIC)
                    setDataSource(applicationContext, songlist[currentIndexSongList])
                    prepareAsync()
                    setOnPreparedListener {
                        start()
                    }
                }
            }
            mMediaSessionCompat!!.isActive = true
            setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING)
            mMediaPlayer!!.setOnCompletionListener {
                nextSong()
                showPlayingNotification()
            }
        }
    }

    fun showPlayingNotification() {
        val builder = MediaStyleHelper.from(this@Playback, mMediaSessionCompat, getMetaData())
        builder.addAction(NotificationCompat.Action(R.drawable.previous, "Previous", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)))
        builder.addAction(NotificationCompat.Action(R.drawable.pause, "Pause", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY_PAUSE)))
        builder.addAction(NotificationCompat.Action(R.drawable.next, "Next", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_NEXT)))
        builder.setStyle(android.support.v4.media.app.NotificationCompat.MediaStyle().setShowActionsInCompactView(0,1,2).setMediaSession(mMediaSessionCompat!!.sessionToken))
        builder.setSmallIcon(R.drawable.launcher)
        NotificationManagerCompat.from(this@Playback).notify(1, builder.build())
    }

    fun showPausedNotification() {
        val builder = MediaStyleHelper.from(this, mMediaSessionCompat, getMetaData())
        builder.addAction(NotificationCompat.Action(R.drawable.previous, "Previous", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)))
        builder.addAction(NotificationCompat.Action(R.drawable.play, "Play", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY_PAUSE)))
        builder.addAction(NotificationCompat.Action(R.drawable.next, "Next", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_NEXT)))
        builder.setStyle(android.support.v4.media.app.NotificationCompat.MediaStyle().setShowActionsInCompactView(0,1,2).setMediaSession(mMediaSessionCompat!!.sessionToken))
        builder.setSmallIcon(R.drawable.launcher)
        NotificationManagerCompat.from(this).notify(1, builder.build())
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
        if (mMediaPlayer != null) {
            return mMediaPlayer!!.isPlaying
        } else
            return false
    }

    fun pauseMusic() {
        musicpausedposition = mMediaPlayer!!.currentPosition
        mMediaPlayer!!.pause()
        setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED)
        showPausedNotification()
        isPaused = true

    }

    fun setCurrentSong(currentSong: Int) {
        currentIndexSongList = currentSong
    }

    fun releasePlayer() {
        if(mMediaPlayer!=null) {
            mMediaPlayer!!.reset()
            mMediaPlayer!!.stop()
            mMediaPlayer!!.release()
            mMediaPlayer = null
        }
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


    fun clearPlaylist() {
        songlist.removeAll(songlist)
    }

    fun nextSong() {
        if (songlist.size > 1) {
            currentIndexSongList = getCurrentSong()
            if (currentIndexSongList < (songlist.size - 1)) {
                currentIndexSongList += 1
                releasePlayer()
                setCurrentSong(currentIndexSongList)
                setMusicPausedPosition(0)
                isPaused = false
                playSongs()

            }
        }
    }

    fun previousSong() {
        currentIndexSongList = getCurrentSong()
        if (currentIndexSongList < songlist.size && currentIndexSongList > 0) {
            currentIndexSongList -= 1
            releasePlayer()
            setCurrentSong(currentIndexSongList)
            setMusicPausedPosition(0)
            isPaused = false
            playSongs()
        }
    }

    fun setPausedState(state: Boolean){
        isPaused = state
    }

    fun getPlaylist(): MutableList<Uri?>{
        return songlist
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




}
