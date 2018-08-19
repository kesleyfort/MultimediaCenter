package kalves.multimediacenter

import android.content.Context
import android.graphics.BitmapFactory
import android.support.v4.app.NotificationCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaButtonReceiver
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat

object MediaStyleHelper {
    /**
     * Build a notification using the information from the given media session. Makes heavy use
     * of [MediaMetadataCompat.getDescription] to extract the appropriate information.
     *
     * @param context      Context used to construct the notification.
     * @param mediaSession Media session to get information.
     * @return A pre-built notification with information from the given media session.
     */
    fun from(
            context: Context, mediaSession: MediaSessionCompat?, metadata: Triple<String, String, ByteArray>): NotificationCompat.Builder {
        val controller = mediaSession!!.controller
        val image = BitmapFactory.decodeByteArray(metadata.third, 0, metadata.third.size)
        return NotificationCompat.Builder(context)
                .setContentTitle(metadata.second)
                .setContentText(metadata.first)
                .setSubText("Now Playing")
                .setLargeIcon(image)
                .setContentIntent(controller.sessionActivity)
                .setDeleteIntent(
                        MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_STOP))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
    }
}