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

public class MainActivity extends AppCompatActivity {

    TextInputEditText edtEmailLog, edtPassLog;
    TextInputLayout tilEmail, tilPassword;
    MaterialButton btnLogin;
    TextView btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        edtEmailLog = findViewById(R.id.email);
        edtPassLog = findViewById(R.id.password);

        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);

        btnLogin = findViewById(R.id.loginbtn);
        btnRegister = findViewById(R.id.registerbtn);

        btnLogin.setOnClickListener(v -> {
            clearErrors();

            String email = edtEmailLog.getText() == null
                    ? ""
                    : edtEmailLog.getText().toString().trim();

            String pass = edtPassLog.getText() == null
                    ? ""
                    : edtPassLog.getText().toString();

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

            dbConnect db = new dbConnect(MainActivity.this);

            int userId = db.checkLoginGetUserId(email, pass);

            if (userId != -1) {
                new SessionManager(MainActivity.this).saveUser(userId, email);

                showSuccessSnackbar("Login successful!");

                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                    startActivity(intent);
                    finish();
                }, 1200);

            } else {
                showPasswordError("Invalid email or password");
            }
        });

        btnRegister.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void clearErrors() {
        tilEmail.setError(null);
        tilPassword.setError(null);

        tilPassword.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
    }

    private void showPasswordError(String msg) {
        tilPassword.setError(msg);

        // Го тргаме eye icon за да не се преклопува со error icon.
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
}