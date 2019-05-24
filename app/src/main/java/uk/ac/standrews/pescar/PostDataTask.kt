package uk.ac.standrews.pescar

import android.content.Context
import android.os.AsyncTask;
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import com.android.volley.Request;
import com.android.volley.RequestQueue
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley
import java.util.concurrent.Executors

class PostDataTask(val context: Context) : AsyncTask<Void, Void, Void>() {

    override fun doInBackground(vararg params: Void?): Void? {
        val db = AppDatabase.getAppDataBase(context.applicationContext)
        val fishingDao = db.fishingDao()
        val trackDao = db.trackDao()
        val towsToUpload = fishingDao.getUnuploadedTows()
        val landedsToUpload = fishingDao.getUnuploadedLandeds()
        val positionsToUpload = trackDao.getUnuploadedPositions()

        if (towsToUpload.isNotEmpty() || landedsToUpload.isNotEmpty() || positionsToUpload.isNotEmpty()) {
            val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'")
            df.timeZone = TimeZone.getTimeZone("UTC")

            val towsJson = JSONArray()
            towsToUpload.forEach { tow ->
                val towJson = JSONObject()
                towJson.put("id", tow.id)
                towJson.put("weight", tow.weight)
                towJson.put("timestamp", df.format(tow.timestamp))
                towsJson.put(towJson)
            }

            val landedsJson = JSONArray()
            landedsToUpload.forEach { lws ->
                val landedJson = JSONObject()
                landedJson.put("id", lws.landed.id)
                landedJson.put("species", lws.species.first().name)
                landedJson.put("weight", lws.landed.weight)
                landedJson.put("timestamp", lws.landed.timestamp)
                landedsJson.put(landedJson)
            }

            val positionsJson = JSONArray()
            positionsToUpload.forEach { pos ->
                val positionJson = JSONObject()
                positionJson.put("id", pos.id)
                positionJson.put("latitude", pos.latitude)
                positionJson.put("longitude", pos.longitude)
                positionJson.put("accuracy", pos.accuracy)
                positionJson.put("timestamp", pos.timestamp)
                positionsJson.put(positionJson)
            }

            val json = JSONObject()
            json.put("tows", towsJson)
            json.put("hauls", landedsJson)
            json.put("positions", positionsJson)

            val url = ""

            val callback = object : VolleyCallback {
                override fun onSuccess(result: JSONObject) {
                    val towsJson = result.getJSONArray("tows")
                    val towIds = arrayListOf<Int>()
                    for (tow in 0 until towsJson.length()) {
                        towIds.add((tow as JSONObject).getInt("id"))
                    }
                    val landedsJson = result.getJSONArray("hauls")
                    val landedIds = arrayListOf<Int>()
                    for (landed in 0 until landedsJson.length()) {
                        landedIds.add((landed as JSONObject).getInt("id"))
                    }
                    val positionsJson = result.getJSONArray("positions")
                    val positionIds = arrayListOf<Int>()
                    for (position in 0 until positionsJson.length()) {
                        positionIds.add((position as JSONObject).getInt("id"))
                    }
                    Executors.newSingleThreadExecutor().execute {
                        if (towIds.isNotEmpty()) {
                            if (towIds.size > 999) {
                                val chunkedTowIds = towIds.chunked(999)
                                chunkedTowIds.forEach { list ->
                                    fishingDao.markTowsUploaded(list, Date())
                                }
                            }
                            else {
                                fishingDao.markTowsUploaded(towIds, Date())
                            }
                        }
                        if (landedIds.isNotEmpty()) {
                            if (landedIds.size > 999) {
                                val chunkedLandedIds = landedIds.chunked(999)
                                chunkedLandedIds.forEach { list ->
                                    fishingDao.markLandedsUploaded(list, Date())
                                }
                            }
                            else {
                                fishingDao.markLandedsUploaded(landedIds, Date())
                            }
                        }
                        if (positionIds.isNotEmpty()) {
                            if (positionIds.size > 999) {
                                val chunkedPositionIds = positionIds.chunked(999)
                                chunkedPositionIds.forEach { list ->
                                    trackDao.markPositionsUploaded(list, Date())
                                }
                            }
                            else {
                                trackDao.markPositionsUploaded(positionIds, Date())
                            }
                        }
                    }
                }

                override fun onError(result: String?) {
                    //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                }
            }

            val request = JsonObjectRequest(
                Request.Method.POST, url, json,
                Response.Listener {
                    callback.onSuccess(json)
                },
                Response.ErrorListener { error ->
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        callback.onError(VolleyError(String(error.networkResponse.data)).message)
                    }
                }
            )
            RequestQueueSingleton.getInstance(context.applicationContext).addToRequestQueue(request)
        }

        return null
    }

}

class RequestQueueSingleton constructor(context: Context) {
    companion object {
        @Volatile
        private var INSTANCE: RequestQueueSingleton? = null
        fun getInstance(context: Context) =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: RequestQueueSingleton(context).also {
                    INSTANCE = it
                }
            }
    }
    val requestQueue: RequestQueue by lazy {
        // applicationContext is key, it keeps you from leaking the
        // Activity or BroadcastReceiver if someone passes one in.
        Volley.newRequestQueue(context.applicationContext)
    }
    fun <T> addToRequestQueue(req: Request<T>) {
        requestQueue.add(req)
    }
}

interface VolleyCallback {
    fun onSuccess(result: JSONObject)
    fun onError(result: String?)
}
