package com.itaisapir.yum.Logic;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.itaisapir.yum.R;

import java.util.List;

public class ShowImagesAdapter extends RecyclerView.Adapter<ShowImagesAdapter.ViewHolder>{

    private List<Uri> uris;
    private Context context;
    private ShowImagesAdapter.OnItemClicked onClick;

    public ShowImagesAdapter(List<Uri> uris, Context context){
        this.uris = uris;
        this.context=context;
    }

    public interface OnItemClicked {
        void onItemClick(int position);
    }

    @NonNull
    @Override
    public ShowImagesAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        View imageView = inflater.inflate(R.layout.image_view_item, parent, false);

        ShowImagesAdapter.ViewHolder holder = new ShowImagesAdapter.ViewHolder(imageView);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
        Glide.with(this.context)
                .load(uris.get(position))
                .addListener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        holder.getProgressBar().setVisibility(View.GONE);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        holder.getProgressBar().setVisibility(View.GONE);
                        holder.getImageLayout().setBackgroundColor(Color.WHITE);
                        return false;
                    }
                })
                .into(holder.getImage());

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClick.onItemClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return uris.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private ImageView image;
        private ProgressBar progressBar;
        private LinearLayout imageLayout;

        public ViewHolder(View parent) {
            super(parent);
            image = parent.findViewById(R.id.imageView);
            progressBar = parent.findViewById(R.id.progress);
            imageLayout = parent.findViewById(R.id.imageLayout);
        }

        public ImageView getImage() {
            return image;
        }
        public void setImage(ImageView image) {
            this.image = image;
        }

        public ProgressBar getProgressBar() {
            return progressBar;
        }
        public void setProgressBar(ProgressBar progressBar) {
            this.progressBar = progressBar;
        }

        public LinearLayout getImageLayout() {
            return imageLayout;
        }
        public void setImageLayout(LinearLayout imageLayout) {
            this.imageLayout = imageLayout;
        }
    }

    public void setOnClick(ShowImagesAdapter.OnItemClicked onClick)
    {
        this.onClick=onClick;
    }
}
