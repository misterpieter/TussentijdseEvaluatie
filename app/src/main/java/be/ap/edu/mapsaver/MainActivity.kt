package be.ap.edu.mapsaver

import android.Manifest
import android.app.Activity
import android.content.Context
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
import androidx.core.content.ContextCompat
import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
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
import java.net.URL
import java.net.URLEncoder
import java.util.*


class MainActivity : Activity() {

    private var mMapView: MapView? = null
    private var mMyLocationOverlay: ItemizedOverlay<OverlayItem>? = null
    private var items = ArrayList<OverlayItem>()
    private var searchField: EditText? = null
    private var searchButton: Button? = null
    private var clearButton: Button? = null
    private val urlNominatim = "https://nominatim.openstreetmap.org/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Problem with SQLite db, solution :
        // https://stackoverflow.com/questions/40100080/osmdroid-maps-not-loading-on-my-device
        val osmConfig = Configuration.getInstance()
        osmConfig.userAgentValue = packageName
        val basePath = File(cacheDir.absolutePath, "osmdroid")
        osmConfig.osmdroidBasePath = basePath
        val tileCache = File(osmConfig.osmdroidBasePath, "tile")
        osmConfig.osmdroidTileCache = tileCache

        setContentView(R.layout.activity_main)
        mMapView = findViewById<MapView>(R.id.mapview)

        searchField = findViewById(R.id.search_txtview)
        searchButton = findViewById(R.id.search_button)
        searchButton?.setOnClickListener {
            val url = URL(urlNominatim + "search?q=" + URLEncoder.encode(searchField?.text.toString(), "UTF-8") + "&format=json")
            it.hideKeyboard()
            val task = MyAsyncTask()
            task.execute(url)
        }

        clearButton = findViewById(R.id.clear_button)
        clearButton?.setOnClickListener {
            mMapView?.overlays?.clear()
            // Redraw map
            mMapView?.invalidate()
        }

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
        mMapView?.setTileSource(TileSourceFactory.MAPNIK)
        // create a static ItemizedOverlay showing some markers
        addMarker(GeoPoint(51.2162764, 4.41160291036386), "Campus Meistraat")
        addMarker(GeoPoint(51.2196911, 4.4092625), "Campus Lange Nieuwstraat")
        // add receiver to get location from tap
        val mReceive: MapEventsReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                //Toast.makeText(baseContext, p.latitude.toString() + " - " + p.longitude, Toast.LENGTH_LONG).show()
                val url = URL(urlNominatim + "reverse?format=json&lat=" + p.latitude.toString() + "&lon=" + p.longitude.toString())
                val task = MyAsyncTask()
                task.execute(url)
                return false
            }

            override fun longPressHelper(p: GeoPoint): Boolean {
                return false
            }
        }
        mMapView?.getOverlays()?.add(MapEventsOverlay(mReceive))

         // MiniMap
        //val miniMapOverlay = MinimapOverlay(this, mMapView!!.tileRequestCompleteHandler)
        //this.mMapView?.overlays?.add(miniMapOverlay)

        mMapView?.controller?.setZoom(17.0)
        // default = Ellermanstraat 33
        setCenter(GeoPoint(51.23020595, 4.41655480828479), "Campus Ellermanstraat")
    }

    private fun addMarker(geoPoint: GeoPoint, name: String) {
        items.add(OverlayItem(name, name, geoPoint))
        mMyLocationOverlay = ItemizedIconOverlay(items, null, applicationContext)
        mMapView?.overlays?.add(mMyLocationOverlay)
    }

    fun setCenter(geoPoint: GeoPoint, name: String) {
        mMapView?.controller?.setCenter(geoPoint)
        addMarker(geoPoint, name)
    }

    override fun onPause() {
        super.onPause()
        mMapView?.onPause()
    }

    override fun onResume() {
        super.onResume()
        mMapView?.onResume()
    }

    // AsyncTask inner class
    inner class MyAsyncTask : AsyncTask<URL, Int, String>() {

        private var search = 0

        override fun doInBackground(vararg params: URL?): String {

            search = if (params[0]!!.toString().indexOf("reverse", 0, true) > -1) 1 else 0
            val client = OkHttpClient()
            val response: Response
            val request = Request.Builder()
                    .url(params[0]!!)
                    .build()
            response = client.newCall(request).execute()

            return response.body!!.string()
        }

        // vararg : variable number of arguments
        // * : spread operator, unpacks an array into the list of values from the array
        override fun onProgressUpdate(vararg values: Int?) {
            super.onProgressUpdate(*values)
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)

            val jsonString = StringBuilder(result!!)
            Log.d("be.ap.edu.mapsaver", jsonString.toString())
            val parser: Parser = Parser.default()

            if (search == 1) {
                val obj = parser.parse(jsonString) as JsonObject
                Toast.makeText(applicationContext, obj.string("display_name")!!, Toast.LENGTH_SHORT).show()
            }
            else {
                val array = parser.parse(jsonString) as JsonArray<JsonObject>

                if (array.size > 0) {
                    val obj = array[0]
                    //Log.d("be.ap.edu.mapsaver", "onResponse" + obj.string("lat")!! + " " + obj.string("lon")!!)
                    // mapView center must be updated here and not in doInBackground because of UIThread exception
                    val geoPoint = GeoPoint(obj.string("lat")!!.toDouble(), obj.string("lon")!!.toDouble())
                    setCenter(geoPoint, searchField?.text.toString())
                } else {
                    Toast.makeText(applicationContext, "Address not found", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
