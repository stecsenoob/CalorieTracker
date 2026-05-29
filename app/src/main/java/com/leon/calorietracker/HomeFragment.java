package com.leon.calorietracker;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;

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

        MaterialButton btnLogout = view.findViewById(R.id.btnLogout);

        MaterialCardView cardBreakfast = view.findViewById(R.id.cardBreakfast);
        MaterialCardView cardLunch     = view.findViewById(R.id.cardLunch);
        MaterialCardView cardDinner    = view.findViewById(R.id.cardDinner);
        MaterialCardView cardSnacks    = view.findViewById(R.id.cardSnacks);

        MaterialButton btnOpenScanDialog = view.findViewById(R.id.btnOpenScanDialog);

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

        hideMealKcalChips();

        btnLogout.setOnClickListener(v -> logoutUser());

        cardBreakfast.setOnClickListener(v -> openMealPage("breakfast"));
        cardLunch.setOnClickListener(v -> openMealPage("lunch"));
        cardDinner.setOnClickListener(v -> openMealPage("dinner"));
        cardSnacks.setOnClickListener(v -> openMealPage("snacks"));

        btnOpenScanDialog.setOnClickListener(v -> showScanChoiceBottomSheet());

        refreshUI();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshUI();
    }

    private void logoutUser() {
        FirebaseAuth.getInstance().signOut();
        new SessionManager(requireContext()).logout();

        Intent intent = new Intent(requireContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        requireActivity().finish();
    }

    private void hideMealKcalChips() {
        tvBreakfastKcal.setVisibility(View.GONE);
        tvLunchKcal.setVisibility(View.GONE);
        tvDinnerKcal.setVisibility(View.GONE);
        tvSnacksKcal.setVisibility(View.GONE);
    }

    private void openMealPage(String mealType) {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frameLayout, MealFragment.newInstance(mealType))
                .addToBackStack(null)
                .commit();
    }


    private void showScanChoiceBottomSheet() {
        BottomSheetDialog sheet = new BottomSheetDialog(requireContext());

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(14), dp(22), dp(28));
        root.setBackgroundColor(Color.WHITE);

        View handle = new View(requireContext());
        LinearLayout.LayoutParams handleLp = new LinearLayout.LayoutParams(dp(44), dp(5));
        handleLp.gravity = Gravity.CENTER_HORIZONTAL;
        handleLp.setMargins(0, 0, 0, dp(18));
        handle.setLayoutParams(handleLp);
        handle.setBackgroundColor(Color.parseColor("#E7DFF6"));
        root.addView(handle);

        TextView title = new TextView(requireContext());
        title.setText("Choose scan option");
        title.setTextSize(22f);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setTextColor(Color.parseColor("#111111"));
        root.addView(title);

        TextView subtitle = new TextView(requireContext());
        subtitle.setText("Select how you want to add food using the camera.");
        subtitle.setTextSize(14f);
        subtitle.setTextColor(Color.parseColor("#777777"));
        LinearLayout.LayoutParams subtitleLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        subtitleLp.setMargins(0, dp(5), 0, dp(18));
        subtitle.setLayoutParams(subtitleLp);
        root.addView(subtitle);

        MaterialButton scanFood = new MaterialButton(requireContext());
        scanFood.setText("Scan Food");
        scanFood.setTextSize(16f);
        scanFood.setTypeface(null, android.graphics.Typeface.BOLD);
        scanFood.setTextColor(Color.WHITE);
        scanFood.setAllCaps(false);
        scanFood.setIconResource(R.drawable.outline_photo_camera_24);
        scanFood.setIconTintResource(android.R.color.white);
        scanFood.setIconPadding(dp(8));
        scanFood.setCornerRadius(dp(18));
        scanFood.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#7B1FA2")));
        LinearLayout.LayoutParams btnLp1 = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(56)
        );
        btnLp1.setMargins(0, 0, 0, dp(10));
        scanFood.setLayoutParams(btnLp1);
        root.addView(scanFood);

        MaterialButton scanBarcode = new MaterialButton(requireContext());
        scanBarcode.setText("Scan Barcode");
        scanBarcode.setTextSize(16f);
        scanBarcode.setTypeface(null, android.graphics.Typeface.BOLD);
        scanBarcode.setTextColor(Color.parseColor("#7B1FA2"));
        scanBarcode.setAllCaps(false);
        scanBarcode.setIconResource(R.drawable.ic_barcode);
        scanBarcode.setIconTint(android.content.res.ColorStateList.valueOf(Color.parseColor("#7B1FA2")));
        scanBarcode.setIconPadding(dp(8));
        scanBarcode.setCornerRadius(dp(18));
        scanBarcode.setStrokeWidth(dp(1));
        scanBarcode.setStrokeColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#D7BDEB")));
        scanBarcode.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#F5EFFB")));
        LinearLayout.LayoutParams btnLp2 = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(56)
        );
        scanBarcode.setLayoutParams(btnLp2);
        root.addView(scanBarcode);

        scanFood.setOnClickListener(v -> {
            sheet.dismiss();
            openScanFoodPage();
        });

        scanBarcode.setOnClickListener(v -> {
            sheet.dismiss();
            openBarcodeScannerPage();
        });

        sheet.setContentView(root);
        sheet.show();
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

        setMealCard(db, userId, "breakfast", tvBreakfastSub, tvBreakfastP, tvBreakfastF, tvBreakfastC);
        setMealCard(db, userId, "lunch",     tvLunchSub,     tvLunchP,     tvLunchF,     tvLunchC);
        setMealCard(db, userId, "dinner",    tvDinnerSub,    tvDinnerP,    tvDinnerF,    tvDinnerC);
        setMealCard(db, userId, "snacks",    tvSnacksSub,    tvSnacksP,    tvSnacksF,    tvSnacksC);

        dbConnect.Totals g = db.getGrandTotals(userId);
        setGrandChips(g, tvGrandKcal, tvGrandP, tvGrandF, tvGrandC);
    }

    private void setMealCard(dbConnect db, int userId, String meal,
                             TextView tvSub,
                             TextView tvP, TextView tvF, TextView tvC) {

        dbConnect.Totals t = db.getMealTotals(userId, meal);

        tvSub.setText(t.items + " items • " + t.calories + " kcal");
        setMacroChips(t, tvP, tvF, tvC);
    }

    private void setMacroChips(dbConnect.Totals t,
                               TextView tvP, TextView tvF, TextView tvC) {

        tvP.setText(String.format(Locale.ROOT, "Protein %.1fg", t.protein));
        tvF.setText(String.format(Locale.ROOT, "Fat %.1fg", t.fat));
        tvC.setText(String.format(Locale.ROOT, "Carbs %.1fg", t.carbs));
    }

    private void setGrandChips(dbConnect.Totals t,
                               TextView tvKcal, TextView tvP, TextView tvF, TextView tvC) {

        tvKcal.setText(String.format(Locale.ROOT, "%d kcal", t.calories));
        tvP.setText(String.format(Locale.ROOT, "Protein %.1fg", t.protein));
        tvF.setText(String.format(Locale.ROOT, "Fat %.1fg", t.fat));
        tvC.setText(String.format(Locale.ROOT, "Carbs %.1fg", t.carbs));
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}