package com.example.android.rendezvous.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.example.android.rendezvous.R;
import com.example.android.rendezvous.utils.CircularTransform;
import com.example.android.rendezvous.utils.DocumentHelper;
import com.example.android.rendezvous.utils.FontCache;
import com.example.android.rendezvous.utils.ImageUtils;
import com.example.android.rendezvous.utils.PicassoCache;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ProfileSettingsActivity extends AppCompatActivity {
    private static final int PICK_IMAGE = 1;
    private static final String TAG = ProfileSettingsActivity.class.getSimpleName();
    private static final int MAX_LENGTH = 10;
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 9001;
    @Bind(R.id.avatar)
    ImageView circleImageView;
    @Bind(R.id.display_name_txt)
    TextView mDisplayName;
    @Bind(R.id.about_txt)
    TextView mAbout;
    @Bind(R.id.change_img)
    Button mChangeImageBtn;
    @Bind(R.id.change_about)
    Button mChangeAboutBtn;
    private Uri imagePath;
    private DatabaseReference mUserDatabase;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private FirebaseUser mCurrentUser;
    private FirebaseAuth mAuth;
    private StorageReference mStorageRef;
    private MaterialDialog dialog;
    private String uId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_settings);
        ButterKnife.bind(this);
        Typeface tf = FontCache.get("fonts/Aller_Rg.ttf", this);
        mDisplayName.setTypeface(tf);
        Typeface tf1 = FontCache.get("fonts/Aller_Lt.ttf", this);
        mAbout.setTypeface(tf1);
        mChangeImageBtn.setTypeface(tf1);
        mChangeAboutBtn.setTypeface(tf1);
        mStorageRef = FirebaseStorage.getInstance().getReferenceFromUrl("gs://rendezvous-a14ef.appspot.com/");
        mAuth = FirebaseAuth.getInstance();
        mCurrentUser = mAuth.getCurrentUser();
        uId = mCurrentUser.getUid();
        mUserDatabase = FirebaseDatabase.getInstance().getReference()
                .child("Users").child(uId);
        mUserDatabase.keepSynced(true);
        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    mUserDatabase.child("online").setValue("true");
                    mUserDatabase.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            String name = dataSnapshot.child("name").getValue().toString();
                            final String image = dataSnapshot.child("image").getValue().toString();
                            String about = dataSnapshot.child("about").getValue().toString();
                            if (image != null && !TextUtils.isEmpty(image)) {
                                PicassoCache.get(ProfileSettingsActivity.this)
                                        .load(image)
                                        .networkPolicy(NetworkPolicy.OFFLINE)
                                        .placeholder(R.drawable.ic_place_holder)
                                        .resizeDimen(R.dimen.user_avatar_size, R.dimen.user_avatar_size)
                                        .centerCrop()
                                        .transform(new CircularTransform())
                                        .into(circleImageView, new Callback() {
                                            @Override
                                            public void onSuccess() {

                                            }

                                            @Override
                                            public void onError() {
                                                Picasso.with(ProfileSettingsActivity.this)
                                                        .load(image)
                                                        .placeholder(R.drawable.ic_place_holder)
                                                        .resizeDimen(R.dimen.user_avatar_size, R.dimen.user_avatar_size)
                                                        .centerCrop()
                                                        .transform(new CircularTransform())
                                                        .into(circleImageView);
                                            }
                                        });
                            }
                            mDisplayName.setText(name);
                            mAbout.setText(about);
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                } else {
                    mUserDatabase.child("online").setValue(ServerValue.TIMESTAMP);
                    FirebaseAuth.getInstance().signOut();
                    startActivity(new Intent(ProfileSettingsActivity.this, LoginActivity.class));
                }
            }
        };

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mAuthStateListener != null) {
            mAuth.removeAuthStateListener(mAuthStateListener);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mAuth.addAuthStateListener(mAuthStateListener);
    }


    @OnClick(R.id.change_about)
    void changeAbout() {
        String about = mAbout.getText().toString().trim();
        String username = mDisplayName.getText().toString().trim();
        Intent i = new Intent(ProfileSettingsActivity.this, AboutActivity.class);
        i.putExtra("about", about);
        i.putExtra("username", username);
        startActivity(i);
    }

    @OnClick(R.id.change_img)
    void changeImage() {
        if (ContextCompat.checkSelfPermission(ProfileSettingsActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(ProfileSettingsActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
        } else {
            Intent getIntent = new Intent(Intent.ACTION_GET_CONTENT);
            getIntent.setType("image/*");
            startActivityForResult(getIntent, PICK_IMAGE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_IMAGE)
            if (resultCode == RESULT_OK) {
                imagePath = data.getData();
                if (data.getData() != null) {
                    showLoading();
                    String filePath = DocumentHelper.getPath(this, imagePath);
                    final Uri file = Uri.fromFile(new File(ImageUtils.compressImage(filePath)));
                    StorageReference fileStorage = mStorageRef.child("profile_images").
                            child("profile_" + mCurrentUser.getUid() + ".jpg");
                    final StorageReference thumbStorage = mStorageRef.child("profile_images")
                            .child("thumbs").child("profile_" + mCurrentUser.getUid() + ".jpg");
                    fileStorage.putFile(imagePath).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            final String downloadUrl = taskSnapshot.getDownloadUrl().toString();
                            UploadTask uploadTask = thumbStorage.putFile(file);
                            uploadTask.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                                    String thumbUrl = task.getResult().getDownloadUrl().toString();
                                    if (task.isSuccessful()) {
                                        Map updateHash = new HashMap();
                                        updateHash.put("image", downloadUrl);
                                        updateHash.put("thumb_image", thumbUrl);
                                        mUserDatabase.updateChildren(updateHash).
                                                addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        dialog.dismiss();
                                                        if (!task.isSuccessful()) {
                                                            Log.d(TAG, "onFailure: " + task.getException());
                                                            Toast.makeText(ProfileSettingsActivity.this, task.getException().toString(),
                                                                    Toast.LENGTH_SHORT).show();
                                                        }
                                                    }
                                                });
                                    } else {
                                        Log.d(TAG, "onFailure: " + task.getException());
                                        dialog.dismiss();
                                        Toast.makeText(ProfileSettingsActivity.this, task.getException().toString(),
                                                Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            Log.d(TAG, "onFailure: " + exception.getMessage());
                            dialog.dismiss();
                            Toast.makeText(ProfileSettingsActivity.this, exception.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });

                }
            }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void showLoading() {
        if (dialog != null && dialog.isShowing())
            dialog.dismiss();
        dialog = new MaterialDialog.Builder(this)
                .title("Uploading your image")
                .content(R.string.please_wait)
                .progress(true, 0)
                .cancelable(false)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Intent getIntent = new Intent(Intent.ACTION_GET_CONTENT);
                    getIntent.setType("image/*");
                    startActivityForResult(getIntent, PICK_IMAGE);
                } else {
                    Toast.makeText(ProfileSettingsActivity.this, "You should grant permission to proceed",
                            Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }

}
