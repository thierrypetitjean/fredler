package com.example.myapplication

import DeviceAdapter
import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.AdapterView
import android.widget.GridView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.liveData
import com.example.myapplication.data.Access
import com.example.myapplication.data.Device
import com.example.myapplication.databinding.ActivityMainBinding
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import okhttp3.internal.notifyAll
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import org.json.JSONObject
import java.io.IOException


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val app = AppRepository

    private lateinit var grvDevices: GridView
    private lateinit var deviceList: ArrayList<Device>

    private lateinit var courseAdapter: DeviceAdapter

    private lateinit var devicesLiveData: LiveData<ArrayList<Device>>

    private var alreadyGetted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //binding gridview
        grvDevices = binding.idGRV

        //init device list
        deviceList = ArrayList<Device>()

        //Init adpater
        courseAdapter = DeviceAdapter(deviceList, applicationContext)
        grvDevices.adapter = courseAdapter
        grvDevices.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            // inside on click method we are simply displaying
            // a toast message with course name.
            val def = deviceList[position]
            val intent = Intent(applicationContext, DetailActivity::class.java)
            intent.putExtra("DEVICE", def);
            startActivity(intent)

            Toast.makeText(
                applicationContext, deviceList[position].devicename + " selected",
                Toast.LENGTH_SHORT
            ).show()
        }

        devicesLiveData = liveData {
            val devList = getDevJson(app.devicesJson)
            emit(devList)
        }

        // Create the observer which updates the UI.
        val devicesObserver = Observer<ArrayList<Device>> { dev ->
            // Update the UI, in this case, a GridView.
            println(dev)
            courseAdapter.notifyDataSetChanged()

        }
        devicesLiveData.observe(this, devicesObserver)


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

                    if(type.equals("message")) {
                        val dat = JSONObject(data)
                        val topic = dat.getString("topic")

                        if (topic == "devices") {

                            // Make the network call and suspend execution until it finishes
                            if (!alreadyGetted) {
                                alreadyGetted = true
                                val result = getDevJson(data)
                                if (result.isNotEmpty()) {
                                    runOnUiThread {
                                        // Stuff that updates the UI
                                        devicesLiveData = liveData {
                                            val devList = getDevJson(app.devicesJson)
                                            emit(devList)
                                        }
                                        //courseAdapter.notifyDataSetChanged()
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

        GlobalScope.launch {
            withContext(Dispatchers.IO) {
                app.client.newCall(app.request).enqueue(responseCallback = object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Log.e(TAG, "API Call Failure ${e.localizedMessage}")

                        //Show a error message on de UI Thread
                        runOnUiThread {
                            Toast.makeText(
                                applicationContext, "Geen connectie!",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }

                    override fun onResponse(call: Call, response: Response) {
                        Log.d(TAG, "APi Call Success ${response.body.toString()}")
                    }
                })
            }
        }

        EventSources.createFactory(app.client)
            .newEventSource(request = app.request, listener = eventSourceListener)

    }

    private fun getDevJson(json: String): ArrayList<Device> {

        val gson = Gson()
        val dat = JSONObject(json)
        val topic = dat.getString("topic")

        if (topic.equals("devices")) {

            val datarray = dat.getJSONArray("payload")
            val accesArray = ArrayList<Access>()

            for (i in 0 until datarray.length()) {
                val payload = datarray.getJSONObject(i)
                Log.d("payload", "" + payload)
                val friendlyName = payload.getString("friendly_name")
                val ieee = payload.getString("ieee_address")

                if (payload.has("definition") && !payload.isNull("definition")) {

                    val defenition = payload.getJSONObject("definition")
                    val description = defenition.getString("description")
                    val model = defenition.getString("model")
                    val exposes = defenition.getJSONArray("exposes")
                    for (x in 0 until exposes.length()) {
                        val features = exposes.getJSONObject(x)

                        if (features.has("features") && !features.isNull("features")) {
                            val featureArray = features.getJSONArray("features")
                            val featureObj = featureArray.getJSONObject(0)
                            accesArray.add(
                                gson.fromJson(
                                    featureObj.toString(),
                                    Access::class.java
                                )
                            )
                        } else {
                            val access = exposes.getJSONObject(x)
                            accesArray.add(
                                gson.fromJson(
                                    access.toString(),
                                    Access::class.java
                                )
                            )
                        }
                    }
                    val dev = Device(model, description, friendlyName, ieee, accesArray)
                    deviceList.add(dev)
                }
            }
        }
        return deviceList
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

}