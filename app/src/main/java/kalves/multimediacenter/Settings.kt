package kalves.multimediacenter

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Bundle
import android.provider.Settings
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Switch
import kotlinx.android.synthetic.main.activity_settings.*

class Settings : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (mBluetoothAdapter != null) {
            if (mBluetoothAdapter.isEnabled) {
                bt_switch.isChecked = true
            }
        }
        val wifiManager = this.getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (wifiManager.isWifiEnabled) wifi_switch.isChecked = true
    }

    private fun bluetoothSwitchOff() {
        var REQUEST_ENABLE_BT = 1
        val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (mBluetoothAdapter != null) {
            if (mBluetoothAdapter.isEnabled) {
                mBluetoothAdapter.disable()
            }
        }
    }

    private fun bluetoothSwitch() {
        var REQUEST_ENABLE_BT = 1
        val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (mBluetoothAdapter != null) {
            if (!mBluetoothAdapter.isEnabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            }
        }
    }

    private fun wifiSwitch() {
        val wifiManager = this.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiManager.isWifiEnabled = true
    }

    private fun wifiSwitchOff() {
        val wifiManager = this.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiManager.isWifiEnabled = false
    }

    fun onBluetoothSwitchClicked(v: View) {
        //Is the switch on?
        val on = (v as Switch).isChecked

        if (on) {
            bluetoothSwitch()
        } else {
            bluetoothSwitchOff()
        }
    }

    fun onSearchDevicesClicked(v: View) {
        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        }
    }

    fun onSearchWifiClicked(v: View) {
        val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        }
    }

    fun onSwitchWifiClicked(v: View) {
        val on = (v as Switch).isChecked

        if (on) {
            wifiSwitch()
        } else {
            wifiSwitchOff()
        }
    }
}
