package com.itaisapir.yum.Logic;

import android.content.Context;
import android.net.Uri;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.itaisapir.yum.R;
import com.ortiz.touchview.TouchImageView;

import java.util.ArrayList;


public class SlidingImage_Adapter extends PagerAdapter {
    private ArrayList<Uri> imagesUriList;
    private LayoutInflater inflater;
    private Context context;

    public SlidingImage_Adapter(Context context,ArrayList<String> imagesStringList) {
        this.context = context;
        imagesUriList = new ArrayList<>();
        for (int i = 0; i < imagesStringList.size() ; i++) {
            imagesUriList.add(Uri.parse(imagesStringList.get(i)));
        }
        inflater = LayoutInflater.from(context);
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }

    @Override
    public int getCount() {
        return imagesUriList.size();
    }

    @Override
    public Object instantiateItem(ViewGroup view, int position) {
        View imageLayout = inflater.inflate(R.layout.slidingimages_item, view, false);

        final TouchImageView imageView = (TouchImageView) imageLayout.findViewById(R.id.image);

        Glide.with(this.context)
                .load(imagesUriList.get(position))
                .into(imageView);

        view.addView(imageLayout, 0);

        return imageLayout;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view.equals(object);
    }

    @Override
    public void restoreState(Parcelable state, ClassLoader loader) {
    }

    @Override
    public Parcelable saveState() {
        return null;
    }


}