package com.example.android.rendezvous.widget;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.example.android.rendezvous.R;
import com.example.android.rendezvous.models.User;
import com.example.android.rendezvous.utils.PicassoCache;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Etman on 8/6/2017.
 */

public class WidgetRemoteService extends RemoteViewsService {
    private static final String TAG = WidgetRemoteService.class.getSimpleName();

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new ListViewFactory(this.getApplicationContext());
    }

    class ListViewFactory implements RemoteViewsService.RemoteViewsFactory {
        private final String TAG = WidgetRemoteService.class.getSimpleName();
        private final Context context;
        private final ArrayList<User> friendsList = new ArrayList<>();
        private final List<String> idsList = new ArrayList<>();
        private final AppWidgetManager appWidgetManager;
        private final int[] appWidgetId;


        public ListViewFactory(Context context) {
            this.context = context;
            appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName component = new ComponentName(context, RendezvousWidget.class);
            appWidgetId = appWidgetManager.getAppWidgetIds(component);
        }

        @Override
        public void onCreate() {
            fetchData();
        }

        private void fetchData() {
            FirebaseAuth mAuth = FirebaseAuth.getInstance();
            FirebaseUser mFirebaseUser = mAuth.getCurrentUser();
            final RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.rendezvous_widget);
            if (mFirebaseUser != null) {
                DatabaseReference mFriendsDatabase = FirebaseDatabase.getInstance().getReference()
                        .child("Friends").child(mFirebaseUser.getUid());
                final DatabaseReference mUsersDatabase = FirebaseDatabase.getInstance().getReference()
                        .child("Users");
                mFriendsDatabase.addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                        if (dataSnapshot != null) {
                            String uId = dataSnapshot.getKey();
                            idsList.add(uId);
                            mUsersDatabase.child(uId).addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    User user = new User();
                                    user.setName(dataSnapshot.child("name").getValue().toString());
                                    user.setThumb_image(dataSnapshot.child("thumb_image").getValue().toString());
                                    user.setAbout(dataSnapshot.child("about").getValue().toString());
                                    user.setOnline(dataSnapshot.child("online").getValue().toString());
                                    friendsList.add(user);
                                    Log.d(TAG, "onDataChange: " + friendsList.size());
                                    appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_friends_list);
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {
                                    views.setViewVisibility(R.id.empty_txt, View.VISIBLE);
                                    views.setTextViewText(R.id.empty_txt, databaseError.getMessage());
                                    appWidgetManager.updateAppWidget(appWidgetId, views);
                                }
                            });
                        }

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
            } else {
                views.setViewVisibility(R.id.empty_txt, View.VISIBLE);
                views.setTextViewText(R.id.empty_txt, getString(R.string.no_friends));
                appWidgetManager.updateAppWidget(appWidgetId, views);
            }
        }

        @Override
        public void onDataSetChanged() {

        }

        @Override
        public void onDestroy() {
            friendsList.clear();
            idsList.clear();
        }

        @Override
        public int getCount() {
            return friendsList.size();
        }

        @Override
        public RemoteViews getViewAt(int position) {
            RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.item_widget);
            if (friendsList.size() != 0) {
                User friend = friendsList.get(position);
                String id = idsList.get(position);
                rv.setTextViewText(R.id.user_name_txt, friend.getName());
                rv.setTextViewText(R.id.user_about_txt, friend.getAbout());
                if (friend.getThumb_image() != null && !friend.getThumb_image().trim().isEmpty()) {
                    try {
                        Bitmap bitmap = PicassoCache.get(context).load(Uri.parse(friend.getThumb_image())).get();
                        rv.setImageViewBitmap(R.id.user_avatar, bitmap);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.d(TAG, "getViewAt: " + e.getMessage());
                    }
                }
                Bundle extras = new Bundle();
                extras.putString("user_id", id);
                extras.putString("user_name", friend.getName());
                Intent fillInIntent = new Intent();
                fillInIntent.putExtras(extras);
                rv.setOnClickFillInIntent(R.id.user_name_txt, fillInIntent);
            }else{
                rv.setViewVisibility(R.id.empty_txt, View.VISIBLE);
                rv.setTextViewText(R.id.empty_txt, getString(R.string.no_friends));
            }
            return rv;
        }

        @Override
        public RemoteViews getLoadingView() {
            return null;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }
    }
}
