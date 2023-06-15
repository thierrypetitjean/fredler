package com.example.myapplication

import android.app.Activity
import android.content.ContentValues
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.liveData
import com.example.myapplication.data.Device
import com.example.myapplication.data.Payload
import com.google.gson.Gson
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import org.json.JSONObject


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
            } else {
                println("off")
            }
        }

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
                                        if(device.payload?.linkquality != null) {
                                            seekBar.progress = device.payload?.linkquality!!
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
}