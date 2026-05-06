package com.example.calorietracker;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MealFragment extends Fragment {

    public static final String ARG_MEAL_TYPE = "mealType";

    private String mealType = "breakfast";

    private TextView tvTitle, tvSub, tvTotals;
    private RecyclerView rvMealFoods;

    private MealFoodsAdapter adapter;
    private final List<dbConnect.LoggedFoodRow> data = new ArrayList<>();

    private dbConnect db;
    private int userId = -1;

    public MealFragment() {
        super(R.layout.fragment_meal);
    }

    public static MealFragment newInstance(String mealType) {
        MealFragment f = new MealFragment();
        Bundle b = new Bundle();
        b.putString(ARG_MEAL_TYPE, mealType);
        f.setArguments(b);
        return f;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = new dbConnect(requireContext());
        userId = new SessionManager(requireContext()).getUserId();

        ImageView btnBack = view.findViewById(R.id.btnBack);
        tvTitle = view.findViewById(R.id.tvMealTitle);
        tvSub = view.findViewById(R.id.tvMealSub);
        tvTotals = view.findViewById(R.id.tvMealTotals);
        rvMealFoods = view.findViewById(R.id.rvMealFoods);

        Bundle b = getArguments();
        if (b != null) mealType = b.getString(ARG_MEAL_TYPE, "breakfast");

        String title = mealType.substring(0, 1).toUpperCase()
                + mealType.substring(1).toLowerCase();
        tvTitle.setText(title);

        rvMealFoods.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new MealFoodsAdapter(data, row -> {
            if (userId == -1) return;

            db.deleteFoodLog(userId, row.id);
            refreshUI();

            showSnack(requireView(), "Removed from " + mealType + " 🗑️", true);
        });

        rvMealFoods.setAdapter(adapter);

        refreshUI();

        btnBack.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack()
        );
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshUI();
    }

    private void refreshUI() {
        if (userId == -1) return;

        dbConnect.Totals t = db.getMealTotals(userId, mealType);
        List<dbConnect.LoggedFoodRow> list = db.getMealItems(userId, mealType);

        tvSub.setText(t.items + " items • " + t.calories + " kcal");

        tvTotals.setText(String.format(Locale.ROOT,
                "Protein: %.1fg  |  Fat: %.1fg  |  Carbs: %.1fg",
                t.protein, t.fat, t.carbs));

        data.clear();
        data.addAll(list);
        adapter.notifyDataSetChanged();
    }

    // ================= Snackbar helper =================
    private void showSnack(View anchor, String message, boolean isError) {
        Snackbar snackbar = Snackbar.make(anchor, message, Snackbar.LENGTH_SHORT);
        snackbar.setDuration(1000); // 1 секунда


        snackbar.setTextColor(Color.WHITE);
        snackbar.setBackgroundTint(
                Color.parseColor(isError ? "#E53935" : "#4CAF50")
        );

        View snackbarView = snackbar.getView();
        try {
            android.widget.FrameLayout.LayoutParams params =
                    (android.widget.FrameLayout.LayoutParams) snackbarView.getLayoutParams();
            params.setMargins(40, 0, 40, 40);
            snackbarView.setLayoutParams(params);
        } catch (Exception ignored) {}

        snackbarView.setBackground(
                requireContext().getDrawable(R.drawable.bg_chip_protein)
        );

        snackbar.show();
    }

    // ---------------- Adapter ----------------
    static class MealFoodsAdapter extends RecyclerView.Adapter<MealFoodsAdapter.VH> {

        interface OnRemoveClick {
            void onRemove(dbConnect.LoggedFoodRow row);
        }

        private final List<dbConnect.LoggedFoodRow> items;
        private final OnRemoveClick onRemove;

        MealFoodsAdapter(List<dbConnect.LoggedFoodRow> items, OnRemoveClick onRemove) {
            this.items = items;
            this.onRemove = onRemove;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_logged_food, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            dbConnect.LoggedFoodRow f = items.get(position);

            holder.tvTitle.setText(f.name);

            holder.tvSub.setText(String.format(Locale.ROOT,
                    "%.0fg • %.2f portions • %d kcal",
                    f.grams, f.portions, f.calories));

            holder.tvMacros.setText(String.format(Locale.ROOT,
                    "Protein: %.1fg | Fats: %.1fg | Carbs: %.1fg",
                    f.protein, f.fat, f.carbs));

            holder.btnRemove.setOnClickListener(v -> {
                int pos = holder.getAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;
                if (onRemove != null) onRemove.onRemove(items.get(pos));
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvTitle, tvSub, tvMacros;
            ImageView btnRemove;

            VH(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvTitle);
                tvSub = itemView.findViewById(R.id.tvSub);
                tvMacros = itemView.findViewById(R.id.tvMacros);
                btnRemove = itemView.findViewById(R.id.btnRemove);
            }
        }
    }
}