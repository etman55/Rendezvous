package com.example.android.rendezvous.fragments;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.android.rendezvous.R;
import com.example.android.rendezvous.activities.ChatActivity;
import com.example.android.rendezvous.activities.ProfileActivity;
import com.example.android.rendezvous.models.Friends;
import com.example.android.rendezvous.utils.CircularTransform;
import com.example.android.rendezvous.utils.FontCache;
import com.example.android.rendezvous.utils.PicassoCache;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by Etman on 8/5/2017.
 */

public class FriendsFragment extends Fragment {
    @Bind(R.id.friends_list)
    RecyclerView friendsList;
    @Bind(R.id.no_friends_txt)
    TextView noFriendsTxt;
    private DatabaseReference mFriendsDatabase;
    private DatabaseReference mUserDatabase;
    private FirebaseAuth mAuth;
    private String mCurrentUserId;
    private FirebaseRecyclerAdapter<Friends, FriendsViewHolder> friendsAdapter;

    public FriendsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_friends, container, false);
        ButterKnife.bind(this, view);
        mAuth = FirebaseAuth.getInstance();
        mCurrentUserId = mAuth.getCurrentUser().getUid();
        mFriendsDatabase = FirebaseDatabase.getInstance().getReference()
                .child("Friends").child(mCurrentUserId);
        mFriendsDatabase.keepSynced(true);
        mUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users");
        mUserDatabase.keepSynced(true);
        friendsList.setHasFixedSize(true);
        friendsList.setLayoutManager(new LinearLayoutManager(getContext()));
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        friendsAdapter =
                new FirebaseRecyclerAdapter<Friends, FriendsViewHolder>(
                        Friends.class,
                        R.layout.item_user,
                        FriendsViewHolder.class,
                        mFriendsDatabase) {
                    @Override
                    protected void populateViewHolder(final FriendsViewHolder viewHolder, final Friends model, int position) {
                        if (model != null)
                            noFriendsTxt.setVisibility(View.INVISIBLE);
                        viewHolder.setmAbout(model.getDate());
                        final String listUserId = getRef(position).getKey();
                        mUserDatabase.child(listUserId).addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(final DataSnapshot dataSnapshot) {
                                final String userName = dataSnapshot.child("name").getValue().toString();
                                String online = dataSnapshot.child("online").getValue().toString();
                                String thumb = dataSnapshot.child("thumb_image").getValue().toString();
                                viewHolder.setName(userName);
                                if (thumb != null && !TextUtils.isEmpty(thumb)) {
                                    viewHolder.setUserAvatar(thumb,
                                            getContext());
                                }
                                viewHolder.setOnlineStatus(online, getContext());
                                viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        CharSequence options[] = new CharSequence[]{"Open Profile", "Send a message"};
                                        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                                        builder.setTitle("Select Option");
                                        builder.setItems(options, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                switch (which) {
                                                    case 0:
                                                        Intent profileIntent = new Intent(getContext(), ProfileActivity.class);
                                                        profileIntent.putExtra("user_id", listUserId);
                                                        startActivity(profileIntent);
                                                        break;
                                                    case 1:
                                                        Intent chatIntent = new Intent(getContext(), ChatActivity.class);
                                                        chatIntent.putExtra("user_id", listUserId);
                                                        chatIntent.putExtra("user_name", userName);
                                                        chatIntent.putExtra("user_thumb", dataSnapshot.child("thumb_image").toString());
                                                        startActivity(chatIntent);
                                                        break;
                                                }
                                            }
                                        });
                                        builder.show();
                                    }
                                });

                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
                    }
                };
        friendsList.setAdapter(friendsAdapter);

    }

    @Override
    public void onResume() {
        super.onResume();
        if (friendsAdapter.getItemCount() == 0)
            noFriendsTxt.setVisibility(View.VISIBLE);
        else
            noFriendsTxt.setVisibility(View.INVISIBLE);
    }

    public static class FriendsViewHolder extends RecyclerView.ViewHolder {
        @Bind(R.id.user_name_txt)
        TextView displayName;
        @Bind(R.id.user_about_txt)
        TextView user_about;
        @Bind(R.id.user_avatar)
        ImageView avatar;
        @Bind(R.id.user_status_img)
        ImageView onlineStatus;
        Typeface tf;
        Typeface tf1;
        public FriendsViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            tf = FontCache.get("fonts/Aller_Rg.ttf", itemView.getContext());
            tf1 = FontCache.get("fonts/Aller_Lt.ttf", itemView.getContext());
            displayName.setTypeface(tf);
            user_about.setTypeface(tf1);
        }

        public void setName(String name) {
            displayName.setText(name);
        }

        public void setmAbout(String about) {
            user_about.setText(about);
        }

        public void setUserAvatar(final String thumbUrl, final Context context) {
            PicassoCache.get(context)
                    .load(thumbUrl)
                    .networkPolicy(NetworkPolicy.OFFLINE)
                    .placeholder(R.drawable.ic_place_holder)
                    .resizeDimen(R.dimen.user_avatar_size, R.dimen.user_avatar_size)
                    .centerCrop()
                    .transform(new CircularTransform())
                    .into(avatar, new Callback() {
                        @Override
                        public void onSuccess() {

                        }

                        @Override
                        public void onError() {
                            Picasso.with(context)
                                    .load(thumbUrl)
                                    .placeholder(R.drawable.ic_place_holder)
                                    .resizeDimen(R.dimen.user_avatar_size, R.dimen.user_avatar_size)
                                    .centerCrop()
                                    .transform(new CircularTransform())
                                    .into(avatar);
                        }
                    });
        }

        public void setOnlineStatus(String status, Context context) {
            if (status.equals("true"))
                onlineStatus.setVisibility(View.VISIBLE);
            else
                onlineStatus.setVisibility(View.INVISIBLE);
        }
    }
}
