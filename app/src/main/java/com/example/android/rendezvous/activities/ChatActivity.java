package com.example.android.rendezvous.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.rendezvous.R;
import com.example.android.rendezvous.adapters.MessageAdapter;
import com.example.android.rendezvous.models.Message;
import com.example.android.rendezvous.utils.CircularTransform;
import com.example.android.rendezvous.utils.FontCache;
import com.example.android.rendezvous.utils.GetTimeAgo;
import com.example.android.rendezvous.utils.PicassoCache;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ChatActivity extends AppCompatActivity {
    private static final String TAG = ChatActivity.class.getSimpleName();
    private static final int PICK_IMAGE = 6;
    @Bind(R.id.chat_toolbar)
    Toolbar mToolbar;
    @Bind(R.id.chat_bar_display_name_tv)
    TextView mTitleTv;
    @Bind(R.id.chat_bar_last_seen_tv)
    TextView mLastSeenTv;
    @Bind(R.id.chat_bar_img)
    ImageView mProfileImg;
    @Bind(R.id.add_img_btn)
    ImageButton addImgBtn;
    @Bind(R.id.chat_msg_tv)
    TextView chatMsg;
    @Bind(R.id.send_msg_btn)
    ImageButton sendMsgBtn;
    @Bind(R.id.msgs_list)
    RecyclerView messagesList;
    private String mChatUser;
    private DatabaseReference mRootRef;
    private String mCurrentUserId;
    private final ArrayList<Message> mMsgList = new ArrayList<>();
    private MessageAdapter msgAdapter;
    private StorageReference mChatPhotosStorageReference;
    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        ButterKnife.bind(this);
        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        Typeface tf = FontCache.get(getString(R.string.aller_bold), this);
        mTitleTv.setTypeface(tf);
        Typeface tf1 = FontCache.get(getString(R.string.aller_light), this);
        mLastSeenTv.setTypeface(tf1);
        chatMsg.setTypeface(tf1);
        mChatUser = getIntent().getStringExtra("user_id");
        String mChatUserName = getIntent().getStringExtra("user_name");
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        mRootRef = FirebaseDatabase.getInstance().getReference();
        mChatPhotosStorageReference = FirebaseStorage.getInstance().getReferenceFromUrl("gs://rendezvous-a14ef.appspot.com/chat_images");
        if (mTitleTv != null) {
            mTitleTv.setText(mChatUserName);
        }
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        messagesList.setHasFixedSize(true);
        messagesList.setLayoutManager(linearLayoutManager);
        msgAdapter = new MessageAdapter(mMsgList);
        messagesList.setAdapter(msgAdapter);
        mRootRef.child("Users").child(mChatUser).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String online = dataSnapshot.child("online").getValue().toString();
                final String image = dataSnapshot.child("image").getValue().toString();
                if (image != null && !TextUtils.isEmpty(image)) {
                    PicassoCache.get(ChatActivity.this)
                            .load(image)
                            .networkPolicy(NetworkPolicy.OFFLINE)
                            .placeholder(R.drawable.ic_place_holder)
                            .resizeDimen(R.dimen.user_avatar_size, R.dimen.user_avatar_size)
                            .centerCrop()
                            .transform(new CircularTransform())
                            .into(mProfileImg, new Callback() {
                                @Override
                                public void onSuccess() {

                                }

                                @Override
                                public void onError() {
                                    Picasso.with(ChatActivity.this)
                                            .load(image)
                                            .placeholder(R.drawable.ic_place_holder)
                                            .resizeDimen(R.dimen.user_avatar_size, R.dimen.user_avatar_size)
                                            .centerCrop()
                                            .transform(new CircularTransform())
                                            .into(mProfileImg);
                                }
                            });
                }
                if (online.equals("true"))
                    mLastSeenTv.setText(getString(R.string.online_now));
                else {
                    long lastTime = Long.parseLong(online);
                    String lastSeen = GetTimeAgo.getTimeAgo(lastTime);
                    mLastSeenTv.setText(lastSeen);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        mCurrentUserId = mAuth.getCurrentUser().getUid();
        mRootRef.child("messages").child(mCurrentUserId).child(mChatUser).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Message msg = dataSnapshot.getValue(Message.class);
                mMsgList.add(msg);
                msgAdapter.notifyDataSetChanged();
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        mRootRef.child("Chat").child(mCurrentUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.hasChild(mChatUser)) {
                    Map chatAddMap = new HashMap<>();
                    chatAddMap.put("seen", false);
                    chatAddMap.put("time_stamp", ServerValue.TIMESTAMP);
                    Map chatUserMap = new HashMap<>();
                    chatUserMap.put("Chat/" + mCurrentUserId + "/" + mChatUser, chatAddMap);
                    chatUserMap.put("Chat/" + mChatUser + "/" + mCurrentUserId, chatAddMap);
                    mRootRef.updateChildren(chatUserMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                            if (databaseError != null) {
                                Log.d(TAG, "onComplete Error: " + databaseError.getMessage());
                            }
                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    @OnClick(R.id.send_msg_btn)
    void sendMessage() {
        String msg = chatMsg.getText().toString();
        if (!TextUtils.isEmpty(msg)) {
            chatMsg.setText("");
            String currentUserRef = "messages/" + mCurrentUserId + "/" + mChatUser;
            String chatUserRef = "messages/" + mChatUser + "/" + mCurrentUserId;
            DatabaseReference userMsgPush = mRootRef.child("messages")
                    .child(mCurrentUserId).child(mChatUser).push();
            String pushId = userMsgPush.getKey();
            Map msgMap = new HashMap<>();
            msgMap.put("message", msg);
            msgMap.put("seen", false);
            msgMap.put("type", "text");
            msgMap.put("time_stamp", ServerValue.TIMESTAMP);
            msgMap.put("from", mCurrentUserId);
            Map msgUserMap = new HashMap<>();
            msgUserMap.put(currentUserRef + "/" + pushId, msgMap);
            msgUserMap.put(chatUserRef + "/" + pushId, msgMap);
            mRootRef.updateChildren(msgUserMap, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                    if (databaseError != null)
                        Log.d(TAG, "onComplete Error: " + databaseError.getMessage());
                }
            });

        }
    }

    @OnClick(R.id.add_img_btn)
    void sendImg() {
        Intent getIntent = new Intent(Intent.ACTION_GET_CONTENT);
        getIntent.setType("image/*");
        startActivityForResult(getIntent, PICK_IMAGE);
    }

    private void showLoading() {
        mProgressDialog = new ProgressDialog(ChatActivity.this);
        mProgressDialog.setTitle("Uploading Image");
        mProgressDialog.setMessage("Loading Please wait!");
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE)
            if (resultCode == RESULT_OK) {
                showLoading();
                Uri selectedImageUri = data.getData();
                StorageReference photoRef = mChatPhotosStorageReference.child("chat_images").
                        child("chat_" + selectedImageUri.getLastPathSegment() + ".jpg");
                photoRef.putFile(selectedImageUri)
                        .addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                String downloadUrl = taskSnapshot.getDownloadUrl().toString();
                                Log.d(TAG, "onSuccess: " + downloadUrl);
                                String currentUserRef = "messages/" + mCurrentUserId + "/" + mChatUser;
                                String chatUserRef = "messages/" + mChatUser + "/" + mCurrentUserId;
                                DatabaseReference userMsgPush = mRootRef.child("messages")
                                        .child(mCurrentUserId).child(mChatUser).push();
                                String pushId = userMsgPush.getKey();
                                Map msgMap = new HashMap<>();
                                msgMap.put("message", downloadUrl);
                                msgMap.put("seen", false);
                                msgMap.put("type", "image");
                                msgMap.put("time_stamp", ServerValue.TIMESTAMP);
                                msgMap.put("from", mCurrentUserId);
                                Map msgUserMap = new HashMap<>();
                                msgUserMap.put(currentUserRef + "/" + pushId, msgMap);
                                msgUserMap.put(chatUserRef + "/" + pushId, msgMap);
                                mRootRef.updateChildren(msgUserMap, new DatabaseReference.CompletionListener() {
                                    @Override
                                    public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                        mProgressDialog.dismiss();
                                        if (databaseError != null)
                                            Log.d(TAG, "onComplete Error: " + databaseError.getMessage());
                                    }
                                });
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        mProgressDialog.dismiss();
                        Toast.makeText(ChatActivity.this, e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
    }

}
