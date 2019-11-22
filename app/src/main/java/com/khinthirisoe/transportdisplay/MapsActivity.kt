package com.khinthirisoe.transportdisplay

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private val TAG = MapsActivity::class.java.simpleName
    private val mMarkers: HashMap<String, Marker> = HashMap()
    private lateinit var mMap: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
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

        mMap.setMaxZoomPreference(16f)
        loginToFirebase()
    }

    private fun loginToFirebase() {
        val email = getString(R.string.firebase_email)
        val password = getString(R.string.firebase_password)
        // Authenticate with Firebase and subscribe to updates
        FirebaseAuth.getInstance().signInWithEmailAndPassword(
            email, password
        ).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                subscribeToUpdates()
                Log.d(TAG, "firebase auth success")
            } else {
                Log.d(TAG, "firebase auth failed")
            }
        }
    }

    private fun subscribeToUpdates() {
        val ref = FirebaseDatabase.getInstance().getReference(getString(R.string.firebase_path))
        ref.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, previousChildName: String?) {
                setMarker(dataSnapshot)
                Log.d("message","onChildAdded " + dataSnapshot.value)
            }

            override fun onChildChanged(dataSnapshot: DataSnapshot, previousChildName: String?) {
                setMarker(dataSnapshot)
                Log.d("message","onChildChanged " + dataSnapshot.value)
            }

            override fun onChildMoved(dataSnapshot: DataSnapshot, previousChildName: String?) {
                Log.d("message","onChildMoved " + dataSnapshot.value)
            }

            override fun onChildRemoved(dataSnapshot: DataSnapshot) {
                Log.d("message","onChildRemoved " + dataSnapshot.value)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("message", "Failed to read value" , error.toException())
            }
        })
    }

    private fun setMarker(dataSnapshot: DataSnapshot) {
        // When a location update is received, put or update
        // its value in mMarkers, which contains all the markers
        // for locations received, so that we can build the
        // boundaries required to show them all on the map at once
        val key = dataSnapshot.key
        val value = dataSnapshot.value as HashMap<String, Marker>?
        val lat = value!!["latitude"].toString().toDouble()
        val lng = value["longitude"].toString().toDouble()
        val location = LatLng(lat, lng)
        if (!mMarkers.containsKey(key)) {
            mMarkers[key!!] = mMap.addMarker(MarkerOptions().title(key).position(location))
        } else {
            mMarkers[key]!!.position = location
        }
        val builder = LatLngBounds.Builder()
        for (marker in mMarkers.values) {
            builder.include(marker.position)
        }
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 300))
    }
}
