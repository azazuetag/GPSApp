package com.mtw.juancarlos.gpsapp

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import  android.Manifest
import android.animation.AnimatorInflater
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.support.v4.app.ActivityCompat
import android.util.Log
import android.widget.Toast
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task

class MainActivity : AppCompatActivity() {

    companion object {
        private val REQUEST_LOCATION_PERMISSION = 1
        private val REQUEST_CHECK_SETTINGS = 1
    }

    private lateinit var locationCallback: LocationCallback
    private  lateinit var fusedLocationClient: FusedLocationProviderClient
    private var requestingLocationUpdate = false
    private val animRotate by lazy {
       AnimatorInflater.loadAnimator(this, R.animator.rotate)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        progressBar.visibility = View.INVISIBLE

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)


        button_location.setOnClickListener(){
            if (checkPermission()){
                Log.i("GSAPP","Acceso Concedido")
                getLastLocation()
            }
            else {
                Log.i("GSAPP","Acceso Denegado")
            }
        }
        button_startTrack.setOnClickListener(){
            if (checkPermission()){
             if (!requestingLocationUpdate){
                 startLocationUpdates()
             }
                else{
                 stopLocationUpdate()
             }
            }
            else {

            }
        }
        animRotate.setTarget(imageview_android)
        locationCallback = object  : LocationCallback(){
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                onLocationChanged(locationResult.lastLocation)
            }
        }
    }

    private fun getLastLocation(){
        try {
            fusedLocationClient.lastLocation
                    .addOnSuccessListener {location: Location? ->
                        Log.i("GPSAPP","addOnSuccessListener")
                        onLocationChanged(location)
                    }
                    .addOnFailureListener {
                        Log.i("GPSAPP","addOnFailureListener")
                        Toast.makeText(this@MainActivity, "Error en la lectura del GPS",Toast.LENGTH_LONG).show()
                    }
        } catch (e: SecurityException) {
            Toast.makeText(this@MainActivity, "SecEx:" + e.message,Toast.LENGTH_LONG).show()
        }
    }

    private fun onLocationChanged(location: Location?){
        if (location != null){
            textview_location.text = getString(R.string.location_text,
                    location?.latitude,
                    location?.longitude,
                    location?.time
                    )
        } else{
            textview_location.text == "No se recuero la ubicación"
        }
    }

    private fun checkPermission(): Boolean{
        if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return  true
         }
        else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_LOCATION_PERMISSION)
            return false
        }

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode){
            REQUEST_LOCATION_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0]== PackageManager.PERMISSION_GRANTED){
                    //Obtener geolocalización
                    getLastLocation()
                }
                else
                {
                    Toast.makeText(this, "Acceso al GPS denegado",Toast.LENGTH_LONG).show()
                }
                return
            }
        }
    }

    private fun startLocationUpdates(){
        progressBar.visibility = View.VISIBLE
        animRotate.start()
        requestingLocationUpdate = true
        button_startTrack.text = "Detener"
        textview_location.text="Localizando..."
        //*****************************************

        val locationRequest = LocationRequest().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        val builder = LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)
        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())
        task.addOnSuccessListener { locationSettingsResponse ->
            // All location settings are satisfied. The client can initialize
            // location requests here.
            // ...
            Log.e("GPSANDROIDAPP", "OnSucessListener Task")
            try {
                fusedLocationClient.requestLocationUpdates(locationRequest,locationCallback,null)

            } catch (e: SecurityException) {
                Log.e("GPSANDROIDAPP", "SecEx "+ e.message)
                Toast.makeText(this@MainActivity,"secex:"+ e.message,Toast.LENGTH_LONG)
                progressBar.visibility = View.INVISIBLE
            }
        }

        task.addOnFailureListener { exception ->
            progressBar.visibility = View.INVISIBLE
            if (exception is ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    Log.e("GPSANDROIDAPP", "OnFailureListener")
                    exception.startResolutionForResult(this@MainActivity,
                            REQUEST_CHECK_SETTINGS)
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode){
            REQUEST_CHECK_SETTINGS -> {
                if (resultCode == Activity.RESULT_OK){
                    Log.e("GPSANDROIDAPP","CONFIGURACIÓN DE GPS OK")
                    startLocationUpdates()
                }
                return
            }
        }
    }

    private  fun stopLocationUpdate(){
        if (requestingLocationUpdate){
            progressBar.visibility = View.INVISIBLE
            animRotate.end()
            requestingLocationUpdate = true
            button_startTrack.text = "Rastrear"
            textview_location.text="Presiona el boton para obtener la ultima ubicación"
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    override fun onResume() {
        if (requestingLocationUpdate) startLocationUpdates()
        super.onResume()
    }
    override fun onPause() {
        if (requestingLocationUpdate ) {
            //stopLocationUpdates()
            stopLocationUpdate()
            requestingLocationUpdate = false
        }
        super.onPause()
    }
}
