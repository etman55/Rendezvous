package com.example.android.rendezvous.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.rendezvous.R;
import com.example.android.rendezvous.activities.ProfileActivity;
import com.example.android.rendezvous.adapters.PeopleAdapter;
import com.example.android.rendezvous.models.User;
import com.example.android.rendezvous.utils.GpsLocation;
import com.example.android.rendezvous.utils.PrefUtil;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by Etman on 8/5/2017.
 */

public class FindPeopleFragment extends Fragment implements GeoQueryEventListener, GpsLocation.GpsEventHandler {
    private static final GeoLocation INITIAL_CENTER = new GeoLocation(37.7789, -122.4017);
    private static final String TAG = FindPeopleFragment.class.getSimpleName();
    @Bind(R.id.people_list)
    RecyclerView peopleList;
    @Bind(R.id.progress_find_people)
    ProgressBar progressBar;
    @Bind(R.id.no_people_txt)
    TextView noPeopleTxt;
    private DatabaseReference mUserDatabase;
    private DatabaseReference mGeoFireDatabase;
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private GeoFire geoFire;
    private GeoQuery geoQuery;
    private PeopleAdapter peopleAdapter;
    private List<User> userList = new ArrayList<>();
    private List<String> peopleIds = new ArrayList<>();
    private LinearLayoutManager linearLayoutManager;
    private ValueEventListener peopleEventListener;
    private boolean newKeyEntered = false;
    private GpsLocation gpsLocation = null;
    private int geoLocationRadius = 0;

    public FindPeopleFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_find_people, container, false);
        ButterKnife.bind(this, view);
        if (!isLocationServicesEnabled())
            buildAlertMessageNoGps();
        if (gpsLocation == null) {
            gpsLocation = new GpsLocation(getContext());
            gpsLocation.setGpsEventHandler(this);
        }
        noPeopleTxt.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.INVISIBLE);
        userList.clear();
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        if (PrefUtil.getIntToPref("Radius") == 0)
            PrefUtil.saveIntToPref(5, "Radius");
        else {
            geoLocationRadius = PrefUtil.getIntToPref("Radius");
        }
        if (mUserDatabase == null)
            mUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users");
        if (mGeoFireDatabase == null)
            mGeoFireDatabase = FirebaseDatabase.getInstance().getReference().child("users_locations");
        if (geoFire == null)
            geoFire = new GeoFire(mGeoFireDatabase);
        if (geoQuery == null)
            geoQuery = geoFire.queryAtLocation(INITIAL_CENTER, 5);
        if (geoQuery != null)
            geoQuery.addGeoQueryEventListener(this);
        peopleList.setHasFixedSize(true);
        linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        peopleList.setLayoutManager(linearLayoutManager);
        peopleAdapter = new PeopleAdapter(userList);
        peopleList.setAdapter(peopleAdapter);
        peopleAdapter.setItemClickHandler(new PeopleAdapter.ItemClickHandler() {
            @Override
            public void onItem(int position, String thumb) {
                String userId = peopleIds.get(position);
                Intent i = new Intent(getActivity(), ProfileActivity.class);
                i.putExtra("user_id", userId);
                i.putExtra("thumb", thumb);
                getActivity().startActivity(i);
            }
        });
        if (peopleEventListener == null)
            peopleEventListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    User user = new User();
                    user.setName(dataSnapshot.child("name").getValue().toString());
                    user.setThumb_image(dataSnapshot.child("thumb_image").getValue().toString());
                    user.setAbout(dataSnapshot.child("about").getValue().toString());
                    user.setOnline(dataSnapshot.child("online").getValue().toString());
                    userList.add(user);
                    peopleAdapter.notifyDataSetChanged();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.d(TAG, "onCancelled: " + databaseError.getMessage());
                }
            };

        return view;
    }

    @Override
    public void onStop() {
        super.onStop();
        geoQuery.removeAllListeners();
        peopleIds.clear();
        userList.clear();
        if (gpsLocation != null) {
            gpsLocation.setGpsEventHandler(null);
            gpsLocation = null;
        }
        if (peopleEventListener != null)
            peopleEventListener = null;
        if (newKeyEntered)
            newKeyEntered = false;
    }

    @Override
    public void onKeyEntered(final String key, GeoLocation location) {
        if (peopleIds.size() != 0) {
            noPeopleTxt.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.INVISIBLE);
        }
        if (!key.equals(mUser.getUid())) {
            peopleIds.add(key);
            newKeyEntered = true;
        }
    }

    @Override
    public void onKeyExited(String key) {
        if (peopleIds.contains(key)) {
            peopleIds.remove(key);
            newKeyEntered = false;
            if (peopleIds.size() == 0) {
                noPeopleTxt.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.INVISIBLE);
            } else {
                noPeopleTxt.setVisibility(View.INVISIBLE);
                progressBar.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onKeyMoved(String key, GeoLocation location) {
    }

    @Override
    public void onGeoQueryReady() {
        if (peopleIds.size() == 0) {
            noPeopleTxt.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.INVISIBLE);
        } else {
            noPeopleTxt.setVisibility(View.INVISIBLE);
            progressBar.setVisibility(View.INVISIBLE);
        }
        Log.d(TAG, "onGeoQueryReady: ");
        if (newKeyEntered)
            for (String key : peopleIds)
                mUserDatabase.child(key).addValueEventListener(peopleEventListener);
    }

    @Override
    public void onGeoQueryError(DatabaseError error) {
        Log.d(TAG, "onGeoQueryError: " + error.getMessage());
    }

    @Override
    public void onLocationChange(Location lastLocation) {
        geoFire.setLocation(mUser.getUid(),
                new GeoLocation(lastLocation.getLatitude(), lastLocation.getLongitude()), new GeoFire.CompletionListener() {
                    @Override
                    public void onComplete(String key, DatabaseError error) {
                        Log.d(TAG, "onComplete: success");
                    }
                });
        geoQuery.setCenter(
                new GeoLocation(lastLocation.getLatitude(), lastLocation.getLongitude()));
        geoQuery = geoFire.queryAtLocation(
                new GeoLocation(lastLocation.getLatitude(), lastLocation.getLongitude()), geoLocationRadius);
    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(
                getContext());
        builder.setMessage(
                "We need you to enable the location services ")
                .setCancelable(false)
                .setPositiveButton("Ok",
                        new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog,
                                                final int id) {
                                startActivity(
                                        new Intent(
                                                Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                            }
                        })
                .setNegativeButton("later", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog,
                                        final int id) {
                        dialog.cancel();
                        Toast.makeText(getContext(), "Please enable location services to get better experience", Toast.LENGTH_SHORT).show();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    private boolean isLocationServicesEnabled() {
        int locationMode = 0;
        String locationProviders;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                locationMode = Settings.Secure.getInt(getContext().getContentResolver(), Settings.Secure.LOCATION_MODE);
            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
            }
            return locationMode != Settings.Secure.LOCATION_MODE_OFF;
        } else {
            locationProviders = Settings.Secure.getString(getContext().getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            return !TextUtils.isEmpty(locationProviders);
        }
    }


}
