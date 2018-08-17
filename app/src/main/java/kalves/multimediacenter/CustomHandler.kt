package kalves.multimediacenter


import android.os.Handler
import android.os.Message

class CustomHandler(private val appReceiver: AppReceiver) : Handler() {

    override fun handleMessage(msg: Message) {
        super.handleMessage(msg)
        appReceiver.onReceiveResult(msg)
    }


    interface AppReceiver {
        fun onReceiveResult(message: Message)
    }
}
