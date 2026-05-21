package com.example.calorietracker;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FoodFragment extends Fragment {

    private TextInputEditText etSearchFood;
    private RecyclerView rvFoods;
    private MaterialButton btnAddFood;
    private LinearLayout categoryContainer;

    private FoodAdapter adapter;
    private final List<FoodItem> allFoods = new ArrayList<>();

    private SessionManager sm;
    private dbConnect db;

    private static final String CATEGORY_ALL = "All";

    private final String[] categories = {
            CATEGORY_ALL,
            "Eggs",
            "Meat",
            "Fish",
            "Dairy",
            "Grains",
            "Bread",
            "Vegetables",
            "Legumes",
            "Fruit",
            "Nuts & Seeds",
            "Sweets & Snacks",
            "Traditional/Prepared",
            "Drinks",
            "Oils & Sauces"
    };

    private String selectedCategory = CATEGORY_ALL;

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
        categoryContainer = view.findViewById(R.id.categoryContainer);

        adapter = new FoodAdapter(this::openFoodDetail);

        rvFoods.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvFoods.setAdapter(adapter);

        setupCategoryChips();

        etSearchFood.addTextChangedListener(new SimpleTextWatcher(text -> {
            adapter.setSearchQuery(text);
            adapter.setSelectedCategory(selectedCategory);
        }));

        btnAddFood.setOnClickListener(v -> showAddFoodBottomSheet());

        loadFoods();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadFoods();
    }

    private void setupCategoryChips() {
        categoryContainer.removeAllViews();

        for (String category : categories) {
            TextView chip = new TextView(requireContext());
            chip.setText(category);
            chip.setTextSize(13f);
            chip.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            chip.setSingleLine(true);
            chip.setGravity(android.view.Gravity.CENTER);
            chip.setPadding(dp(14), dp(8), dp(14), dp(8));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            lp.setMarginEnd(dp(8));
            chip.setLayoutParams(lp);

            chip.setOnClickListener(v -> {
                selectedCategory = category;
                updateCategoryChips();
                adapter.setSelectedCategory(selectedCategory);
            });

            categoryContainer.addView(chip);
        }

        updateCategoryChips();
    }

    private void updateCategoryChips() {
        for (int i = 0; i < categoryContainer.getChildCount(); i++) {
            View child = categoryContainer.getChildAt(i);

            if (child instanceof TextView) {
                TextView chip = (TextView) child;
                boolean selected = chip.getText().toString().equals(selectedCategory);

                chip.setTextColor(selected ? Color.WHITE : Color.parseColor("#5D4A6F"));
                chip.setBackground(makeCategoryChipBackground(selected));
            }
        }
    }

    private GradientDrawable makeCategoryChipBackground(boolean selected) {
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dp(18));

        if (selected) {
            bg.setColor(Color.parseColor("#6A0DAD"));
            bg.setStroke(dp(1), Color.parseColor("#6A0DAD"));
        } else {
            bg.setColor(Color.parseColor("#F4ECFB"));
            bg.setStroke(dp(1), Color.parseColor("#E7DFF6"));
        }

        return bg;
    }

    private void loadFoods() {
        allFoods.clear();

        seedFoods();

        int userId = sm.getUserId();

        if (userId != -1) {
            List<dbConnect.UserFoodRow> custom = db.getUserFoods(userId);

            for (dbConnect.UserFoodRow r : custom) {
                allFoods.add(new FoodItem(
                        r.id,
                        true,
                        r.name,
                        r.portion,
                        r.baseGrams,
                        r.calories,
                        r.protein,
                        r.fat,
                        r.carbs,
                        getCustomFoodImageResOrCategory(r.name, r.category),
                        r.category
                ));
            }
        }

        adapter.setData(allFoods);

        String q = (etSearchFood.getText() == null) ? "" : etSearchFood.getText().toString();
        adapter.setSearchQuery(q);
        adapter.setSelectedCategory(selectedCategory);
    }

    private void showAddFoodBottomSheet() {
        int userId = sm.getUserId();

        if (userId == -1) {
            showSimpleDialog("Not Logged In", "Please log in before adding food.");
            return;
        }

        BottomSheetDialog sheet = new BottomSheetDialog(requireContext());
        View v = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_add_food, null, false);

        TextInputEditText etName = v.findViewById(R.id.etName);
        AutoCompleteTextView actCategory = v.findViewById(R.id.actCategory);
        TextInputEditText etPortion = v.findViewById(R.id.etPortion);
        TextInputEditText etGrams = v.findViewById(R.id.etGrams);
        TextInputEditText etKcal = v.findViewById(R.id.etKcal);
        TextInputEditText etProtein = v.findViewById(R.id.etProtein);
        TextInputEditText etFat = v.findViewById(R.id.etFat);
        TextInputEditText etCarbs = v.findViewById(R.id.etCarbs);

        MaterialButton btnCancel = v.findViewById(R.id.btnCancelAddFood);
        MaterialButton btnSave = v.findViewById(R.id.btnSaveAddFood);

        String[] addFoodCategories = {
                "Eggs",
                "Meat",
                "Fish",
                "Dairy",
                "Grains",
                "Bread",
                "Vegetables",
                "Legumes",
                "Fruit",
                "Nuts & Seeds",
                "Sweets & Snacks",
                "Traditional/Prepared",
                "Drinks",
                "Oils & Sauces"
        };

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                addFoodCategories
        );

        actCategory.setAdapter(categoryAdapter);
        actCategory.setText("Traditional/Prepared", false);

        etPortion.setText("100g");
        etGrams.setText("100");

        btnCancel.setOnClickListener(view -> sheet.dismiss());

        btnSave.setOnClickListener(view -> {
            String name = text(etName).trim();
            String category = actCategory.getText() == null ? "" : actCategory.getText().toString().trim();
            String portion = text(etPortion).trim();

            float grams = parseFloat(text(etGrams), 100f);
            int kcal = (int) parseFloat(text(etKcal), 0f);
            float p = parseFloat(text(etProtein), 0f);
            float f = parseFloat(text(etFat), 0f);
            float c = parseFloat(text(etCarbs), 0f);

            if (name.isEmpty()) {
                etName.setError("Required");
                return;
            }

            if (category.isEmpty()) {
                actCategory.setError("Required");
                return;
            }

            if (!isValidCategory(category)) {
                actCategory.setError("Choose a valid category");
                return;
            }

            if (portion.isEmpty()) {
                etPortion.setError("Required");
                return;
            }

            if (grams <= 0) {
                etGrams.setError("Must be > 0");
                return;
            }

            if (kcal < 0) {
                etKcal.setError("Must be >= 0");
                return;
            }

            long id = db.addUserFood(userId, name, portion, grams, kcal, p, f, c, category);

            if (id != -1) {
                sheet.dismiss();
                loadFoods();

                selectedCategory = category;
                updateCategoryChips();
                adapter.setSelectedCategory(selectedCategory);
            } else {
                showSimpleDialog("Save Failed", "The food could not be saved. Please try again.");
            }
        });

        sheet.setContentView(v);
        sheet.show();
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

    private void confirmDeleteUserFood(FoodItem item) {
        int userId = sm.getUserId();

        if (userId == -1) {
            showSimpleDialog("Not Logged In", "Please log in before deleting food.");
            return;
        }

        if (!item.isCustom || item.userFoodId == -1) {
            showSimpleDialog("Cannot Delete", "Default foods cannot be deleted.");
            return;
        }

        View v = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_delete_food, null, false);

        TextView tvDeleteMessage = v.findViewById(R.id.tvDeleteMessage);
        MaterialButton btnCancelDelete = v.findViewById(R.id.btnCancelDelete);
        MaterialButton btnConfirmDelete = v.findViewById(R.id.btnConfirmDelete);

        tvDeleteMessage.setText("Delete \"" + item.name + "\" from My Foods?");

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(v)
                .create();

        btnCancelDelete.setOnClickListener(view -> dialog.dismiss());

        btnConfirmDelete.setOnClickListener(view -> {
            db.deleteUserFood(userId, item.userFoodId);
            dialog.dismiss();
            loadFoods();
        });

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    private void showSimpleDialog(String title, String message) {
        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private boolean isValidCategory(String category) {
        if (category == null) return false;

        for (String c : categories) {
            if (!c.equals(CATEGORY_ALL) && c.equals(category)) {
                return true;
            }
        }

        return false;
    }

    private void seedFoods() {
        allFoods.add(new FoodItem("Boiled egg", "100g", 100f, 155, 13.0f, 10.6f, 1.1f, R.drawable.food_boiled_egg));
        allFoods.add(new FoodItem("Fried egg", "100g", 100f, 196, 13.6f, 15.3f, 0.8f, R.drawable.food_fried_egg));
        allFoods.add(new FoodItem("Scrambled eggs", "100g", 100f, 149, 10.0f, 11.0f, 1.6f, R.drawable.food_scrambled_eggs));
        allFoods.add(new FoodItem("Plain omelette", "100g", 100f, 154, 10.6f, 12.0f, 1.9f, R.drawable.food_plain_omelette));
        allFoods.add(new FoodItem("Egg white", "100g", 100f, 52, 10.9f, 0.2f, 0.7f, R.drawable.food_egg_white));
        allFoods.add(new FoodItem("Chicken breast cooked", "100g", 100f, 165, 31.0f, 3.6f, 0.0f, R.drawable.food_chicken_breast_cooked));
        allFoods.add(new FoodItem("Chicken thigh cooked", "100g", 100f, 209, 26.0f, 10.9f, 0.0f, R.drawable.food_chicken_thigh_cooked));
        allFoods.add(new FoodItem("Chicken drumstick cooked", "100g", 100f, 172, 28.0f, 6.2f, 0.0f, R.drawable.food_chicken_drumstick_cooked));
        allFoods.add(new FoodItem("Turkey breast cooked", "100g", 100f, 135, 29.0f, 1.0f, 0.0f, R.drawable.food_turkey_breast_cooked));
        allFoods.add(new FoodItem("Beef steak grilled", "100g", 100f, 271, 25.0f, 19.0f, 0.0f, R.drawable.food_beef_steak_grilled));
        allFoods.add(new FoodItem("Ground beef cooked 85% lean", "100g", 100f, 250, 26.0f, 15.0f, 0.0f, R.drawable.food_ground_beef_cooked_85_lean));
        allFoods.add(new FoodItem("Pork chop cooked", "100g", 100f, 231, 25.7f, 13.9f, 0.0f, R.drawable.food_pork_chop_cooked));
        allFoods.add(new FoodItem("Ham", "100g", 100f, 145, 21.0f, 5.5f, 1.5f, R.drawable.food_ham));
        allFoods.add(new FoodItem("Chicken sausage", "100g", 100f, 172, 17.0f, 10.0f, 2.0f, R.drawable.food_chicken_sausage));
        allFoods.add(new FoodItem("Beef sausage", "100g", 100f, 332, 14.0f, 30.0f, 2.0f, R.drawable.food_beef_sausage));
        allFoods.add(new FoodItem("Hot dog", "100g", 100f, 290, 10.0f, 26.0f, 4.2f, R.drawable.food_hot_dog));
        allFoods.add(new FoodItem("Salami", "100g", 100f, 407, 22.6f, 33.7f, 1.6f, R.drawable.food_salami));
        allFoods.add(new FoodItem("Bacon cooked", "100g", 100f, 541, 37.0f, 42.0f, 1.4f, R.drawable.food_bacon_cooked));
        allFoods.add(new FoodItem("Meatballs", "100g", 100f, 202, 13.5f, 13.0f, 7.0f, R.drawable.food_meatballs));
        allFoods.add(new FoodItem("Grilled burger patty", "100g", 100f, 254, 17.0f, 20.0f, 0.0f, R.drawable.food_grilled_burger_patty));
        allFoods.add(new FoodItem("Tuna canned in water", "100g", 100f, 116, 25.5f, 0.8f, 0.0f, R.drawable.food_tuna_canned_in_water));
        allFoods.add(new FoodItem("Tuna canned in oil", "100g", 100f, 198, 29.0f, 8.2f, 0.0f, R.drawable.food_tuna_canned_in_oil));
        allFoods.add(new FoodItem("Salmon cooked", "100g", 100f, 208, 20.4f, 13.4f, 0.0f, R.drawable.food_salmon_cooked));
        allFoods.add(new FoodItem("Sardines canned", "100g", 100f, 208, 24.6f, 11.5f, 0.0f, R.drawable.food_sardines_canned));
        allFoods.add(new FoodItem("Trout cooked", "100g", 100f, 190, 26.6f, 8.5f, 0.0f, R.drawable.food_trout_cooked));
        allFoods.add(new FoodItem("Mackerel cooked", "100g", 100f, 262, 24.0f, 18.0f, 0.0f, R.drawable.food_mackerel_cooked));
        allFoods.add(new FoodItem("Cod cooked", "100g", 100f, 105, 23.0f, 0.9f, 0.0f, R.drawable.food_cod_cooked));
        allFoods.add(new FoodItem("Shrimp cooked", "100g", 100f, 99, 24.0f, 0.3f, 0.2f, R.drawable.food_shrimp_cooked));
        allFoods.add(new FoodItem("Milk 3.25%", "100g", 100f, 61, 3.2f, 3.3f, 4.8f, R.drawable.food_milk_3_25));
        allFoods.add(new FoodItem("Milk 1%", "100g", 100f, 42, 3.4f, 1.0f, 5.0f, R.drawable.food_milk_1));
        allFoods.add(new FoodItem("Greek yogurt plain", "100g", 100f, 97, 9.0f, 5.0f, 3.9f, R.drawable.food_greek_yogurt_plain));
        allFoods.add(new FoodItem("Yogurt plain low-fat", "100g", 100f, 63, 5.3f, 1.6f, 7.0f, R.drawable.food_yogurt_plain_low_fat));
        allFoods.add(new FoodItem("Kefir plain", "100g", 100f, 41, 3.8f, 1.0f, 4.5f, R.drawable.food_kefir_plain));
        allFoods.add(new FoodItem("Cottage cheese", "100g", 100f, 98, 11.1f, 4.3f, 3.4f, R.drawable.food_cottage_cheese));
        allFoods.add(new FoodItem("Feta cheese", "100g", 100f, 264, 14.2f, 21.3f, 4.1f, R.drawable.food_feta_cheese));
        allFoods.add(new FoodItem("Mozzarella cheese", "100g", 100f, 280, 28.0f, 17.0f, 3.1f, R.drawable.food_mozzarella_cheese));
        allFoods.add(new FoodItem("Cheddar cheese", "100g", 100f, 403, 25.0f, 33.0f, 1.3f, R.drawable.food_cheddar_cheese));
        allFoods.add(new FoodItem("Parmesan cheese", "100g", 100f, 431, 38.0f, 29.0f, 4.1f, R.drawable.food_parmesan_cheese));
        allFoods.add(new FoodItem("Cream cheese", "100g", 100f, 342, 6.2f, 34.0f, 4.1f, R.drawable.food_cream_cheese));
        allFoods.add(new FoodItem("Sour cream", "100g", 100f, 198, 2.4f, 19.0f, 4.6f, R.drawable.food_sour_cream));
        allFoods.add(new FoodItem("White rice cooked", "100g", 100f, 130, 2.7f, 0.3f, 28.2f, R.drawable.food_white_rice_cooked));
        allFoods.add(new FoodItem("Brown rice cooked", "100g", 100f, 123, 2.7f, 1.0f, 25.6f, R.drawable.food_brown_rice_cooked));
        allFoods.add(new FoodItem("Pasta cooked", "100g", 100f, 158, 5.8f, 0.9f, 30.9f, R.drawable.food_pasta_cooked));
        allFoods.add(new FoodItem("Spaghetti cooked", "100g", 100f, 158, 5.8f, 0.9f, 30.9f, R.drawable.food_spaghetti_cooked));
        allFoods.add(new FoodItem("Macaroni cooked", "100g", 100f, 164, 5.8f, 0.9f, 31.0f, R.drawable.food_macaroni_cooked));
        allFoods.add(new FoodItem("Oats dry", "100g", 100f, 389, 16.9f, 6.9f, 66.3f, R.drawable.food_oats_dry));
        allFoods.add(new FoodItem("Oatmeal cooked with water", "100g", 100f, 71, 2.5f, 1.5f, 12.0f, R.drawable.food_oatmeal_cooked_with_water));
        allFoods.add(new FoodItem("Cornflakes", "100g", 100f, 357, 7.5f, 0.4f, 84.0f, R.drawable.food_cornflakes));
        allFoods.add(new FoodItem("Muesli", "100g", 100f, 340, 10.0f, 5.0f, 66.0f, R.drawable.food_muesli));
        allFoods.add(new FoodItem("Granola", "100g", 100f, 471, 10.0f, 20.0f, 64.0f, R.drawable.food_granola));
        allFoods.add(new FoodItem("White bread", "100g", 100f, 265, 9.0f, 3.2f, 49.0f, R.drawable.food_white_bread));
        allFoods.add(new FoodItem("Whole wheat bread", "100g", 100f, 247, 13.0f, 4.2f, 41.0f, R.drawable.food_whole_wheat_bread));
        allFoods.add(new FoodItem("Rye bread", "100g", 100f, 259, 8.5f, 3.3f, 48.0f, R.drawable.food_rye_bread));
        allFoods.add(new FoodItem("Toast bread", "100g", 100f, 265, 9.0f, 3.2f, 49.0f, R.drawable.food_toast_bread));
        allFoods.add(new FoodItem("Bagel plain", "100g", 100f, 250, 10.0f, 1.5f, 49.0f, R.drawable.food_bagel_plain));
        allFoods.add(new FoodItem("Tortilla wheat", "100g", 100f, 306, 8.2f, 7.9f, 49.0f, R.drawable.food_tortilla_wheat));
        allFoods.add(new FoodItem("Pita bread", "100g", 100f, 275, 9.1f, 1.2f, 55.7f, R.drawable.food_pita_bread));
        allFoods.add(new FoodItem("Croissant", "100g", 100f, 406, 8.2f, 21.0f, 45.8f, R.drawable.food_croissant));
        allFoods.add(new FoodItem("Pancakes plain", "100g", 100f, 227, 6.4f, 9.7f, 28.3f, R.drawable.food_pancakes_plain));
        allFoods.add(new FoodItem("Waffles plain", "100g", 100f, 291, 7.9f, 14.1f, 32.9f, R.drawable.food_waffles_plain));
        allFoods.add(new FoodItem("Potato boiled", "100g", 100f, 87, 1.9f, 0.1f, 20.1f, R.drawable.food_potato_boiled));
        allFoods.add(new FoodItem("Potato baked", "100g", 100f, 93, 2.5f, 0.1f, 21.2f, R.drawable.food_potato_baked));
        allFoods.add(new FoodItem("French fries", "100g", 100f, 312, 3.4f, 15.0f, 41.0f, R.drawable.food_french_fries));
        allFoods.add(new FoodItem("Sweet potato cooked", "100g", 100f, 86, 1.6f, 0.1f, 20.1f, R.drawable.food_sweet_potato_cooked));
        allFoods.add(new FoodItem("Tomato", "100g", 100f, 18, 0.9f, 0.2f, 3.9f, R.drawable.food_tomato));
        allFoods.add(new FoodItem("Cucumber", "100g", 100f, 15, 0.7f, 0.1f, 3.6f, R.drawable.food_cucumber));
        allFoods.add(new FoodItem("Lettuce", "100g", 100f, 15, 1.4f, 0.2f, 2.9f, R.drawable.food_lettuce));
        allFoods.add(new FoodItem("Spinach raw", "100g", 100f, 23, 2.9f, 0.4f, 3.6f, R.drawable.food_spinach_raw));
        allFoods.add(new FoodItem("Cabbage raw", "100g", 100f, 25, 1.3f, 0.1f, 5.8f, R.drawable.food_cabbage_raw));
        allFoods.add(new FoodItem("Carrot raw", "100g", 100f, 41, 0.9f, 0.2f, 9.6f, R.drawable.food_carrot_raw));
        allFoods.add(new FoodItem("Broccoli cooked", "100g", 100f, 35, 2.4f, 0.4f, 7.2f, R.drawable.food_broccoli_cooked));
        allFoods.add(new FoodItem("Cauliflower cooked", "100g", 100f, 23, 1.8f, 0.5f, 4.1f, R.drawable.food_cauliflower_cooked));
        allFoods.add(new FoodItem("Zucchini cooked", "100g", 100f, 17, 1.2f, 0.3f, 3.1f, R.drawable.food_zucchini_cooked));
        allFoods.add(new FoodItem("Bell pepper", "100g", 100f, 31, 1.0f, 0.3f, 6.0f, R.drawable.food_bell_pepper));
        allFoods.add(new FoodItem("Onion", "100g", 100f, 40, 1.1f, 0.1f, 9.3f, R.drawable.food_onion));
        allFoods.add(new FoodItem("Garlic", "100g", 100f, 149, 6.4f, 0.5f, 33.1f, R.drawable.food_garlic));
        allFoods.add(new FoodItem("Mushrooms raw", "100g", 100f, 22, 3.1f, 0.3f, 3.3f, R.drawable.food_mushrooms_raw));
        allFoods.add(new FoodItem("Corn cooked", "100g", 100f, 96, 3.4f, 1.5f, 21.0f, R.drawable.food_corn_cooked));
        allFoods.add(new FoodItem("Green peas cooked", "100g", 100f, 84, 5.4f, 0.4f, 15.6f, R.drawable.food_green_peas_cooked));
        allFoods.add(new FoodItem("Beans cooked", "100g", 100f, 127, 8.7f, 0.5f, 22.8f, R.drawable.food_beans_cooked));
        allFoods.add(new FoodItem("Lentils cooked", "100g", 100f, 116, 9.0f, 0.4f, 20.1f, R.drawable.food_lentils_cooked));
        allFoods.add(new FoodItem("Chickpeas cooked", "100g", 100f, 164, 8.9f, 2.6f, 27.4f, R.drawable.food_chickpeas_cooked));
        allFoods.add(new FoodItem("Black beans cooked", "100g", 100f, 132, 8.9f, 0.5f, 23.7f, R.drawable.food_black_beans_cooked));
        allFoods.add(new FoodItem("Kidney beans cooked", "100g", 100f, 127, 8.7f, 0.5f, 22.8f, R.drawable.food_kidney_beans_cooked));
        allFoods.add(new FoodItem("Hummus", "100g", 100f, 166, 7.9f, 9.6f, 14.3f, R.drawable.food_hummus));
        allFoods.add(new FoodItem("Apple", "100g", 100f, 52, 0.3f, 0.2f, 13.8f, R.drawable.food_apple));
        allFoods.add(new FoodItem("Banana", "100g", 100f, 89, 1.1f, 0.3f, 22.8f, R.drawable.food_banana));
        allFoods.add(new FoodItem("Orange", "100g", 100f, 47, 0.9f, 0.1f, 11.8f, R.drawable.food_orange));
        allFoods.add(new FoodItem("Mandarin", "100g", 100f, 53, 0.8f, 0.3f, 13.3f, R.drawable.food_mandarin));
        allFoods.add(new FoodItem("Lemon", "100g", 100f, 29, 1.1f, 0.3f, 9.3f, R.drawable.food_lemon));
        allFoods.add(new FoodItem("Lime", "100g", 100f, 30, 0.7f, 0.2f, 10.5f, R.drawable.food_lime));
        allFoods.add(new FoodItem("Grapefruit", "100g", 100f, 42, 0.8f, 0.1f, 10.7f, R.drawable.food_grapefruit));
        allFoods.add(new FoodItem("Pomegranate", "100g", 100f, 83, 1.7f, 1.2f, 18.7f, R.drawable.food_pomegranate));
        allFoods.add(new FoodItem("Pear", "100g", 100f, 57, 0.4f, 0.1f, 15.2f, R.drawable.food_pear));
        allFoods.add(new FoodItem("Peach", "100g", 100f, 39, 0.9f, 0.3f, 9.5f, R.drawable.food_peach));
        allFoods.add(new FoodItem("Apricot", "100g", 100f, 48, 1.4f, 0.4f, 11.1f, R.drawable.food_apricot));
        allFoods.add(new FoodItem("Plum", "100g", 100f, 46, 0.7f, 0.3f, 11.4f, R.drawable.food_plum));
        allFoods.add(new FoodItem("Grapes", "100g", 100f, 69, 0.7f, 0.2f, 18.1f, R.drawable.food_grapes));
        allFoods.add(new FoodItem("Strawberries", "100g", 100f, 32, 0.7f, 0.3f, 7.7f, R.drawable.food_strawberries));
        allFoods.add(new FoodItem("Raspberries", "100g", 100f, 52, 1.2f, 0.7f, 11.9f, R.drawable.food_raspberries));
        allFoods.add(new FoodItem("Blueberries", "100g", 100f, 57, 0.7f, 0.3f, 14.5f, R.drawable.food_blueberries));
        allFoods.add(new FoodItem("Kiwi", "100g", 100f, 61, 1.1f, 0.5f, 14.7f, R.drawable.food_kiwi));
        allFoods.add(new FoodItem("Pineapple", "100g", 100f, 50, 0.5f, 0.1f, 13.1f, R.drawable.food_pineapple));
        allFoods.add(new FoodItem("Mango", "100g", 100f, 60, 0.8f, 0.4f, 15.0f, R.drawable.food_mango));
        allFoods.add(new FoodItem("Watermelon", "100g", 100f, 30, 0.6f, 0.2f, 7.6f, R.drawable.food_watermelon));
        allFoods.add(new FoodItem("Melon", "100g", 100f, 34, 0.8f, 0.2f, 8.2f, R.drawable.food_melon));
        allFoods.add(new FoodItem("Cherries", "100g", 100f, 63, 1.1f, 0.2f, 16.0f, R.drawable.food_cherries));
        allFoods.add(new FoodItem("Figs fresh", "100g", 100f, 74, 0.8f, 0.3f, 19.2f, R.drawable.food_figs_fresh));
        allFoods.add(new FoodItem("Avocado", "100g", 100f, 160, 2.0f, 14.7f, 8.5f, R.drawable.food_avocado));
        allFoods.add(new FoodItem("Almonds", "100g", 100f, 579, 21.2f, 49.9f, 21.6f, R.drawable.food_almonds));
        allFoods.add(new FoodItem("Walnuts", "100g", 100f, 654, 15.2f, 65.2f, 13.7f, R.drawable.food_walnuts));
        allFoods.add(new FoodItem("Hazelnuts", "100g", 100f, 628, 15.0f, 60.8f, 16.7f, R.drawable.food_hazelnuts));
        allFoods.add(new FoodItem("Peanuts", "100g", 100f, 567, 25.8f, 49.2f, 16.1f, R.drawable.food_peanuts));
        allFoods.add(new FoodItem("Cashews", "100g", 100f, 553, 18.2f, 43.9f, 30.2f, R.drawable.food_cashews));
        allFoods.add(new FoodItem("Sunflower seeds", "100g", 100f, 584, 20.8f, 51.5f, 20.0f, R.drawable.food_sunflower_seeds));
        allFoods.add(new FoodItem("Pumpkin seeds", "100g", 100f, 559, 30.2f, 49.0f, 10.7f, R.drawable.food_pumpkin_seeds));
        allFoods.add(new FoodItem("Chia seeds", "100g", 100f, 486, 16.5f, 30.7f, 42.1f, R.drawable.food_chia_seeds));
        allFoods.add(new FoodItem("Flax seeds", "100g", 100f, 534, 18.3f, 42.2f, 28.9f, R.drawable.food_flax_seeds));
        allFoods.add(new FoodItem("Peanut butter", "100g", 100f, 588, 25.1f, 50.0f, 20.0f, R.drawable.food_peanut_butter));
        allFoods.add(new FoodItem("Dark chocolate 70-85%", "100g", 100f, 598, 7.8f, 42.6f, 45.9f, R.drawable.food_dark_chocolate_70_85));
        allFoods.add(new FoodItem("Milk chocolate", "100g", 100f, 535, 7.7f, 29.7f, 59.4f, R.drawable.food_milk_chocolate));
        allFoods.add(new FoodItem("Cookies plain", "100g", 100f, 488, 6.0f, 22.0f, 68.0f, R.drawable.food_cookies_plain));
        allFoods.add(new FoodItem("Biscuits", "100g", 100f, 353, 7.0f, 8.0f, 72.0f, R.drawable.food_biscuits));
        allFoods.add(new FoodItem("Donut glazed", "100g", 100f, 452, 4.9f, 25.0f, 51.0f, R.drawable.food_donut_glazed));
        allFoods.add(new FoodItem("Ice cream vanilla", "100g", 100f, 207, 3.5f, 11.0f, 24.0f, R.drawable.food_ice_cream_vanilla));
        allFoods.add(new FoodItem("Potato chips", "100g", 100f, 536, 7.0f, 35.0f, 53.0f, R.drawable.food_chips_potato));
        allFoods.add(new FoodItem("Popcorn air-popped", "100g", 100f, 387, 12.9f, 4.5f, 78.0f, R.drawable.food_popcorn_air_popped));
        allFoods.add(new FoodItem("Crackers", "100g", 100f, 502, 7.1f, 25.0f, 61.0f, R.drawable.food_crackers));
        allFoods.add(new FoodItem("Protein bar average", "100g", 100f, 350, 30.0f, 10.0f, 35.0f, R.drawable.food_protein_bar_average));
        allFoods.add(new FoodItem("Honey", "100g", 100f, 304, 0.3f, 0.0f, 82.4f, R.drawable.food_honey));
        allFoods.add(new FoodItem("Jam", "100g", 100f, 278, 0.4f, 0.1f, 68.9f, R.drawable.food_jam));
        allFoods.add(new FoodItem("Chocolate hazelnut spread", "100g", 100f, 539, 6.3f, 30.9f, 57.5f, R.drawable.food_chocolate_hazelnut_spread));
        allFoods.add(new FoodItem("Ajvar", "100g", 100f, 80, 1.5f, 4.5f, 8.0f, R.drawable.food_ajvar));
        allFoods.add(new FoodItem("Baked beans", "100g", 100f, 94, 4.8f, 0.4f, 21.0f, R.drawable.food_baked_beans));
        allFoods.add(new FoodItem("Stuffed peppers", "100g", 100f, 120, 5.5f, 5.0f, 12.0f, R.drawable.food_stuffed_peppers));
        allFoods.add(new FoodItem("Moussaka", "100g", 100f, 145, 7.0f, 8.0f, 11.0f, R.drawable.food_moussaka));
        allFoods.add(new FoodItem("Sarma", "100g", 100f, 150, 7.0f, 9.0f, 10.0f, R.drawable.food_sarma));
        allFoods.add(new FoodItem("Chicken soup", "100g", 100f, 36, 3.0f, 1.2f, 3.5f, R.drawable.food_chicken_soup));
        allFoods.add(new FoodItem("Beef stew", "100g", 100f, 110, 8.5f, 5.0f, 7.0f, R.drawable.food_beef_stew));
        allFoods.add(new FoodItem("Goulash", "100g", 100f, 120, 9.0f, 6.0f, 6.0f, R.drawable.food_goulash));
        allFoods.add(new FoodItem("Pizza cheese", "100g", 100f, 266, 11.0f, 10.0f, 33.0f, R.drawable.food_pizza_cheese));
        allFoods.add(new FoodItem("Pizza pepperoni", "100g", 100f, 298, 12.0f, 14.0f, 32.0f, R.drawable.food_pizza_pepperoni));
        allFoods.add(new FoodItem("Hamburger sandwich", "100g", 100f, 254, 11.0f, 10.0f, 25.0f, R.drawable.food_hamburger_sandwich));
        allFoods.add(new FoodItem("Chicken sandwich", "100g", 100f, 220, 13.0f, 8.0f, 25.0f, R.drawable.food_chicken_sandwich));
        allFoods.add(new FoodItem("Hot dog sandwich", "100g", 100f, 290, 10.0f, 16.0f, 27.0f, R.drawable.food_hot_dog_sandwich));
        allFoods.add(new FoodItem("Doner kebab", "100g", 100f, 215, 12.0f, 10.0f, 20.0f, R.drawable.food_doner_kebab));
        allFoods.add(new FoodItem("Gyros", "100g", 100f, 220, 12.0f, 11.0f, 19.0f, R.drawable.food_gyros));
        allFoods.add(new FoodItem("Caesar salad", "100g", 100f, 180, 7.0f, 14.0f, 8.0f, R.drawable.food_caesar_salad));
        allFoods.add(new FoodItem("Shopska salad", "100g", 100f, 90, 4.0f, 6.0f, 5.0f, R.drawable.food_shopska_salad));
        allFoods.add(new FoodItem("Coca-Cola", "100g", 100f, 42, 0.0f, 0.0f, 10.6f, R.drawable.food_coca_cola));
        allFoods.add(new FoodItem("Coca-Cola Zero", "100g", 100f, 0, 0.0f, 0.0f, 0.0f, R.drawable.food_coca_cola_zero));
        allFoods.add(new FoodItem("Beer", "100g", 100f, 43, 0.5f, 0.0f, 3.6f, R.drawable.food_beer));
        allFoods.add(new FoodItem("Orange juice", "100g", 100f, 45, 0.7f, 0.2f, 10.4f, R.drawable.food_orange_juice));
        allFoods.add(new FoodItem("Apple juice", "100g", 100f, 46, 0.1f, 0.1f, 11.3f, R.drawable.food_apple_juice));
        allFoods.add(new FoodItem("Lemonade sweetened", "100g", 100f, 40, 0.0f, 0.0f, 10.0f, R.drawable.food_lemonade_sweetened));
        allFoods.add(new FoodItem("Coffee black", "100g", 100f, 2, 0.3f, 0.0f, 0.0f, R.drawable.food_coffee_black));
        allFoods.add(new FoodItem("Cappuccino", "100g", 100f, 31, 1.7f, 1.5f, 2.9f, R.drawable.food_cappuccino));
        allFoods.add(new FoodItem("Latte", "100g", 100f, 43, 2.7f, 1.6f, 4.0f, R.drawable.food_latte));
        allFoods.add(new FoodItem("Tea unsweetened", "100g", 100f, 1, 0.0f, 0.0f, 0.0f, R.drawable.food_tea_unsweetened));
        allFoods.add(new FoodItem("Protein shake with water", "100g", 100f, 60, 12.0f, 1.0f, 2.0f, R.drawable.food_protein_shake_with_water));
        allFoods.add(new FoodItem("Olive oil", "100g", 100f, 884, 0.0f, 100.0f, 0.0f, R.drawable.food_olive_oil));
        allFoods.add(new FoodItem("Sunflower oil", "100g", 100f, 884, 0.0f, 100.0f, 0.0f, R.drawable.food_sunflower_oil));
        allFoods.add(new FoodItem("Butter", "100g", 100f, 717, 0.9f, 81.0f, 0.1f, R.drawable.food_butter));
        allFoods.add(new FoodItem("Mayonnaise", "100g", 100f, 680, 1.0f, 75.0f, 0.6f, R.drawable.food_mayonnaise));
        allFoods.add(new FoodItem("Ketchup", "100g", 100f, 112, 1.7f, 0.1f, 26.0f, R.drawable.food_ketchup));
        allFoods.add(new FoodItem("Mustard", "100g", 100f, 66, 4.4f, 4.0f, 5.8f, R.drawable.food_mustard));
        allFoods.add(new FoodItem("BBQ sauce", "100g", 100f, 172, 1.0f, 0.5f, 40.0f, R.drawable.food_bbq_sauce));
        allFoods.add(new FoodItem("Soy sauce", "100g", 100f, 53, 8.1f, 0.6f, 4.9f, R.drawable.food_soy_sauce));
    }

    private String getCategoryForFood(String foodName) {
        if (foodName == null) return "Traditional/Prepared";

        String n = foodName.toLowerCase(Locale.ROOT).trim();

        if (n.contains("egg") || n.contains("omelette")) {
            return "Eggs";
        }

        if (n.contains("chicken") || n.contains("turkey") || n.contains("beef") ||
                n.contains("pork") || n.contains("ham") || n.contains("sausage") ||
                n.contains("hot dog") || n.contains("salami") || n.contains("bacon") ||
                n.contains("meatball") || n.contains("burger patty")) {
            return "Meat";
        }

        if (n.contains("tuna") || n.contains("salmon") || n.contains("sardines") ||
                n.contains("trout") || n.contains("mackerel") || n.contains("cod") ||
                n.contains("shrimp") || n.contains("fish")) {
            return "Fish";
        }

        if (n.contains("milk") || n.contains("yogurt") || n.contains("kefir") ||
                n.contains("cheese") || n.contains("cream")) {
            return "Dairy";
        }

        if (n.contains("rice") || n.contains("pasta") || n.contains("spaghetti") ||
                n.contains("macaroni") || n.contains("oats") || n.contains("oatmeal") ||
                n.contains("cornflakes") || n.contains("muesli") || n.contains("granola")) {
            return "Grains";
        }

        if (n.contains("bread") || n.contains("toast") || n.contains("bagel") ||
                n.contains("tortilla") || n.contains("pita") || n.contains("croissant") ||
                n.contains("pancake") || n.contains("waffle")) {
            return "Bread";
        }

        if (n.contains("potato") || n.contains("tomato") || n.contains("cucumber") ||
                n.contains("lettuce") || n.contains("spinach") || n.contains("cabbage") ||
                n.contains("carrot") || n.contains("broccoli") || n.contains("cauliflower") ||
                n.contains("zucchini") || n.contains("pepper") || n.contains("onion") ||
                n.contains("garlic") || n.contains("mushroom") || n.contains("corn cooked") ||
                n.contains("peas")) {
            return "Vegetables";
        }

        if (n.contains("beans") || n.contains("lentils") || n.contains("chickpeas") ||
                n.contains("hummus")) {
            return "Legumes";
        }

        if (n.contains("apple") || n.contains("banana") || n.contains("orange") ||
                n.contains("mandarin") || n.contains("lemon") || n.contains("lime") ||
                n.contains("grapefruit") || n.contains("pomegranate") || n.contains("pear") ||
                n.contains("peach") || n.contains("apricot") || n.contains("plum") ||
                n.contains("grapes") || n.contains("strawberries") || n.contains("raspberries") ||
                n.contains("blueberries") || n.contains("kiwi") || n.contains("pineapple") ||
                n.contains("mango") || n.contains("watermelon") || n.contains("melon") ||
                n.contains("cherries") || n.contains("figs") || n.contains("avocado")) {
            return "Fruit";
        }

        if (n.contains("almonds") || n.contains("walnuts") || n.contains("hazelnuts") ||
                n.contains("peanuts") || n.contains("cashews") || n.contains("seeds") ||
                n.contains("peanut butter")) {
            return "Nuts & Seeds";
        }

        if (n.contains("chocolate") || n.contains("cookies") || n.contains("biscuits") ||
                n.contains("donut") || n.contains("ice cream") || n.contains("chips") ||
                n.contains("popcorn") || n.contains("crackers") || n.contains("protein bar") ||
                n.contains("honey") || n.contains("jam") || n.contains("spread")) {
            return "Sweets & Snacks";
        }

        if (n.contains("ajvar") || n.contains("stuffed") || n.contains("moussaka") ||
                n.contains("sarma") || n.contains("soup") || n.contains("stew") ||
                n.contains("goulash") || n.contains("pizza") || n.contains("sandwich") ||
                n.contains("doner") || n.contains("gyros") || n.contains("salad")) {
            return "Traditional/Prepared";
        }

        if (n.contains("cola") || n.contains("juice") || n.contains("lemonade") ||
                n.contains("coffee") || n.contains("cappuccino") || n.contains("latte") ||
                n.contains("tea") || n.contains("shake") || n.contains("beer") ||
                n.contains("pivo")) {
            return "Drinks";
        }

        if (n.contains("oil") || n.contains("butter") || n.contains("mayonnaise") ||
                n.contains("ketchup") || n.contains("mustard") || n.contains("sauce")) {
            return "Oils & Sauces";
        }

        return "Traditional/Prepared";
    }

    private int getCustomFoodImageResOrCategory(String foodName, String category) {
        if (foodName == null || foodName.trim().isEmpty()) {
            return getDefaultCategoryImageRes(category);
        }

        String drawableName = "food_" + foodName
                .toLowerCase(Locale.ROOT)
                .trim()
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");

        int resId = getResources().getIdentifier(
                drawableName,
                "drawable",
                requireContext().getPackageName()
        );

        if (resId == 0) {
            return getDefaultCategoryImageRes(category);
        }

        return resId;
    }

    private int getDefaultCategoryImageRes(String category) {
        if (category == null) {
            return android.R.drawable.ic_menu_gallery;
        }

        switch (category) {
            case "Eggs":
                return R.drawable.ic_cat_eggs;

            case "Meat":
                return R.drawable.ic_cat_meat;

            case "Fish":
                return R.drawable.ic_cat_fish;

            case "Dairy":
                return R.drawable.ic_cat_dairy;

            case "Grains":
                return R.drawable.ic_cat_grains;

            case "Bread":
                return R.drawable.ic_cat_bread;

            case "Vegetables":
                return R.drawable.ic_cat_vegetables;

            case "Legumes":
                return R.drawable.ic_cat_legumes;

            case "Fruit":
                return R.drawable.ic_cat_fruit;

            case "Nuts & Seeds":
                return R.drawable.ic_cat_nuts_seeds;

            case "Sweets & Snacks":
                return R.drawable.ic_cat_sweets_snacks;

            case "Traditional/Prepared":
                return R.drawable.ic_cat_traditional_prepared;

            case "Drinks":
                return R.drawable.ic_cat_drinks;

            case "Oils & Sauces":
                return R.drawable.ic_cat_oils_sauces;

            default:
                return android.R.drawable.ic_menu_gallery;
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    static class FoodItem {
        final int userFoodId;
        final boolean isCustom;
        final String name;
        final String portion;
        final float baseGrams;
        final int calories;
        final float protein;
        final float fat;
        final float carbs;
        final int imageResId;
        final String category;

        FoodItem(String name, String portion, float baseGrams,
                 int calories, float protein, float fat, float carbs, int imageResId) {
            this(-1, false, name, portion, baseGrams, calories, protein, fat, carbs, imageResId, null);
        }

        FoodItem(int userFoodId, boolean isCustom, String name, String portion,
                 float baseGrams, int calories, float protein, float fat, float carbs,
                 int imageResId, String category) {
            this.userFoodId = userFoodId;
            this.isCustom = isCustom;
            this.name = name;
            this.portion = portion;
            this.baseGrams = baseGrams;
            this.calories = calories;
            this.protein = protein;
            this.fat = fat;
            this.carbs = carbs;
            this.imageResId = imageResId;
            this.category = category;
        }
    }

    class FoodAdapter extends RecyclerView.Adapter<FoodAdapter.FoodVH> {

        interface OnFoodClick {
            void onClick(FoodItem item);
        }

        private final List<FoodItem> original = new ArrayList<>();
        private final List<FoodItem> filtered = new ArrayList<>();
        private final OnFoodClick listener;

        private String searchQuery = "";
        private String selectedCategory = CATEGORY_ALL;

        FoodAdapter(OnFoodClick listener) {
            this.listener = listener;
        }

        void setData(List<FoodItem> items) {
            original.clear();
            filtered.clear();

            if (items != null) {
                original.addAll(items);
            }

            applyFilters();
        }

        void setSearchQuery(String query) {
            searchQuery = (query == null) ? "" : query.trim().toLowerCase(Locale.ROOT);
            applyFilters();
        }

        void setSelectedCategory(String category) {
            selectedCategory = (category == null || category.trim().isEmpty()) ? CATEGORY_ALL : category;
            applyFilters();
        }

        private void applyFilters() {
            filtered.clear();

            for (FoodItem item : original) {
                boolean matchesSearch = searchQuery.isEmpty() ||
                        item.name.toLowerCase(Locale.ROOT).contains(searchQuery);

                String itemCategory = item.isCustom && item.category != null && !item.category.trim().isEmpty()
                        ? item.category
                        : getCategoryForFood(item.name);

                boolean matchesCategory = selectedCategory.equals(CATEGORY_ALL) ||
                        itemCategory.equals(selectedCategory);

                if (matchesSearch && matchesCategory) {
                    filtered.add(item);
                }
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

            holder.itemView.setMinimumHeight(dp(76));

            holder.ivFood.setImageResource(item.imageResId);
            holder.ivFood.setClipToOutline(true);

            holder.tvTitle.setText(item.name);
            holder.tvTitle.setTextSize(17f);
            holder.tvTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            holder.tvTitle.setIncludeFontPadding(true);

            holder.tvPortionKcal.setText(String.format(
                    Locale.US,
                    "%s • %d kcal",
                    item.portion,
                    item.calories
            ));

            holder.tvProteinChip.setText(String.format(
                    Locale.US,
                    "Protein: %.1fg",
                    item.protein
            ));

            holder.tvFatChip.setText(String.format(
                    Locale.US,
                    "Fat: %.1fg",
                    item.fat
            ));

            holder.tvCarbsChip.setText(String.format(
                    Locale.US,
                    "Carbs: %.1fg",
                    item.carbs
            ));

            int userId = sm.getUserId();
            String key = makeFoodKey(item);

            boolean fav = (userId != -1) && db.isFavorite(userId, key);
            paintHeart(holder.ivFav, fav);

            holder.ivFav.setOnClickListener(v -> {
                int uid = sm.getUserId();

                if (uid == -1) {
                    showSimpleDialog("Not Logged In", "Please log in before using favorites.");
                    return;
                }

                boolean nowFav = db.isFavorite(uid, key);

                if (nowFav) {
                    db.removeFavorite(uid, key);
                    paintHeart(holder.ivFav, false);
                } else {
                    db.addFavorite(
                            uid,
                            key,
                            item.name,
                            item.portion,
                            item.baseGrams,
                            item.calories,
                            item.protein,
                            item.fat,
                            item.carbs
                    );
                    paintHeart(holder.ivFav, true);
                }
            });

            if (item.isCustom) {
                holder.cardDelete.setVisibility(View.VISIBLE);
                holder.cardDelete.setOnClickListener(v -> confirmDeleteUserFood(item));
                holder.ivDelete.setOnClickListener(v -> confirmDeleteUserFood(item));
            } else {
                holder.cardDelete.setVisibility(View.GONE);
                holder.cardDelete.setOnClickListener(null);
                holder.ivDelete.setOnClickListener(null);
            }

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onClick(item);
                }
            });
        }

        @Override
        public int getItemCount() {
            return filtered.size();
        }

        class FoodVH extends RecyclerView.ViewHolder {
            ImageView ivFood, ivFav, ivDelete;
            TextView tvTitle, tvPortionKcal, tvProteinChip, tvFatChip, tvCarbsChip;
            View cardDelete;

            FoodVH(@NonNull View itemView) {
                super(itemView);

                ivFood = itemView.findViewById(R.id.ivFood);
                tvTitle = itemView.findViewById(R.id.tvTitle);
                tvPortionKcal = itemView.findViewById(R.id.tvPortionKcal);
                tvProteinChip = itemView.findViewById(R.id.tvProteinChip);
                tvFatChip = itemView.findViewById(R.id.tvFatChip);
                tvCarbsChip = itemView.findViewById(R.id.tvCarbsChip);
                ivFav = itemView.findViewById(R.id.ivFav);
                ivDelete = itemView.findViewById(R.id.ivDelete);
                cardDelete = itemView.findViewById(R.id.cardDelete);
            }
        }
    }

    private String makeFoodKey(FoodItem item) {
        return item.name + "|" + item.portion + "|" + item.baseGrams + "|" +
                item.calories + "|" + item.protein + "|" + item.fat + "|" + item.carbs;
    }

    private void paintHeart(ImageView iv, boolean fav) {
        if (iv == null) return;

        if (fav) {
            iv.setImageResource(R.drawable.baseline_favorite_24);
            iv.setColorFilter(0xFFE53935);
        } else {
            iv.setImageResource(R.drawable.outline_favorite_24);
            iv.setColorFilter(0xFFB0B0B0);
        }
    }

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

    static class SimpleTextWatcher implements TextWatcher {
        interface OnTextChanged {
            void onChanged(String text);
        }

        private final OnTextChanged cb;

        SimpleTextWatcher(OnTextChanged cb) {
            this.cb = cb;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {
            if (cb != null) {
                cb.onChanged(s == null ? "" : s.toString());
            }
        }
    }
}