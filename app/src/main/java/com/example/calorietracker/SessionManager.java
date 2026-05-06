package com.example.calorietracker;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF = "ct_session";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";

    private final SharedPreferences sp;

    public SessionManager(Context ctx) {
        sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public void saveUser(int userId, String username) {
        sp.edit()
                .putInt(KEY_USER_ID, userId)
                .putString(KEY_USERNAME, username)
                .apply();
    }

    public int getUserId() {
        return sp.getInt(KEY_USER_ID, -1);
    }

    public String getUsername() {
        return sp.getString(KEY_USERNAME, "");
    }

    public boolean isLoggedIn() {
        return getUserId() != -1;
    }

    public void logout() {
        sp.edit().clear().apply();
    }
}