package com.example.calorietracker;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.Locale;

public class HomeFragment extends Fragment {

    private TextView tvBreakfastSub, tvLunchSub, tvDinnerSub, tvSnacksSub;

    private TextView tvBreakfastKcal, tvBreakfastP, tvBreakfastF, tvBreakfastC;
    private TextView tvLunchKcal, tvLunchP, tvLunchF, tvLunchC;
    private TextView tvDinnerKcal, tvDinnerP, tvDinnerF, tvDinnerC;
    private TextView tvSnacksKcal, tvSnacksP, tvSnacksF, tvSnacksC;

    private TextView tvGrandKcal, tvGrandP, tvGrandF, tvGrandC;

    public HomeFragment() {
        super(R.layout.fragment_home);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialCardView cardBreakfast = view.findViewById(R.id.cardBreakfast);
        MaterialCardView cardLunch     = view.findViewById(R.id.cardLunch);
        MaterialCardView cardDinner    = view.findViewById(R.id.cardDinner);
        MaterialCardView cardSnacks    = view.findViewById(R.id.cardSnacks);

        MaterialButton btnScanFood = view.findViewById(R.id.btnScanFood);
        MaterialButton btnScanBarcode = view.findViewById(R.id.btnScanBarcode);

        tvBreakfastSub = view.findViewById(R.id.tvBreakfastSub);
        tvLunchSub     = view.findViewById(R.id.tvLunchSub);
        tvDinnerSub    = view.findViewById(R.id.tvDinnerSub);
        tvSnacksSub    = view.findViewById(R.id.tvSnacksSub);

        tvBreakfastKcal = view.findViewById(R.id.tvBreakfastKcal);
        tvBreakfastP    = view.findViewById(R.id.tvBreakfastP);
        tvBreakfastF    = view.findViewById(R.id.tvBreakfastF);
        tvBreakfastC    = view.findViewById(R.id.tvBreakfastC);

        tvLunchKcal = view.findViewById(R.id.tvLunchKcal);
        tvLunchP    = view.findViewById(R.id.tvLunchP);
        tvLunchF    = view.findViewById(R.id.tvLunchF);
        tvLunchC    = view.findViewById(R.id.tvLunchC);

        tvDinnerKcal = view.findViewById(R.id.tvDinnerKcal);
        tvDinnerP    = view.findViewById(R.id.tvDinnerP);
        tvDinnerF    = view.findViewById(R.id.tvDinnerF);
        tvDinnerC    = view.findViewById(R.id.tvDinnerC);

        tvSnacksKcal = view.findViewById(R.id.tvSnacksKcal);
        tvSnacksP    = view.findViewById(R.id.tvSnacksP);
        tvSnacksF    = view.findViewById(R.id.tvSnacksF);
        tvSnacksC    = view.findViewById(R.id.tvSnacksC);

        tvGrandKcal = view.findViewById(R.id.tvGrandKcal);
        tvGrandP    = view.findViewById(R.id.tvGrandP);
        tvGrandF    = view.findViewById(R.id.tvGrandF);
        tvGrandC    = view.findViewById(R.id.tvGrandC);

        cardBreakfast.setOnClickListener(v -> openMealPage("breakfast"));
        cardLunch.setOnClickListener(v -> openMealPage("lunch"));
        cardDinner.setOnClickListener(v -> openMealPage("dinner"));
        cardSnacks.setOnClickListener(v -> openMealPage("snacks"));

        btnScanFood.setOnClickListener(v -> openScanFoodPage());
        btnScanBarcode.setOnClickListener(v -> openBarcodeScannerPage());

        refreshUI();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshUI();
    }

    private void openMealPage(String mealType) {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frameLayout, MealFragment.newInstance(mealType))
                .addToBackStack(null)
                .commit();
    }

    private void openScanFoodPage() {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frameLayout, new ScanFoodFragment())
                .addToBackStack(null)
                .commit();
    }

    private void openBarcodeScannerPage() {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frameLayout, new BarcodeScannerFragment())
                .addToBackStack(null)
                .commit();
    }

    private void refreshUI() {
        int userId = new SessionManager(requireContext()).getUserId();
        if (userId == -1) return;

        dbConnect db = new dbConnect(requireContext());

        setMealCard(db, userId, "breakfast", tvBreakfastSub, tvBreakfastKcal, tvBreakfastP, tvBreakfastF, tvBreakfastC);
        setMealCard(db, userId, "lunch",     tvLunchSub,     tvLunchKcal,     tvLunchP,     tvLunchF,     tvLunchC);
        setMealCard(db, userId, "dinner",    tvDinnerSub,    tvDinnerKcal,    tvDinnerP,    tvDinnerF,    tvDinnerC);
        setMealCard(db, userId, "snacks",    tvSnacksSub,    tvSnacksKcal,    tvSnacksP,    tvSnacksF,    tvSnacksC);

        dbConnect.Totals g = db.getGrandTotals(userId);
        setChips(g, tvGrandKcal, tvGrandP, tvGrandF, tvGrandC);
    }

    private void setMealCard(dbConnect db, int userId, String meal,
                             TextView tvSub,
                             TextView tvKcal, TextView tvP, TextView tvF, TextView tvC) {

        dbConnect.Totals t = db.getMealTotals(userId, meal);

        tvSub.setText(t.items + " items • " + t.calories + " kcal");
        setChips(t, tvKcal, tvP, tvF, tvC);
    }

    private void setChips(dbConnect.Totals t,
                          TextView tvKcal, TextView tvP, TextView tvF, TextView tvC) {

        tvKcal.setText(String.format(Locale.ROOT, "%d kcal", t.calories));
        tvP.setText(String.format(Locale.ROOT, "Protein %.1fg", t.protein));
        tvF.setText(String.format(Locale.ROOT, "Fat %.1fg", t.fat));
        tvC.setText(String.format(Locale.ROOT, "Carbs %.1fg", t.carbs));
    }
}