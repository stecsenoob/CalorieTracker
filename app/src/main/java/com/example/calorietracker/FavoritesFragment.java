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

public class FavoritesFragment extends Fragment {

    private RecyclerView rvFav;
    private FavAdapter adapter;

    private SessionManager sm;
    private dbConnect db;

    private final List<dbConnect.FavoriteFoodRow> data = new ArrayList<>();

    public FavoritesFragment() {
        super(R.layout.fragment_favorites);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sm = new SessionManager(requireContext());
        db = new dbConnect(requireContext());

        rvFav = view.findViewById(R.id.rvFavorites);
        rvFav.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new FavAdapter(data, new FavAdapter.OnFavActions() {
            @Override public void onOpen(dbConnect.FavoriteFoodRow item) {
                openFoodDetail(item);
            }

            @Override public void onToggleHeart(dbConnect.FavoriteFoodRow item) {
                int uid = sm.getUserId();
                if (uid == -1) {
                    showSnack(requireView(), "Not logged in", true);
                    return;
                }

                // 1) remove immediately
                db.removeFavorite(uid, item.key);
                loadFavorites();

                // 2) show snackbar with UNDO
                showUndoRemoveFavorite(requireView(), uid, item);
            }
        });

        rvFav.setAdapter(adapter);

        loadFavorites();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadFavorites();
    }

    private void loadFavorites() {
        data.clear();
        int uid = sm.getUserId();
        if (uid != -1) {
            data.addAll(db.getFavorites(uid));
        }
        adapter.notifyDataSetChanged();
    }

    private void openFoodDetail(dbConnect.FavoriteFoodRow item) {
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

    // ================= UNDO Snackbar =================
    private void showUndoRemoveFavorite(View anchor, int uid, dbConnect.FavoriteFoodRow item) {
        Snackbar snackbar = Snackbar.make(anchor, "Removed from favorites 💔", Snackbar.LENGTH_SHORT);
        snackbar.setDuration(1000); // 1 секунда

        // colors
        snackbar.setTextColor(Color.WHITE);
        snackbar.setBackgroundTint(Color.parseColor("#E53935")); // 🔴 red

        // action
        snackbar.setAction("UNDO", v -> {
            // Restore favorite back
            db.addFavorite(uid, item.key,
                    item.name, item.portion, item.baseGrams,
                    item.calories, item.protein, item.fat, item.carbs);

            loadFavorites();
            showSnack(anchor, "Restored ✅", false); // 🟢 green
        });

        // Optional: style action text (ke e bel/kontrast)
        snackbar.setActionTextColor(Color.WHITE);

        // floating look + background drawable like RegisterActivity
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

    // ================= Adapter =================
    static class FavAdapter extends RecyclerView.Adapter<FavAdapter.VH> {

        interface OnFavActions {
            void onOpen(dbConnect.FavoriteFoodRow item);
            void onToggleHeart(dbConnect.FavoriteFoodRow item);
        }

        private final List<dbConnect.FavoriteFoodRow> items;
        private final OnFavActions actions;

        FavAdapter(List<dbConnect.FavoriteFoodRow> items, OnFavActions actions) {
            this.items = items;
            this.actions = actions;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_food_card, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            dbConnect.FavoriteFoodRow item = items.get(position);

            holder.tvTitle.setText(item.name);

            String line1 = item.portion + " • " + item.calories + " kcal";
            String line2 = "Protein: " + format1(item.protein) + "g | " +
                    "Fat: " + format1(item.fat) + "g | " +
                    "Carbs: " + format1(item.carbs) + "g";
            holder.tvSub.setText(line1 + "\n" + line2);

            // Always red in Favorites
            holder.ivFav.setImageResource(R.drawable.baseline_favorite_24);
            holder.ivFav.setColorFilter(0xFFE53935);

            holder.ivFav.setOnClickListener(v -> {
                if (actions != null) actions.onToggleHeart(item);
            });

            holder.itemView.setOnClickListener(v -> {
                if (actions != null) actions.onOpen(item);
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvTitle, tvSub;
            ImageView ivFav;

            VH(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvTitle);
                tvSub = itemView.findViewById(R.id.tvSub);
                ivFav = itemView.findViewById(R.id.ivFav);
            }
        }

        private static String format1(float v) {
            return String.format(Locale.ROOT, "%.1f", v);
        }
    }
}