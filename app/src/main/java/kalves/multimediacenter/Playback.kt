package kalves.multimediacenter

import android.app.PendingIntent
import android.app.Service
import android.content.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.MediaMetadata
import android.net.Uri
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserServiceCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaButtonReceiver
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.ui.PlayerNotificationManager.MediaDescriptionAdapter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import wseemann.media.FFmpegMediaMetadataRetriever


class Playback :  MediaBrowserServiceCompat(), AudioManager.OnAudioFocusChangeListener, MediaDescriptionAdapter {
    override fun onGetRoot(p0: String, p1: Int, p2: Bundle?): BrowserRoot? {
        return null
    }

    override fun onLoadChildren(p0: String, p1: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        return p1 as Unit
    }

    override fun startForegroundService(service: Intent?): ComponentName? {
        return super.startForegroundService(service)
    }
    override fun createCurrentContentIntent(player: Player?): PendingIntent? {
        var window = mediaPlayer?.currentWindowIndex
        val intent = Intent(this, MainActivity::class.java)
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_NO_CREATE)
    }

    override fun getCurrentContentText(player: Player?): String? {
        val mmr = FFmpegMediaMetadataRetriever()
        mmr.setDataSource(applicationContext, songlist[currentindex])
        var musicArtist = mmr.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_ARTIST)
        return musicArtist
    }

    override fun getCurrentContentTitle(player: Player?): String {
        val mmr = FFmpegMediaMetadataRetriever()
        mmr.setDataSource(applicationContext, songlist[currentindex])
        var musictitle = mmr.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_TITLE)
        return musictitle
    }

    override fun getCurrentLargeIcon(player: Player?, callback: PlayerNotificationManager.BitmapCallback?): Bitmap? {
        val mmr = FFmpegMediaMetadataRetriever()
        mmr.setDataSource(applicationContext, songlist[currentindex])
        var artwork = mmr.embeddedPicture
        mmr.release()
        val image = BitmapFactory.decodeByteArray(artwork, 0, artwork.size)
        return image
    }

    //Variables
    var mediaPlayer: SimpleExoPlayer? = null
    var songlist: MutableList<Uri?> = mutableListOf()
    private var mMediaSessionCompat: MediaSessionCompat? = null
    private val mNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (mediaPlayer != null) {
                //mediaPlayer?.pause()
            }
        }
    }
    private val Main: MainActivity = MainActivity()
    var currentindex = 0
    var musiclist = ConcatenatingMediaSource()
    var mediaplaylistcreated: Boolean = false
    var AVRCP_PLAYSTATE_CHANGED = "com.android.music.playstatechanged";
    var AVRCP_META_CHANGED = "com.android.music.metachanged";
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
            mediaPlayer?.playWhenReady = true
            mediaPlayer?.playbackState
            setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING)
            bluetoothNotifyChange(AVRCP_PLAYSTATE_CHANGED)
        }

        override fun onPause() {
            super.onPause()
            mediaPlayer?.playWhenReady = false
            mediaPlayer?.playbackState
            setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED)
            bluetoothNotifyChange(AVRCP_PLAYSTATE_CHANGED)
        }

        override fun onSkipToNext() {
            super.onSkipToNext()
            if(mediaPlayer!!.nextWindowIndex < musiclist.size-1) {
                currentindex = mediaPlayer!!.nextWindowIndex
                mediaPlayer?.seekTo(mediaPlayer!!.nextWindowIndex, 0)
                bluetoothNotifyChange(AVRCP_META_CHANGED)
            }
        }

        override fun onSkipToPrevious() {
            super.onSkipToPrevious()
            if(mediaPlayer!!.previousWindowIndex > 1) {
                currentindex = mediaPlayer!!.previousWindowIndex
                mediaPlayer?.seekTo(mediaPlayer!!.previousWindowIndex, 0)
                bluetoothNotifyChange(AVRCP_META_CHANGED)
            }
        }
    }
fun bluetoothNotifyChange(what: String) {
    var metada =
            MediaMetadataCompat.Builder()
                    .putString(MediaMetadata.METADATA_KEY_TITLE, getTrackName())
                    .putString(MediaMetadata.METADATA_KEY_ARTIST, getArtistName())
                    .putString(MediaMetadata.METADATA_KEY_ALBUM, getAlbumName())
                    .putLong(MediaMetadata.METADATA_KEY_DURATION, mediaPlayer!!.duration)
                    .build()
    mMediaSessionCompat?.setMetadata(metada)
}
    fun getArtistName(): String{
        val mmr = FFmpegMediaMetadataRetriever()
        mmr.setDataSource(applicationContext, songlist[currentindex])
        var musicArtist = mmr.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_ARTIST)
        return musicArtist
    }
    fun getTrackName(): String{
        val mmr = FFmpegMediaMetadataRetriever()
        mmr.setDataSource(applicationContext, songlist[currentindex])
        var musicTitle = mmr.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_TITLE)
        return musicTitle
    }
    fun getAlbumName(): String{
        val mmr = FFmpegMediaMetadataRetriever()
        mmr.setDataSource(applicationContext, songlist[currentindex])
        var musicAlbum = mmr.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_ALBUM)
        return musicAlbum
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        MediaButtonReceiver.handleIntent(mMediaSessionCompat, intent)
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCreate() {
        super.onCreate()
        initMediaPlayer()
        initMediaSession()
        initNoisyReceiver()
        initNotifications()
        onListener()

    }


    private fun initMediaPlayer() {
        val trackSelector = DefaultTrackSelector()
        mediaPlayer = ExoPlayerFactory.newSimpleInstance(baseContext, trackSelector)
        mediaPlayer?.playWhenReady = false
    }

    private fun initMediaSession() {
        val mediaButtonReceiver = ComponentName(applicationContext, MediaButtonReceiver::class.java)
        mMediaSessionCompat = MediaSessionCompat(applicationContext, "Tag", mediaButtonReceiver, null)

        mMediaSessionCompat?.setCallback(mMediaSessionCallback)
        mMediaSessionCompat?.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)

        val mediaButtonIntent = Intent(Intent.ACTION_MEDIA_BUTTON)
        mediaButtonIntent.setClass(this, MediaButtonReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent, 0)
        mMediaSessionCompat?.setMediaButtonReceiver(pendingIntent)
        sessionToken = mMediaSessionCompat?.sessionToken
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
    fun createMediaSourcePlaylist(uri: MutableList<Uri?>) {
        songlist.addAll(uri)
        var mediasourcelist: MutableList<MediaSource> = mutableListOf()
        val userAgent = Util.getUserAgent(baseContext, "ExoPlayer")
        if (mediaplaylistcreated) {
            for (i in musiclist.size until uri.size) {
                mediasourcelist.add(ExtractorMediaSource(uri[i],
                        DefaultDataSourceFactory(baseContext, userAgent),
                        DefaultExtractorsFactory(), null, null))
            }
            for (i in 0 until (mediasourcelist.size)) {
                musiclist.addMediaSource(mediasourcelist[i])
            }
        } else {
            for (i in 0 until uri.size) {
                mediasourcelist.add(ExtractorMediaSource(uri[i],
                        DefaultDataSourceFactory(baseContext, userAgent),
                        DefaultExtractorsFactory(), null, null))

            }
            for (i in 0 until (mediasourcelist.size)) {
                musiclist.addMediaSource(mediasourcelist[i])
            }
        }
    }
    private fun initNotifications(){
        var playerNotificationManager = PlayerNotificationManager(
                this,
                "",
                1,
                this)
        playerNotificationManager.setPlayer(mediaPlayer)
        playerNotificationManager.setSmallIcon(R.drawable.launcher)
        playerNotificationManager.setRewindIncrementMs(0)
        playerNotificationManager.setFastForwardIncrementMs(0)
        playerNotificationManager.setOngoing(true)
    }
    private fun destroyNotifications(){
        var playerNotificationManager = PlayerNotificationManager(
                this,
                "",
                1,
                this)
        playerNotificationManager.setPlayer(null)
    }
    fun playMusic() {
        mediaPlayer?.prepare(musiclist)
        mMediaSessionCompat!!.isActive = true
        setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED)
    }
    private fun onListener(){
        mediaPlayer?.addListener(object : Player.DefaultEventListener() {
            override fun onPlayerStateChanged(
                    playWhenReady: Boolean, playbackState: Int) {
                when (playbackState) {
                    Player.STATE_IDLE -> {


                    }
                    Player.STATE_BUFFERING -> {
                    }
                    Player.STATE_READY -> {
                        bluetoothNotifyChange("")
                    }
                    Player.STATE_ENDED -> {
                    }

                }
            }

            override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {
                super.onTracksChanged(trackGroups, trackSelections)
                currentindex = mediaPlayer!!.currentWindowIndex
                //myService!!.showPlayingNotification(updateMetaDataNotification())
            }

            override fun onPositionDiscontinuity(reason: Int) {
                super.onPositionDiscontinuity(reason)
                if(reason==Player.DISCONTINUITY_REASON_PERIOD_TRANSITION) {
                    Log.i("DISCONTINUITY", "DISCONTINUITY_REASON_PERIOD_TRANSITION")
                    currentindex = mediaPlayer!!.currentPeriodIndex
                }
            }
        })
    }
    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                    mediaPlayer?.stop()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                mediaPlayer?.playWhenReady = false
                mediaPlayer?.playbackState
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                if (mediaPlayer != null) {
                    mediaPlayer?.volume = 0.3f
                }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (mediaPlayer != null) {
                    mediaPlayer?.volume = 1.0f
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
    override fun onDestroy() {
        super.onDestroy()
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.abandonAudioFocus(this)
        unregisterReceiver(mNoisyReceiver)
        mMediaSessionCompat?.release()
        destroyNotifications()
    }


}
