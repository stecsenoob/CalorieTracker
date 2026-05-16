package com.example.calorietracker;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText edtEmail, edtPass, edtRepass;
    private TextInputLayout tilEmail, tilPass, tilRepass;

    private MaterialButton btnRegister;
    private TextView btnLogin;

    private dbConnect db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        db = new dbConnect(this);

        edtEmail = findViewById(R.id.email);
        edtPass = findViewById(R.id.password);
        edtRepass = findViewById(R.id.repassword);

        tilEmail = findViewById(R.id.tilEmail);
        tilPass = findViewById(R.id.tilPassword);
        tilRepass = findViewById(R.id.tilRepassword);

        tilEmail.setErrorIconDrawable(null);
        tilPass.setErrorIconDrawable(null);
        tilRepass.setErrorIconDrawable(null);

        btnRegister = findViewById(R.id.registerbtn);
        btnLogin = findViewById(R.id.loginbtn);

        addClearOnType(edtEmail, tilEmail, null);
        addClearOnType(edtPass, tilPass, tilPass);
        addClearOnType(edtRepass, tilRepass, tilRepass);

        btnLogin.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        btnRegister.setOnClickListener(v -> doRegister());
    }

    private void doRegister() {
        clearAllErrors();

        String email = txt(edtEmail).trim();
        String pass = txt(edtPass).trim();
        String repass = txt(edtRepass).trim();

        if (email.isEmpty()) {
            tilEmail.setError("Email is required");
            edtEmail.requestFocus();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Enter a valid email address");
            edtEmail.requestFocus();
            return;
        }

        if (pass.isEmpty()) {
            showPasswordError(tilPass, "Password is required");
            edtPass.requestFocus();
            return;
        }

        if (pass.length() < 10 || !pass.matches(".*\\d.*")) {
            showPasswordError(tilPass, "Password must be at least 10 characters and contain at least 1 number");
            edtPass.requestFocus();
            return;
        }

        if (repass.isEmpty()) {
            showPasswordError(tilRepass, "Confirm password is required");
            edtRepass.requestFocus();
            return;
        }

        if (!pass.equals(repass)) {
            showPasswordError(tilRepass, "Passwords don't match");
            edtRepass.requestFocus();
            return;
        }

        if (db.userExists(email)) {
            tilEmail.setError("Email already registered");
            edtEmail.requestFocus();
            return;
        }

        Users newUser = new Users(email, pass, repass);
        db.addUser(newUser);

        showSuccessSnackbar("Registration successful!");

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }, 1200);
    }

    private void clearAllErrors() {
        tilEmail.setError(null);
        tilPass.setError(null);
        tilRepass.setError(null);

        tilPass.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
        tilRepass.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
    }

    private void showPasswordError(TextInputLayout til, String msg) {
        til.setError(msg);
        til.setEndIconMode(TextInputLayout.END_ICON_NONE);
    }

    private void showSuccessSnackbar(String message) {
        View rootView = findViewById(android.R.id.content);

        Snackbar snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT);
        snackbar.setTextColor(Color.WHITE);

        View snackbarView = snackbar.getView();

        snackbarView.setBackground(ContextCompat.getDrawable(
                RegisterActivity.this,
                R.drawable.bg_chip_protein
        ));

        android.widget.FrameLayout.LayoutParams params =
                (android.widget.FrameLayout.LayoutParams) snackbarView.getLayoutParams();

        params.setMargins(40, 0, 40, 40);
        snackbarView.setLayoutParams(params);

        snackbar.show();
    }

    private static String txt(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString();
    }

    private void addClearOnType(TextInputEditText et, TextInputLayout til, TextInputLayout passwordTil) {
        et.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                til.setError(null);

                if (passwordTil != null) {
                    passwordTil.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
                }
            }
        });
    }
}