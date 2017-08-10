package com.example.android.rendezvous.activities;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.example.android.rendezvous.R;
import com.example.android.rendezvous.utils.FontCache;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.HashMap;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class LoginActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = LoginActivity.class.getSimpleName();
    private static final int RC_SIGN_IN = 9001;
    @Bind(R.id.sign_up_btn)
    TextView register;
    @Bind(R.id.reset_password)
    TextView resetPassword;
    @Bind(R.id.email)
    EditText inputEmail;
    @Bind(R.id.password)
    EditText inputPassword;
    @Bind(R.id.login_btn)
    Button loginBtn;
    @Bind(R.id.google_btn)
    ImageView googleBtn;
    @Bind(R.id.facebook_btn)
    ImageView facebookBtn;
    @Bind(R.id.facebook_builtin_btn)
    LoginButton loginButton;
    @Bind(R.id.content_layout)
    RelativeLayout contentLayout;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser user;
    private DatabaseReference userDatabase;
    private GoogleApiClient mGoogleApiClient;
    private String deviceToken;
    private GoogleSignInOptions gso;
    private CallbackManager callbackManager;
    private MaterialDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);
        firebaseAuth = FirebaseAuth.getInstance();
        userDatabase = FirebaseDatabase.getInstance().getReference();
        Typeface tf = FontCache.get("fonts/Aller_Rg.ttf",this);
        inputEmail.setTypeface(tf);
        inputPassword.setTypeface(tf);
        resetPassword.setTypeface(tf);
        register.setTypeface(tf);
        loginBtn.setTypeface(tf);
        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleApiClient = new GoogleApiClient.Builder(LoginActivity.this)
                .enableAutoManage(LoginActivity.this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();
        firebaseAuthWithFacebook();

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()) {
                GoogleSignInAccount account = result.getSignInAccount();
                firebaseAuthWithGoogle(account);
            } else
                Snackbar.make(contentLayout, "Authentication Failed!", Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.stopAutoManage(this);
            mGoogleApiClient.disconnect();
        }
        if (dialog != null && dialog.isShowing())
            dialog.dismiss();
        if (callbackManager != null)
            callbackManager = null;

    }

    @OnClick(R.id.sign_up_btn)
    public void signUp() {
        startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
    }

    @OnClick(R.id.reset_password)
    void resetPassword() {
        new MaterialDialog.Builder(LoginActivity.this)
                .title(R.string.forget_password_dialog)
                .inputType(InputType.TYPE_CLASS_TEXT)
                .input(R.string.email_forget_password_hint, R.string.prefill, new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(@NonNull final MaterialDialog dialog, CharSequence input) {
                        firebaseAuth.sendPasswordResetEmail(input.toString())
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        dialog.dismiss();
                                        Snackbar.make(contentLayout, "Please check your e-mail !", Snackbar.LENGTH_SHORT).show();
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                dialog.dismiss();
                                Snackbar.make(contentLayout, e.getMessage(), Snackbar.LENGTH_SHORT).show();
                            }
                        });
                    }
                }).show();
    }

    @OnClick(R.id.login_btn)
    public void login() {
        String email = inputEmail.getText().toString().trim();
        String password = inputPassword.getText().toString().trim();
        if (TextUtils.isEmpty(password)) {
            inputEmail.setError(getString(R.string.input_empty_error));
            return;
        }
        if (TextUtils.isEmpty(password)) {
            inputPassword.setError(getString(R.string.input_empty_error));
            return;
        }
        if (password.length() < 8) {
            inputPassword.setError(getString(R.string.input_short_pass_error));
            return;
        }
        showLoading();
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        dialog.dismiss();
                        user = firebaseAuth.getCurrentUser();
                        deviceToken = FirebaseInstanceId.getInstance().getToken();
                        if (user != null) {
                            userDatabase
                                    .child("Users")
                                    .child(user.getUid())
                                    .child("device_token")
                                    .setValue(deviceToken)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            Intent i = new Intent(LoginActivity.this, MainActivity.class);
                                            i.setFlags(i.getFlags() | Intent.FLAG_ACTIVITY_NO_HISTORY);
                                            startActivity(i);
                                            finish();
                                        }
                                    });
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                dialog.dismiss();
                Snackbar.make(contentLayout, e.getMessage(),
                        Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    @OnClick(R.id.google_btn)
    void signInWithGoogle() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @OnClick(R.id.facebook_btn)
    void signInWithFacebookClicked() {
        loginButton.performClick();
    }

    private void firebaseAuthWithFacebook() {
        callbackManager = CallbackManager.Factory.create();
        loginButton.setReadPermissions("email", "public_profile");
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                AuthCredential credential = FacebookAuthProvider
                        .getCredential(loginResult.getAccessToken().getToken());
                signInWithCredential(credential);
            }

            @Override
            public void onCancel() {
                Log.d(TAG, "facebook:onCancel");
            }

            @Override
            public void onError(FacebookException error) {
                Log.d(TAG, "facebook:onError", error);
            }
        });
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        signInWithCredential(credential);
    }

    private void signInWithCredential(AuthCredential credential) {
        showLoading();
        firebaseAuth.signInWithCredential(credential)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        deviceToken = FirebaseInstanceId.getInstance().getToken();
                        user = firebaseAuth.getCurrentUser();

                        if (user != null)
                            userDatabase.child("Users").child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.getValue() != null) {
                                        userDatabase
                                                .child("Users")
                                                .child(user.getUid())
                                                .child("device_token")
                                                .setValue(deviceToken).addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                dialog.dismiss();
                                                Intent i = new Intent(LoginActivity.this, MainActivity.class);
                                                i.setFlags(i.getFlags() | Intent.FLAG_ACTIVITY_NO_HISTORY);
                                                startActivity(i);
                                                finish();
                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                dialog.dismiss();
                                                Snackbar.make(contentLayout, e.getMessage(), Snackbar.LENGTH_SHORT).show();
                                            }
                                        });
                                    } else {
                                        String username = user.getDisplayName();
                                        HashMap<String, String> userMap = new HashMap<>();
                                        userMap.put("name", username);
                                        userMap.put("about", "default");
                                        userMap.put("image", "");
                                        userMap.put("online", "true");
                                        userMap.put("thumb_image", "");
                                        userMap.put("device_token", deviceToken);
                                        userDatabase
                                                .child("Users")
                                                .child(user.getUid())
                                                .setValue(userMap).addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                dialog.dismiss();
                                                Intent i = new Intent(LoginActivity.this, MainActivity.class);
                                                i.setFlags(i.getFlags() | Intent.FLAG_ACTIVITY_NO_HISTORY);
                                                startActivity(i);
                                                finish();
                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                dialog.dismiss();
                                                Snackbar.make(contentLayout, e.getMessage(), Snackbar.LENGTH_SHORT).show();
                                            }
                                        });
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {
                                    dialog.dismiss();
                                    Snackbar.make(contentLayout, databaseError.getMessage(), Snackbar.LENGTH_SHORT).show();
                                }
                            });
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                dialog.dismiss();
                Snackbar.make(contentLayout, "Authentication failed.",
                        Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Snackbar.make(contentLayout, "Connection failed.",
                Snackbar.LENGTH_SHORT).show();
        if (dialog != null && dialog.isShowing())
            dialog.dismiss();

    }

    private void showLoading() {
        if (dialog != null && dialog.isShowing())
            dialog.dismiss();
        dialog = new MaterialDialog.Builder(this)
                .title("Login")
                .content(R.string.please_wait)
                .progress(true, 0)
                .cancelable(false)
                .show();
    }


}
