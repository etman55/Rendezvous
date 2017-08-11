package com.example.android.rendezvous.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.example.android.rendezvous.R;
import com.example.android.rendezvous.utils.FontCache;
import com.example.android.rendezvous.utils.PrefUtil;
import com.example.android.rendezvous.utils.Utils;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = SettingsActivity.class.getSimpleName();
    @Bind(R.id.settings_app_bar)
    Toolbar mToolbar;
    @Bind(R.id.content_layout)
    RelativeLayout contentLayout;
    @Bind(R.id.delete_account_btn)
    Button deleteAccountBtn;
    @Bind(R.id.save_radius)
    Button saveRadiusBtn;
    @Bind(R.id.radius_input_tv)
    TextInputLayout radiusInput;
    @Bind(R.id.tool_bar_txt)
    TextView toolbarTxt;
    @Bind(R.id.textView2)
    TextView textView;
    private FirebaseUser mFirebaseUser;
    private DatabaseReference mDatabaseRef;
    private MaterialDialog materialDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        ButterKnife.bind(this);
        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDefaultDisplayHomeAsUpEnabled(true);
        Typeface tf = FontCache.get(getString(R.string.aller_bold), this);
        toolbarTxt.setTypeface(tf);
        Typeface tf2 = FontCache.get(getString(R.string.aller_regular), this);
        textView.setTypeface(tf2);
        toolbarTxt.setText(getString(R.string.settings));
        Typeface tf1 = FontCache.get(getString(R.string.aller_light), this);
        radiusInput.setTypeface(tf1);
        saveRadiusBtn.setTypeface(tf1);
        deleteAccountBtn.setTypeface(tf1);
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mAuth.getCurrentUser();
        mDatabaseRef = FirebaseDatabase.getInstance().getReference();
        if (PrefUtil.getIntToPref() != 0) {
            radiusInput.getEditText().setText(String.valueOf(PrefUtil.getIntToPref()));
        }
    }

    @OnClick(R.id.save_radius)
    void saveRadius() {
        if (TextUtils.isEmpty(radiusInput.getEditText().getText().toString().trim())) {
            radiusInput.setError(getString(R.string.input_empty_error));
            return;
        }
        int radius = Integer.parseInt(radiusInput.getEditText().getText().toString().trim());
        PrefUtil.saveIntToPref(radius);
        Utils.hideKeyboard(radiusInput.getEditText());
        Snackbar.make(contentLayout, "Radius parameter saved: " + radius + " KM", Snackbar.LENGTH_SHORT).show();
    }

    @OnClick(R.id.delete_account_btn)
    void deleteAccount() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(
                SettingsActivity.this);
        builder.setMessage(
                getString(R.string.delete_account_msg))
                .setCancelable(false)
                .setPositiveButton("Ok",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                final int id) {
                                showLoading();
                                String userRef = "Users/" + mFirebaseUser.getUid();
                                String friendsRef = "Friends/" + mFirebaseUser.getUid();
                                String geoFireRef = "users_locations/" + mFirebaseUser.getUid();
                                String messagesRef = "messages/" + mFirebaseUser.getUid();
                                final Map deleteMap = new HashMap<>();
                                deleteMap.put(userRef, null);
                                deleteMap.put(friendsRef, null);
                                deleteMap.put(geoFireRef, null);
                                deleteMap.put(messagesRef, null);
                                mDatabaseRef.child("Friends").addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        if (dataSnapshot.hasChild(mFirebaseUser.getUid()))
                                            mDatabaseRef.child("Friends")
                                                    .child(dataSnapshot.getKey())
                                                    .removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    mDatabaseRef.child("messages").addListenerForSingleValueEvent(new ValueEventListener() {
                                                        @Override
                                                        public void onDataChange(DataSnapshot dataSnapshot) {
                                                            if (dataSnapshot.hasChild(mFirebaseUser.getUid()))
                                                                mDatabaseRef
                                                                        .child("messages")
                                                                        .child(dataSnapshot.getKey())
                                                                        .removeValue()
                                                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                            @Override
                                                                            public void onSuccess(Void aVoid) {
                                                                                mDatabaseRef.child("Chat").addListenerForSingleValueEvent(new ValueEventListener() {
                                                                                    @Override
                                                                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                                                                        if (dataSnapshot.hasChild(mFirebaseUser.getUid()))
                                                                                            mDatabaseRef.child("Chat").child(dataSnapshot.getKey()).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                                                @Override
                                                                                                public void onSuccess(Void aVoid) {
                                                                                                    mDatabaseRef.updateChildren(deleteMap, new DatabaseReference.CompletionListener() {
                                                                                                        @Override
                                                                                                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                                                                                            mFirebaseUser
                                                                                                                    .delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                                                                @Override
                                                                                                                public void onSuccess(Void aVoid) {
                                                                                                                    materialDialog.dismiss();
                                                                                                                    Log.d(TAG, "OK! Works fine!");
                                                                                                                    startActivity(new Intent(SettingsActivity.this, LoginActivity.class));
                                                                                                                }
                                                                                                            }).addOnFailureListener(new OnFailureListener() {
                                                                                                                @Override
                                                                                                                public void onFailure(@NonNull Exception e) {
                                                                                                                    materialDialog.dismiss();
                                                                                                                    Log.e(TAG, "onFailure: " + e.getMessage());
                                                                                                                    Snackbar.make(contentLayout, e.getMessage(), Snackbar.LENGTH_SHORT).show();
                                                                                                                }
                                                                                                            });
                                                                                                            if (databaseError != null) {
                                                                                                                materialDialog.dismiss();
                                                                                                                Log.e(TAG, "onFailure: " + databaseError.getMessage());
                                                                                                                Snackbar.make(contentLayout, databaseError.getMessage(), Snackbar.LENGTH_SHORT).show();
                                                                                                            }
                                                                                                        }
                                                                                                    });
                                                                                                }
                                                                                            });
                                                                                    }

                                                                                    @Override
                                                                                    public void onCancelled(DatabaseError databaseError) {
                                                                                        materialDialog.dismiss();
                                                                                        Log.e(TAG, "onFailure: " + databaseError.getMessage());
                                                                                        Snackbar.make(contentLayout, databaseError.getMessage(), Snackbar.LENGTH_SHORT).show();
                                                                                    }
                                                                                });

                                                                            }
                                                                        }).addOnFailureListener(new OnFailureListener() {
                                                                    @Override
                                                                    public void onFailure(@NonNull Exception e) {
                                                                        materialDialog.dismiss();
                                                                        Log.e(TAG, "onFailure: " + e.getMessage());
                                                                        Snackbar.make(contentLayout, e.getMessage(), Snackbar.LENGTH_SHORT).show();
                                                                    }
                                                                });
                                                        }

                                                        @Override
                                                        public void onCancelled(DatabaseError databaseError) {
                                                            materialDialog.dismiss();
                                                            Log.e(TAG, "onFailure: " + databaseError.getMessage());
                                                            Snackbar.make(contentLayout, databaseError.getMessage(), Snackbar.LENGTH_SHORT).show();
                                                        }
                                                    });
                                                }
                                            }).addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    materialDialog.dismiss();
                                                    Log.e(TAG, "onFailure: " + e.getMessage());
                                                    Snackbar.make(contentLayout, e.getMessage(), Snackbar.LENGTH_SHORT).show();
                                                }
                                            });
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {
                                        materialDialog.dismiss();
                                        Log.e(TAG, "onFailure: " + databaseError.getMessage());
                                        Snackbar.make(contentLayout, databaseError.getMessage(), Snackbar.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        })
                .setNegativeButton("later", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog,
                                        final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    private void showLoading() {
        if (materialDialog != null && materialDialog.isShowing())
            materialDialog.dismiss();
        materialDialog = new MaterialDialog.Builder(this)
                .title(R.string.delete_account)
                .content(R.string.please_wait)
                .progress(true, 0)
                .cancelable(false)
                .show();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (materialDialog != null && materialDialog.isShowing())
            materialDialog.dismiss();
    }
}
