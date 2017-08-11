package com.example.android.rendezvous.activities;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.example.android.rendezvous.R;
import com.example.android.rendezvous.utils.FontCache;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class AboutActivity extends AppCompatActivity {
    @Bind(R.id.about_toolbar)
    Toolbar mToolbar;
    @Bind(R.id.content_layout)
    RelativeLayout contentLayout;
    @Bind(R.id.save_about_btn)
    Button mSaveAboutBtn;
    @Bind(R.id.about_input)
    TextInputLayout mAboutInput;
    @Bind(R.id.username_input)
    TextInputLayout mUsernameInput;
    @Bind(R.id.save_username_btn)
    Button mUsernameBtn;
    @Bind(R.id.tool_bar_txt)
    TextView toolbarTxt;
    private DatabaseReference mUserDatabase;
    private MaterialDialog dialog;
    private String lastAbout;
    private String lastUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        ButterKnife.bind(this);
        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        Typeface tf = FontCache.get(getString(R.string.aller_bold), this);
        toolbarTxt.setTypeface(tf);
        toolbarTxt.setText(getString(R.string.account_info));
        Typeface tf1 = FontCache.get(getString(R.string.aller_light), this);
        mAboutInput.setTypeface(tf1);
        mUsernameInput.setTypeface(tf1);
        mSaveAboutBtn.setTypeface(tf1);
        mUsernameBtn.setTypeface(tf1);
        if (savedInstanceState == null) {
            lastAbout = getIntent().getStringExtra("about");
            lastUsername = getIntent().getStringExtra("username");
        } else {
            lastAbout = savedInstanceState.getString("about");
            lastUsername = savedInstanceState.getString("username");
        }
        mAboutInput.getEditText().setText(lastAbout);
        mUsernameInput.getEditText().setText(lastUsername);
        FirebaseUser mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
        String uId = mCurrentUser.getUid();
        mUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(uId);
    }

    @OnClick(R.id.save_about_btn)
    void saveAbout() {
        final String about = mAboutInput.getEditText().getText().toString().trim();
        if (TextUtils.isEmpty(about)) {
            mAboutInput.setError(getString(R.string.input_empty_error));
            return;
        }
        showLoading();
        mUserDatabase.child("name").setValue(about).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                dialog.dismiss();
                Snackbar.make(contentLayout, "Your new about: " + about, Snackbar.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                dialog.dismiss();
                Snackbar.make(contentLayout, e.getMessage(), Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    @OnClick(R.id.save_username_btn)
    void saveUsername() {
        final String username = mUsernameInput.getEditText().getText().toString().trim();
        if (TextUtils.isEmpty(username)) {
            mAboutInput.setError(getString(R.string.input_empty_error));
            return;
        }
        showLoading();
        mUserDatabase.child("name").setValue(username).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                dialog.dismiss();
                Snackbar.make(contentLayout, "Your new username: " + username, Snackbar.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                dialog.dismiss();
                Snackbar.make(contentLayout, e.getMessage(), Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void showLoading() {
        if (dialog != null && dialog.isShowing())
            dialog.dismiss();
        dialog = new MaterialDialog.Builder(this)
                .title("Saving")
                .content(R.string.please_wait)
                .progress(true, 0)
                .cancelable(false)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dialog != null && dialog.isShowing())
            dialog.dismiss();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("about", lastAbout);
        outState.putString("username", lastUsername);
    }
}
