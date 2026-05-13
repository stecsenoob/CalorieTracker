package com.example.calorietracker;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

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
    private TextView tvResult;

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
        tvResult = view.findViewById(R.id.tvResult);

        cardQuantity.setVisibility(View.GONE);
        disableSaveButton();

        btnBack.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().popBackStack();
        });

        btnScanBarcode.setOnClickListener(v -> {
            startBarcodeScanner();
        });

        btnSaveFood.setOnClickListener(v -> {
            if (!hasProduct) {
                Toast.makeText(requireContext(), "Scan a product first", Toast.LENGTH_SHORT).show();
                return;
            }

            if (hasSavedFood) {
                Toast.makeText(requireContext(), "This food is already saved", Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(requireContext(), "No barcode detected", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    barcodeValue = rawValue.trim();
                    fetchProductFromOpenFoodFacts(barcodeValue);
                })
                .addOnCanceledListener(() -> {
                    Toast.makeText(requireContext(), "Scan cancelled", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Scanner error", Toast.LENGTH_LONG).show();

                    tvResult.setText(
                            "Scanner error:\n\n" +
                                    e.getMessage() + "\n\n" +
                                    "Please try again."
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

                    updateResultText();
                    enableSaveButton();
                    setLoadingState(false);

                    Toast.makeText(requireContext(), "Product found", Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (!isAdded()) return;

                    hasProduct = false;
                    hasSavedFood = false;

                    cardQuantity.setVisibility(View.GONE);
                    disableSaveButton();
                    setLoadingState(false);

                    tvResult.setText(
                            "Product not found or incomplete.\n\n" +
                                    "Barcode: " + barcode + "\n\n" +
                                    "Reason:\n" +
                                    e.getMessage() + "\n\n" +
                                    "You can try another barcode or use AI Scan Food instead."
                    );

                    showProductNotFoundDialog(barcode);
                });
            }
        });
    }

    private void showProductNotFoundDialog(String barcode) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Product not found")
                .setMessage(
                        "This barcode was scanned, but the product was not found or has missing nutrition data.\n\n" +
                                "Barcode: " + barcode + "\n\n" +
                                "What do you want to do?"
                )
                .setPositiveButton("Try another", (dialog, which) -> {
                    startBarcodeScanner();
                })
                .setNegativeButton("Use AI Scan", (dialog, which) -> {
                    openScanFoodPage();
                })
                .setNeutralButton("Cancel", null)
                .show();
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
            updateResultText();

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

    private void updateResultText() {
        tvResult.setText(
                "Barcode result:\n\n" +
                        "Barcode: " + barcodeValue + "\n" +
                        "Product: " + productName + "\n" +
                        "Quantity: " + formatGrams(grams) + "g\n\n" +
                        "Calories: " + currentCalories + " kcal\n" +
                        "Protein: " + formatDouble(currentProtein) + "g\n" +
                        "Carbs: " + formatDouble(currentCarbs) + "g\n" +
                        "Fat: " + formatDouble(currentFat) + "g\n\n" +
                        "Values are based on nutrition per 100g from Open Food Facts."
        );
    }

    private void showMealChoiceDialog() {
        String[] mealLabels = {"Breakfast", "Lunch", "Dinner", "Snacks"};
        String[] mealValues = {"breakfast", "lunch", "dinner", "snacks"};

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Add to meal")
                .setItems(mealLabels, (dialog, which) -> {
                    saveProductToMeal(mealValues[which], mealLabels[which]);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveProductToMeal(String mealValue, String mealLabel) {
        int userId = new SessionManager(requireContext()).getUserId();

        if (userId == -1) {
            Toast.makeText(requireContext(), "User is not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        if (productName == null || productName.trim().isEmpty()) {
            Toast.makeText(requireContext(), "Product name is missing", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(requireContext(), "Failed to save food", Toast.LENGTH_SHORT).show();
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

        String toastMessage;

        if (savedToMyFoods) {
            toastMessage = "Saved to " + mealLabel + " and My Foods";
        } else if (alreadyExistsInMyFoods) {
            toastMessage = "Saved to " + mealLabel + ". Already exists in My Foods";
        } else {
            toastMessage = "Saved to " + mealLabel;
        }

        Toast.makeText(requireContext(), toastMessage, Toast.LENGTH_SHORT).show();

        String resultText =
                "Saved successfully!\n\n" +
                        "Product: " + finalProductName + "\n" +
                        "Meal: " + mealLabel + "\n" +
                        "Quantity: " + formatGrams(grams) + "g\n\n" +
                        "Calories: " + currentCalories + " kcal\n" +
                        "Protein: " + formatDouble(currentProtein) + "g\n" +
                        "Carbs: " + formatDouble(currentCarbs) + "g\n" +
                        "Fat: " + formatDouble(currentFat) + "g\n\n";

        if (savedToMyFoods) {
            resultText += "Also saved to My Foods.\n\n";
        } else if (alreadyExistsInMyFoods) {
            resultText += "Not added to My Foods because it already exists.\n\n";
        }

        resultText += "You can go back to Home to see updated totals.";

        tvResult.setText(resultText);
    }

    private void setLoadingState(boolean isLoading) {
        if (isLoading) {
            btnScanBarcode.setEnabled(false);
            btnSaveFood.setEnabled(false);
            btnScanBarcode.setText("Loading product...");
            tvResult.setText("Loading product from barcode...\n\nPlease wait.");
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