package com.xecipes.demo;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ExampleImageSelectionActivity extends AppCompatActivity implements ImageAdapter.IImageClickListener {

    public static final String SELECTED_DRAWABLE_ID_TAG = "SELECTED_DRAWABLE_ID_TAG";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_selection);

        ImageAdapter imageAdapter = new ImageAdapter(getExampleImages() , this);

        RecyclerView imagesRecyclerView = findViewById(R.id.example_image_recycler);
        imagesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        imagesRecyclerView.setAdapter(imageAdapter);
    }

    private List<ImageView> getExampleImages() {
        List<Integer> exampleDrawables = getExampleDrawableIDs();

        List<ImageView> exampleImages = new ArrayList<>();

        fillExampleImages(exampleDrawables, exampleImages);

        return exampleImages;
    }

    private List<Integer> getExampleDrawableIDs() {
        List<Integer> exampleDrawables = new ArrayList<>();
        exampleDrawables.add(R.drawable.img_egg_salad);
        exampleDrawables.add(R.drawable.img_egg_salad2);
        exampleDrawables.add(R.drawable.img_meat_rice);
        exampleDrawables.add(R.drawable.img_meat_salad);
        return exampleDrawables;
    }

    private void fillExampleImages(List<Integer> exampleDrawables, List<ImageView> exampleImages) {
        for (Integer drawableId : exampleDrawables){
            Drawable drawable = getResources().getDrawable(drawableId);

            ImageView imageView = new ImageView(this);
            imageView.setImageDrawable(drawable);
            imageView.setTag((drawableId));

            exampleImages.add(imageView);
        }
    }

    @Override
    public void onItemClick(ImageView imageView) {
        Intent data = new Intent();

        data.putExtra(SELECTED_DRAWABLE_ID_TAG, (int)imageView.getTag());
        setResult(Activity.RESULT_OK, data);
        finish();
    }
}
