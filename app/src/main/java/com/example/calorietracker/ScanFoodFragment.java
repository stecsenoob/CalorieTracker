package com.example.calorietracker;

import android.Manifest;

import androidx.appcompat.app.AlertDialog;

import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScanFoodFragment extends Fragment {

    private static final String SERVER_URL = "https://food-ai-server-wfc9.onrender.com/analyze-food";

    private TextView tvResultTitle;
    private TextView tvResultSubtitle;
    private TextView tvFoodValue;
    private TextView tvQuantityValue;
    private TextView tvCaloriesValue;
    private TextView tvProteinValue;
    private TextView tvCarbsValue;
    private TextView tvFatValue;
    private TextView tvInsightValue;
    private TextView tvConfidenceValue;
    private TextView tvNoteValue;

    private MaterialCardView cardQuantity;
    private TextInputEditText etGrams;
    private MaterialButton btnSaveToMyFoods;

    private MaterialButton btnTakePhoto;
    private MaterialButton btnSaveFood;
    private MaterialButton btnBack;

    private Uri currentPhotoUri;
    private boolean hasPhoto = false;
    private boolean hasAnalyzedFood = false;
    private boolean hasSavedFood = false;
    private boolean hasSavedToMyFoods = false;
    private boolean isUpdatingGramsText = false;

    private String analyzedFoodName = "";

    private double originalQuantityGrams = 0.0;
    private int originalCalories = 0;
    private double originalProtein = 0.0;
    private double originalCarbs = 0.0;
    private double originalFat = 0.0;

    private double currentQuantityGrams = 0.0;
    private int currentCalories = 0;
    private double currentProtein = 0.0;
    private double currentCarbs = 0.0;
    private double currentFat = 0.0;

    private String analyzedConfidence = "";
    private String analyzedNote = "";

    private ActivityResultLauncher<Uri> takePictureLauncher;
    private ActivityResultLauncher<String> requestCameraPermissionLauncher;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public ScanFoodFragment() {
        super(R.layout.fragment_scan_food);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestCameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        openCameraFullQuality();
                    } else {
                        if (tvResultTitle != null) {
                            showInfoState(
                                    "Camera Permission Needed",
                                    "Camera access is required to take a food photo.\n\nPlease allow camera permission and try again."
                            );
                        }
                    }
                }
        );

        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && currentPhotoUri != null) {
                        hasPhoto = true;
                        hasAnalyzedFood = false;
                        hasSavedFood = false;
                        hasSavedToMyFoods = false;

                        resetAnalyzedValues();

                        cardQuantity.setVisibility(View.GONE);
                        disableSaveButton();

                        showInfoState(
                                "Photo Captured",
                                "Your photo was captured successfully.\n\nAnalyzing food now..."
                        );

                        analyzeFoodWithAI();

                    } else {
                        showInfoState(
                                "No Image Captured",
                                "The camera did not return a photo.\n\nPlease tap Take Photo and try again."
                        );
                    }
                }
        );
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvResultTitle = view.findViewById(R.id.tvResultTitle);
        tvResultSubtitle = view.findViewById(R.id.tvResultSubtitle);
        tvFoodValue = view.findViewById(R.id.tvFoodValue);
        tvQuantityValue = view.findViewById(R.id.tvQuantityValue);
        tvCaloriesValue = view.findViewById(R.id.tvCaloriesValue);
        tvProteinValue = view.findViewById(R.id.tvProteinValue);
        tvCarbsValue = view.findViewById(R.id.tvCarbsValue);
        tvFatValue = view.findViewById(R.id.tvFatValue);
        tvInsightValue = view.findViewById(R.id.tvInsightValue);
        tvConfidenceValue = view.findViewById(R.id.tvConfidenceValue);
        tvNoteValue = view.findViewById(R.id.tvNoteValue);

        cardQuantity = view.findViewById(R.id.cardQuantity);
        etGrams = view.findViewById(R.id.etGrams);
        btnSaveToMyFoods = view.findViewById(R.id.btnSaveToMyFoods);

        btnTakePhoto = view.findViewById(R.id.btnTakePhoto);
        btnSaveFood = view.findViewById(R.id.btnSaveFood);
        btnBack = view.findViewById(R.id.btnBack);

        cardQuantity.setVisibility(View.GONE);
        disableSaveButton();
        showEmptyResultState();

        btnBack.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().popBackStack();
        });

        btnTakePhoto.setOnClickListener(v -> {
            checkCameraPermissionAndOpenCamera();
        });

        btnSaveFood.setOnClickListener(v -> {
            if (!hasAnalyzedFood) {
                showInfoState(
                        "Analysis Required",
                        "Please take a photo first. The app will analyze it automatically."
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
            if (!hasAnalyzedFood) {
                showInfoState(
                        "Analysis Required",
                        "Please take a photo first. The app will analyze it automatically."
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
                if (isUpdatingGramsText) return;
                if (!hasAnalyzedFood) return;

                recalculateFromEditedGrams(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not needed
            }
        });
    }

    private void checkCameraPermissionAndOpenCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {

            openCameraFullQuality();

        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void openCameraFullQuality() {
        try {
            File imageFile = createImageFile();

            currentPhotoUri = FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".provider",
                    imageFile
            );

            takePictureLauncher.launch(currentPhotoUri);

        } catch (Exception e) {
            showInfoState(
                    "Camera Error",
                    "The camera could not be opened.\n\nPlease check app permissions and try again."
            );
        }
    }

    private File createImageFile() throws IOException {
        String fileName = "food_scan_" + System.currentTimeMillis();
        File storageDir = new File(requireContext().getCacheDir(), "images");

        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }

        return File.createTempFile(fileName, ".jpg", storageDir);
    }

    private void analyzeFoodWithAI() {
        if (!hasPhoto || currentPhotoUri == null) {
            showInfoState(
                    "Photo Required",
                    "Please take a food photo first."
            );
            return;
        }

        setLoadingState(true);

        executorService.execute(() -> {
            try {
                String response = uploadImageToServer(currentPhotoUri);

                JSONObject jsonObject = new JSONObject(response);

                analyzedFoodName = jsonObject.optString("food_name", "Unknown food");

                originalQuantityGrams = jsonObject.optDouble("estimated_quantity_grams", 0.0);
                originalCalories = jsonObject.optInt("calories", 0);
                originalProtein = jsonObject.optDouble("protein_g", 0.0);
                originalCarbs = jsonObject.optDouble("carbs_g", 0.0);
                originalFat = jsonObject.optDouble("fat_g", 0.0);

                if (originalQuantityGrams <= 0) {
                    originalQuantityGrams = 100.0;
                }

                currentQuantityGrams = originalQuantityGrams;
                currentCalories = originalCalories;
                currentProtein = originalProtein;
                currentCarbs = originalCarbs;
                currentFat = originalFat;

                analyzedConfidence = jsonObject.optString("confidence", "unknown");
                analyzedNote = jsonObject.optString("note", "Nutrition values are estimated.");

                mainHandler.post(() -> {
                    if (!isAdded()) return;

                    hasAnalyzedFood = true;
                    hasSavedFood = false;
                    hasSavedToMyFoods = false;

                    cardQuantity.setVisibility(View.VISIBLE);
                    setGramsInputText(currentQuantityGrams);

                    updateResultCard();
                    enableSaveButton();
                    setLoadingState(false);
                });

            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (!isAdded()) return;

                    hasAnalyzedFood = false;
                    hasSavedFood = false;
                    hasSavedToMyFoods = false;

                    cardQuantity.setVisibility(View.GONE);
                    disableSaveButton();
                    setLoadingState(false);

                    showAnalysisFailedCard();
                });
            }
        });
    }

    private String uploadImageToServer(Uri imageUri) throws Exception {
        String boundary = "Boundary-" + System.currentTimeMillis();
        String lineEnd = "\r\n";
        String twoHyphens = "--";

        URL url = new URL(SERVER_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setConnectTimeout(30000);
        connection.setReadTimeout(60000);
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setUseCaches(false);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Connection", "Keep-Alive");
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        OutputStream outputStream = connection.getOutputStream();

        outputStream.write((twoHyphens + boundary + lineEnd).getBytes());
        outputStream.write(("Content-Disposition: form-data; name=\"image\"; filename=\"food.jpg\"" + lineEnd).getBytes());
        outputStream.write(("Content-Type: image/jpeg" + lineEnd).getBytes());
        outputStream.write(lineEnd.getBytes());

        InputStream inputStream = requireContext().getContentResolver().openInputStream(imageUri);

        if (inputStream == null) {
            throw new IOException("Could not open image file");
        }

        byte[] buffer = new byte[4096];
        int bytesRead;

        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }

        inputStream.close();

        outputStream.write(lineEnd.getBytes());
        outputStream.write((twoHyphens + boundary + twoHyphens + lineEnd).getBytes());
        outputStream.flush();
        outputStream.close();

        int responseCode = connection.getResponseCode();

        InputStream responseStream;

        if (responseCode >= 200 && responseCode < 300) {
            responseStream = connection.getInputStream();
        } else {
            responseStream = connection.getErrorStream();
        }

        String response = readStream(responseStream);

        connection.disconnect();

        if (responseCode < 200 || responseCode >= 300) {
            throw new IOException("Server error " + responseCode + ":\n" + response);
        }

        return response;
    }

    private String readStream(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return "";
        }

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        byte[] buffer = new byte[4096];
        int length;

        while ((length = inputStream.read(buffer)) != -1) {
            byteArrayOutputStream.write(buffer, 0, length);
        }

        inputStream.close();

        return byteArrayOutputStream.toString("UTF-8");
    }

    private void recalculateFromEditedGrams(String gramsText) {
        if (gramsText == null || gramsText.trim().isEmpty()) {
            return;
        }

        try {
            double newGrams = Double.parseDouble(gramsText.trim());

            if (newGrams <= 0 || originalQuantityGrams <= 0) {
                return;
            }

            double ratio = newGrams / originalQuantityGrams;

            currentQuantityGrams = newGrams;
            currentCalories = (int) Math.round(originalCalories * ratio);
            currentProtein = originalProtein * ratio;
            currentCarbs = originalCarbs * ratio;
            currentFat = originalFat * ratio;

            hasSavedFood = false;
            enableSaveButton();

            updateResultCard();

        } catch (NumberFormatException ignored) {
            // Ignore invalid input while user is typing
        }
    }

    private void updateResultCard() {
        tvResultTitle.setText("AI Food Analysis");
        tvResultSubtitle.setText("Review the estimate and adjust grams before saving.");

        tvFoodValue.setText(capitalize(analyzedFoodName));
        tvQuantityValue.setText(formatGrams(currentQuantityGrams) + "g");
        tvCaloriesValue.setText(currentCalories + " kcal");

        tvProteinValue.setText("Protein\n" + formatDouble(currentProtein) + "g");
        tvCarbsValue.setText("Carbs\n" + formatDouble(currentCarbs) + "g");
        tvFatValue.setText("Fat\n" + formatDouble(currentFat) + "g");

        tvInsightValue.setText(generateNutritionInsight());

        tvConfidenceValue.setText("Confidence: " + capitalize(analyzedConfidence));
        tvNoteValue.setText("Nutrition values are estimates. You can edit grams before saving.");
    }

    private String generateNutritionInsight() {
        if (analyzedNote != null && !analyzedNote.trim().isEmpty()) {
            return analyzedNote.trim();
        }

        if (currentCalories <= 0 && currentProtein <= 0 && currentCarbs <= 0 && currentFat <= 0) {
            return "Not enough nutrition data is available for this food.";
        }

        StringBuilder insight = new StringBuilder();

        if (currentCalories >= 600) {
            insight.append("Watch portion: This food is high in calories for the estimated quantity. ");
        } else if (currentCalories >= 300) {
            insight.append("Moderate choice: This food has a moderate amount of calories. ");
        } else {
            insight.append("Good choice: This food is relatively lower in calories for the estimated quantity. ");
        }

        if (currentProtein >= 25) {
            insight.append("It also provides a good amount of protein. ");
        } else if (currentProtein >= 10) {
            insight.append("It contains a moderate amount of protein. ");
        }

        if (currentCarbs >= 60) {
            insight.append("It is high in carbohydrates, so portion size matters. ");
        } else if (currentCarbs >= 30) {
            insight.append("It has a moderate amount of carbohydrates. ");
        }

        if (currentFat >= 25) {
            insight.append("It is also high in fat, so it can add calories quickly. ");
        } else if (currentFat >= 10) {
            insight.append("It has a moderate amount of fat. ");
        }

        return insight.toString().trim();
    }

    private void showEmptyResultState() {
        tvResultTitle.setText("No food analyzed yet");
        tvResultSubtitle.setText("Take a photo and the app will analyze it automatically.");

        tvFoodValue.setText("-");
        tvQuantityValue.setText("-");
        tvCaloriesValue.setText("-");

        tvProteinValue.setText("Protein\n-");
        tvCarbsValue.setText("Carbs\n-");
        tvFatValue.setText("Fat\n-");

        tvInsightValue.setText("Nutrition insight will appear here after analysis.");
        tvConfidenceValue.setText("Confidence: -");
        tvNoteValue.setText("Nutrition values will appear here after analysis.");
    }

    private void showInfoState(String title, String message) {
        tvResultTitle.setText(title);
        tvResultSubtitle.setText(message);

        tvFoodValue.setText("-");
        tvQuantityValue.setText("-");
        tvCaloriesValue.setText("-");

        tvProteinValue.setText("Protein\n-");
        tvCarbsValue.setText("Carbs\n-");
        tvFatValue.setText("Fat\n-");

        tvInsightValue.setText("Nutrition insight is available after food analysis.");
        tvConfidenceValue.setText("Status: Attention needed");
        tvNoteValue.setText(message);
    }

    private void showAnalysisFailedCard() {
        tvResultTitle.setText("Analysis Failed");
        tvResultSubtitle.setText("We couldn’t analyze this photo.");

        tvFoodValue.setText("-");
        tvQuantityValue.setText("-");
        tvCaloriesValue.setText("-");

        tvProteinValue.setText("Protein\n-");
        tvCarbsValue.setText("Carbs\n-");
        tvFatValue.setText("Fat\n-");

        tvInsightValue.setText("No nutrition insight is available because the analysis failed.");
        tvConfidenceValue.setText("Status: Try again");
        tvNoteValue.setText(
                "What you can try:\n" +
                        "• Check your internet connection\n" +
                        "• Take a clearer photo\n" +
                        "• Make sure the food is visible\n" +
                        "• Try again in a few seconds"
        );
    }

    private void showSavedResultCard(String mealLabel, boolean savedToMyFoods, boolean alreadyExistsInMyFoods) {
        tvResultTitle.setText("Food Saved Successfully");
        tvResultSubtitle.setText("Added to " + mealLabel + ".");

        tvFoodValue.setText(capitalize(analyzedFoodName));
        tvQuantityValue.setText(formatGrams(currentQuantityGrams) + "g");
        tvCaloriesValue.setText(currentCalories + " kcal");

        tvProteinValue.setText("Protein\n" + formatDouble(currentProtein) + "g");
        tvCarbsValue.setText("Carbs\n" + formatDouble(currentCarbs) + "g");
        tvFatValue.setText("Fat\n" + formatDouble(currentFat) + "g");

        tvInsightValue.setText(generateNutritionInsight());
        tvConfidenceValue.setText("Meal: " + mealLabel);

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
            showInfoState("Not Logged In", "Please log in before saving food to My Foods.");
            return;
        }

        if (analyzedFoodName == null || analyzedFoodName.trim().isEmpty()) {
            showInfoState("Missing Food Name", "The food name is missing. Please analyze the image again.");
            return;
        }

        String foodName = capitalize(analyzedFoodName.trim());
        float grams = currentQuantityGrams > 0 ? (float) currentQuantityGrams : 100f;
        String portion = formatGrams(grams) + "g";
        String category = guessCategoryForFood(foodName);

        showAddFoodBottomSheetPrefilled(
                userId,
                foodName,
                category,
                portion,
                grams,
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
            tvAddFoodSubtitle.setText("Review and edit the scanned nutrition values before saving.");
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
            saveAnalyzedFoodToMeal("breakfast", "Breakfast");
        });

        btnLunch.setOnClickListener(v -> {
            dialog.dismiss();
            saveAnalyzedFoodToMeal("lunch", "Lunch");
        });

        btnDinner.setOnClickListener(v -> {
            dialog.dismiss();
            saveAnalyzedFoodToMeal("dinner", "Dinner");
        });

        btnSnacks.setOnClickListener(v -> {
            dialog.dismiss();
            saveAnalyzedFoodToMeal("snacks", "Snacks");
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    private void saveAnalyzedFoodToMeal(String mealValue, String mealLabel) {
        int userId = new SessionManager(requireContext()).getUserId();

        if (userId == -1) {
            showInfoState("Not Logged In", "Please log in before saving food.");
            return;
        }

        if (analyzedFoodName == null || analyzedFoodName.trim().isEmpty()) {
            showInfoState("Missing Food Name", "The food name is missing. Please analyze the image again.");
            return;
        }

        String finalFoodName = capitalize(analyzedFoodName.trim());

        float grams = currentQuantityGrams > 0 ? (float) currentQuantityGrams : 100f;
        float portions = 1f;

        dbConnect db = new dbConnect(requireContext());

        long logResult = db.addFoodLog(
                userId,
                mealValue,
                finalFoodName,
                grams,
                portions,
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
            btnTakePhoto.setEnabled(false);
            btnSaveFood.setEnabled(false);
            if (btnSaveToMyFoods != null) {
                btnSaveToMyFoods.setEnabled(false);
                btnSaveToMyFoods.setClickable(false);
                btnSaveToMyFoods.setAlpha(0.5f);
            }

            btnTakePhoto.setText("Analyzing...");

            tvResultTitle.setText("Analyzing Food Image");
            tvResultSubtitle.setText("Please wait while the app estimates nutrition values.");
            tvFoodValue.setText("Processing...");
            tvQuantityValue.setText("-");
            tvCaloriesValue.setText("-");
            tvProteinValue.setText("Protein\n-");
            tvCarbsValue.setText("Carbs\n-");
            tvFatValue.setText("Fat\n-");
            tvInsightValue.setText("Analyzing nutrition data...");
            tvConfidenceValue.setText("Status: Processing");
            tvNoteValue.setText("This may take a few seconds.");
        } else {
            btnTakePhoto.setEnabled(true);
            btnTakePhoto.setText("Take Photo and Analyze");

            if (hasAnalyzedFood && !hasSavedFood) {
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

            if (!hasAnalyzedFood) {
                btnSaveToMyFoods.setText("Save to My Foods");
            }
        }
    }

    private void setGramsInputText(double grams) {
        isUpdatingGramsText = true;
        etGrams.setText(formatGrams(grams));
        etGrams.setSelection(etGrams.getText() == null ? 0 : etGrams.getText().length());
        isUpdatingGramsText = false;
    }

    private void resetAnalyzedValues() {
        analyzedFoodName = "";

        originalQuantityGrams = 0.0;
        originalCalories = 0;
        originalProtein = 0.0;
        originalCarbs = 0.0;
        originalFat = 0.0;

        currentQuantityGrams = 0.0;
        currentCalories = 0;
        currentProtein = 0.0;
        currentCarbs = 0.0;
        currentFat = 0.0;

        analyzedConfidence = "";
        analyzedNote = "";

        if (etGrams != null) {
            etGrams.setText("");
        }

        if (btnSaveToMyFoods != null) {
            btnSaveToMyFoods.setText("Save to My Foods");
            btnSaveToMyFoods.setAlpha(0.5f);
        }

        showEmptyResultState();
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

    private String capitalize(String text) {
        if (text == null || text.trim().isEmpty()) return "";

        text = text.trim();

        return text.substring(0, 1).toUpperCase(Locale.ROOT) + text.substring(1);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}
