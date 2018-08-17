package kalves.multimediacenter

import android.content.Intent
import android.os.Bundle
import android.os.Message
import android.os.Messenger


class HandlerActivity : AppCompatPreferenceActivity(), CustomHandler.AppReceiver {

    private var handler: CustomHandler? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        registerService()
    }


    /*
     * Step 1: Register the intent service in the activity
     * */
    private fun registerService() {
        val intent = Intent(applicationContext, Playback::class.java)

        /*
         * Step 2: We pass the handler via the intent to the intent service
         * */
        handler = CustomHandler(this)
        intent.putExtra("handler", Messenger(handler))
        startService(intent)
    }


    override fun onReceiveResult(message: Message) {
        /*
         * Step 3: Handle the results from the intent service here!
         * */
        when (message.what) {

        }
    }
}