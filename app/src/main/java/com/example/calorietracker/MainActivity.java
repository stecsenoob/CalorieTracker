package com.example.calorietracker;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class MainActivity extends AppCompatActivity {

    private static final int RC_GOOGLE_SIGN_IN = 1001;

    private TextInputEditText edtEmailLog, edtPassLog;
    private TextInputLayout tilEmail, tilPassword;
    private MaterialButton btnLogin, btnGoogleSignIn;
    private TextView btnRegister, tvForgotPassword;

    private FirebaseAuth mAuth;
    private GoogleSignInClient googleSignInClient;

    private dbConnect db;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        db = new dbConnect(MainActivity.this);
        session = new SessionManager(MainActivity.this);

        setupGoogleSignIn();

        edtEmailLog = findViewById(R.id.email);
        edtPassLog = findViewById(R.id.password);

        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);

        btnLogin = findViewById(R.id.loginbtn);
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);

        btnRegister = findViewById(R.id.registerbtn);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        btnLogin.setOnClickListener(v -> loginWithFirebase());

        btnGoogleSignIn.setOnClickListener(v -> signInWithGoogle());

        btnRegister.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            saveSessionAndOpenHome(currentUser);
        }
    }

    private void setupGoogleSignIn() {
        GoogleSignInOptions googleSignInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(getString(R.string.default_web_client_id))
                        .requestEmail()
                        .build();

        googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions);
    }

    private void signInWithGoogle() {
        clearErrors();

        btnGoogleSignIn.setEnabled(false);
        btnGoogleSignIn.setText("Opening Google...");

        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_GOOGLE_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);

            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);

                if (account == null) {
                    resetGoogleButton();
                    showSuccessSnackbar("Google sign in cancelled");
                    return;
                }

                firebaseAuthWithGoogle(account);

            } catch (ApiException e) {
                resetGoogleButton();

                String message = "Google sign in failed";

                if (e.getStatusCode() == 10) {
                    message = "Google sign in setup error. Check SHA-1 and google-services.json.";
                }

                showErrorSnackbar(message);
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);

        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    resetGoogleButton();

                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();

                        if (firebaseUser == null) {
                            showErrorSnackbar("Google login failed. Try again.");
                            return;
                        }

                        showSuccessSnackbar("Google login successful!");

                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            saveSessionAndOpenHome(firebaseUser);
                        }, 900);

                    } else {
                        String error = "Google login failed";

                        if (task.getException() != null && task.getException().getMessage() != null) {
                            error = task.getException().getMessage();
                        }

                        showErrorSnackbar(error);
                    }
                });
    }

    private void resetGoogleButton() {
        btnGoogleSignIn.setEnabled(true);
        btnGoogleSignIn.setText("Continue with Google");
    }

    private void loginWithFirebase() {
        clearErrors();

        String email = edtEmailLog.getText() == null
                ? ""
                : edtEmailLog.getText().toString().trim();

        String pass = edtPassLog.getText() == null
                ? ""
                : edtPassLog.getText().toString().trim();

        if (email.isEmpty()) {
            tilEmail.setError("Email is required");
            edtEmailLog.requestFocus();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Enter a valid email address");
            edtEmailLog.requestFocus();
            return;
        }

        if (pass.isEmpty()) {
            showPasswordError("Password is required");
            edtPassLog.requestFocus();
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("Logging in...");

        mAuth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener(task -> {
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Log in");

                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();

                        if (firebaseUser == null) {
                            showPasswordError("Login failed. Try again.");
                            return;
                        }

                        showSuccessSnackbar("Login successful!");

                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            saveSessionAndOpenHome(firebaseUser);
                        }, 1000);

                    } else {
                        String error = "Invalid email or password";

                        if (task.getException() != null && task.getException().getMessage() != null) {
                            error = task.getException().getMessage();
                        }

                        showPasswordError(error);
                    }
                });
    }

    private void saveSessionAndOpenHome(FirebaseUser firebaseUser) {
        String firebaseUid = firebaseUser.getUid();

        String email = firebaseUser.getEmail();

        if (email == null || email.trim().isEmpty()) {
            email = firebaseUid + "@googleuser.local";
        }

        int localUserId = db.getOrCreateLocalUserId(email);
        session.saveFirebaseUser(localUserId, email, firebaseUid);

        Intent intent = new Intent(MainActivity.this, HomeActivity.class);
        startActivity(intent);
        finish();
    }

    private void showForgotPasswordDialog() {
        clearErrors();

        LinearLayout container = new LinearLayout(MainActivity.this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(48, 16, 48, 0);

        TextInputLayout tilResetEmail = new TextInputLayout(MainActivity.this);
        tilResetEmail.setHint("Email");
        tilResetEmail.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        tilResetEmail.setBoxCornerRadii(14, 14, 14, 14);
        tilResetEmail.setErrorEnabled(true);
        tilResetEmail.setErrorIconDrawable(null);

        TextInputEditText edtResetEmail = new TextInputEditText(MainActivity.this);
        edtResetEmail.setSingleLine(true);
        edtResetEmail.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        edtResetEmail.setTextColor(Color.parseColor("#111827"));
        edtResetEmail.setHintTextColor(Color.parseColor("#6B7280"));

        tilResetEmail.addView(edtResetEmail);
        container.addView(tilResetEmail);

        String currentEmail = edtEmailLog.getText() == null
                ? ""
                : edtEmailLog.getText().toString().trim();

        if (!currentEmail.isEmpty()) {
            edtResetEmail.setText(currentEmail);
            edtResetEmail.setSelection(currentEmail.length());
        }

        AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
                .setTitle("Reset password")
                .setMessage("Enter your email address and we will send you a password reset link.")
                .setView(container)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Send", null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#6F50B5"));
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#6F50B5"));

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String email = edtResetEmail.getText() == null
                        ? ""
                        : edtResetEmail.getText().toString().trim();

                tilResetEmail.setError(null);

                if (email.isEmpty()) {
                    tilResetEmail.setError("Email is required");
                    edtResetEmail.requestFocus();
                    return;
                }

                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    tilResetEmail.setError("Enter a valid email address");
                    edtResetEmail.requestFocus();
                    return;
                }

                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setText("Sending...");

                mAuth.sendPasswordResetEmail(email)
                        .addOnCompleteListener(task -> {
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setText("Send");

                            if (task.isSuccessful()) {
                                dialog.dismiss();
                                showSuccessSnackbar("Password reset email sent!");
                            } else {
                                String error = "Failed to send reset email";

                                if (task.getException() != null && task.getException().getMessage() != null) {
                                    error = task.getException().getMessage();
                                }

                                tilResetEmail.setError(error);
                                edtResetEmail.requestFocus();
                            }
                        });
            });
        });

        dialog.show();
    }

    private void clearErrors() {
        tilEmail.setError(null);
        tilPassword.setError(null);
        tilPassword.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
    }

    private void showPasswordError(String msg) {
        tilPassword.setError(msg);
        tilPassword.setEndIconMode(TextInputLayout.END_ICON_NONE);
    }

    private void showSuccessSnackbar(String message) {
        View rootView = findViewById(android.R.id.content);

        Snackbar snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT);
        snackbar.setTextColor(Color.WHITE);

        View snackbarView = snackbar.getView();

        snackbarView.setBackground(ContextCompat.getDrawable(
                MainActivity.this,
                R.drawable.bg_chip_protein
        ));

        android.widget.FrameLayout.LayoutParams params =
                (android.widget.FrameLayout.LayoutParams) snackbarView.getLayoutParams();

        params.setMargins(40, 0, 40, 40);
        snackbarView.setLayoutParams(params);

        snackbar.show();
    }

    private void showErrorSnackbar(String message) {
        View rootView = findViewById(android.R.id.content);

        Snackbar snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_LONG);
        snackbar.setTextColor(Color.WHITE);
        snackbar.setBackgroundTint(Color.parseColor("#D32F2F"));
        snackbar.show();
    }
}