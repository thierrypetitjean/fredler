package com.example.myapplication

import android.content.ContentValues
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.liveData
import com.example.myapplication.data.Device
import com.example.myapplication.data.Payload
import com.google.gson.Gson
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import org.json.JSONObject
import java.io.IOException


class DetailActivity() : AppCompatActivity() {

    private lateinit var txtDev: TextView
    private lateinit var swOnOff: SwitchCompat
    private lateinit var ivDev: ImageView
    private lateinit var seekBar: SeekBar
    private lateinit var device: Device
    private val app = AppRepository
    private lateinit var deviceLiveData: LiveData<Device>

    private var alreadyGetted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        // To retrieve object in second Activity

        device = (intent.getSerializableExtra("DEVICE") as Device?)!!

        ivDev = findViewById<ImageView>(R.id.ivDevice)
        txtDev = findViewById<TextView>(R.id.txtDev)
        swOnOff = findViewById<SwitchCompat>(R.id.switch1)
        seekBar = findViewById<SeekBar>(R.id.seekBar)

        swOnOff.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                println("on")
                runBlocking {
                    turnLampOnOff(device.devicename!!)
                }
            } else {
                println("off")
                runBlocking {
                    turnLampOnOff(device.devicename!!)
                }
            }
        }

        seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                // TODO Auto-generated method stub
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                // TODO Auto-generated method stub
            }

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                // TODO Auto-generated method stub
                turnBrightness(device.devicename!!, progress)
                Toast.makeText(applicationContext, progress.toString(), Toast.LENGTH_LONG).show()
            }
        })

        deviceLiveData = liveData {
            val singleDevice = getDevicesFromJson(app.updateJson)
            emit(singleDevice)
        }

        // Create the observer which updates the UI.
        val devicesObserver = Observer<Device> { dev ->
            // Update the UI, in this case, a GridView.
            println(dev)

            txtDev.setText(device.devicename)

            if (device.devicename!!.contains("smart_plug")) {
                ivDev.setImageResource(R.drawable.plug)
            } else if (device.devicename!!.contains("beweging")) {
                ivDev.setImageResource(R.drawable.sensor)
            } else if (device.devicename!!.contains("klimaat")) {
                ivDev.setImageResource(R.drawable.humid)
            } else {
                ivDev.setImageResource(R.drawable.lamp)

                Log.d("testdevice","detail = " + device)
                if(device.payload?.linkquality != null) {
                    seekBar.progress = device.payload?.linkquality!!
                }
            }

        }

        deviceLiveData.observe(this, devicesObserver)

        val eventSourceListener = object : EventSourceListener() {
            override fun onOpen(eventSource: EventSource, response: Response) {
                super.onOpen(eventSource, response)
                Log.d(ContentValues.TAG, "Connection Opened")
            }

            override fun onClosed(eventSource: EventSource) {
                super.onClosed(eventSource)
                Log.d(ContentValues.TAG, "Connection Closed")
            }

            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String
            ) {
                super.onEvent(eventSource, id, type, data)
                Log.d(ContentValues.TAG, "On Event Received! Data -: $data")

                GlobalScope.launch {

                    if (type.equals("message")) {
                        val dat = JSONObject(data)
                        val topic = dat.getString("topic")

                        if (topic != "devices") {

                            // Make the network call and suspend execution until it finishes
                            if (!alreadyGetted) {
                                alreadyGetted = true
                                val result = getDevicesFromJson(data)

                                // Display result of the network request to the user


                                runOnUiThread {
                                    // Stuff that updates the UI

                                    txtDev.setText(device.devicename)

                                    if (device.devicename!!.contains("smart_plug")) {
                                        ivDev.setImageResource(R.drawable.plug)
                                    } else if (device.devicename!!.contains("beweging")) {
                                        ivDev.setImageResource(R.drawable.sensor)
                                    } else if (device.devicename!!.contains("klimaat")) {
                                        ivDev.setImageResource(R.drawable.humid)
                                    } else {
                                        ivDev.setImageResource(R.drawable.lamp)

                                        Log.d("testdevice","detail = " + device)
                                        if(device.payload?.brightness != null) {
                                            seekBar.progress = device.payload?.brightness!!
                                        }
                                    }
                                }


                            }
                        }
                    }
                }

            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                super.onFailure(eventSource, t, response)
                Log.d(ContentValues.TAG, "On Failure -: ${response?.body}")
            }
        }

        EventSources.createFactory(app.client)
            .newEventSource(request = app.request, listener = eventSourceListener)

    }

    fun getDevicesFromJson(json: String): Device {

        val gson = Gson()
        val data = JSONObject(json)

        Log.d("test test test ", "" + data.getString("topic"))
        Log.d("test test test ", "" + device.devicename)
        if (data.getString("topic") != "devices") {
            val payloadObj = data.getJSONObject("payload")

            val payload = gson.fromJson(
                payloadObj.toString(),
                Payload::class.java
            )

            device.payload = payload

            Log.d("test ", "" + payload)
            Log.d("test ", "dev " + device.payload)

        }

        return device
    }

    fun turnLampOnOff(deviceName: String){
        val payload = "{'topic':'"+deviceName+"','feature':{'state':'toggle'}}"

        val json = JSONObject(payload)
        val okHttpClient = OkHttpClient()
        val requestBody = json.toString().toRequestBody()
        val request = Request.Builder()
            .method("POST", requestBody)
            .addHeader("Accept", "application/json; q=0.5")
            .url("http://192.168.0.100:8000/api/set")
            .build()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Handle this
                Log.d("API ", "error = " + e)

            }

            override fun onResponse(call: Call, response: Response) {
                // Handle this
                Log.d("API ", "response " + response.message)
            }
        })
    }

    fun turnBrightness(deviceName: String, progress: Int){
        val payload = "{'topic':'"+deviceName+"','feature':{'brightness':'" + progress +"'}}"

        val json = JSONObject(payload)
        val okHttpClient = OkHttpClient()
        val requestBody = json.toString().toRequestBody()
        val request = Request.Builder()
            .method("POST", requestBody)
            .addHeader("Accept", "application/json; q=0.5")
            .url("http://192.168.0.100:8000/api/set")
            .build()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Handle this
                Log.d("API ", "error = " + e)

            }

            override fun onResponse(call: Call, response: Response) {
                // Handle this
                Log.d("API ", "response " + response.message)
            }
        })
    }
}