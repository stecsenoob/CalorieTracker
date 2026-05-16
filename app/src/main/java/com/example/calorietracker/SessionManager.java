package com.example.calorietracker;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF = "ct_session";

    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_FIREBASE_UID = "firebase_uid";

    private final SharedPreferences sp;

    public SessionManager(Context ctx) {
        sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public void saveUser(int userId, String email) {
        sp.edit()
                .putInt(KEY_USER_ID, userId)
                .putString(KEY_EMAIL, email)
                .apply();
    }

    public void saveFirebaseUser(int userId, String email, String firebaseUid) {
        sp.edit()
                .putInt(KEY_USER_ID, userId)
                .putString(KEY_EMAIL, email)
                .putString(KEY_FIREBASE_UID, firebaseUid)
                .apply();
    }

    public int getUserId() {
        return sp.getInt(KEY_USER_ID, -1);
    }

    public String getEmail() {
        return sp.getString(KEY_EMAIL, "");
    }

    public String getFirebaseUid() {
        return sp.getString(KEY_FIREBASE_UID, "");
    }

    public String getUsername() {
        return getEmail();
    }

    public boolean isLoggedIn() {
        return getUserId() != -1;
    }

    public void logout() {
        sp.edit().clear().apply();
    }
}