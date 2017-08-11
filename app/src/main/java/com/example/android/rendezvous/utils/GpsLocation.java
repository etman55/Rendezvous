package com.example.android.rendezvous.utils;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

/**
 * Created by Etman on 8/3/2017.
 */

public class GpsLocation implements LocationListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = GpsLocation.class.getSimpleName();
    private GpsEventHandler eventHandler;
    private LocationRequest mLocationRequest;
    private GoogleApiClient mLocationClient;
    private Location lastKnownLocation;
    private static final int SERVICE_VERSION_UPDATE_REQUIRED = 2,
            SERVICE_MISSING = 1, SERVICE_DISABLED = 3, CANCELED = 13,
            SUCCESS = 0;
    private boolean isFirstTime = false;

    private final Context appContext;

    public GpsLocation(Context context) {
        appContext = context;
        mLocationClient = new GoogleApiClient.Builder(appContext)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        LocationManager manager = (LocationManager) appContext.getSystemService(
                Context.LOCATION_SERVICE);
        // check if Google play services available, no dialog displayed
        if (isGooglePlayServicesAvailableNoDialogs()) {
            if (manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
                startGps();
        }
    }

    public void setGpsEventHandler(GpsEventHandler eventHandler) {
        this.eventHandler = eventHandler;

    }

    private void startGps() {
        mLocationRequest = LocationRequest.create();
        int PERIOD = 5000;
        mLocationRequest.setInterval(PERIOD);
        mLocationRequest.setFastestInterval(PERIOD);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationClient = new GoogleApiClient.Builder(appContext)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        if (mLocationClient != null)
            mLocationClient.connect();

    }

    public void stopGps() {
        if (mLocationClient != null && mLocationClient.isConnected()) {
            mLocationClient.disconnect();
            mLocationClient.unregisterConnectionCallbacks(this);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (eventHandler != null && mLocationClient.isConnected()) {
            if (lastKnownLocation != location || isFirstTime) {
                isFirstTime = false;
                lastKnownLocation = location;
                eventHandler.onLocationChange(location);
            }
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        try {
            if (mLocationRequest != null
                    && mLocationClient != null) {
                LocationServices.FusedLocationApi.requestLocationUpdates(
                        mLocationClient, mLocationRequest, this);
            }
        } catch (SecurityException | IllegalStateException ignored) {
            Log.d(TAG, "onConnected: " + ignored.getMessage());
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    public Location getLastKnownLocation() {
        return lastKnownLocation;
    }

    private boolean isGooglePlayServicesAvailableNoDialogs() {
        // Check that Google Play services is available
        int resultCode = GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(appContext);
        switch (resultCode) {
            case SUCCESS:
                return true;
            case SERVICE_DISABLED:
                return false;
            case SERVICE_MISSING:
                return false;
            case SERVICE_VERSION_UPDATE_REQUIRED:
                return false;
            case CANCELED:
                return false;
        }
        return false;

    }


    public interface GpsEventHandler {
        void onLocationChange(Location lastLocation);

    }

}
