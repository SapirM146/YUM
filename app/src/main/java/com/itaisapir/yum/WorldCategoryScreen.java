package com.itaisapir.yum;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.itaisapir.yum.Logic.CategoryOrRecipe;
import com.itaisapir.yum.Logic.CategoryRecipeAdapter;
import com.itaisapir.yum.Logic.Recipe;
import com.itaisapir.yum.utils.InnerIds;

import java.util.ArrayList;
import java.util.List;

public class WorldCategoryScreen extends AppCompatActivity implements OnMapReadyCallback, WorldCategoryFragment.OnWorldCategoryActionListener, GoogleMap.OnMarkerClickListener {

    private GoogleMap mGoogleMap;
    private ArrayList<Marker> markerList = new ArrayList<>();
    private List<CategoryOrRecipe> categoryAndRecipeList;
    private Button openButton;
    private Button backButton;
    private String currentPath;
    private int level; // level 0  sub category and recipes, level 1 recipes only
    private RecyclerView recyclerView;
    private WorldCategoryFragment listFragment;
    private int recipeIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.world_category_screen);
        categoryAndRecipeList = new ArrayList<>();

        openButton = findViewById(R.id.openButton);
        openButton.setOnClickListener(new View.OnClickListener() { // todo
            @Override
            public void onClick(View view) {
                String path = currentPath + ((Recipe)categoryAndRecipeList.get(recipeIndex)).getRecipeID()+"/";
                Intent intent = new Intent(getBaseContext(), ShowRecipeActivity.class);
                intent.putExtra(InnerIds.PATH_CATEGORY, path);
                intent.putExtra(InnerIds.PRIVATE_OR_PUBLIC, InnerIds.PUBLIC);
                startActivity(intent);
            }
        });

        backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() { // todo
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        mGoogleMap = googleMap;

        //Initialize Google Play Services
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                mGoogleMap.setMyLocationEnabled(false);
            }
        } else {
            mGoogleMap.setMyLocationEnabled(false);
        }
        mGoogleMap.setOnMarkerClickListener(this);
    }

    @Override
    public void updateMarkersData(List<CategoryOrRecipe> categoryAndRecipeList) {
        this.categoryAndRecipeList = categoryAndRecipeList;
        if(mGoogleMap != null) {
            mGoogleMap.clear();
            markerList.clear();
            for (CategoryOrRecipe recipeFromList : this.categoryAndRecipeList) {
                Recipe recipe = (Recipe)recipeFromList;
                LatLng location = new LatLng(recipe.getLat(), recipe.getLon());
                Marker marker = mGoogleMap.addMarker(new MarkerOptions().position(location).title(recipe.getName()));
                markerList.add(marker);
            }
        }
    }

    @Override
    public void moveToMarker(int position) {
        if(mGoogleMap != null) {
            markerList.get(position).showInfoWindow();
            mGoogleMap.animateCamera(CameraUpdateFactory.newLatLng(markerList.get(position).getPosition()));
        }
    }

    @Override
    public void toEnableOpenRecipeButton(boolean toEnable) {
        openButton.setEnabled(toEnable);
    }

    @Override
    public void setCurrentPath(String currentPath) {
        this.currentPath = currentPath;
    }

    @Override
    public void setLevel(int level) {
        this.level = level;
    }

    @Override
    public void setRecyclerView(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
    }

    @Override
    public void setPosition(int position) {
        this.recipeIndex = position;
    }

    @Override
    public void setFragment(WorldCategoryFragment fragment) {
        this.listFragment = fragment;
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        int markerIndex = markerList.indexOf(marker);
        if(markerIndex != -1){
            ((CategoryRecipeAdapter)recyclerView.getAdapter()).setLastClickedPosition(markerIndex);
            recyclerView.getAdapter().notifyDataSetChanged();
            recyclerView.smoothScrollToPosition(markerIndex);
            marker.showInfoWindow();
            mGoogleMap.animateCamera(CameraUpdateFactory.newLatLng(marker.getPosition()));
            setPosition(markerIndex);
            toEnableOpenRecipeButton(true);
            return true;
        }
        toEnableOpenRecipeButton(false);
        return false;
    }

    @Override
    public void onBackPressed() {
        if(level > 0) {
            mGoogleMap.clear();
            toEnableOpenRecipeButton(false);
            listFragment.fragmentBackToBaseCategories();
        } else {
            listFragment.removeListenersFromFragment();
            super.onBackPressed();
        }
    }
}
