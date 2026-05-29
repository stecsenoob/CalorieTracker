package com.leon.calorietracker;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class FoodScannerFragment extends Fragment {

    public FoodScannerFragment() {
        // користи layout fragment_food_scanner (ќе го додадеме следно)
        super(R.layout.fragment_food_scanner);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // подоцна тука ќе оди кодот за камера или API
    }
}
