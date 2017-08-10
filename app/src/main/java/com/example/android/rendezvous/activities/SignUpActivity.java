package com.example.android.rendezvous.activities;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.example.android.rendezvous.R;
import com.example.android.rendezvous.models.User;
import com.example.android.rendezvous.utils.FontCache;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SignUpActivity extends AppCompatActivity {
    private static final String TAG = SignUpActivity.class.getSimpleName();
    @Bind(R.id.sign_up_tool_bar)
    Toolbar toolbar;
    @Bind(R.id.content_layout)
    RelativeLayout contentLayout;
    @Bind(R.id.username)
    EditText inputUsername;
    @Bind(R.id.email)
    EditText inputEmail;
    @Bind(R.id.password)
    EditText inputPassword;
    @Bind(R.id.repeat_password)
    EditText inputRepeatPassword;
    @Bind(R.id.tool_bar_txt)
    TextView toolbarTxt;
    @Bind(R.id.textView)
    TextView textView;
    @Bind(R.id.sign_up_button)
    Button sinUpBtn;
    @Bind(R.id.sign_in_button)
    TextView signIn;
    private FirebaseAuth firebaseAuth;
    private String deviceToken;
    private DatabaseReference mDatabase;
    private MaterialDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        Typeface tf = FontCache.get("fonts/Aller_Bd.ttf", this);
        toolbarTxt.setTypeface(tf);
        Typeface tf2 = FontCache.get("fonts/Aller_Rg.ttf", this);
        textView.setTypeface(tf2);
        Typeface tf1 = FontCache.get("fonts/Aller_Rg.ttf", this);
        toolbarTxt.setText("Sign up");
        inputUsername.setTypeface(tf1);
        inputEmail.setTypeface(tf1);
        inputPassword.setTypeface(tf1);
        inputRepeatPassword.setTypeface(tf1);
        sinUpBtn.setTypeface(tf1);
        signIn.setTypeface(tf1);
        deviceToken = FirebaseInstanceId.getInstance().getToken();
        firebaseAuth = FirebaseAuth.getInstance();
    }

    @OnClick(R.id.sign_in_button)
    void signInBtn() {
        startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
    }


    @OnClick(R.id.sign_up_button)
    void validateSignUp() {
        final String username = inputUsername.getText().toString().trim();
        final String email = inputEmail.getText().toString().trim();
        final String password = inputPassword.getText().toString().trim();
        String repeatedPassword = inputRepeatPassword.getText().toString().trim();

        if (TextUtils.isEmpty(username)) {
            inputUsername.setError(getString(R.string.input_empty_error));
            return;
        }
        if (TextUtils.isEmpty(email)) {
            inputEmail.setError(getString(R.string.input_empty_error));
            return;
        }
        if (TextUtils.isEmpty(password)) {
            inputPassword.setError(getString(R.string.input_empty_error));
            return;
        }
        if (TextUtils.isEmpty(repeatedPassword)) {
            inputRepeatPassword.setError(getString(R.string.input_empty_error));
            return;
        }
        if (password.length() < 8) {
            inputPassword.setError(getString(R.string.input_short_pass_error));
            return;
        }
        if (!repeatedPassword.equals(password)) {
            inputRepeatPassword.setError(getString(R.string.input_pass_match_error));
            return;
        }
        showLoading();
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        dialog.dismiss();
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        if (user != null) {
                            String uId = user.getUid();
                            deviceToken = FirebaseInstanceId.getInstance().getToken();
                            mDatabase = FirebaseDatabase.getInstance().getReference().
                                    child("Users").child(uId);
                            User userModel = new User(deviceToken, username,
                                    "", "true", "default", "");
                            mDatabase.setValue(userModel).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    startActivity(new Intent(SignUpActivity.this, MainActivity.class));
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    dialog.dismiss();
                                    Log.d(TAG, "onFailure: " + e.getMessage());
                                    Snackbar.make(contentLayout, e.getMessage(), Snackbar.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                dialog.dismiss();
                Log.d(TAG, "onFailure: " + e.getMessage());
                Snackbar.make(contentLayout, e.getMessage(), Snackbar.LENGTH_SHORT).show();
            }
        });
    }


    private void showLoading() {
        if (dialog != null && dialog.isShowing())
            dialog.dismiss();
        dialog = new MaterialDialog.Builder(this)
                .title("Create new account")
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

}