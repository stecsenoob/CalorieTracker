package com.example.calorietracker;

import androidx.appcompat.app.AlertDialog;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.snackbar.Snackbar;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner;
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BarcodeScannerFragment extends Fragment {

    private static final String AI_BARCODE_URL = "https://food-ai-server-wfc9.onrender.com/analyze-barcode-product";

    private MaterialButton btnBack;
    private MaterialButton btnScanBarcode;
    private MaterialButton btnSaveFood;

    private MaterialCardView cardQuantity;
    private MaterialCardView cardProductImage;
    private ImageView imgProduct;

    private TextInputEditText etGrams;
    private MaterialButton btnSaveToMyFoods;

    private TextView tvResultTitle;
    private TextView tvResultSubtitle;
    private TextView tvProductValue;
    private TextView tvBarcodeValue;
    private TextView tvQuantityValue;
    private TextView tvCaloriesValue;
    private TextView tvProteinValue;
    private TextView tvCarbsValue;
    private TextView tvFatValue;
    private TextView tvInsightValue;
    private TextView tvSourceValue;
    private TextView tvNoteValue;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private boolean hasProduct = false;
    private boolean hasSavedFood = false;
    private boolean hasSavedToMyFoods = false;
    private boolean isUpdatingGrams = false;

    private String barcodeValue = "";
    private String productName = "";
    private String productImageUrl = "";
    private String aiNutritionInsight = "";

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
        cardProductImage = view.findViewById(R.id.cardProductImage);
        imgProduct = view.findViewById(R.id.imgProduct);

        etGrams = view.findViewById(R.id.etGrams);
        btnSaveToMyFoods = view.findViewById(R.id.btnSaveToMyFoods);

        tvResultTitle = view.findViewById(R.id.tvResultTitle);
        tvResultSubtitle = view.findViewById(R.id.tvResultSubtitle);
        tvProductValue = view.findViewById(R.id.tvProductValue);
        tvBarcodeValue = view.findViewById(R.id.tvBarcodeValue);
        tvQuantityValue = view.findViewById(R.id.tvQuantityValue);
        tvCaloriesValue = view.findViewById(R.id.tvCaloriesValue);
        tvProteinValue = view.findViewById(R.id.tvProteinValue);
        tvCarbsValue = view.findViewById(R.id.tvCarbsValue);
        tvFatValue = view.findViewById(R.id.tvFatValue);
        tvInsightValue = view.findViewById(R.id.tvInsightValue);
        tvSourceValue = view.findViewById(R.id.tvSourceValue);
        tvNoteValue = view.findViewById(R.id.tvNoteValue);

        cardQuantity.setVisibility(View.GONE);
        hideProductImage();
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

        btnSaveToMyFoods.setOnClickListener(v -> {
            if (!hasProduct) {
                showInfoState(
                        "Product Required",
                        "Please scan a barcode first.\n\nAfter the product is found, you can save it to My Foods."
                );
                return;
            }

            if (hasSavedToMyFoods) {
                showSnackbar("Already saved to My Foods");
                return;
            }

            showSaveToMyFoodsBottomSheet();
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
                    aiNutritionInsight = "";
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
                                "?fields=product_name,nutriments,image_url,image_front_url,selected_images";

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

                productImageUrl = product.optString("image_url", "");

                if (productImageUrl == null || productImageUrl.trim().isEmpty()) {
                    productImageUrl = product.optString("image_front_url", "");
                }

                Bitmap productBitmap = null;

                if (productImageUrl != null && !productImageUrl.trim().isEmpty()) {
                    productBitmap = downloadBitmap(productImageUrl.trim());
                }

                caloriesPer100g = getCaloriesPer100g(nutriments);
                proteinPer100g = nutriments.optDouble("proteins_100g", 0.0);
                carbsPer100g = nutriments.optDouble("carbohydrates_100g", 0.0);
                fatPer100g = nutriments.optDouble("fat_100g", 0.0);

                grams = 100.0;
                recalculateNutritionValues();

                aiNutritionInsight = fetchBarcodeNutritionInsightFromAI();

                Bitmap finalProductBitmap = productBitmap;

                mainHandler.post(() -> {
                    if (!isAdded()) return;

                    hasProduct = true;
                    hasSavedFood = false;
                    hasSavedToMyFoods = false;

                    cardQuantity.setVisibility(View.VISIBLE);
                    setGramsInputText(grams);

                    updateResultCard();

                    if (finalProductBitmap != null) {
                        showProductImage(finalProductBitmap);
                    } else {
                        hideProductImage();
                    }

                    enableSaveButton();
                    setLoadingState(false);
                });

            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (!isAdded()) return;

                    hasProduct = false;
                    hasSavedFood = false;
                    hasSavedToMyFoods = false;
                    aiNutritionInsight = "";

                    cardQuantity.setVisibility(View.GONE);
                    hideProductImage();
                    disableSaveButton();
                    setLoadingState(false);

                    showProductNotFoundCard(barcode);
                    showProductNotFoundDialog(barcode);
                });
            }
        });
    }

    private String fetchBarcodeNutritionInsightFromAI() {
        try {
            JSONObject requestBody = new JSONObject();

            requestBody.put("product_name", productName);
            requestBody.put("barcode", barcodeValue);
            requestBody.put("grams", grams);
            requestBody.put("calories", currentCalories);
            requestBody.put("protein_g", currentProtein);
            requestBody.put("carbs_g", currentCarbs);
            requestBody.put("fat_g", currentFat);

            String response = httpPostJson(AI_BARCODE_URL, requestBody.toString());

            JSONObject jsonObject = new JSONObject(response);

            String note = jsonObject.optString("note", "");

            if (note == null || note.trim().isEmpty()) {
                return "";
            }

            return note.trim();

        } catch (Exception ignored) {
            return "";
        }
    }

    private String httpPostJson(String apiUrl, String jsonBody) throws Exception {
        URL url = new URL(apiUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(60000);
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setRequestProperty("Accept", "application/json");

        OutputStream outputStream = connection.getOutputStream();
        outputStream.write(jsonBody.getBytes("UTF-8"));
        outputStream.flush();
        outputStream.close();

        int responseCode = connection.getResponseCode();

        InputStream inputStream;

        if (responseCode >= 200 && responseCode < 300) {
            inputStream = connection.getInputStream();
        } else {
            inputStream = connection.getErrorStream();
        }

        String response = readStream(inputStream);

        connection.disconnect();

        if (responseCode < 200 || responseCode >= 300) {
            throw new Exception("Server error: " + responseCode + "\n" + response);
        }

        return response;
    }

    private Bitmap downloadBitmap(String imageUrl) {
        HttpURLConnection connection = null;

        try {
            URL url = new URL(imageUrl);
            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            connection.setRequestProperty("User-Agent", "CalorieTrackerAndroid/1.0");
            connection.setDoInput(true);
            connection.connect();

            int responseCode = connection.getResponseCode();

            if (responseCode < 200 || responseCode >= 300) {
                return null;
            }

            InputStream inputStream = connection.getInputStream();
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();

            return bitmap;

        } catch (Exception ignored) {
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void showProductImage(Bitmap bitmap) {
        if (bitmap == null) {
            hideProductImage();
            return;
        }

        imgProduct.setImageBitmap(bitmap);
        cardProductImage.setVisibility(View.VISIBLE);
    }

    private void hideProductImage() {
        if (cardProductImage != null) {
            cardProductImage.setVisibility(View.GONE);
        }

        if (imgProduct != null) {
            imgProduct.setImageDrawable(null);
        }
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

        String response = readStream(inputStream);

        connection.disconnect();

        if (responseCode < 200 || responseCode >= 300) {
            throw new Exception("Server error: " + responseCode);
        }

        return response;
    }

    private String readStream(InputStream inputStream) throws Exception {
        if (inputStream == null) {
            return "";
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder builder = new StringBuilder();

        String line;

        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }

        reader.close();

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

        tvInsightValue.setText(generateNutritionInsight());

        tvSourceValue.setText("Source: Open Food Facts + AI");
        tvNoteValue.setText("Nutrition values are based on product data per 100g. You can edit grams before saving.");
    }

    private String generateNutritionInsight() {
        if (aiNutritionInsight != null && !aiNutritionInsight.trim().isEmpty()) {
            return aiNutritionInsight.trim();
        }

        return generateLocalNutritionInsight();
    }

    private String generateLocalNutritionInsight() {
        String lowerName = productName == null ? "" : productName.toLowerCase(Locale.ROOT);

        if (lowerName.contains("nescafe") ||
                lowerName.contains("coffee") ||
                lowerName.contains("instant coffee") ||
                lowerName.contains("café") ||
                lowerName.contains("cafe")) {

            return "Coffee itself is usually very low in calories when prepared without sugar, milk, or cream. Calories can increase depending on what you add to it.";
        }

        if (lowerName.contains("zero") ||
                lowerName.contains("diet") ||
                lowerName.contains("light") ||
                lowerName.contains("no sugar") ||
                lowerName.contains("sugar free")) {

            return "Very low calorie product. It can help reduce calorie intake compared to the regular version, but portion and overall daily habits still matter.";
        }

        if (lowerName.contains("coca") ||
                lowerName.contains("cola") ||
                lowerName.contains("pepsi") ||
                lowerName.contains("fanta") ||
                lowerName.contains("sprite") ||
                lowerName.contains("soft drink") ||
                lowerName.contains("soda")) {

            if (caloriesPer100g <= 5) {
                return "Very low calorie drink. It can fit into a calorie-tracking diet, but water is still the better everyday choice.";
            } else {
                return "This drink contains calories, usually from sugar. It is better to watch portion size if you are tracking calories.";
            }
        }

        if (caloriesPer100g <= 0 && proteinPer100g <= 0 && carbsPer100g <= 0 && fatPer100g <= 0) {
            return "Not enough nutrition data is available for this product.";
        }

        StringBuilder insight = new StringBuilder();

        if (caloriesPer100g >= 450) {
            insight.append("Watch portion: This product is high in calories per 100g. ");
        } else if (caloriesPer100g >= 250) {
            insight.append("Moderate choice: This product has a moderate calorie density. ");
        } else {
            insight.append("Good choice: This product is relatively lower in calories per 100g. ");
        }

        if (proteinPer100g >= 15) {
            insight.append("It also provides a good amount of protein. ");
        } else if (proteinPer100g >= 8) {
            insight.append("It contains a moderate amount of protein. ");
        }

        if (carbsPer100g >= 60) {
            insight.append("It is high in carbohydrates, so portion size matters. ");
        } else if (carbsPer100g >= 30) {
            insight.append("It has a moderate amount of carbohydrates. ");
        }

        if (fatPer100g >= 20) {
            insight.append("It is also high in fat, so it can add calories quickly. ");
        } else if (fatPer100g >= 10) {
            insight.append("It has a moderate amount of fat. ");
        }

        return insight.toString().trim();
    }

    private void showEmptyResultState() {
        hideProductImage();

        tvResultTitle.setText("No barcode scanned yet");
        tvResultSubtitle.setText("Scan a packaged food barcode to load nutrition values.");

        tvProductValue.setText("-");
        tvBarcodeValue.setText("Barcode: -");
        tvQuantityValue.setText("-");
        tvCaloriesValue.setText("-");

        tvProteinValue.setText("Protein\n-");
        tvCarbsValue.setText("Carbs\n-");
        tvFatValue.setText("Fat\n-");

        tvInsightValue.setText("Nutrition insight will appear here after scanning.");
        tvSourceValue.setText("Source: -");
        tvNoteValue.setText("Product nutrition values will appear here after scanning.");
    }

    private void showInfoState(String title, String message) {
        hideProductImage();

        tvResultTitle.setText(title);
        tvResultSubtitle.setText(message);

        tvProductValue.setText("-");
        tvBarcodeValue.setText("Barcode: " + (barcodeValue == null || barcodeValue.isEmpty() ? "-" : barcodeValue));
        tvQuantityValue.setText("-");
        tvCaloriesValue.setText("-");

        tvProteinValue.setText("Protein\n-");
        tvCarbsValue.setText("Carbs\n-");
        tvFatValue.setText("Fat\n-");

        tvInsightValue.setText("Nutrition insight is available after a product is scanned.");
        tvSourceValue.setText("Status: Attention needed");
        tvNoteValue.setText(message);
    }

    private void showProductNotFoundCard(String barcode) {
        hideProductImage();

        tvResultTitle.setText("Product Not Found");
        tvResultSubtitle.setText("This product is not available in the food database.");

        tvProductValue.setText("-");
        tvBarcodeValue.setText("Barcode: " + barcode);
        tvQuantityValue.setText("-");
        tvCaloriesValue.setText("-");

        tvProteinValue.setText("Protein\n-");
        tvCarbsValue.setText("Carbs\n-");
        tvFatValue.setText("Fat\n-");

        tvInsightValue.setText("No product insight is available because this barcode was not found.");
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

        tvInsightValue.setText(generateNutritionInsight());
        tvSourceValue.setText("Meal: " + mealLabel);

        if (savedToMyFoods) {
            tvNoteValue.setText("Also saved to My Foods. You can go back to Home to see updated totals.");
        } else if (alreadyExistsInMyFoods) {
            tvNoteValue.setText("Not added to My Foods because it already exists. You can go back to Home to see updated totals.");
        } else {
            tvNoteValue.setText("You can go back to Home to see updated totals.");
        }
    }

    private void showSaveToMyFoodsBottomSheet() {
        if (hasSavedToMyFoods) {
            showSnackbar("Already saved to My Foods");
            return;
        }

        int userId = new SessionManager(requireContext()).getUserId();

        if (userId == -1) {
            showInfoState("Not Logged In", "Please log in before saving product to My Foods.");
            return;
        }

        if (productName == null || productName.trim().isEmpty()) {
            showInfoState("Missing Product Name", "The product name is missing. Please scan the barcode again.");
            return;
        }

        String finalProductName = productName.trim();
        float finalGrams = grams > 0 ? (float) grams : 100f;
        String portion = formatGrams(finalGrams) + "g";
        String category = guessCategoryForFood(finalProductName);

        showAddFoodBottomSheetPrefilled(
                userId,
                finalProductName,
                category,
                portion,
                finalGrams,
                currentCalories,
                (float) currentProtein,
                (float) currentFat,
                (float) currentCarbs
        );
    }

    private void showAddFoodBottomSheetPrefilled(int userId,
                                                 String defaultName,
                                                 String defaultCategory,
                                                 String defaultPortion,
                                                 float defaultGrams,
                                                 int defaultKcal,
                                                 float defaultProtein,
                                                 float defaultFat,
                                                 float defaultCarbs) {
        BottomSheetDialog sheet = new BottomSheetDialog(requireContext());
        View v = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_add_food, null, false);

        TextView tvAddFoodTitle = v.findViewById(R.id.tvAddFoodTitle);
        TextView tvAddFoodSubtitle = v.findViewById(R.id.tvAddFoodSubtitle);
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

        String[] addFoodCategories = getFoodCategories();

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                addFoodCategories
        );

        actCategory.setAdapter(categoryAdapter);

        if (tvAddFoodTitle != null) {
            tvAddFoodTitle.setText("Save to My Foods");
        }

        if (tvAddFoodSubtitle != null) {
            tvAddFoodSubtitle.setText("Review and edit the scanned product values before saving.");
        }

        etName.setText(defaultName);
        actCategory.setText(isValidFoodCategory(defaultCategory) ? defaultCategory : "Traditional/Prepared", false);
        etPortion.setText(defaultPortion);
        etGrams.setText(formatGrams(defaultGrams));
        etKcal.setText(String.valueOf(defaultKcal));
        etProtein.setText(formatDouble(defaultProtein));
        etFat.setText(formatDouble(defaultFat));
        etCarbs.setText(formatDouble(defaultCarbs));
        btnSave.setText("Save Food");

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

            if (!isValidFoodCategory(category)) {
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

            dbConnect db = new dbConnect(requireContext());

            if (db.userFoodExists(userId, name, grams, kcal, p, f, c)) {
                sheet.dismiss();
                hasSavedToMyFoods = true;
                markSaveToMyFoodsButton("Already in My Foods");
                showSnackbar("Already in My Foods");
                return;
            }

            long result = db.addUserFood(userId, name, portion, grams, kcal, p, f, c, category);

            if (result != -1) {
                sheet.dismiss();
                hasSavedToMyFoods = true;
                markSaveToMyFoodsButton("Saved to My Foods");
                showSnackbar("Saved to My Foods");
                updateResultCard();
            } else {
                showSnackbar("Save failed. Please try again.");
            }
        });

        sheet.setContentView(v);
        sheet.show();
    }

    private void markSaveToMyFoodsButton(String text) {
        if (btnSaveToMyFoods == null) return;

        btnSaveToMyFoods.setText(text);
        btnSaveToMyFoods.setEnabled(false);
        btnSaveToMyFoods.setClickable(false);
        btnSaveToMyFoods.setAlpha(0.75f);
    }

    private void showSnackbar(String message) {
        View rootView = getView();

        if (rootView != null) {
            Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT).show();
        }
    }

    private String[] getFoodCategories() {
        return new String[]{
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
    }

    private boolean isValidFoodCategory(String category) {
        if (category == null) return false;

        for (String c : getFoodCategories()) {
            if (c.equals(category)) {
                return true;
            }
        }

        return false;
    }

    private String guessCategoryForFood(String foodName) {
        if (foodName == null) return "Traditional/Prepared";

        String n = foodName.toLowerCase(Locale.ROOT).trim();

        if (n.contains("egg") || n.contains("omelette")) return "Eggs";

        if (n.contains("chicken") || n.contains("turkey") || n.contains("beef") ||
                n.contains("pork") || n.contains("ham") || n.contains("sausage") ||
                n.contains("hot dog") || n.contains("salami") || n.contains("bacon") ||
                n.contains("meatball") || n.contains("burger")) return "Meat";

        if (n.contains("tuna") || n.contains("salmon") || n.contains("sardines") ||
                n.contains("trout") || n.contains("mackerel") || n.contains("cod") ||
                n.contains("shrimp") || n.contains("fish")) return "Fish";

        if (n.contains("milk") || n.contains("yogurt") || n.contains("kefir") ||
                n.contains("cheese") || n.contains("cream")) return "Dairy";

        if (n.contains("rice") || n.contains("pasta") || n.contains("spaghetti") ||
                n.contains("macaroni") || n.contains("oats") || n.contains("oatmeal") ||
                n.contains("cornflakes") || n.contains("muesli") || n.contains("granola")) return "Grains";

        if (n.contains("bread") || n.contains("toast") || n.contains("bagel") ||
                n.contains("tortilla") || n.contains("pita") || n.contains("croissant") ||
                n.contains("pancake") || n.contains("waffle")) return "Bread";

        if (n.contains("potato") || n.contains("tomato") || n.contains("cucumber") ||
                n.contains("lettuce") || n.contains("spinach") || n.contains("cabbage") ||
                n.contains("carrot") || n.contains("broccoli") || n.contains("cauliflower") ||
                n.contains("zucchini") || n.contains("pepper") || n.contains("onion") ||
                n.contains("garlic") || n.contains("mushroom") || n.contains("peas")) return "Vegetables";

        if (n.contains("beans") || n.contains("lentils") || n.contains("chickpeas") ||
                n.contains("hummus")) return "Legumes";

        if (n.contains("apple") || n.contains("banana") || n.contains("orange") ||
                n.contains("mandarin") || n.contains("lemon") || n.contains("lime") ||
                n.contains("grapefruit") || n.contains("pomegranate") || n.contains("pear") ||
                n.contains("peach") || n.contains("apricot") || n.contains("plum") ||
                n.contains("grapes") || n.contains("strawberries") || n.contains("raspberries") ||
                n.contains("blueberries") || n.contains("kiwi") || n.contains("pineapple") ||
                n.contains("mango") || n.contains("watermelon") || n.contains("melon") ||
                n.contains("cherries") || n.contains("figs") || n.contains("avocado")) return "Fruit";

        if (n.contains("almond") || n.contains("walnut") || n.contains("hazelnut") ||
                n.contains("peanut") || n.contains("cashew") || n.contains("seed")) return "Nuts & Seeds";

        if (n.contains("chocolate") || n.contains("cookie") || n.contains("biscuit") ||
                n.contains("donut") || n.contains("ice cream") || n.contains("chips") ||
                n.contains("popcorn") || n.contains("cracker") || n.contains("protein bar") ||
                n.contains("honey") || n.contains("jam") || n.contains("spread")) return "Sweets & Snacks";

        if (n.contains("cola") || n.contains("juice") || n.contains("lemonade") ||
                n.contains("coffee") || n.contains("cappuccino") || n.contains("latte") ||
                n.contains("tea") || n.contains("shake") || n.contains("beer") ||
                n.contains("pivo")) return "Drinks";

        if (n.contains("oil") || n.contains("butter") || n.contains("mayonnaise") ||
                n.contains("ketchup") || n.contains("mustard") || n.contains("sauce")) return "Oils & Sauces";

        return "Traditional/Prepared";
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

        hasSavedFood = true;
        btnSaveFood.setEnabled(false);
        btnSaveFood.setAlpha(0.5f);
        btnSaveFood.setText("Added to Meal");

        showSavedResultCard(mealLabel, savedToMyFoods, alreadyExistsInMyFoods);
    }

    private void setLoadingState(boolean isLoading) {
        if (isLoading) {
            btnScanBarcode.setEnabled(false);
            btnSaveFood.setEnabled(false);
            if (btnSaveToMyFoods != null) {
                btnSaveToMyFoods.setEnabled(false);
                btnSaveToMyFoods.setAlpha(0.5f);
            }
            btnScanBarcode.setText("Loading product...");

            hideProductImage();

            tvResultTitle.setText("Loading Product");
            tvResultSubtitle.setText("Searching nutrition information from the barcode.");
            tvProductValue.setText("Processing...");
            tvBarcodeValue.setText("Barcode: " + (barcodeValue == null || barcodeValue.isEmpty() ? "-" : barcodeValue));
            tvQuantityValue.setText("-");
            tvCaloriesValue.setText("-");
            tvProteinValue.setText("Protein\n-");
            tvCarbsValue.setText("Carbs\n-");
            tvFatValue.setText("Fat\n-");
            tvInsightValue.setText("Analyzing nutrition data...");
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
        btnSaveFood.setText("Add to Meal");

        if (btnSaveToMyFoods != null) {
            if (hasSavedToMyFoods) {
                markSaveToMyFoodsButton("Saved to My Foods");
            } else {
                btnSaveToMyFoods.setEnabled(true);
                btnSaveToMyFoods.setClickable(true);
                btnSaveToMyFoods.setAlpha(1.0f);
                btnSaveToMyFoods.setText("Save to My Foods");
            }
        }
    }

    private void disableSaveButton() {
        btnSaveFood.setEnabled(false);
        btnSaveFood.setAlpha(0.5f);

        if (!hasSavedFood) {
            btnSaveFood.setText("Add to Meal");
        }

        if (btnSaveToMyFoods != null) {
            btnSaveToMyFoods.setEnabled(false);
            btnSaveToMyFoods.setClickable(false);
            btnSaveToMyFoods.setAlpha(0.5f);

            if (!hasProduct) {
                btnSaveToMyFoods.setText("Save to My Foods");
            }
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
