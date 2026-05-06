package com.example.calorietracker;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.Locale;

public class FoodDetailFragment extends Fragment {

    public static final String ARG_FOOD_NAME = "food_name";
    public static final String ARG_FOOD_PORTION = "food_portion";
    public static final String ARG_FOOD_BASE_GRAMS = "food_base_grams";
    public static final String ARG_FOOD_CAL = "food_cal";
    public static final String ARG_FOOD_P = "food_p";
    public static final String ARG_FOOD_F = "food_f";
    public static final String ARG_FOOD_C = "food_c";

    private String name, portion;
    private float baseGrams;
    private int baseCal;
    private float baseP, baseF, baseC;

    private TextView tvTitle, tvPortion, tvCalories;
    private TextView tvProtein, tvFat, tvCarbs;

    private EditText etPortionCount, etGrams;
    private MaterialButton btnBreakfast, btnLunch, btnDinner, btnSnacks;

    private boolean lock = false;

    private int calNow = 0;
    private float pNow = 0f, fNow = 0f, cNow = 0f;
    private float gramsNow = 0f, portionsNow = 1f;

    public FoodDetailFragment() {
        super(R.layout.fragment_food_detail);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageView btnBack = view.findViewById(R.id.btnBack);

        tvTitle = view.findViewById(R.id.tvFoodTitle);
        tvPortion = view.findViewById(R.id.tvFoodPortion);
        tvCalories = view.findViewById(R.id.tvFoodCalories);

        tvProtein = view.findViewById(R.id.tvProtein);
        tvFat = view.findViewById(R.id.tvFat);
        tvCarbs = view.findViewById(R.id.tvCarbs);

        etPortionCount = view.findViewById(R.id.etPortionCount);
        etGrams = view.findViewById(R.id.etGrams);

        btnBreakfast = view.findViewById(R.id.btnAddBreakfast);
        btnLunch = view.findViewById(R.id.btnAddLunch);
        btnDinner = view.findViewById(R.id.btnAddDinner);
        btnSnacks = view.findViewById(R.id.btnAddSnacks);

        readArgs();
        bindUI();
        setupInputs();

        btnBack.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack()
        );

        btnBreakfast.setOnClickListener(v -> addToMeal("breakfast"));
        btnLunch.setOnClickListener(v -> addToMeal("lunch"));
        btnDinner.setOnClickListener(v -> addToMeal("dinner"));
        btnSnacks.setOnClickListener(v -> addToMeal("snacks"));
    }

    private void readArgs() {
        Bundle b = getArguments();

        name = (b != null) ? b.getString(ARG_FOOD_NAME, "") : "";
        portion = (b != null) ? b.getString(ARG_FOOD_PORTION, "") : "";

        baseGrams = (b != null) ? b.getFloat(ARG_FOOD_BASE_GRAMS, 100f) : 100f;
        baseCal = (b != null) ? b.getInt(ARG_FOOD_CAL, 0) : 0;
        baseP = (b != null) ? b.getFloat(ARG_FOOD_P, 0f) : 0f;
        baseF = (b != null) ? b.getFloat(ARG_FOOD_F, 0f) : 0f;
        baseC = (b != null) ? b.getFloat(ARG_FOOD_C, 0f) : 0f;

        if (baseGrams <= 0f) baseGrams = 100f;
    }

    private void bindUI() {
        tvTitle.setText(name);
        tvPortion.setText(portion);

        lock = true;
        etPortionCount.setText("1");
        etGrams.setText(String.format(Locale.ROOT, "%.0f", baseGrams));
        lock = false;

        computeByFactor(1f);
    }

    private void setupInputs() {
        etPortionCount.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (lock) return;

                float portions = parseFloatSafe(s.toString(), 1f);
                if (portions <= 0f) portions = 1f;

                lock = true;
                etGrams.setText(String.format(Locale.ROOT, "%.0f", baseGrams * portions));
                lock = false;

                computeByFactor(portions);
            }
        });

        etGrams.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (lock) return;

                float grams = parseFloatSafe(s.toString(), baseGrams);
                if (grams <= 0f) grams = baseGrams;

                float factor = grams / baseGrams;
                if (factor <= 0f) factor = 1f;

                lock = true;
                etPortionCount.setText(String.format(Locale.ROOT, "%.2f", factor));
                lock = false;

                computeByFactor(factor);
            }
        });
    }

    private void computeByFactor(float factor) {
        portionsNow = factor;
        gramsNow = baseGrams * factor;

        calNow = Math.round(baseCal * factor);
        pNow = baseP * factor;
        fNow = baseF * factor;
        cNow = baseC * factor;

        tvCalories.setText(calNow + " kcal");
        tvProtein.setText(String.format(Locale.ROOT, "%.1fg", pNow));
        tvFat.setText(String.format(Locale.ROOT, "%.1fg", fNow));
        tvCarbs.setText(String.format(Locale.ROOT, "%.1fg", cNow));
    }

    private void addToMeal(String meal) {
        int userId = new SessionManager(requireContext()).getUserId();
        if (userId == -1) {
            showSnack(requireView(), "Not logged in", true);
            return;
        }

        dbConnect db = new dbConnect(requireContext());
        db.addFoodLog(userId, meal, name, gramsNow, portionsNow, calNow, pNow, fNow, cNow);

        showSnack(requireView(), "Added to " + meal + " ✅", false);
        // остануваш на истата страна
    }

    private void showSnack(View anchor, String message, boolean isError) {
        Snackbar snackbar = Snackbar.make(anchor, message, Snackbar.LENGTH_SHORT);
        snackbar.setDuration(1000); // 1 секунда

        snackbar.setTextColor(Color.WHITE);

        // позадина: црвена ако е грешка, зелена ако е успех
        snackbar.setBackgroundTint(Color.parseColor(isError ? "#E53935" : "#4CAF50"));

        View snackbarView = snackbar.getView();
        try {
            android.widget.FrameLayout.LayoutParams params =
                    (android.widget.FrameLayout.LayoutParams) snackbarView.getLayoutParams();
            params.setMargins(40, 0, 40, 40);
            snackbarView.setLayoutParams(params);
        } catch (Exception ignored) {}

        // ако сакаш ист изглед како RegisterActivity
        snackbarView.setBackground(getResources().getDrawable(R.drawable.bg_chip_protein));

        snackbar.show();
    }

    private float parseFloatSafe(String s, float def) {
        try {
            if (s == null) return def;
            s = s.trim();
            if (s.isEmpty()) return def;
            return Float.parseFloat(s);
        } catch (Exception e) {
            return def;
        }
    }
}