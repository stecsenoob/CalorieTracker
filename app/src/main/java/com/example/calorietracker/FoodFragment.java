package com.example.calorietracker;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FoodFragment extends Fragment {

    private TextInputEditText etSearchFood;
    private RecyclerView rvFoods;
    private MaterialButton btnAddFood;

    private FoodAdapter adapter;
    private final List<FoodItem> allFoods = new ArrayList<>();

    private SessionManager sm;
    private dbConnect db;

    public FoodFragment() {
        super(R.layout.fragment_food);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sm = new SessionManager(requireContext());
        db = new dbConnect(requireContext());

        etSearchFood = view.findViewById(R.id.etSearchFood);
        rvFoods = view.findViewById(R.id.rvFoods);
        btnAddFood = view.findViewById(R.id.btnAddFood);

        adapter = new FoodAdapter(this::openFoodDetail);

        rvFoods.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvFoods.setAdapter(adapter);

        // ✅ Search filter (no SearchView)
        etSearchFood.addTextChangedListener(new SimpleTextWatcher(text -> adapter.filter(text)));

        btnAddFood.setOnClickListener(v -> showAddFoodDialog());

        loadFoods();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadFoods(); // refresh hearts + list
    }

    private void loadFoods() {
        allFoods.clear();

        // 1) seed foods (global)
        seedFoods();

        // 2) user foods from DB
        int userId = sm.getUserId();
        if (userId != -1) {
            List<dbConnect.UserFoodRow> custom = db.getUserFoods(userId);
            for (dbConnect.UserFoodRow r : custom) {
                allFoods.add(new FoodItem(r.name, r.portion, r.baseGrams, r.calories, r.protein, r.fat, r.carbs));
            }
        }

        adapter.setData(allFoods);

        // re-apply current search
        String q = (etSearchFood.getText() == null) ? "" : etSearchFood.getText().toString();
        adapter.filter(q);
    }

    private void showAddFoodDialog() {
        int userId = sm.getUserId();
        if (userId == -1) {
            showSnack(requireView(), "Not logged in", true);
            return;
        }

        View v = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_food, null, false);

        TextInputEditText etName = v.findViewById(R.id.etName);
        TextInputEditText etPortion = v.findViewById(R.id.etPortion);
        TextInputEditText etGrams = v.findViewById(R.id.etGrams);
        TextInputEditText etKcal = v.findViewById(R.id.etKcal);
        TextInputEditText etProtein = v.findViewById(R.id.etProtein);
        TextInputEditText etFat = v.findViewById(R.id.etFat);
        TextInputEditText etCarbs = v.findViewById(R.id.etCarbs);

        etPortion.setText("100g");
        etGrams.setText("100");

        AlertDialog d = new AlertDialog.Builder(requireContext())
                .setTitle("Add Food")
                .setView(v)
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Save", null)
                .create();

        d.setOnShowListener(dialog -> {
            d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(btn -> {

                String name = text(etName).trim();
                String portion = text(etPortion).trim();

                float grams = parseFloat(text(etGrams), 100f);
                int kcal = (int) parseFloat(text(etKcal), 0f);
                float p = parseFloat(text(etProtein), 0f);
                float f = parseFloat(text(etFat), 0f);
                float c = parseFloat(text(etCarbs), 0f);

                if (name.isEmpty()) { etName.setError("Required"); return; }
                if (portion.isEmpty()) { etPortion.setError("Required"); return; }
                if (grams <= 0) { etGrams.setError("Must be > 0"); return; }
                if (kcal < 0) { etKcal.setError("Must be >= 0"); return; }

                long id = db.addUserFood(userId, name, portion, grams, kcal, p, f, c);

                if (id != -1) {
                    showSnack(requireView(), "Food added ✅", false);
                    d.dismiss();
                    loadFoods();
                } else {
                    showSnack(requireView(), "Failed to save", true);
                }
            });
        });

        d.show();
    }

    private void openFoodDetail(FoodItem item) {
        Bundle args = new Bundle();
        args.putString(FoodDetailFragment.ARG_FOOD_NAME, item.name);
        args.putString(FoodDetailFragment.ARG_FOOD_PORTION, item.portion);
        args.putFloat(FoodDetailFragment.ARG_FOOD_BASE_GRAMS, item.baseGrams);

        args.putInt(FoodDetailFragment.ARG_FOOD_CAL, item.calories);
        args.putFloat(FoodDetailFragment.ARG_FOOD_P, item.protein);
        args.putFloat(FoodDetailFragment.ARG_FOOD_F, item.fat);
        args.putFloat(FoodDetailFragment.ARG_FOOD_C, item.carbs);

        FoodDetailFragment fragment = new FoodDetailFragment();
        fragment.setArguments(args);

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frameLayout, fragment)
                .addToBackStack(null)
                .commit();
    }

    // ================= Snackbar helper =================
    private void showSnack(View anchor, String message, boolean isError) {
        Snackbar snackbar = Snackbar.make(anchor, message, Snackbar.LENGTH_SHORT);
        snackbar.setDuration(1000); // 1 секунда


        snackbar.setTextColor(Color.WHITE);
        snackbar.setBackgroundTint(Color.parseColor(isError ? "#E53935" : "#4CAF50"));

        View snackbarView = snackbar.getView();
        try {
            android.widget.FrameLayout.LayoutParams params =
                    (android.widget.FrameLayout.LayoutParams) snackbarView.getLayoutParams();
            params.setMargins(40, 0, 40, 40);
            snackbarView.setLayoutParams(params);
        } catch (Exception ignored) {}

        snackbarView.setBackground(requireContext().getDrawable(R.drawable.bg_chip_protein));
        snackbar.show();
    }

    private void seedFoods() {
        // (истото како кај тебе - не го менувам)
        // ======================= FRUITS =======================
        allFoods.add(new FoodItem("Apple", "1 medium (182g)", 182f, 95, 0.5f, 0.3f, 25f));
        allFoods.add(new FoodItem("Banana", "1 medium (118g)", 118f, 105, 1.3f, 0.4f, 27f));
        allFoods.add(new FoodItem("Orange", "1 medium (131g)", 131f, 62, 1.2f, 0.2f, 15f));
        allFoods.add(new FoodItem("Lemon", "1 lemon (58g)", 58f, 17, 0.6f, 0.2f, 5.4f));
        allFoods.add(new FoodItem("Lime", "1 lime (67g)", 67f, 20, 0.5f, 0.1f, 7.1f));
        allFoods.add(new FoodItem("Grapefruit", "1/2 medium (123g)", 123f, 52, 1.0f, 0.2f, 13f));
        allFoods.add(new FoodItem("Strawberry", "1 cup (152g)", 152f, 49, 1.0f, 0.5f, 12f));
        allFoods.add(new FoodItem("Blueberry", "1 cup (148g)", 148f, 84, 1.1f, 0.5f, 21f));
        allFoods.add(new FoodItem("Raspberry", "1 cup (123g)", 123f, 64, 1.5f, 0.8f, 15f));
        allFoods.add(new FoodItem("Blackberry", "1 cup (144g)", 144f, 62, 2.0f, 0.7f, 14f));
        allFoods.add(new FoodItem("Grape", "1 cup (151g)", 151f, 104, 1.1f, 0.2f, 27f));
        allFoods.add(new FoodItem("Pineapple", "1 cup (165g)", 165f, 82, 0.9f, 0.2f, 22f));
        allFoods.add(new FoodItem("Mango", "1 cup (165g)", 165f, 99, 1.4f, 0.6f, 25f));
        allFoods.add(new FoodItem("Papaya", "1 cup (140g)", 140f, 55, 0.9f, 0.4f, 14f));
        allFoods.add(new FoodItem("Kiwi", "1 kiwi (69g)", 69f, 42, 0.8f, 0.4f, 10f));
        allFoods.add(new FoodItem("Peach", "1 medium (150g)", 150f, 59, 1.4f, 0.4f, 14f));
        allFoods.add(new FoodItem("Pear", "1 medium (178g)", 178f, 101, 0.6f, 0.2f, 27f));
        allFoods.add(new FoodItem("Plum", "2 plums (132g)", 132f, 61, 0.9f, 0.4f, 15f));
        allFoods.add(new FoodItem("Cherry", "1 cup (154g)", 154f, 97, 1.6f, 0.3f, 25f));
        allFoods.add(new FoodItem("Apricot", "3 apricots (105g)", 105f, 50, 1.4f, 0.4f, 12f));
        allFoods.add(new FoodItem("Watermelon", "1 cup (152g)", 152f, 46, 0.9f, 0.2f, 11.5f));
        allFoods.add(new FoodItem("Cantaloupe", "1 cup (160g)", 160f, 54, 1.3f, 0.3f, 13f));
        allFoods.add(new FoodItem("Honeydew melon", "1 cup (177g)", 177f, 64, 1.0f, 0.2f, 16f));
        allFoods.add(new FoodItem("Pomegranate", "1/2 cup arils (87g)", 87f, 72, 1.5f, 1.0f, 16f));
        allFoods.add(new FoodItem("Fig", "2 medium (100g)", 100f, 74, 0.8f, 0.3f, 19f));
        allFoods.add(new FoodItem("Coconut", "1/2 cup shredded (40g)", 40f, 140, 1.5f, 13f, 6f));
        allFoods.add(new FoodItem("Avocado", "1/2 avocado (100g)", 100f, 160, 2.0f, 15f, 9f));

        // ... продолжи си со твојот seedFoods како што беше (не го скратувам намерно тука)
        // (остави го остатокот од листата ист)
    }

    // -------------------- MODEL --------------------
    static class FoodItem {
        final String name;
        final String portion;
        final float baseGrams;
        final int calories;
        final float protein;
        final float fat;
        final float carbs;

        FoodItem(String name, String portion, float baseGrams, int calories, float protein, float fat, float carbs) {
            this.name = name;
            this.portion = portion;
            this.baseGrams = baseGrams;
            this.calories = calories;
            this.protein = protein;
            this.fat = fat;
            this.carbs = carbs;
        }
    }

    // -------------------- ADAPTER --------------------
    class FoodAdapter extends RecyclerView.Adapter<FoodAdapter.FoodVH> {

        interface OnFoodClick { void onClick(FoodItem item); }

        private final List<FoodItem> original = new ArrayList<>();
        private final List<FoodItem> filtered = new ArrayList<>();
        private final OnFoodClick listener;

        FoodAdapter(OnFoodClick listener) {
            this.listener = listener;
        }

        void setData(List<FoodItem> items) {
            original.clear();
            filtered.clear();
            if (items != null) {
                original.addAll(items);
                filtered.addAll(items);
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public FoodVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_food_card, parent, false);
            return new FoodVH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull FoodVH holder, int position) {
            FoodItem item = filtered.get(position);

            holder.tvTitle.setText(item.name);

            // ✅ 2 lines
            String line1 = item.portion + " • " + item.calories + " kcal";
            String line2 = "Protein: " + item.protein + "g | Fat: " + item.fat + "g | Carbs: " + item.carbs + "g";
            holder.tvSub.setText(line1 + "\n" + line2);

            int userId = sm.getUserId();
            String key = makeFoodKey(item);

            boolean fav = (userId != -1) && db.isFavorite(userId, key);
            paintHeart(holder.ivFav, fav);

            holder.ivFav.setOnClickListener(v -> {
                int uid = sm.getUserId();
                if (uid == -1) {
                    showSnack(requireView(), "Not logged in", true);
                    return;
                }

                boolean nowFav = db.isFavorite(uid, key);
                if (nowFav) {
                    db.removeFavorite(uid, key);
                    paintHeart(holder.ivFav, false);
                    showSnack(requireView(), "Removed from favorites 💔", true); // 🔴
                } else {
                    db.addFavorite(uid, key,
                            item.name, item.portion, item.baseGrams,
                            item.calories, item.protein, item.fat, item.carbs);
                    paintHeart(holder.ivFav, true);
                    showSnack(requireView(), "Added to favorites ❤️", false); // 🟢
                }
            });

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onClick(item);
            });
        }

        @Override
        public int getItemCount() { return filtered.size(); }

        void filter(String query) {
            String q = (query == null) ? "" : query.trim().toLowerCase(Locale.ROOT);
            filtered.clear();
            if (q.isEmpty()) filtered.addAll(original);
            else {
                for (FoodItem item : original) {
                    if (item.name.toLowerCase(Locale.ROOT).contains(q)) filtered.add(item);
                }
            }
            notifyDataSetChanged();
        }

        class FoodVH extends RecyclerView.ViewHolder {
            TextView tvTitle, tvSub;
            ImageView ivFav;

            FoodVH(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvTitle);
                tvSub = itemView.findViewById(R.id.tvSub);
                ivFav = itemView.findViewById(R.id.ivFav);
            }
        }
    }

    // ---------- Favorites helpers ----------
    private String makeFoodKey(FoodItem item) {
        return item.name + "|" + item.portion + "|" + item.baseGrams + "|" +
                item.calories + "|" + item.protein + "|" + item.fat + "|" + item.carbs;
    }

    private void paintHeart(ImageView iv, boolean fav) {
        if (iv == null) return;
        if (fav) {
            iv.setImageResource(R.drawable.baseline_favorite_24);
            iv.setColorFilter(0xFFE53935); // red
        } else {
            iv.setImageResource(R.drawable.outline_favorite_24);
            iv.setColorFilter(0xFFB0B0B0); // gray
        }
    }

    // ---------- small utils ----------
    private static String text(TextInputEditText et) {
        return (et.getText() == null) ? "" : et.getText().toString();
    }

    private static float parseFloat(String s, float def) {
        try {
            if (s == null) return def;
            s = s.trim();
            if (s.isEmpty()) return def;
            return Float.parseFloat(s);
        } catch (Exception e) {
            return def;
        }
    }

    // ✅ FIXED TextWatcher (now compiles)
    static class SimpleTextWatcher implements TextWatcher {
        interface OnTextChanged { void onChanged(String text); }
        private final OnTextChanged cb;

        SimpleTextWatcher(OnTextChanged cb) { this.cb = cb; }

        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {
            if (cb != null) cb.onChanged(s == null ? "" : s.toString());
        }
    }
}