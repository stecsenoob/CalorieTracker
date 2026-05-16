package com.example.calorietracker;

import androidx.appcompat.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner;
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BarcodeScannerFragment extends Fragment {

    private MaterialButton btnBack;
    private MaterialButton btnScanBarcode;
    private MaterialButton btnSaveFood;

    private MaterialCardView cardQuantity;
    private TextInputEditText etGrams;
    private CheckBox cbSaveToMyFoods;

    private TextView tvResultTitle;
    private TextView tvResultSubtitle;
    private TextView tvProductValue;
    private TextView tvBarcodeValue;
    private TextView tvQuantityValue;
    private TextView tvCaloriesValue;
    private TextView tvProteinValue;
    private TextView tvCarbsValue;
    private TextView tvFatValue;
    private TextView tvSourceValue;
    private TextView tvNoteValue;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private boolean hasProduct = false;
    private boolean hasSavedFood = false;
    private boolean isUpdatingGrams = false;

    private String barcodeValue = "";
    private String productName = "";

    private double grams = 100.0;

    private int caloriesPer100g = 0;
    private double proteinPer100g = 0.0;
    private double carbsPer100g = 0.0;
    private double fatPer100g = 0.0;

    private int currentCalories = 0;
    private double currentProtein = 0.0;
    private double currentCarbs = 0.0;
    private double currentFat = 0.0;

    public BarcodeScannerFragment() {
        super(R.layout.fragment_barcode_scanner);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        btnBack = view.findViewById(R.id.btnBack);
        btnScanBarcode = view.findViewById(R.id.btnScanBarcode);
        btnSaveFood = view.findViewById(R.id.btnSaveFood);

        cardQuantity = view.findViewById(R.id.cardQuantity);
        etGrams = view.findViewById(R.id.etGrams);
        cbSaveToMyFoods = view.findViewById(R.id.cbSaveToMyFoods);

        tvResultTitle = view.findViewById(R.id.tvResultTitle);
        tvResultSubtitle = view.findViewById(R.id.tvResultSubtitle);
        tvProductValue = view.findViewById(R.id.tvProductValue);
        tvBarcodeValue = view.findViewById(R.id.tvBarcodeValue);
        tvQuantityValue = view.findViewById(R.id.tvQuantityValue);
        tvCaloriesValue = view.findViewById(R.id.tvCaloriesValue);
        tvProteinValue = view.findViewById(R.id.tvProteinValue);
        tvCarbsValue = view.findViewById(R.id.tvCarbsValue);
        tvFatValue = view.findViewById(R.id.tvFatValue);
        tvSourceValue = view.findViewById(R.id.tvSourceValue);
        tvNoteValue = view.findViewById(R.id.tvNoteValue);

        cardQuantity.setVisibility(View.GONE);
        disableSaveButton();
        showEmptyResultState();

        btnBack.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().popBackStack();
        });

        btnScanBarcode.setOnClickListener(v -> {
            startBarcodeScanner();
        });

        btnSaveFood.setOnClickListener(v -> {
            if (!hasProduct) {
                showInfoState(
                        "Product Required",
                        "Please scan a barcode first.\n\nAfter the product is found, you can save it to a meal."
                );
                return;
            }

            if (hasSavedFood) {
                showInfoState(
                        "Already Saved",
                        "This food has already been saved.\n\nGo back to Home to see updated totals."
                );
                return;
            }

            showMealChoiceDialog();
        });

        etGrams.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isUpdatingGrams) return;
                if (!hasProduct) return;

                recalculateFromGrams(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not needed
            }
        });
    }

    private void startBarcodeScanner() {
        GmsBarcodeScannerOptions options = new GmsBarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                        Barcode.FORMAT_EAN_13,
                        Barcode.FORMAT_EAN_8,
                        Barcode.FORMAT_UPC_A,
                        Barcode.FORMAT_UPC_E
                )
                .enableAutoZoom()
                .build();

        GmsBarcodeScanner scanner = GmsBarcodeScanning.getClient(requireActivity(), options);

        scanner.startScan()
                .addOnSuccessListener(barcode -> {
                    String rawValue = barcode.getRawValue();

                    if (rawValue == null || rawValue.trim().isEmpty()) {
                        showInfoState(
                                "No Barcode Detected",
                                "The scanner did not detect a barcode.\n\nTry holding the camera steady and make sure the barcode is visible."
                        );
                        return;
                    }

                    barcodeValue = rawValue.trim();
                    fetchProductFromOpenFoodFacts(barcodeValue);
                })
                .addOnCanceledListener(() -> {
                    showInfoState(
                            "Scan Cancelled",
                            "Barcode scanning was cancelled.\n\nTap Scan Barcode to try again."
                    );
                })
                .addOnFailureListener(e -> {
                    showInfoState(
                            "Scanner Error",
                            "The barcode scanner could not start.\n\nPlease try again."
                    );
                });
    }

    private void fetchProductFromOpenFoodFacts(String barcode) {
        setLoadingState(true);

        executorService.execute(() -> {
            try {
                String apiUrl =
                        "https://world.openfoodfacts.org/api/v2/product/" + barcode +
                                "?fields=product_name,nutriments";

                String response = httpGet(apiUrl);

                JSONObject root = new JSONObject(response);
                int status = root.optInt("status", 0);

                if (status != 1) {
                    throw new Exception("Product not found in Open Food Facts.");
                }

                JSONObject product = root.getJSONObject("product");
                JSONObject nutriments = product.optJSONObject("nutriments");

                if (nutriments == null) {
                    throw new Exception("This product has no nutrition data.");
                }

                productName = product.optString("product_name", "Unknown product");

                if (productName == null || productName.trim().isEmpty()) {
                    productName = "Unknown product";
                }

                caloriesPer100g = getCaloriesPer100g(nutriments);
                proteinPer100g = nutriments.optDouble("proteins_100g", 0.0);
                carbsPer100g = nutriments.optDouble("carbohydrates_100g", 0.0);
                fatPer100g = nutriments.optDouble("fat_100g", 0.0);

                grams = 100.0;
                recalculateNutritionValues();

                mainHandler.post(() -> {
                    if (!isAdded()) return;

                    hasProduct = true;
                    hasSavedFood = false;

                    cardQuantity.setVisibility(View.VISIBLE);
                    setGramsInputText(grams);

                    updateResultCard();
                    enableSaveButton();
                    setLoadingState(false);
                });

            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (!isAdded()) return;

                    hasProduct = false;
                    hasSavedFood = false;

                    cardQuantity.setVisibility(View.GONE);
                    disableSaveButton();
                    setLoadingState(false);

                    showProductNotFoundCard(barcode);
                    showProductNotFoundDialog(barcode);
                });
            }
        });
    }

    private void showProductNotFoundDialog(String barcode) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_product_not_found, null, false);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .create();

        TextView tvBarcode = dialogView.findViewById(R.id.tvProductNotFoundBarcode);
        MaterialButton btnTryAnother = dialogView.findViewById(R.id.btnTryAnotherBarcode);
        MaterialButton btnUseAiScan = dialogView.findViewById(R.id.btnUseAiScan);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnProductNotFoundCancel);

        tvBarcode.setText("Barcode: " + barcode);

        btnTryAnother.setOnClickListener(v -> {
            dialog.dismiss();
            startBarcodeScanner();
        });

        btnUseAiScan.setOnClickListener(v -> {
            dialog.dismiss();
            openScanFoodPage();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    private void openScanFoodPage() {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frameLayout, new ScanFoodFragment())
                .addToBackStack(null)
                .commit();
    }

    private int getCaloriesPer100g(JSONObject nutriments) {
        double kcal = nutriments.optDouble("energy-kcal_100g", -1);

        if (kcal >= 0) {
            return (int) Math.round(kcal);
        }

        double energyKj = nutriments.optDouble("energy_100g", 0.0);

        if (energyKj > 0) {
            return (int) Math.round(energyKj / 4.184);
        }

        return 0;
    }

    private String httpGet(String apiUrl) throws Exception {
        URL url = new URL(apiUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("GET");
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(30000);
        connection.setRequestProperty("User-Agent", "CalorieTrackerAndroid/1.0");

        int responseCode = connection.getResponseCode();

        InputStream inputStream;

        if (responseCode >= 200 && responseCode < 300) {
            inputStream = connection.getInputStream();
        } else {
            inputStream = connection.getErrorStream();
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder builder = new StringBuilder();

        String line;

        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }

        reader.close();
        connection.disconnect();

        if (responseCode < 200 || responseCode >= 300) {
            throw new Exception("Server error: " + responseCode);
        }

        return builder.toString();
    }

    private void recalculateFromGrams(String gramsText) {
        if (gramsText == null || gramsText.trim().isEmpty()) return;

        try {
            double newGrams = Double.parseDouble(gramsText.trim());

            if (newGrams <= 0) return;

            grams = newGrams;
            recalculateNutritionValues();
            hasSavedFood = false;
            enableSaveButton();
            updateResultCard();

        } catch (NumberFormatException ignored) {
            // Ignore invalid input while user is typing
        }
    }

    private void recalculateNutritionValues() {
        double ratio = grams / 100.0;

        currentCalories = (int) Math.round(caloriesPer100g * ratio);
        currentProtein = proteinPer100g * ratio;
        currentCarbs = carbsPer100g * ratio;
        currentFat = fatPer100g * ratio;
    }

    private void updateResultCard() {
        tvResultTitle.setText("Barcode Food Result");
        tvResultSubtitle.setText("Review the product data and adjust grams before saving.");

        tvProductValue.setText(productName);
        tvBarcodeValue.setText("Barcode: " + barcodeValue);
        tvQuantityValue.setText(formatGrams(grams) + "g");
        tvCaloriesValue.setText(currentCalories + " kcal");

        tvProteinValue.setText("Protein\n" + formatDouble(currentProtein) + "g");
        tvCarbsValue.setText("Carbs\n" + formatDouble(currentCarbs) + "g");
        tvFatValue.setText("Fat\n" + formatDouble(currentFat) + "g");

        tvSourceValue.setText("Source: Open Food Facts");
        tvNoteValue.setText("Nutrition values are based on product data per 100g. You can edit grams before saving.");
    }

    private void showEmptyResultState() {
        tvResultTitle.setText("No barcode scanned yet");
        tvResultSubtitle.setText("Scan a packaged food barcode to load nutrition values.");

        tvProductValue.setText("-");
        tvBarcodeValue.setText("Barcode: -");
        tvQuantityValue.setText("-");
        tvCaloriesValue.setText("-");

        tvProteinValue.setText("Protein\n-");
        tvCarbsValue.setText("Carbs\n-");
        tvFatValue.setText("Fat\n-");

        tvSourceValue.setText("Source: -");
        tvNoteValue.setText("Product nutrition values will appear here after scanning.");
    }

    private void showInfoState(String title, String message) {
        tvResultTitle.setText(title);
        tvResultSubtitle.setText(message);

        tvProductValue.setText("-");
        tvBarcodeValue.setText("Barcode: " + (barcodeValue == null || barcodeValue.isEmpty() ? "-" : barcodeValue));
        tvQuantityValue.setText("-");
        tvCaloriesValue.setText("-");

        tvProteinValue.setText("Protein\n-");
        tvCarbsValue.setText("Carbs\n-");
        tvFatValue.setText("Fat\n-");

        tvSourceValue.setText("Status: Attention needed");
        tvNoteValue.setText(message);
    }

    private void showProductNotFoundCard(String barcode) {
        tvResultTitle.setText("Product Not Found");
        tvResultSubtitle.setText("This product is not available in the food database.");

        tvProductValue.setText("-");
        tvBarcodeValue.setText("Barcode: " + barcode);
        tvQuantityValue.setText("-");
        tvCaloriesValue.setText("-");

        tvProteinValue.setText("Protein\n-");
        tvCarbsValue.setText("Carbs\n-");
        tvFatValue.setText("Fat\n-");

        tvSourceValue.setText("Source: Open Food Facts");
        tvNoteValue.setText(
                "What you can do:\n" +
                        "• Try scanning another barcode\n" +
                        "• Use AI Scan Food instead\n" +
                        "• Add this food manually from My Foods"
        );
    }

    private void showSavedResultCard(String mealLabel, boolean savedToMyFoods, boolean alreadyExistsInMyFoods) {
        tvResultTitle.setText("Food Saved Successfully");
        tvResultSubtitle.setText("Added to " + mealLabel + ".");

        tvProductValue.setText(productName);
        tvBarcodeValue.setText("Barcode: " + barcodeValue);
        tvQuantityValue.setText(formatGrams(grams) + "g");
        tvCaloriesValue.setText(currentCalories + " kcal");

        tvProteinValue.setText("Protein\n" + formatDouble(currentProtein) + "g");
        tvCarbsValue.setText("Carbs\n" + formatDouble(currentCarbs) + "g");
        tvFatValue.setText("Fat\n" + formatDouble(currentFat) + "g");

        tvSourceValue.setText("Meal: " + mealLabel);

        if (savedToMyFoods) {
            tvNoteValue.setText("Also saved to My Foods. You can go back to Home to see updated totals.");
        } else if (alreadyExistsInMyFoods) {
            tvNoteValue.setText("Not added to My Foods because it already exists. You can go back to Home to see updated totals.");
        } else {
            tvNoteValue.setText("You can go back to Home to see updated totals.");
        }
    }

    private void showMealChoiceDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_choose_meal, null, false);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .create();

        MaterialButton btnBreakfast = dialogView.findViewById(R.id.btnMealBreakfast);
        MaterialButton btnLunch = dialogView.findViewById(R.id.btnMealLunch);
        MaterialButton btnDinner = dialogView.findViewById(R.id.btnMealDinner);
        MaterialButton btnSnacks = dialogView.findViewById(R.id.btnMealSnacks);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnMealCancel);

        btnBreakfast.setOnClickListener(v -> {
            dialog.dismiss();
            saveProductToMeal("breakfast", "Breakfast");
        });

        btnLunch.setOnClickListener(v -> {
            dialog.dismiss();
            saveProductToMeal("lunch", "Lunch");
        });

        btnDinner.setOnClickListener(v -> {
            dialog.dismiss();
            saveProductToMeal("dinner", "Dinner");
        });

        btnSnacks.setOnClickListener(v -> {
            dialog.dismiss();
            saveProductToMeal("snacks", "Snacks");
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    private void saveProductToMeal(String mealValue, String mealLabel) {
        int userId = new SessionManager(requireContext()).getUserId();

        if (userId == -1) {
            showInfoState("Not Logged In", "Please log in before saving food.");
            return;
        }

        if (productName == null || productName.trim().isEmpty()) {
            showInfoState("Missing Product Name", "The product name is missing. Please scan the barcode again.");
            return;
        }

        String finalProductName = productName.trim();

        dbConnect db = new dbConnect(requireContext());

        long logResult = db.addFoodLog(
                userId,
                mealValue,
                finalProductName,
                (float) grams,
                1f,
                currentCalories,
                (float) currentProtein,
                (float) currentFat,
                (float) currentCarbs
        );

        if (logResult == -1) {
            showInfoState("Save Failed", "The food could not be saved. Please try again.");
            return;
        }

        boolean savedToMyFoods = false;
        boolean alreadyExistsInMyFoods = false;

        if (cbSaveToMyFoods != null && cbSaveToMyFoods.isChecked()) {
            String portion = formatGrams(grams) + "g";

            float finalGrams = (float) grams;
            float p = (float) currentProtein;
            float f = (float) currentFat;
            float c = (float) currentCarbs;

            if (db.userFoodExists(userId, finalProductName, finalGrams, currentCalories, p, f, c)) {
                alreadyExistsInMyFoods = true;
            } else {
                long foodResult = db.addUserFood(
                        userId,
                        finalProductName,
                        portion,
                        finalGrams,
                        currentCalories,
                        p,
                        f,
                        c
                );

                savedToMyFoods = foodResult != -1;
            }
        }

        hasSavedFood = true;
        btnSaveFood.setEnabled(false);
        btnSaveFood.setAlpha(0.5f);
        btnSaveFood.setText("Saved");

        showSavedResultCard(mealLabel, savedToMyFoods, alreadyExistsInMyFoods);
    }

    private void setLoadingState(boolean isLoading) {
        if (isLoading) {
            btnScanBarcode.setEnabled(false);
            btnSaveFood.setEnabled(false);
            btnScanBarcode.setText("Loading product...");

            tvResultTitle.setText("Loading Product");
            tvResultSubtitle.setText("Searching nutrition information from the barcode.");
            tvProductValue.setText("Processing...");
            tvBarcodeValue.setText("Barcode: " + (barcodeValue == null || barcodeValue.isEmpty() ? "-" : barcodeValue));
            tvQuantityValue.setText("-");
            tvCaloriesValue.setText("-");
            tvProteinValue.setText("Protein\n-");
            tvCarbsValue.setText("Carbs\n-");
            tvFatValue.setText("Fat\n-");
            tvSourceValue.setText("Status: Searching");
            tvNoteValue.setText("Please wait. This may take a few seconds.");
        } else {
            btnScanBarcode.setEnabled(true);
            btnScanBarcode.setText("Scan Barcode");

            if (hasProduct && !hasSavedFood) {
                enableSaveButton();
            } else {
                disableSaveButton();
            }
        }
    }

    private void enableSaveButton() {
        btnSaveFood.setEnabled(true);
        btnSaveFood.setAlpha(1.0f);
        btnSaveFood.setText("Save Food");
    }

    private void disableSaveButton() {
        btnSaveFood.setEnabled(false);
        btnSaveFood.setAlpha(0.5f);

        if (!hasSavedFood) {
            btnSaveFood.setText("Save Food");
        }
    }

    private void setGramsInputText(double value) {
        isUpdatingGrams = true;
        etGrams.setText(formatGrams(value));
        etGrams.setSelection(etGrams.getText() == null ? 0 : etGrams.getText().length());
        isUpdatingGrams = false;
    }

    private String formatDouble(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private String formatGrams(double value) {
        if (value == Math.floor(value)) {
            return String.format(Locale.ROOT, "%.0f", value);
        }

        return String.format(Locale.ROOT, "%.1f", value);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}