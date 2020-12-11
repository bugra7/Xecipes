package com.xecipes.demo;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.huawei.hmf.tasks.OnFailureListener;
import com.huawei.hmf.tasks.OnSuccessListener;
import com.huawei.hmf.tasks.Task;
import com.huawei.hms.image.vision.crop.CropLayoutView;
import com.huawei.hms.mlsdk.MLAnalyzerFactory;
import com.huawei.hms.mlsdk.classification.MLImageClassification;
import com.huawei.hms.mlsdk.classification.MLImageClassificationAnalyzer;
import com.huawei.hms.mlsdk.common.MLException;
import com.huawei.hms.mlsdk.common.MLFrame;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.xecipes.demo.ExampleImageSelectionActivity.SELECTED_DRAWABLE_ID_TAG;

public class MainActivity extends AppCompatActivity {

    private final static int STORAGE_REQ_CODE = 1905;
    private final static int EXAMPLE_IMAGE_REQ_CODE = 1923;

    private final static String NO_INGREDIENTS_TEXT = "No Ingredient is selected!";
    private final static String NO_IMAGE_TEXT = "No Image is selected!";
    private final static String SUCCESS_TEXT = "At least one of the selected ingredients is found in the image.";
    private final static String FAIL_TEXT = "Selected ingredients are not found in the image!";
    private final static String ERROR_TEXT = "An error occured!";

    private Drawable FAIL_ICON, SUCCESS_ICON;

    private int CROPPED_IMAGE_COUNT;
    private int checkedImageCount;

    private ImageView selectedImage, alertImage;
    private Spinner spinner1, spinner2;
    private TextView alertText, descriptionText;
    private Bitmap selectedImageBitmap;
    private CropLayoutView cropLayoutView;
    private AlertDialog progressDialog;

    private final static String[] ingredientOptions = {"<Empty>", "Egg", "Meat", "Rice", "Salad"};
    private List<String> selectedIngredients, foundIngredients;

    private MLImageClassificationAnalyzer cloudAnalyzer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FAIL_ICON = getResources().getDrawable(R.drawable.ic_fail);
        SUCCESS_ICON = getResources().getDrawable(R.drawable.ic_success);

        selectedImage = findViewById(R.id.selected_image);
        alertImage = findViewById(R.id.alert_image);
        alertText = findViewById(R.id.alert_text);
        descriptionText = findViewById(R.id.description_text);
        cropLayoutView = findViewById(R.id.crop_layout_view);

        initializeSpinners();

        Button storageButton = findViewById(R.id.storage_button);
        storageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //get image from storage
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");
                startActivityForResult(photoPickerIntent, STORAGE_REQ_CODE);
            }
        });

        Button exampleImageButton = findViewById(R.id.example_image_button);
        exampleImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //select an image from example images
                Intent exampleImageSelectionIntent = new Intent(MainActivity.this, ExampleImageSelectionActivity.class);
                startActivityForResult(exampleImageSelectionIntent, EXAMPLE_IMAGE_REQ_CODE);
            }
        });

        Button checkImageButton = findViewById(R.id.check_image_button);
        checkImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedImageBitmap == null){
                    alertImage.setImageDrawable(FAIL_ICON);
                    alertText.setText(NO_IMAGE_TEXT);
                }
                else if (spinner1.getSelectedItemId() == 0 && spinner2.getSelectedItemId() == 0){
                    alertImage.setImageDrawable(FAIL_ICON);
                    alertText.setText(NO_INGREDIENTS_TEXT);
                }
                else {
                    progressDialog = createProgressAlertDialog();

                    cloudAnalyzer = MLAnalyzerFactory.getInstance().getRemoteImageClassificationAnalyzer();

                    checkedImageCount = 0;

                    getSelectedIngredients();
                    foundIngredients = new ArrayList<>();

                    List<Bitmap> croppedImages = getCroppedImages(selectedImageBitmap);
                    CROPPED_IMAGE_COUNT = croppedImages.size();

                    for (Bitmap croppedPicture : croppedImages){
                        classifyPicture(croppedPicture);
                    }
                }
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == Activity.RESULT_OK){
            if (requestCode == STORAGE_REQ_CODE) {
                Uri selectedImageUri = data.getData();
                try {
                    selectedImageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
                    selectedImage.setImageBitmap(selectedImageBitmap);
                    selectedImage.setVisibility(View.VISIBLE);
                } catch (IOException ignored) {}
            }

            else if (requestCode == EXAMPLE_IMAGE_REQ_CODE) {
                int selectedDrawableId = data.getIntExtra(SELECTED_DRAWABLE_ID_TAG, 0);

                selectedImageBitmap = BitmapFactory.decodeResource(getResources(), selectedDrawableId);
                selectedImage.setImageBitmap(selectedImageBitmap);
                selectedImage.setVisibility(View.VISIBLE);
            }
        }
    }

    private void initializeSpinners() {
        spinner1 = findViewById(R.id.spinner1);
        spinner2 = findViewById(R.id.spinner2);

        initializeSingleSpinner(spinner1);
        initializeSingleSpinner(spinner2);
    }

    private void initializeSingleSpinner(Spinner spinner) {
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, ingredientOptions);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerAdapter);
    }

    private void getSelectedIngredients() {
        selectedIngredients = new ArrayList<>();
        if (spinner1.getSelectedItemId() != 0){
            selectedIngredients.add(ingredientOptions[(int) spinner1.getSelectedItemId()]);
        }
        if (spinner2.getSelectedItemId() != 0){
            selectedIngredients.add(ingredientOptions[(int) spinner2.getSelectedItemId()]);
        }
    }

    private List<Bitmap> getCroppedImages(Bitmap selectedImageBitmap) {
        List<Bitmap> croppedImages = new ArrayList<>();
        croppedImages.add(selectedImageBitmap);

        int height = selectedImageBitmap.getHeight();
        int width = selectedImageBitmap.getWidth();

        croppedImages.add(cropImage(selectedImageBitmap,width*20/100,height*20/100,width*80/100,height*80/100));
        croppedImages.add(cropImage(selectedImageBitmap,width*5/100,height*25/100,width*50/100,height*75/100));
        croppedImages.add(cropImage(selectedImageBitmap,width*50/100,height*25/100,width*95/100,height*75/100));
        croppedImages.add(cropImage(selectedImageBitmap,width*25/100,height*5/100,width*75/100,height*50/100));
        croppedImages.add(cropImage(selectedImageBitmap,width*25/100,height*50/100,width*75/100,height*95/100));

        return croppedImages;
    }

    private Bitmap cropImage(Bitmap selectedImageBitmap, int left, int top, int right, int bottom) {
        cropLayoutView.setImageBitmap(selectedImageBitmap);
        cropLayoutView.setCropRect(new Rect(left,top,right,bottom));
        return cropLayoutView.getCroppedImage();
    }

    private void classifyPicture(Bitmap bitmap) {
        MLFrame frame = MLFrame.fromBitmap(bitmap);

        Task<List<MLImageClassification>> task = cloudAnalyzer.asyncAnalyseFrame(frame);
        task.addOnSuccessListener(new OnSuccessListener<List<MLImageClassification>>() {
            @Override
            public void onSuccess(List<MLImageClassification> classifications) {
                checkIngredients(classifications);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                checkedImageCount++;
                try {
                    MLException mlException = (MLException) e;

                    alertImage.setImageDrawable(FAIL_ICON);
                    alertText.setText(ERROR_TEXT);
                    descriptionText.setText("Error Code : " + mlException.getErrCode() + "\n\nError Message : " + mlException.getMessage());

                    progressDialog.cancel();
                } catch (Exception ignored) {}
            }
        });
    }

    private void checkIngredients(List<MLImageClassification> classifications) {
        int matchedIngredientCount = 0;
        for (String selectedIngredient : selectedIngredients){
            for (MLImageClassification classification : classifications){
                String foundIngredient = classification.getName();
                if (!foundIngredients.contains(foundIngredient)){
                    foundIngredients.add(foundIngredient);
                }

                if (foundIngredient.toLowerCase().contains(selectedIngredient.toLowerCase())){
                    matchedIngredientCount++;
                    break;
                }
            }
        }

        descriptionText.setText(prepareDescriptionText());
        if (matchedIngredientCount > 0){
            alertImage.setImageDrawable(SUCCESS_ICON);
            alertText.setText(SUCCESS_TEXT);

            progressDialog.cancel();

            releaseML_Analyzer();
        }
        else {
            checkedImageCount++;
            if (checkedImageCount == CROPPED_IMAGE_COUNT){
                alertImage.setImageDrawable(FAIL_ICON);
                alertText.setText(FAIL_TEXT);

                progressDialog.cancel();

                releaseML_Analyzer();
            }
        }
    }

    private void releaseML_Analyzer() {
        try {
            if (cloudAnalyzer != null) {
                cloudAnalyzer.stop();
            }
        } catch (IOException ignored) {}
    }

    private String prepareDescriptionText() {
        String text = "You selected : ";
        for (int i = 0; i < selectedIngredients.size(); i++){
            if (i != 0) text += ", ";
            text += selectedIngredients.get(i);
        }

        text += "\n\nPicture contains : ";
        for (int i = 0; i < foundIngredients.size(); i++){
            if (i != 0) text += ", ";
            text += foundIngredients.get(i);
        }

        return text;
    }

    private AlertDialog createProgressAlertDialog(){
        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(this);
        builder.setTitle("Loading...");

        LayoutInflater factory = LayoutInflater.from(this);
        View progressBar = factory.inflate(R.layout.progress_bar, null);
        builder.setView(progressBar);

        AlertDialog alertDialog = builder.create();
        alertDialog.setCancelable(false);
        alertDialog.show();
        return alertDialog;
    }
}
