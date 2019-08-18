package com.itaisapir.yum;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;

import com.itaisapir.yum.Logic.SlidingImage_Adapter;
import com.itaisapir.yum.utils.InnerIds;
import com.viewpagerindicator.CirclePageIndicator;

import java.util.ArrayList;

public class ImagesSliderActivity extends AppCompatActivity {
    private ViewPager mPager;
    private static int currentPage;
    private ArrayList<String> ImagesArray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.images_slider);
        init();
    }

    private void init() {
        Intent intent = getIntent();
        ImagesArray = intent.getStringArrayListExtra(InnerIds.IMAGES_ARRAY);
        currentPage = intent.getIntExtra(InnerIds.CURRENT_IMAGE,0);

        mPager = findViewById(R.id.pager);

        mPager.setAdapter(new SlidingImage_Adapter(this,ImagesArray));

        CirclePageIndicator indicator = findViewById(R.id.indicator);

        indicator.setViewPager(mPager);

        final float density = getResources().getDisplayMetrics().density;

        //Set circle indicator radius
        indicator.setRadius(7 * density);

        mPager.setCurrentItem(currentPage, true);

        // Pager listener over indicator
        indicator.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageSelected(int position) {
                currentPage = position;
            }

            @Override
            public void onPageScrolled(int pos, float arg1, int arg2) {

            }

            @Override
            public void onPageScrollStateChanged(int pos) {

            }
        });

    }

}
