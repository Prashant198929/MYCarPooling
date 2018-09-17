package com.example.psawarwadkar.mycarpooling;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;


public class DriverMapActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

    private GoogleMap mMap;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    LocationRequest mLocationRequest;

    private Button mLogout, mSettings;

    private String customerId = "";

    private SupportMapFragment mapFragment;

    private Boolean isLoggingOut = false;

    private LinearLayout mCustomerInfo;
    private TextView mCustomerName, mCustomerPhone;



    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(DriverMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        } else {
            mapFragment.getMapAsync(this);
        }


        mCustomerInfo = (LinearLayout) findViewById(R.id.customerInfo);
        mCustomerName = (TextView) findViewById(R.id.customerName);
        mCustomerPhone = (TextView) findViewById(R.id.customerPhone);





        mLogout = (Button) findViewById(R.id.logout);
        mSettings = (Button) findViewById(R.id.settings);

        mLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isLoggingOut = true;

                disconnectDriver();

                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(DriverMapActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });

        mSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DriverMapActivity.this, DriverSettingsActivity.class);
                startActivity(intent);
                finish();

            }
        });

        getAssignedCustomer();

    }

    private void getAssignedCustomer(){
        String driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference assignedCustomerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverId).child("customerRideId");
        assignedCustomerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                        customerId = dataSnapshot.getValue().toString();
                        getAssignedCustomerPickupLocation();
                        getAssignedCustomerInfo();
                    mCustomerInfo.setVisibility(View.GONE);
                    mCustomerName.setText("");
                    mCustomerPhone.setText("");
                    }

                }


            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }
    private void getAssignedCustomerPickupLocation(){
        DatabaseReference assignedCustomerPickupLocation = FirebaseDatabase.getInstance().getReference().child("customerRequest").child(customerId).child("1");
        assignedCustomerPickupLocation.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    if (dataSnapshot.exists()){
                        List<Object> map = (List<Object>) dataSnapshot.getValue();
                        double locationLat = 0;
                        double locationLng = 0;
                        if(map.get(0) != null){
                            locationLat = Double.parseDouble(map.get(0).toString());
                        }
                        if(map.get(1) != null){
                            locationLng = Double.parseDouble(map.get(1).toString());
                        }

                        LatLng driverLatLng = new LatLng(locationLat, locationLng);
                        mMap.addMarker(new MarkerOptions().position(driverLatLng).title("Pickup location"));



                        getAssignedCustomerPickupLocation();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }


    private void getAssignedCustomerInfo() {
        mCustomerInfo.setVisibility(View.VISIBLE);
        DatabaseReference mCustomerDataBase = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(customerId);
        mCustomerDataBase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();

                    if (map.get("name") != null) {
                        mCustomerName.setText(map.get("name").toString());
                    }
                    if (map.get("phone") != null) {
                        mCustomerPhone.setText(map.get("phone").toString());
                    }
                }
            }


            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }



            @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(DriverMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        }
        buildGoogleApiClient();
        mMap.setMyLocationEnabled(true);

    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }


    @Override
    public void onLocationChanged(Location location) {
        if(getApplicationContext()!= null){

            mLastLocation = location;
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(11));

            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            DatabaseReference refAvailable = FirebaseDatabase.getInstance().getReference("driversAvailable");
            DatabaseReference refWorking = FirebaseDatabase.getInstance().getReference("driversAvailable");
            GeoFire geoFireAvailable = new GeoFire(refAvailable);
            GeoFire geoFireWorking = new GeoFire(refWorking);
            switch (customerId){
                case "":
                    geoFireWorking.removeLocation(userId);
                    geoFireAvailable.setLocation(userId, new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()));
                break;

                default:
                    geoFireAvailable.removeLocation(userId);
                    geoFireWorking.setLocation(userId, new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()));
                    break;
            }
        }

        }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(DriverMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);


    }

    @Override
    public void onConnectionSuspended(int i) {

    }


    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    final int LOCATION_REQUEST_CODE = 1;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case LOCATION_REQUEST_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mapFragment.getMapAsync(this);
                } else {
                    Toast.makeText(getApplicationContext(), "Please provide the permission", Toast.LENGTH_LONG).show();
                }
                break;
            }

        }

    }

    private void disconnectDriver(){
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("driverAvailable");


        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(userId);

    }

    protected void onStop(){
        super.onStop();
        if(!isLoggingOut){
           disconnectDriver();

        }

    }
}