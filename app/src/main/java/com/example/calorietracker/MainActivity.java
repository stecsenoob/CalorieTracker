package com.example.calorietracker;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class MainActivity extends AppCompatActivity {

    TextInputEditText edtUserLog, edtPassLog;
    TextInputLayout tilUsername, tilPassword;
    Button btnLogin;
    TextView btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ✅ TextInputEditText
        edtUserLog = findViewById(R.id.username);
        edtPassLog = findViewById(R.id.password);

        // ✅ TextInputLayout IDs (added in XML)
        tilUsername = findViewById(R.id.tilUsername);
        tilPassword = findViewById(R.id.tilPassword);

        btnLogin = findViewById(R.id.loginbtn);
        btnRegister = findViewById(R.id.registerbtn);

        btnLogin.setOnClickListener(v -> {
            clearErrors();

            String user = edtUserLog.getText() == null ? "" : edtUserLog.getText().toString().trim();
            String pass = edtPassLog.getText() == null ? "" : edtPassLog.getText().toString();

            if (user.isEmpty()) {
                tilUsername.setError("Username is required");
                return;
            }
            if (pass.isEmpty()) {
                showPasswordError("Password is required");
                return;
            }

            dbConnect db = new dbConnect(MainActivity.this);

            int userId = db.checkLoginGetUserId(user, pass);
            if (userId != -1) {
                new SessionManager(MainActivity.this).saveUser(userId, user);

                // --- ПРОФЕСИОНАЛЕН SNACKBAR ПОЧЕТОК ---
                View rootView = findViewById(android.R.id.content);
                Snackbar snackbar = Snackbar.make(rootView, "Login successful! 👋", Snackbar.LENGTH_LONG);

                // Зелена боја за успех и бел текст
                snackbar.setBackgroundTint(Color.parseColor("#4CAF50"));
                snackbar.setTextColor(Color.WHITE);

                // Лебдечки ефект (Floating) и заоблена позадина
                View snackbarView = snackbar.getView();
                android.widget.FrameLayout.LayoutParams params = (android.widget.FrameLayout.LayoutParams) snackbarView.getLayoutParams();
                params.setMargins(40, 0, 40, 40);
                snackbarView.setLayoutParams(params);

                // ✅ ДОДАДЕНА ЛИНИЈАТА ЗА ИСТ ИЗГЛЕД КАКО КАЈ REGISTER
                snackbarView.setBackground(getResources().getDrawable(R.drawable.bg_chip_protein));

                snackbar.show();
                // --- ПРОФЕСИОНАЛЕН SNACKBAR КРАЈ ---

                // Пауза од 1.5 секунди пред да се отвори главниот екран
                new Handler().postDelayed(() -> {
                    startActivity(new Intent(MainActivity.this, HomeActivity.class));
                    finish();
                }, 1500);

            } else {
                showPasswordError("Invalid username or password");
            }
        });

        btnRegister.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, RegisterActivity.class));
        });
    }

    private void clearErrors() {
        tilUsername.setError(null);
        tilPassword.setError(null);

        // ✅ keep eye icon when no error
        tilPassword.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
    }

    private void showPasswordError(String msg) {
        tilPassword.setError(msg);

        // ✅ remove eye icon so it doesn't overlap error icon
        tilPassword.setEndIconMode(TextInputLayout.END_ICON_NONE);
    }
}