package be.ap.edu.mapsaver

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.ap.volders.herhaling.RecyclerView.RecyclerViewActivity
import com.ap.volders.herhaling.RecyclerView.RecyclerViewData
import com.beust.klaxon.*
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.ItemizedIconOverlay
import org.osmdroid.views.overlay.ItemizedOverlay
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.OverlayItem
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.lang.StringBuilder
import java.net.URL
import java.net.URLEncoder
import java.util.*
import java.util.regex.Pattern
import kotlin.collections.ArrayList
import kotlin.concurrent.thread

class MainActivity : Activity() {


//    Vertrek van bijgevoegd project en maak een Android app in Kotlin die volgende functionaliteiten bevat :
//
//    KLAAR
//    1/ check bij het opstarten of  volgende dataset :  https://opendata.arcgis.com/datasets/5455b95e8ed1407da444319d54ea97de_256.geojson reeds opgeslagen is in SQLIte.
//      Indien niet, download ze met een AsyncTask en sla ze op (8 punten)
//
//    KLAAR
//    2/ toon daarna een lijst met alle namen van de scholen in een ListView (8 punten)
//
//    3/ op een naam klikken in de ListView toont de locatie op een kaart (4 punten)


    private var mapController: IMapController? = null

    private var geoPointName:String? = null
    private var geoPoint:GeoPoint? = null
    private var mMyLocationOverlay: ItemizedOverlay<OverlayItem>? = null
    private var items = ArrayList<OverlayItem>()

    private var mMapView: MapView? = null
    private var btnDelete: Button? = null
    private val client = OkHttpClient()
    private val TAG = "app"
    var helper : DatabaseHelper? = null
    private var jsonString:String = ""

    private var context:Context? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        context = this
        helper = DatabaseHelper(this)
        val osmConfig = Configuration.getInstance()
        osmConfig.userAgentValue = packageName
        val basePath = File(cacheDir.absolutePath, "osmdroid")
        osmConfig.osmdroidBasePath = basePath
        val tileCache = File(osmConfig.osmdroidBasePath, "tile")
        osmConfig.osmdroidTileCache = tileCache

        setContentView(R.layout.activity_main)
        mMapView = findViewById<MapView>(R.id.mapview)
        btnDelete = findViewById(R.id.btnDeleteDatabase)
        getAllDatabase()


//        btnDelete!!.setOnClickListener {
////            deleteAllInDatabase()
////            intent = Intent(this, RecyclerViewActivity::class.java)
////            intent.putExtra("EXTRA_DATA","mijnen extra data no1")
////            startActivity(intent)
//        }

        if (hasPermissions()) {
            initMap()
        }
        else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION), 100)
        }
    }

    fun View.hideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }

    private fun hasPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            if (hasPermissions()) {
                initMap()
            } else {
                finish()
            }
        }
    }

    fun initMap() {
        mMapView?.controller?.setZoom(17.0)
        mMapView?.controller?.setCenter(GeoPoint(51.23020595, 4.41655480828479))
    }

    override fun onPause() {
        super.onPause()
        mMapView?.onPause()
    }

    override fun onResume() {
        super.onResume()
        mMapView?.onResume()
    }

    private fun addMarker(geoPoint: GeoPoint, name: String) {
        items.add(OverlayItem(name,"description", geoPoint))
        mMyLocationOverlay = ItemizedIconOverlay(items,null,applicationContext)
        mMapView?.overlays?.add(mMyLocationOverlay)
        mapController?.setZoom(15.0)
    }

    private fun setCenter(geoPoint: GeoPoint, name: String) {
        mapController?.setCenter(geoPoint)
        mapController?.setZoom(15.0)
    }




    fun getAllDatabase(){
        var lijstje = helper?.getAll()

        if (lijstje!!.isEmpty()) {
            Log.d(TAG, "DATABASE IS LEEG!!")
            val url = URL("https://opendata.arcgis.com/datasets/5455b95e8ed1407da444319d54ea97de_256.geojson")
            try {
                val task = MyAsyncTask()
                task.execute(url)
//                asyncGet("https://opendata.arcgis.com/datasets/5455b95e8ed1407da444319d54ea97de_256.geojson")

            }catch (e:Exception ){
                Log.d(TAG, "getAllDatabase: ${e.toString()}")
            }
        }
        else {
//            Log.d(TAG, "getAllDatabase: LIJSTJE: ${lijstje}")
            Log.d(TAG, "DATABASE IS GELADEN!")
        }
    }

    fun add(file:String){
        helper?.add(file)
        getAllDatabase()
    }

    fun deleteAllInDatabase(){
        helper?.deleteAll()
        Toast.makeText(this, "database deleted", Toast.LENGTH_SHORT).show()
    }


    // AsyncTask inner class
    inner class MyAsyncTask : AsyncTask<URL, Int, String>() {
        var response = ""

        override fun onPreExecute(){
            super.onPreExecute()
        }

        override fun doInBackground(vararg params: URL?): String {
            val client = OkHttpClient()
            val request = Request.Builder()
                    .url(params[0]!!)
                    .build()
            response = client.newCall(request).execute().body!!.string()
//            Log.d("get", "doInBackground: ${params[0]}")
            return response
        }

        override fun onProgressUpdate(vararg values: Int?) {
            super.onProgressUpdate(*values)
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            val parser: Parser = Parser.default()
            val jsonString = StringBuilder(result!!)
            val obj: JsonObject = parser.parse(jsonString) as JsonObject
            val features = obj.get("features") as JsonArray<JsonObject>
            val prop = features[0].get("properties") as JsonObject
            val geo = features[0].get("geometry") as JsonObject
            val coord = geo.get("coordinates") as JsonArray<Double>

            val lat = coord[0].toString()
            val lon = coord[1].toString()
            val naam = prop.get("naam")
//            val geometry = prop.get("geometry") as JsonObject
            Log.d(TAG, "naam: ${naam.toString()}")
            Log.d(TAG, "lat: ${lat.toString()}")
            Log.d(TAG, "lon: ${lon.toString()}")

            var dataList = ArrayList<String>()
            val tester = features.get("properties").get("naam")
            
            tester.forEach { 
                dataList.add(it.toString())
            }
            dataList.forEach {
                Log.d(TAG, "onPostExecute: ${it.toString()}")
            }
            intent = Intent(context, RecyclerViewActivity::class.java)
            intent.putExtra("EXTRA_DATA",dataList)
            startActivity(intent)
        }
    }

//
//
//
//    //Asynchronous get
//    fun asyncGet(url:String){
//        val request = Request.Builder()
//                .url(url)
//                .build()
//
//        client.newCall(request).enqueue(object : Callback {
//            override fun onFailure(call: Call, e: IOException) {
//                Log.e(TAG, e.toString())
//            }
//
//            override fun onResponse(call: Call, response: Response) {
//                response.use {
//                    if (!response.isSuccessful) throw IOException("errorcod: $response")
//
//                    jsonString = response.body?.string().toString()
//
//                    Log.d(TAG, "JSONSTRING: $jsonString")
//                    add(jsonString)
//                }
//            }
//        })
//    }
}
