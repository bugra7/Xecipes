package com.xecipes.demo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {

    private List<ImageView> images;
    private IImageClickListener imageClickListener;

    public ImageAdapter(List<ImageView> images, IImageClickListener imageClickListener) {
        this.images = images;
        this.imageClickListener = imageClickListener;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_image_selection, parent, false);
        return new ImageAdapter.ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageAdapter.ImageViewHolder holder, int position) {
        holder.exampleImage.setImageDrawable(images.get(position).getDrawable());
        holder.exampleImage.setTag(images.get(position).getTag());
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    class ImageViewHolder extends RecyclerView.ViewHolder{
        ImageView exampleImage;

        public ImageViewHolder(@NonNull final View itemView) {
            super(itemView);

            exampleImage = itemView.findViewById(R.id.example_image);
            exampleImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    imageClickListener.onItemClick(exampleImage);
                }
            });
        }
    }

    public interface IImageClickListener{
        void onItemClick(ImageView imageView);
    }
}
