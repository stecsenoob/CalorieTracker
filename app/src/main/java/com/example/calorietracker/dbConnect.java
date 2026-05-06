package com.example.calorietracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class dbConnect extends SQLiteOpenHelper {

    private static final String dbName = "findFriendsManager";
    private static final int dbVersion = 5; // ✅ bump version (was 4)

    // ================= USERS TABLE =================
    private static final String USERS_TABLE = "users";
    private static final String U_ID = "id";
    private static final String U_USERNAME = "username";
    private static final String U_PASSWORD = "password";

    // ================= FOOD LOGS TABLE =================
    public static final String LOGS_TABLE = "food_logs";
    public static final String L_ID = "id";
    public static final String L_USER_ID = "user_id";
    public static final String L_MEAL = "meal";
    public static final String L_NAME = "name";
    public static final String L_GRAMS = "grams";
    public static final String L_PORTIONS = "portions";
    public static final String L_CAL = "calories";
    public static final String L_P = "protein";
    public static final String L_F = "fat";
    public static final String L_C = "carbs";
    public static final String L_CREATED = "created_at";

    // ================= USER FOODS TABLE =================
    public static final String FOODS_TABLE = "user_foods";
    public static final String F_ID = "id";
    public static final String F_USER_ID = "user_id";
    public static final String F_NAME = "name";
    public static final String F_PORTION = "portion";
    public static final String F_BASE_GRAMS = "base_grams";
    public static final String F_CAL = "calories";
    public static final String F_P = "protein";
    public static final String F_F = "fat";
    public static final String F_C = "carbs";
    public static final String F_CREATED = "created_at";

    // ================= FAVORITES TABLE =================
    public static final String FAV_TABLE = "favorites";
    public static final String FV_ID = "id";
    public static final String FV_USER_ID = "user_id";
    public static final String FV_KEY = "food_key";

    public static final String FV_NAME = "name";
    public static final String FV_PORTION = "portion";
    public static final String FV_BASE_GRAMS = "base_grams";
    public static final String FV_CAL = "calories";
    public static final String FV_P = "protein";
    public static final String FV_F = "fat";
    public static final String FV_C = "carbs";
    public static final String FV_CREATED = "created_at";

    public dbConnect(@Nullable Context context) {
        super(context, dbName, null, dbVersion);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        // USERS
        String usersQuery = "CREATE TABLE " + USERS_TABLE + " (" +
                U_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                U_USERNAME + " TEXT UNIQUE, " +
                U_PASSWORD + " TEXT)";
        db.execSQL(usersQuery);

        // FOOD LOGS
        String logsQuery = "CREATE TABLE " + LOGS_TABLE + " (" +
                L_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                L_USER_ID + " INTEGER NOT NULL, " +
                L_MEAL + " TEXT NOT NULL, " +
                L_NAME + " TEXT NOT NULL, " +
                L_GRAMS + " REAL NOT NULL, " +
                L_PORTIONS + " REAL NOT NULL, " +
                L_CAL + " INTEGER NOT NULL, " +
                L_P + " REAL NOT NULL, " +
                L_F + " REAL NOT NULL, " +
                L_C + " REAL NOT NULL, " +
                L_CREATED + " INTEGER NOT NULL, " +
                "FOREIGN KEY(" + L_USER_ID + ") REFERENCES " + USERS_TABLE + "(" + U_ID + ")" +
                ")";
        db.execSQL(logsQuery);

        db.execSQL("CREATE INDEX idx_logs_user_meal ON " + LOGS_TABLE + "(" + L_USER_ID + ", " + L_MEAL + ")");
        db.execSQL("CREATE INDEX idx_logs_user_created ON " + LOGS_TABLE + "(" + L_USER_ID + ", " + L_CREATED + ")");

        // USER FOODS
        String foodsQuery = "CREATE TABLE " + FOODS_TABLE + " (" +
                F_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                F_USER_ID + " INTEGER NOT NULL, " +
                F_NAME + " TEXT NOT NULL, " +
                F_PORTION + " TEXT NOT NULL, " +
                F_BASE_GRAMS + " REAL NOT NULL, " +
                F_CAL + " INTEGER NOT NULL, " +
                F_P + " REAL NOT NULL, " +
                F_F + " REAL NOT NULL, " +
                F_C + " REAL NOT NULL, " +
                F_CREATED + " INTEGER NOT NULL, " +
                "FOREIGN KEY(" + F_USER_ID + ") REFERENCES " + USERS_TABLE + "(" + U_ID + ")" +
                ")";
        db.execSQL(foodsQuery);

        db.execSQL("CREATE INDEX idx_foods_user ON " + FOODS_TABLE + "(" + F_USER_ID + ")");

        // FAVORITES
        String favQuery = "CREATE TABLE " + FAV_TABLE + " (" +
                FV_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                FV_USER_ID + " INTEGER NOT NULL, " +
                FV_KEY + " TEXT NOT NULL, " +
                FV_NAME + " TEXT NOT NULL, " +
                FV_PORTION + " TEXT NOT NULL, " +
                FV_BASE_GRAMS + " REAL NOT NULL, " +
                FV_CAL + " INTEGER NOT NULL, " +
                FV_P + " REAL NOT NULL, " +
                FV_F + " REAL NOT NULL, " +
                FV_C + " REAL NOT NULL, " +
                FV_CREATED + " INTEGER NOT NULL, " +
                "UNIQUE(" + FV_USER_ID + ", " + FV_KEY + "), " +
                "FOREIGN KEY(" + FV_USER_ID + ") REFERENCES " + USERS_TABLE + "(" + U_ID + ")" +
                ")";
        db.execSQL(favQuery);

        db.execSQL("CREATE INDEX idx_fav_user ON " + FAV_TABLE + "(" + FV_USER_ID + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Development friendly: drop & recreate
        db.execSQL("DROP TABLE IF EXISTS " + LOGS_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + FOODS_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + FAV_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + USERS_TABLE);
        onCreate(db);
    }

    // ================= USERS API =================

    public void addUser(Users user) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(U_USERNAME, user.getUsername());
        values.put(U_PASSWORD, user.getPassword());
        db.insert(USERS_TABLE, null, values);
    }

    public boolean userExists(String user) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT 1 FROM " + USERS_TABLE + " WHERE " + U_USERNAME + "=?",
                new String[]{user}
        );
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    // ✅ returns userId if ok, else -1
    public int checkLoginGetUserId(String user, String pass) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT " + U_ID + " FROM " + USERS_TABLE + " WHERE " + U_USERNAME + "=? AND " + U_PASSWORD + "=?",
                new String[]{user, pass}
        );

        int id = -1;
        if (cursor.moveToFirst()) id = cursor.getInt(0);
        cursor.close();
        return id;
    }

    // ================= FOOD LOGS MODEL =================
    public static class LoggedFoodRow {
        public int id;
        public String name;
        public float grams;
        public float portions;
        public int calories;
        public float protein;
        public float fat;
        public float carbs;
        public String meal;
    }

    public static class Totals {
        public int calories = 0;
        public float protein = 0f;
        public float fat = 0f;
        public float carbs = 0f;
        public int items = 0;
    }

    // ================= FOOD LOGS API =================

    public long addFoodLog(int userId, String meal, String name, float grams, float portions,
                           int calories, float protein, float fat, float carbs) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues v = new ContentValues();
        v.put(L_USER_ID, userId);
        v.put(L_MEAL, normalizeMeal(meal));
        v.put(L_NAME, name);
        v.put(L_GRAMS, grams);
        v.put(L_PORTIONS, portions);
        v.put(L_CAL, calories);
        v.put(L_P, protein);
        v.put(L_F, fat);
        v.put(L_C, carbs);
        v.put(L_CREATED, System.currentTimeMillis());

        return db.insert(LOGS_TABLE, null, v);
    }

    public List<LoggedFoodRow> getMealItems(int userId, String meal) {
        meal = normalizeMeal(meal);
        SQLiteDatabase db = this.getReadableDatabase();

        // ✅ only today's items (so totals reset when date changes)
        long start = startOfToday();
        long end = endOfToday();

        Cursor c = db.rawQuery(
                "SELECT " + L_ID + ", " + L_NAME + ", " + L_GRAMS + ", " + L_PORTIONS + ", " +
                        L_CAL + ", " + L_P + ", " + L_F + ", " + L_C + ", " + L_MEAL +
                        " FROM " + LOGS_TABLE +
                        " WHERE " + L_USER_ID + "=? AND " + L_MEAL + "=? " +
                        "AND " + L_CREATED + " BETWEEN ? AND ? " +
                        "ORDER BY " + L_CREATED + " DESC",
                new String[]{
                        String.valueOf(userId),
                        meal,
                        String.valueOf(start),
                        String.valueOf(end)
                }
        );

        List<LoggedFoodRow> list = new ArrayList<>();
        while (c.moveToNext()) {
            LoggedFoodRow r = new LoggedFoodRow();
            r.id = c.getInt(0);
            r.name = c.getString(1);
            r.grams = c.getFloat(2);
            r.portions = c.getFloat(3);
            r.calories = c.getInt(4);
            r.protein = c.getFloat(5);
            r.fat = c.getFloat(6);
            r.carbs = c.getFloat(7);
            r.meal = c.getString(8);
            list.add(r);
        }
        c.close();
        return list;
    }

    public Totals getMealTotals(int userId, String meal) {
        meal = normalizeMeal(meal);
        SQLiteDatabase db = this.getReadableDatabase();

        // ✅ only today's totals (so totals reset when date changes)
        long start = startOfToday();
        long end = endOfToday();

        Cursor c = db.rawQuery(
                "SELECT COUNT(*), " +
                        "IFNULL(SUM(" + L_CAL + "),0), " +
                        "IFNULL(SUM(" + L_P + "),0), " +
                        "IFNULL(SUM(" + L_F + "),0), " +
                        "IFNULL(SUM(" + L_C + "),0) " +
                        "FROM " + LOGS_TABLE +
                        " WHERE " + L_USER_ID + "=? AND " + L_MEAL + "=? " +
                        "AND " + L_CREATED + " BETWEEN ? AND ?",
                new String[]{
                        String.valueOf(userId),
                        meal,
                        String.valueOf(start),
                        String.valueOf(end)
                }
        );

        Totals t = new Totals();
        if (c.moveToFirst()) {
            t.items = c.getInt(0);
            t.calories = c.getInt(1);
            t.protein = c.getFloat(2);
            t.fat = c.getFloat(3);
            t.carbs = c.getFloat(4);
        }
        c.close();
        return t;
    }

    public Totals getGrandTotals(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();

        // ✅ only today's totals (so totals reset when date changes)
        long start = startOfToday();
        long end = endOfToday();

        Cursor c = db.rawQuery(
                "SELECT COUNT(*), " +
                        "IFNULL(SUM(" + L_CAL + "),0), " +
                        "IFNULL(SUM(" + L_P + "),0), " +
                        "IFNULL(SUM(" + L_F + "),0), " +
                        "IFNULL(SUM(" + L_C + "),0) " +
                        "FROM " + LOGS_TABLE +
                        " WHERE " + L_USER_ID + "=? " +
                        "AND " + L_CREATED + " BETWEEN ? AND ?",
                new String[]{
                        String.valueOf(userId),
                        String.valueOf(start),
                        String.valueOf(end)
                }
        );

        Totals t = new Totals();
        if (c.moveToFirst()) {
            t.items = c.getInt(0);
            t.calories = c.getInt(1);
            t.protein = c.getFloat(2);
            t.fat = c.getFloat(3);
            t.carbs = c.getFloat(4);
        }
        c.close();
        return t;
    }

    public void deleteFoodLog(int userId, int logId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(LOGS_TABLE,
                L_ID + "=? AND " + L_USER_ID + "=?",
                new String[]{String.valueOf(logId), String.valueOf(userId)}
        );
    }

    // ================= USER FOODS API =================

    public static class UserFoodRow {
        public int id;
        public String name;
        public String portion;
        public float baseGrams;
        public int calories;
        public float protein;
        public float fat;
        public float carbs;
    }

    public long addUserFood(int userId, String name, String portion, float baseGrams,
                            int calories, float protein, float fat, float carbs) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(F_USER_ID, userId);
        v.put(F_NAME, name);
        v.put(F_PORTION, portion);
        v.put(F_BASE_GRAMS, baseGrams);
        v.put(F_CAL, calories);
        v.put(F_P, protein);
        v.put(F_F, fat);
        v.put(F_C, carbs);
        v.put(F_CREATED, System.currentTimeMillis());
        return db.insert(FOODS_TABLE, null, v);
    }

    public List<UserFoodRow> getUserFoods(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT " + F_ID + ", " + F_NAME + ", " + F_PORTION + ", " + F_BASE_GRAMS + ", " +
                        F_CAL + ", " + F_P + ", " + F_F + ", " + F_C +
                        " FROM " + FOODS_TABLE +
                        " WHERE " + F_USER_ID + "=? ORDER BY " + F_CREATED + " DESC",
                new String[]{String.valueOf(userId)}
        );

        List<UserFoodRow> list = new ArrayList<>();
        while (c.moveToNext()) {
            UserFoodRow r = new UserFoodRow();
            r.id = c.getInt(0);
            r.name = c.getString(1);
            r.portion = c.getString(2);
            r.baseGrams = c.getFloat(3);
            r.calories = c.getInt(4);
            r.protein = c.getFloat(5);
            r.fat = c.getFloat(6);
            r.carbs = c.getFloat(7);
            list.add(r);
        }
        c.close();
        return list;
    }

    // ================= FAVORITES API =================

    public boolean isFavorite(int userId, String foodKey) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT 1 FROM " + FAV_TABLE + " WHERE " + FV_USER_ID + "=? AND " + FV_KEY + "=?",
                new String[]{String.valueOf(userId), foodKey}
        );
        boolean ok = c.getCount() > 0;
        c.close();
        return ok;
    }

    public long addFavorite(int userId, String foodKey,
                            String name, String portion, float baseGrams,
                            int calories, float protein, float fat, float carbs) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(FV_USER_ID, userId);
        v.put(FV_KEY, foodKey);
        v.put(FV_NAME, name);
        v.put(FV_PORTION, portion);
        v.put(FV_BASE_GRAMS, baseGrams);
        v.put(FV_CAL, calories);
        v.put(FV_P, protein);
        v.put(FV_F, fat);
        v.put(FV_C, carbs);
        v.put(FV_CREATED, System.currentTimeMillis());
        return db.insert(FAV_TABLE, null, v); // if exists -> -1
    }

    public void removeFavorite(int userId, String foodKey) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(FAV_TABLE,
                FV_USER_ID + "=? AND " + FV_KEY + "=?",
                new String[]{String.valueOf(userId), foodKey}
        );
    }

    public static class FavoriteFoodRow {
        public String key;
        public String name;
        public String portion;
        public float baseGrams;
        public int calories;
        public float protein;
        public float fat;
        public float carbs;
    }

    public List<FavoriteFoodRow> getFavorites(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT " + FV_KEY + ", " + FV_NAME + ", " + FV_PORTION + ", " + FV_BASE_GRAMS + ", " +
                        FV_CAL + ", " + FV_P + ", " + FV_F + ", " + FV_C +
                        " FROM " + FAV_TABLE +
                        " WHERE " + FV_USER_ID + "=? ORDER BY " + FV_CREATED + " DESC",
                new String[]{String.valueOf(userId)}
        );

        List<FavoriteFoodRow> list = new ArrayList<>();
        while (c.moveToNext()) {
            FavoriteFoodRow r = new FavoriteFoodRow();
            r.key = c.getString(0);
            r.name = c.getString(1);
            r.portion = c.getString(2);
            r.baseGrams = c.getFloat(3);
            r.calories = c.getInt(4);
            r.protein = c.getFloat(5);
            r.fat = c.getFloat(6);
            r.carbs = c.getFloat(7);
            list.add(r);
        }
        c.close();
        return list;
    }

    // ================= HELPERS =================

    private String normalizeMeal(String meal) {
        if (meal == null) return "breakfast";
        meal = meal.trim().toLowerCase();
        if (!(meal.equals("breakfast") || meal.equals("lunch") || meal.equals("dinner") || meal.equals("snacks"))) {
            return "breakfast";
        }
        return meal;
    }

    // ✅ Date helpers (today range) -> makes totals reset when phone date changes
    private long startOfToday() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private long endOfToday() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTimeInMillis();
    }
}
