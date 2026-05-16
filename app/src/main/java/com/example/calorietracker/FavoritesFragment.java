package com.example.calorietracker;

import android.app.AlertDialog;
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
            @Override
            public void onOpen(dbConnect.FavoriteFoodRow item) {
                openFoodDetail(item);
            }

            @Override
            public void onToggleHeart(dbConnect.FavoriteFoodRow item) {
                int uid = sm.getUserId();

                if (uid == -1) {
                    showSimpleDialog("Not Logged In", "Please log in before using favorites.");
                    return;
                }

                db.removeFavorite(uid, item.key);
                loadFavorites();
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

    private void showSimpleDialog(String title, String message) {
        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

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

            holder.ivFav.setImageResource(R.drawable.baseline_favorite_24);
            holder.ivFav.setColorFilter(0xFFE53935);

            holder.ivFav.setOnClickListener(v -> {
                if (actions != null) {
                    actions.onToggleHeart(item);
                }
            });

            holder.itemView.setOnClickListener(v -> {
                if (actions != null) {
                    actions.onOpen(item);
                }
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