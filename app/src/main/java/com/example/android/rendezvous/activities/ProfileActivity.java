package com.example.android.rendezvous.activities;

import android.app.ProgressDialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.rendezvous.R;
import com.example.android.rendezvous.utils.FontCache;
import com.example.android.rendezvous.utils.PicassoCache;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ProfileActivity extends AppCompatActivity {
    @Bind(R.id.profile_display_name_tv)
    TextView displayName;
    @Bind(R.id.profile_avatar_iv)
    ImageView avatar;
    @Bind(R.id.profile_status_tv)
    TextView about;
    @Bind(R.id.profile_send_request_btn)
    Button friendRequestBtn;
    @Bind(R.id.profile_decline_request_btn)
    Button declineBtn;
    private String uId = "";
    private DatabaseReference mUserDatabase;
    private DatabaseReference mFriendReqDatabase;
    private DatabaseReference mFriendDatabase;
    private DatabaseReference mRootRef;
    private FirebaseUser mCurrentUser;
    private String mCurrentState;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        ButterKnife.bind(this);
        Typeface tf = FontCache.get("fonts/Aller_Rg.ttf", this);
        displayName.setTypeface(tf);
        Typeface tf1 = FontCache.get("fonts/Aller_Lt.ttf", this);
        about.setTypeface(tf1);
        friendRequestBtn.setTypeface(tf1);
        declineBtn.setTypeface(tf1);
        uId = getIntent().getStringExtra("user_id");
        mRootRef = FirebaseDatabase.getInstance().getReference();
        mUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(uId);
        mFriendReqDatabase = FirebaseDatabase.getInstance().getReference().child("Friend_req");
        mFriendDatabase = FirebaseDatabase.getInstance().getReference().child("Friends");
        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (savedInstanceState == null)
            mCurrentState = "not_friends";
        else
            mCurrentState = savedInstanceState.getString("status");
        showLoading();
        declineBtn.setVisibility(View.INVISIBLE);
        mUserDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(final DataSnapshot dataSnapshot) {
                displayName.setText(dataSnapshot.child("name").getValue().toString());
                about.setText(dataSnapshot.child("about").getValue().toString());
                final String image = dataSnapshot.child("image").getValue().toString();
                if (image != null && !TextUtils.isEmpty(image)) {
                    PicassoCache.get(ProfileActivity.this)
                            .load(image)
                            .networkPolicy(NetworkPolicy.OFFLINE)
                            .placeholder(R.drawable.ic_place_holder)
                            .resizeDimen(R.dimen.user_avatar_size, R.dimen.user_avatar_size)
                            .centerCrop()
                            .into(avatar, new Callback() {
                                @Override
                                public void onSuccess() {

                                }

                                @Override
                                public void onError() {
                                    Picasso.with(ProfileActivity.this)
                                            .load(image)
                                            .placeholder(R.drawable.ic_place_holder)
                                            .resizeDimen(R.dimen.user_avatar_size, R.dimen.user_avatar_size)
                                            .centerCrop()
                                            .into(avatar);
                                }
                            });
                }
                progressDialog.dismiss();
                mFriendReqDatabase.child(mCurrentUser.getUid()).addListenerForSingleValueEvent(
                        new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                if (dataSnapshot.hasChild(uId)) {
                                    String reqType = dataSnapshot.child(uId).child("request_type").getValue().toString();
                                    if (reqType.equals("received")) {
                                        mCurrentState = "req_received";
                                        friendRequestBtn.setText("Accept Friend Request");
                                        declineBtn.setVisibility(View.VISIBLE);
                                    } else if (reqType.equals("sent")) {
                                        mCurrentState = "req_sent";
                                        friendRequestBtn.setText("Cancel Friend Request");
                                        declineBtn.setVisibility(View.INVISIBLE);
                                    }
                                } else {
                                    mFriendDatabase.child(mCurrentUser.getUid())
                                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(DataSnapshot dataSnapshot) {
                                                    if (dataSnapshot.hasChild(uId)) {
                                                        mCurrentState = "friends";
                                                        friendRequestBtn.setText("UnFriend this person");
                                                    }
                                                }

                                                @Override
                                                public void onCancelled(DatabaseError databaseError) {

                                                }
                                            });
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        }
                );
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void showLoading() {
        progressDialog = new ProgressDialog(ProfileActivity.this);
        progressDialog.setTitle("Loading User Data");
        progressDialog.setMessage("please wait while loading user data.");
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();
    }

    @OnClick(R.id.profile_send_request_btn)
    void sendRequest() {
        friendRequestBtn.setEnabled(false);
        DatabaseReference notificationDatabase = mRootRef.child("notifications").child(uId).push();
        String notificationId = notificationDatabase.getKey();
        switch (mCurrentState) {
            case "not_friends":
                HashMap<String, String> notificationData = new HashMap<>();
                notificationData.put("from", mCurrentUser.getUid());
                notificationData.put("type", "request");
                Map requestMap = new HashMap<>();
                requestMap.put("Friend_req/" + mCurrentUser.getUid() + "/" + uId + "/request_type", "sent");
                requestMap.put("Friend_req/" + uId + "/" + mCurrentUser.getUid() + "/request_type", "received");
                requestMap.put("notifications/" + uId + "/" + notificationId, notificationData);
                mRootRef.updateChildren(requestMap, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                        friendRequestBtn.setEnabled(true);
                        mCurrentState = "req_sent";
                        friendRequestBtn.setText("Cancel Friend Request");
                        declineBtn.setVisibility(View.INVISIBLE);
                        if (databaseError != null)
                            Toast.makeText(ProfileActivity.this, databaseError.getMessage(), Toast.LENGTH_SHORT)
                                    .show();
                    }
                });
                break;
            case "friends":
                friendRequestBtn.setEnabled(false);
                Map unfriendMap = new HashMap<>();
                unfriendMap.put("Friends/" + mCurrentUser.getUid() + "/" + uId, null);
                unfriendMap.put("Friends/" + uId + "/" + mCurrentUser.getUid(), null);
                mRootRef.updateChildren(unfriendMap, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                        friendRequestBtn.setEnabled(true);
                        mCurrentState = "not_friends";
                        friendRequestBtn.setText("Send Friend Request");
                        declineBtn.setVisibility(View.INVISIBLE);
                        if (databaseError != null)
                            Toast.makeText(ProfileActivity.this, databaseError.getMessage(), Toast.LENGTH_SHORT)
                                    .show();
                    }
                });
                break;
            case "req_sent":
                friendRequestBtn.setEnabled(false);
                Map cancelRequest = new HashMap<>();
                cancelRequest.put("Friend_req/" + mCurrentUser.getUid() + "/" + uId, null);
                cancelRequest.put("Friend_req/" + uId + "/" + mCurrentUser.getUid(), null);
                mRootRef.updateChildren(cancelRequest, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                        friendRequestBtn.setEnabled(true);
                        mCurrentState = "not_friends";
                        friendRequestBtn.setText("Send Friend Request");
                        declineBtn.setVisibility(View.INVISIBLE);
                        if (databaseError != null)
                            Toast.makeText(ProfileActivity.this, databaseError.getMessage(), Toast.LENGTH_SHORT)
                                    .show();
                    }
                });
                break;
            case "req_received":
                final String currentDate = getCurrentDate();
                Map friendsMap = new HashMap<>();
                friendsMap.put("Friends/" + mCurrentUser.getUid() + "/" + uId + "/date", currentDate);
                friendsMap.put("Friends/" + uId + "/" + mCurrentUser.getUid() + "/date", currentDate);
                friendsMap.put("Friend_req/" + mCurrentUser.getUid() + "/" + uId, null);
                friendsMap.put("Friend_req/" + uId + "/" + mCurrentUser.getUid(), null);
                friendsMap.put("notifications/" + uId + "/" + notificationId, null);
                mRootRef.updateChildren(friendsMap, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                        friendRequestBtn.setEnabled(true);
                        mCurrentState = "friends";
                        friendRequestBtn.setText("UnFriend this person");
                        declineBtn.setVisibility(View.INVISIBLE);
                        if (databaseError != null)
                            Toast.makeText(ProfileActivity.this, databaseError.getMessage(), Toast.LENGTH_SHORT)
                                    .show();
                    }
                });
                break;
        }
    }

    @OnClick(R.id.profile_decline_request_btn)
    void declineRequest() {
        friendRequestBtn.setEnabled(false);
        Map unfriendMap = new HashMap<>();
        unfriendMap.put("Friends/" + mCurrentUser.getUid() + "/" + uId, null);
        unfriendMap.put("Friends/" + uId + "/" + mCurrentUser.getUid(), null);
        mRootRef.updateChildren(unfriendMap, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                friendRequestBtn.setEnabled(true);
                mCurrentState = "not_friends";
                friendRequestBtn.setText("Send Friend Request");
                declineBtn.setVisibility(View.INVISIBLE);
                if (databaseError != null)
                    Toast.makeText(ProfileActivity.this, databaseError.getMessage(), Toast.LENGTH_SHORT)
                            .show();
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("status", mCurrentState);
    }

    private String getCurrentDate() {
        Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return df.format(c.getTime());
    }
}
