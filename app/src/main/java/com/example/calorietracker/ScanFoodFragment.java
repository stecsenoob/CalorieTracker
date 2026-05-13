package com.example.calorietracker;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

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

    private ImageView imgFoodPreview;
    private TextView tvImagePlaceholderIcon;
    private TextView tvImagePlaceholder;
    private TextView tvResult;

    private MaterialCardView cardQuantity;
    private TextInputEditText etGrams;
    private CheckBox cbSaveToMyFoods;

    private MaterialButton btnTakePhoto;
    private MaterialButton btnAnalyzeFood;
    private MaterialButton btnSaveFood;
    private MaterialButton btnBack;

    private Uri currentPhotoUri;
    private boolean hasPhoto = false;
    private boolean hasAnalyzedFood = false;
    private boolean hasSavedFood = false;
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
                        Toast.makeText(requireContext(), "Camera permission is required", Toast.LENGTH_SHORT).show();

                        if (tvResult != null) {
                            tvResult.setText("Camera permission is required to take a food photo.");
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

                        resetAnalyzedValues();
                        showImagePreview(currentPhotoUri);

                        cardQuantity.setVisibility(View.GONE);
                        tvResult.setText("Image captured successfully.\n\nNow click Analyze Food.");
                        disableSaveButton();

                    } else {
                        Toast.makeText(requireContext(), "No image captured", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        imgFoodPreview = view.findViewById(R.id.imgFoodPreview);
        tvImagePlaceholderIcon = view.findViewById(R.id.tvImagePlaceholderIcon);
        tvImagePlaceholder = view.findViewById(R.id.tvImagePlaceholder);
        tvResult = view.findViewById(R.id.tvResult);

        cardQuantity = view.findViewById(R.id.cardQuantity);
        etGrams = view.findViewById(R.id.etGrams);
        cbSaveToMyFoods = view.findViewById(R.id.cbSaveToMyFoods);

        btnTakePhoto = view.findViewById(R.id.btnTakePhoto);
        btnAnalyzeFood = view.findViewById(R.id.btnAnalyzeFood);
        btnSaveFood = view.findViewById(R.id.btnSaveFood);
        btnBack = view.findViewById(R.id.btnBack);

        cardQuantity.setVisibility(View.GONE);
        disableSaveButton();

        btnBack.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().popBackStack();
        });

        btnTakePhoto.setOnClickListener(v -> {
            checkCameraPermissionAndOpenCamera();
        });

        btnAnalyzeFood.setOnClickListener(v -> {
            if (!hasPhoto || currentPhotoUri == null) {
                Toast.makeText(requireContext(), "Please take a photo first", Toast.LENGTH_SHORT).show();
                return;
            }

            analyzeFoodWithAI();
        });

        btnSaveFood.setOnClickListener(v -> {
            if (!hasAnalyzedFood) {
                Toast.makeText(requireContext(), "Analyze the food first", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(requireContext(), "Camera error: " + e.getMessage(), Toast.LENGTH_LONG).show();

            if (tvResult != null) {
                tvResult.setText(
                        "Camera error:\n\n" +
                                e.toString() + "\n\n" +
                                "Check:\n" +
                                "1. AndroidManifest.xml provider\n" +
                                "2. res/xml/file_paths.xml\n" +
                                "3. CAMERA permission\n" +
                                "4. Package name / authority"
                );
            }
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

    private void showImagePreview(Uri imageUri) {
        imgFoodPreview.setImageURI(imageUri);
        imgFoodPreview.setVisibility(View.VISIBLE);

        tvImagePlaceholderIcon.setVisibility(View.GONE);
        tvImagePlaceholder.setVisibility(View.GONE);
    }

    private void analyzeFoodWithAI() {
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
                    if (!isAdded() || tvResult == null) return;

                    hasAnalyzedFood = true;
                    hasSavedFood = false;

                    cardQuantity.setVisibility(View.VISIBLE);
                    setGramsInputText(currentQuantityGrams);

                    updateResultText();
                    enableSaveButton();
                    setLoadingState(false);

                    Toast.makeText(requireContext(), "Food analyzed successfully", Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (!isAdded() || tvResult == null) return;

                    hasAnalyzedFood = false;
                    hasSavedFood = false;

                    cardQuantity.setVisibility(View.GONE);
                    disableSaveButton();
                    setLoadingState(false);

                    tvResult.setText(
                            "Error analyzing food:\n\n" +
                                    e.getMessage() + "\n\n" +
                                    "Check:\n" +
                                    "1. Internet connection\n" +
                                    "2. Render server is running\n" +
                                    "3. OpenAI API key is valid\n" +
                                    "4. OpenAI billing/credits are active"
                    );

                    Toast.makeText(requireContext(), "AI analysis failed", Toast.LENGTH_SHORT).show();
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

            updateResultText();

        } catch (NumberFormatException ignored) {
            // Ignore invalid input while user is typing
        }
    }

    private void updateResultText() {
        tvResult.setText(
                "AI result:\n\n" +
                        "Food: " + capitalize(analyzedFoodName) + "\n" +
                        "Quantity: " + formatGrams(currentQuantityGrams) + "g\n\n" +
                        "Calories: " + currentCalories + " kcal\n" +
                        "Protein: " + formatDouble(currentProtein) + "g\n" +
                        "Carbs: " + formatDouble(currentCarbs) + "g\n" +
                        "Fat: " + formatDouble(currentFat) + "g\n\n" +
                        "Confidence: " + analyzedConfidence + "\n" +
                        "Note: " + analyzedNote + "\n\n" +
                        "You can edit grams before saving."
        );
    }

    private void showMealChoiceDialog() {
        String[] mealLabels = {"Breakfast", "Lunch", "Dinner", "Snacks"};
        String[] mealValues = {"breakfast", "lunch", "dinner", "snacks"};

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Add to meal")
                .setItems(mealLabels, (dialog, which) -> {
                    saveAnalyzedFoodToMeal(mealValues[which], mealLabels[which]);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveAnalyzedFoodToMeal(String mealValue, String mealLabel) {
        int userId = new SessionManager(requireContext()).getUserId();

        if (userId == -1) {
            Toast.makeText(requireContext(), "User is not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        if (analyzedFoodName == null || analyzedFoodName.trim().isEmpty()) {
            Toast.makeText(requireContext(), "Food name is missing", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(requireContext(), "Failed to save food", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean savedToMyFoods = false;
        boolean alreadyExistsInMyFoods = false;

        if (cbSaveToMyFoods != null && cbSaveToMyFoods.isChecked()) {
            String portion = formatGrams(grams) + "g";

            float p = (float) currentProtein;
            float f = (float) currentFat;
            float c = (float) currentCarbs;

            if (db.userFoodExists(userId, finalFoodName, grams, currentCalories, p, f, c)) {
                alreadyExistsInMyFoods = true;
            } else {
                long foodResult = db.addUserFood(
                        userId,
                        finalFoodName,
                        portion,
                        grams,
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
                        "Food: " + finalFoodName + "\n" +
                        "Meal: " + mealLabel + "\n" +
                        "Quantity: " + formatGrams(currentQuantityGrams) + "g\n\n" +
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
            btnAnalyzeFood.setEnabled(false);
            btnTakePhoto.setEnabled(false);
            btnSaveFood.setEnabled(false);

            btnAnalyzeFood.setText("Analyzing...");
            tvResult.setText("Analyzing food image...\n\nPlease wait.");
        } else {
            btnAnalyzeFood.setEnabled(true);
            btnTakePhoto.setEnabled(true);

            btnAnalyzeFood.setText("Analyze Food");

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
        btnSaveFood.setText("Save Food");
    }

    private void disableSaveButton() {
        btnSaveFood.setEnabled(false);
        btnSaveFood.setAlpha(0.5f);

        if (!hasSavedFood) {
            btnSaveFood.setText("Save Food");
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

        if (cbSaveToMyFoods != null) {
            cbSaveToMyFoods.setChecked(false);
        }
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