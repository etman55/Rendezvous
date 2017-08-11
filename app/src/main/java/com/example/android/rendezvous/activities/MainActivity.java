package com.example.android.rendezvous.activities;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.android.rendezvous.R;
import com.example.android.rendezvous.adapters.ViewPagerAdapter;
import com.example.android.rendezvous.fragments.FindPeopleFragment;
import com.example.android.rendezvous.fragments.FriendsFragment;
import com.example.android.rendezvous.utils.FontCache;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import butterknife.Bind;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    @Bind(R.id.main_toolbar)
    Toolbar mToolbar;
    @Bind(R.id.tab_pager)
    ViewPager mViewPager;
    @Bind(R.id.main_tabs)
    TabLayout mTabLayout;
    @Bind(R.id.content_layout)
    RelativeLayout contentLayout;
    @Bind(R.id.tool_bar_txt)
    TextView toolbarTxt;
    private DatabaseReference mUserDatabase;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        setSupportActionBar(mToolbar);
        toolbarTxt.setText(getString(R.string.app_name));
        Typeface tf = FontCache.get(getString(R.string.negra_bold),this);
        toolbarTxt.setTypeface(tf);
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser mUser = mAuth.getCurrentUser();
        mUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(mUser.getUid());
        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    setupViewPager(mViewPager);
                    mTabLayout.setupWithViewPager(mViewPager);
                    mUserDatabase.child("online").setValue("true");
                } else {
                    mUserDatabase.child("online").setValue(ServerValue.TIMESTAMP);
                    FirebaseAuth.getInstance().signOut();
                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                }
            }
        };
    }

    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(new FindPeopleFragment(), "Find People");
        adapter.addFragment(new FriendsFragment(), "Fiends");
        viewPager.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mAuth.addAuthStateListener(mAuthStateListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mAuthStateListener != null) {
            mAuth.removeAuthStateListener(mAuthStateListener);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        if (item.getItemId() == R.id.main_logout_btn) {
            FirebaseAuth.getInstance().signOut();
            if (mAuth.getCurrentUser() == null)
                mUserDatabase.child("online").setValue(ServerValue.TIMESTAMP);
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
        }
        if (item.getItemId() == R.id.main_settings_btn)
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        if (item.getItemId() == R.id.main_edit_profile_btn) {
            startActivity(new Intent(MainActivity.this, ProfileSettingsActivity.class));
        }
        return true;
    }

}
