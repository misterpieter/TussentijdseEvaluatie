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
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.beust.klaxon.*
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.*
import org.osmdroid.config.Configuration

import org.osmdroid.util.GeoPoint
import org.osmdroid.views.*

import java.util.*
import org.osmdroid.views.overlay.*
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import java.io.*
import java.net.URL
import java.net.URLEncoder

class MainActivity : Activity() {

    var mMapView: MapView? = null
    var mMyLocationOverlay: ItemizedOverlay<OverlayItem>? = null
    var searchField: EditText? = null
    var searchButton: Button? = null
    var clearButton: Button? = null
    private val urlSearch = "https://nominatim.openstreetmap.org/search?q="

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
        mMapView = findViewById(R.id.mapview) as MapView

        searchField = findViewById(R.id.search_txtview)
        searchButton = findViewById(R.id.search_button)
        searchButton?.setOnClickListener {
            val url = URL(urlSearch + URLEncoder.encode(searchField?.text.toString(), "UTF-8") + "&format=json")
            it.hideKeyboard()
            val task = MyAsyncTask()
            task.execute(url)
        }

        clearButton = findViewById(R.id.clear_button)
        clearButton?.setOnClickListener {
            mMapView?.overlays.clear()
            // Redraw map
            mMapView?.invalidate()
        }

        if (hasPermissions()) {
            initMap()
        } else {
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

        run {
            // create a static ItemizedOverlay showing some markers
            val items = ArrayList<OverlayItem>()
            items.add(OverlayItem("Meistraat", "Campus Meistraat",
                    GeoPoint(51.2162764, 4.41160291036386)))
            items.add(OverlayItem("Lange Nieuwstraat", "Campus Lange Nieuwstraat",
                    GeoPoint(51.2196911, 4.4092625)))

            // OnTapListener for the Markers, shows a simple Toast
            mMyLocationOverlay = ItemizedIconOverlay(items,
                    object : ItemizedIconOverlay.OnItemGestureListener<OverlayItem> {
                        override fun onItemSingleTapUp(index: Int, item: OverlayItem): Boolean {
                            Toast.makeText(
                                    applicationContext, "Item '" + item.title + "' (index=" + index
                                    + ") got single tapped up", Toast.LENGTH_LONG).show()
                            return true
                        }

                        override fun onItemLongPress(index: Int, item: OverlayItem): Boolean {
                            Toast.makeText(
                                    applicationContext, "Item '" + item.title + "' (index=" + index
                                    + ") got long pressed", Toast.LENGTH_LONG).show()
                            return true
                        }
                    }, applicationContext)
            mMapView?.overlays?.add(mMyLocationOverlay)
        }

        // MiniMap
        /*run {
            val miniMapOverlay = MinimapOverlay(this, mMapView!!.tileRequestCompleteHandler)
            this.mMapView?.overlays?.add(miniMapOverlay)
        }*/

        val mapController = mMapView?.controller
        mapController?.setZoom(17.0)
        // Default = Ellermanstraat 33
        setCenter(GeoPoint(51.23020595, 4.41655480828479))
    }

    fun setCenter(geoPoint: GeoPoint) {
        //items.add(OverlayItem(searchField?.text.toString(), searchField?.text.toString(), geoPoint)
        mMapView?.controller?.setCenter(geoPoint)
        mMapView?.overlays?[0]
        //mMapView?.overlays?.add(mMyLocationOverlay)
        mMapView?.invalidate()
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

        override fun doInBackground(vararg params: URL?): String {
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
            //Log.d("be.ap.edu.mapsaver", jsonString.toString())
            val parser: Parser = Parser.default()
            val array = parser.parse(jsonString) as JsonArray<JsonObject>

            if (array.size > 0) {
                // Use low-level API
                val obj = array[0]

                //Log.d("be.ap.edu.mapsaver", "onResponse" + obj.string("lat")!! + " " + obj.string("lon")!!)
                // mapView center must be updated here and not in doInBackground because of UIThread exception
                setCenter(GeoPoint(obj.string("lat")!!.toDouble(), obj.string("lon")!!.toDouble()))
            }
        }
    }
}
