package com.example.calorietracker;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText edtUser, edtPass, edtRepass;
    private TextInputLayout tilUser, tilPass, tilRepass;

    private MaterialButton btnRegister;
    private android.widget.TextView btnLogin;

    private dbConnect db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        db = new dbConnect(this);

        edtUser = findViewById(R.id.username);
        edtPass = findViewById(R.id.password);
        edtRepass = findViewById(R.id.repassword);

        tilUser = findViewById(R.id.tilUsername);
        tilPass = findViewById(R.id.tilPassword);
        tilRepass = findViewById(R.id.tilRepassword);

        // extra safety: remove error icon programmatically too
        tilUser.setErrorIconDrawable(null);
        tilPass.setErrorIconDrawable(null);
        tilRepass.setErrorIconDrawable(null);

        btnRegister = findViewById(R.id.registerbtn);
        btnLogin = findViewById(R.id.loginbtn);

        // clear errors when user types
        addClearOnType(edtUser, tilUser, null);
        addClearOnType(edtPass, tilPass, tilPass);
        addClearOnType(edtRepass, tilRepass, tilRepass);

        btnLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, MainActivity.class));
            finish();
        });

        btnRegister.setOnClickListener(v -> doRegister());
    }

    private void doRegister() {
        clearAllErrors();

        String user = txt(edtUser).trim();
        String pass = txt(edtPass).trim();
        String repass = txt(edtRepass).trim();

        if (user.isEmpty()) {
            tilUser.setError("Username is required");
            return;
        }

        if (pass.isEmpty()) {
            showPasswordError(tilPass, "Password is required");
            return;
        }

        if (repass.isEmpty()) {
            showPasswordError(tilRepass, "Confirm password is required");
            return;
        }

        if (!pass.equals(repass)) {
            // show error ONLY under Confirm Password
            showPasswordError(tilRepass, "Passwords don't match");
            return;
        }

        if (db.userExists(user)) {
            tilUser.setError("Username already taken");
            return;
        }

        if (pass.length() < 10 || !pass.matches(".*\\d.*")) {
            showPasswordError(tilPass, "Password must be at least 10 characters and contain at least 1 number");
            return;
        }

        // Save user
        Users newUser = new Users(user, pass, repass);
        db.addUser(newUser);

        View rootView = findViewById(android.R.id.content);
        Snackbar snackbar = Snackbar.make(rootView, "Registration successful! 🎉", Snackbar.LENGTH_LONG);

// 1. Правилна команда за зелена боја
        snackbar.setBackgroundTint(Color.parseColor("#4CAF50"));
        snackbar.setTextColor(Color.WHITE);

// 2. Трик за да изгледа модерно (да "лебди" со заоблени агли)
        View snackbarView = snackbar.getView();
        android.widget.FrameLayout.LayoutParams params = (android.widget.FrameLayout.LayoutParams) snackbarView.getLayoutParams();
        params.setMargins(40, 0, 40, 40); // Додава простор лево, десно и долу
        snackbarView.setLayoutParams(params);
        snackbarView.setBackground(getResources().getDrawable(R.drawable.bg_chip_protein)); // Можеш да искористиш некоја твоја заоблена позадина, или Android ќе го заобли сам ако е верзија 12+

        snackbar.show();

        // ВАЖНО: Додаваме мало доцнење пред да го префрлиме на MainActivity
        // За да има време корисникот да ја види убавата Snackbar порака!
        new Handler().postDelayed(() -> {
            startActivity(new Intent(RegisterActivity.this, MainActivity.class));
            finish();
        }, 1500); // 1500 милисекунди = 1.5 секунди пауза
    }

    private void clearAllErrors() {
        tilUser.setError(null);
        tilPass.setError(null);
        tilRepass.setError(null);

        // restore eye icons
        tilPass.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
        tilRepass.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
    }

    private void showPasswordError(TextInputLayout til, String msg) {
        til.setError(msg);

        // disable eye icon so it can never overlap anything
        til.setEndIconMode(TextInputLayout.END_ICON_NONE);
    }

    private static String txt(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString();
    }

    // Clears error and restores eye icon when typing
    private void addClearOnType(TextInputEditText et, TextInputLayout til, TextInputLayout passwordTil) {
        et.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                til.setError(null);
                if (passwordTil != null) {
                    passwordTil.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
                }
            }
        });
    }
}