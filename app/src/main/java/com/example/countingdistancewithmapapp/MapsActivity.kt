package com.example.countingdistancewithmapapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.core.app.ActivityCompat
import com.example.countingdistancewithmapapp.databinding.ActivityMapsBinding
import com.google.android.gms.location.*

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar

class MapsActivity : AppCompatActivity(), OnMapReadyCallback,
    GoogleMap.OnMapLoadedCallback,
    GoogleMap.OnMarkerClickListener,
    GoogleMap.OnMapLongClickListener    {

    private lateinit var binding: ActivityMapsBinding
    private val MY_PERMISSION_REQUEST_ACCESS_FINE_LOCATION = 101
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var mLocationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    var gpsMarker: Marker? = null
    private lateinit var mMap: GoogleMap
    val markerList: ArrayList<Marker> = ArrayList()
    var totalDistance = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                MY_PERMISSION_REQUEST_ACCESS_FINE_LOCATION
            )
            return
        }
        binding.distanceInfo.text = String.format(getString(R.string.total_distance_format), totalDistance)
        binding.clearButton.setOnClickListener {
            markerList.clear()
            mMap.clear()
            totalDistance = 0f
            Snackbar.make(binding.root, "Map cleared", Snackbar.LENGTH_LONG).show()
            showLastLocationMarker()
            mMap.moveCamera(CameraUpdateFactory.zoomTo(5f))
            binding.distanceInfo.text = String.format(getString(R.string.total_distance_format), totalDistance)
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isMapToolbarEnabled = false
        mMap.setOnMapLoadedCallback(this)
        mMap.setOnMarkerClickListener(this)
        mMap.setOnMapLongClickListener(this)
        // Add a marker in Sydney and move the camera
        val koronowo = LatLng(53.3, 17.9)
        mMap.addMarker(MarkerOptions().position(koronowo).title("Marker in Koronowo City"))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(koronowo))
    }

    override fun onMapLoaded() {
        Log.i(localClassName, "Map loaded")
        showLastLocationMarker()
    }

    override fun onMarkerClick(p0: Marker): Boolean {
        if(mMap.cameraPosition.zoom < 14f)
        {
            mMap.moveCamera(CameraUpdateFactory.zoomTo(14f))
        }
        return false
    }

    override fun onMapLongClick(latLng: LatLng) {
        var distance = 0f
        if (markerList.size > 0) {
            val lastMarker = markerList.get(markerList.size - 1)
            val result = FloatArray(3)
            Location.distanceBetween(
                lastMarker.position.latitude,
                lastMarker.position.longitude,
                latLng.latitude,
                latLng.longitude,
                result
            )
            distance = result[0]
            totalDistance += distance / 1000f
            val rectOptions = with(PolylineOptions()) {
                add(lastMarker.position)
                add(latLng)
                width(10f)
                color(Color.BLUE)
            }
            mMap.addPolyline(rectOptions)
        }
        val marker = mMap.addMarker(with(MarkerOptions()){
            position(LatLng(latLng.latitude, latLng.longitude))
            icon(BitmapDescriptorFactory.fromResource(R.drawable.marker2))
            alpha(0.8f)
            title(
                String.format(
                    getString(R.string.marker_info_format),
                    latLng.latitude,
                    latLng.longitude,
                    distance
                )
            )
        })
        markerList.add(marker!!)
        binding.distanceInfo.text =
            String.format(getString(R.string.total_distance_format), totalDistance)
    }

    private fun createLocationRequest(){
        mLocationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }

    private fun createLocationCallback(){
        Log.i(localClassName, "createCallback")
        locationCallback = object: LocationCallback(){
            override fun onLocationResult(locationResult: LocationResult) {
                gpsMarker?.remove()
                gpsMarker = mMap.addMarker(with(MarkerOptions()) {
                    position(
                        LatLng(locationResult.lastLocation.latitude,
                        locationResult.lastLocation.longitude
                    )
                    )
                    icon(BitmapDescriptorFactory.fromResource(R.drawable.marker))
                    alpha(0.8f)
                    title(getString(R.string.current_loc_msg))
                }) }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates(){
        fusedLocationClient.requestLocationUpdates(mLocationRequest,locationCallback,mainLooper)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == MY_PERMISSION_REQUEST_ACCESS_FINE_LOCATION){
            val indexOf = permissions.indexOf(Manifest.permission.ACCESS_FINE_LOCATION)
            if (indexOf != -1 && grantResults[indexOf] != PackageManager.PERMISSION_GRANTED) {
                Snackbar.make(
                    binding.root,
                    "Permission is required to continue",
                    Snackbar.LENGTH_LONG
                )
                    .setAction("RETRY") {
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                            MY_PERMISSION_REQUEST_ACCESS_FINE_LOCATION
                        )
                    }.show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.i(localClassName, "onResume")
        createLocationRequest()
        createLocationCallback()
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        startLocationUpdates()
    }

    @SuppressLint("MissingPermission")
    private fun showLastLocationMarker(){
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                mMap.addMarker(with(MarkerOptions()){
                    position(LatLng(it.latitude, it.longitude))
                    icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                    title(getString(R.string.last_known_loc_msg))
                })
            }
        }
    }

    override fun onPause() {
        Log.i(localClassName, "onPause")
        super.onPause()
        stopLocationUpdates()
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.normalMap -> mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
            R.id.hybridMap -> mMap.mapType = GoogleMap.MAP_TYPE_HYBRID
            R.id.satelliteMap -> mMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
            R.id.terrainMap -> mMap.mapType = GoogleMap.MAP_TYPE_TERRAIN
        }
        return super.onOptionsItemSelected(item)
    }
    fun zoomInClick(view: View){
        mMap.moveCamera(CameraUpdateFactory.zoomIn())
    }
    fun zoomOutClick(view: View){
        mMap.moveCamera(CameraUpdateFactory.zoomOut())
    }
    fun moveToMyLocation(view: View){
        gpsMarker?.let { marker ->
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.position, 12f))
        }
    }
}