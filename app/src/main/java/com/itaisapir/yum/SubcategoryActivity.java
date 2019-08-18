package com.itaisapir.yum;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.itaisapir.yum.Logic.CategoryOrRecipe;
import com.itaisapir.yum.Logic.CategoryRecipeAdapter;
import com.itaisapir.yum.Logic.Recipe;
import com.itaisapir.yum.utils.InnerIds;
import com.itaisapir.yum.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SubcategoryActivity extends AppCompatActivity implements CategoryRecipeAdapter.OnItemClicked{
    private Utils utils;
    private DatabaseReference categoryRef;

    private Button newCategoryButton;
    private Button newRecipeButton;
    private TextView subCategoryTitle;
    private EditText searchField;

    private RecyclerView categoryRecipeRecyclerView;
    private CategoryRecipeAdapter categoryRecipeRecyclerAdapter;
    private RecyclerView.LayoutManager categoryRecipeLayoutManager;

    private SharedPreferences preferences;

    private int level; // level 0: sub category and recipes, level 1: recipes only
    private String basePath ;
    private String baseTitle;
    private String currentPath ;
    private String searchFirstLevel;

    private List<CategoryOrRecipe> categoryAndRecipeList;
    private List<CategoryOrRecipe> searchCategoryAndRecipeList;

    private ValueEventListener recyclerlistener;
    private TextWatcher searchlistener;

    private LinearLayout searchBarIsEmpty;
    private TextView notFoundText;
    private LinearLayout listOfCategories;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.subcategory_layout);
        subCategoryTitle = findViewById(R.id.subCategoryTitle);
        categoryAndRecipeList = new ArrayList<>();

        Intent intent = getIntent();
        basePath = intent.getStringExtra(InnerIds.PATH_CATEGORY);
        currentPath = basePath;
        String[] title = basePath.split("/");
        baseTitle = title[title.length-2];
        utils = Utils.getInstance();
        categoryRef = FirebaseDatabase.getInstance().getReference(InnerIds.PRIVATE);
        searchCategoryAndRecipeList = new ArrayList<>();

        notFoundText = findViewById(R.id.notFoundText);
        searchBarIsEmpty = findViewById(R.id.searchBarIsEmpty);
        listOfCategories = findViewById(R.id.listOfCategories);

        level=0;
        newCategoryButton = findViewById(R.id.newSubcategoryButton);
        newRecipeButton = findViewById(R.id.newRecipeButton);

        searchField = findViewById(R.id.subSearchField);

        categoryRecipeRecyclerView = findViewById(R.id.subCategoryRecipeList);
        categoryRecipeRecyclerView.setHasFixedSize(true);

        categoryRecipeLayoutManager = new LinearLayoutManager(this);
        categoryRecipeRecyclerView.setLayoutManager(categoryRecipeLayoutManager);

        createListenerRecycler();
        createSearchListener();
        enterCategory(basePath,baseTitle );

        final android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle(R.string.add_new_category_title);

        // Set up the input
        final EditText input = new EditText(this);
        final int maxLength = getResources().getInteger(R.integer.maxNameLength);
        final int minLength = getResources().getInteger(R.integer.minNameLength);

        input.setFilters(new InputFilter[] {new InputFilter.LengthFilter(maxLength)});
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton(R.string.ok, null);
        builder.setNegativeButton(R.string.cancel, null);

        final AlertDialog newCategoryAlert = builder.create();
        newCategoryAlert.setOnShowListener(new DialogInterface.OnShowListener() {

            @Override
            public void onShow(final DialogInterface dialog) {
                Button okButton = newCategoryAlert.getButton(AlertDialog.BUTTON_POSITIVE);
                okButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        final String newCategoryName = input.getText().toString();
                        if (newCategoryName.length() >= minLength) {
                            if(!utils.checkCategoryName(newCategoryName))
                                showWhiteAndTopToast(getResources().getString(R.string.category_name_not_special_letters));

                            else if(newCategoryName.equalsIgnoreCase(InnerIds.WORLD_CATEGORY_ENGLISH)
                                    || newCategoryName.equals(InnerIds.WORLD_CATEGORY_HEBREW))
                                showWhiteAndTopToast(getResources().getString(R.string.category_name_not_world_category)+ " " + newCategoryName);

                            else {
                                final Map<String, Object> generalCategories = utils.createCategory(basePath, newCategoryName);
                                categoryRef.child(basePath).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot)
                                    {
                                        try{
                                            if (dataSnapshot.hasChild(newCategoryName))
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        Toast toast = utils.createBigToast(getBaseContext(), getResources().getString(R.string.category_exists), Toast.LENGTH_LONG);
                                                        if (toast != null)
                                                            toast.show();
                                                    }
                                                });
                                            else
                                                categoryRef.updateChildren(generalCategories);
                                        }catch(com.google.firebase.database.DatabaseException e){
                                            showWhiteAndTopToast(getResources().getString(R.string.error_message));
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });
                                input.getText().clear();
                                dialog.dismiss();

                            }
                        } else {
                            showWhiteAndTopToast(getResources().getString(R.string.category_name_more_then_2));
                        }
                    }
                });

                Button cancelButton = newCategoryAlert.getButton(AlertDialog.BUTTON_NEGATIVE);
                cancelButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        input.getText().clear();
                        dialog.cancel();
                    }
                });
            }
        });

        newCategoryButton.setOnClickListener(new View.OnClickListener() { // todo
            @Override
            public void onClick(View view) {
                newCategoryAlert.show();
            }
        });

        newRecipeButton.setOnClickListener(new View.OnClickListener() { // todo
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getBaseContext(), AddRecipeActivity.class);
                intent.putExtra(InnerIds.PATH_CATEGORY, currentPath);
                startActivity(intent);
            }
        });

        searchField.addTextChangedListener(searchlistener);
    }

    private void createSearchListener() {
        searchlistener = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchCategoryAndRecipeList.clear();

                if(s.length() == 0) {
                    searchFirstLevel = "";
                    searchCategoryAndRecipeList.addAll(categoryAndRecipeList);
                    searchBarIsEmpty.setVisibility(View.GONE);
                    listOfCategories.setVisibility(View.VISIBLE);
                }
                else{
                    if(level == 0)
                        searchFirstLevel = s.toString();
                    for (CategoryOrRecipe item:categoryAndRecipeList) {
                        if(item.getName().toLowerCase().contains(s.toString().toLowerCase()))
                            searchCategoryAndRecipeList.add(item);
                    }
                    if(searchCategoryAndRecipeList.size() == 0) {
                        String notFound = getResources().getString(R.string.world_search_not_found)
                                + " " + s.toString();
                        notFoundText.setText(notFound);
                        listOfCategories.setVisibility(View.GONE);
                        searchBarIsEmpty.setVisibility(View.VISIBLE);
                    } else{
                        searchBarIsEmpty.setVisibility(View.GONE);
                        listOfCategories.setVisibility(View.VISIBLE);
                    }
                }

                categoryRecipeRecyclerAdapter = new CategoryRecipeAdapter(searchCategoryAndRecipeList);
                categoryRecipeRecyclerAdapter.setOnClick(SubcategoryActivity.this);
                categoryRecipeRecyclerAdapter.setOnLongClick(SubcategoryActivity.this);
                categoryRecipeRecyclerView.setAdapter(categoryRecipeRecyclerAdapter);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        };
    }

    private void createListenerRecycler() {
        recyclerlistener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                categoryAndRecipeList = utils.loadCategory(dataSnapshot);
                searchCategoryAndRecipeList.clear();
                if(level > 0) {
                    searchCategoryAndRecipeList.addAll(categoryAndRecipeList);

                    categoryRecipeRecyclerAdapter = new CategoryRecipeAdapter(searchCategoryAndRecipeList);
                    categoryRecipeRecyclerAdapter.setOnClick(SubcategoryActivity.this);
                    categoryRecipeRecyclerAdapter.setOnLongClick(SubcategoryActivity.this);
                    categoryRecipeRecyclerView.setAdapter(categoryRecipeRecyclerAdapter);
                }else
                    searchField.setText(searchFirstLevel);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.d("Load Categories", "Failed to read value.", error.toException());
            }
        };
    }

    public void showWhiteAndTopToast(String message) {
        Toast toast = utils.createBigToastWithColor(getBaseContext(), message, Toast.LENGTH_LONG, Color.WHITE, Color.BLACK);
        if (toast != null) {
            toast.setGravity(Gravity.TOP, 0, 40);
            toast.show();
        }
    }

    @Override
    public void onBackPressed() {
        if(level > 0) {
            --level;
            newCategoryButton.setVisibility(View.VISIBLE);
            enterCategory(basePath, baseTitle);
        } else
            super.onBackPressed();
    }

    @Override
    public void onItemClick(int position, View v) {
        InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.hideSoftInputFromWindow(searchField.getWindowToken(), 0);
        if (searchCategoryAndRecipeList.get(position) instanceof Recipe) {
            String path = currentPath + ((Recipe)searchCategoryAndRecipeList.get(position)).getRecipeID()+"/";
            Intent intent = new Intent(getBaseContext(), ShowRecipeActivity.class);
            intent.putExtra(InnerIds.PATH_CATEGORY, path);
            intent.putExtra(InnerIds.PRIVATE_OR_PUBLIC, InnerIds.PRIVATE);
            startActivity(intent);
        }
        else if (level == 0) {
            ++level;
            final String nextSubCategoryName = searchCategoryAndRecipeList.get(position).getName();
            newCategoryButton.setVisibility(View.INVISIBLE);

            final LinearLayout subcategoryLayout = findViewById(R.id.subcategoryLayout);
            subcategoryLayout.setVisibility(View.GONE);

            final LinearLayout loadingLayout = findViewById(R.id.loadingLayout);
            loadingLayout.setVisibility(View.VISIBLE);
            final ImageView progressImage = findViewById(R.id.progressImage);
            Glide.with(getBaseContext())
                    .asGif().load(R.drawable.cooking_loader2)
                    .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true))
                    .into(progressImage);
            enterCategory(basePath + nextSubCategoryName + "/" + InnerIds.IN_CATEGORY, nextSubCategoryName);


            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    loadingLayout.setVisibility(View.GONE);
                    subcategoryLayout.setVisibility(View.VISIBLE);
                }
            }, 2000);
        }
    }

    @Override
    public void onItemLongClick(final int position, View v) {
        if(!searchCategoryAndRecipeList.get(position).getName().equals(getResources().getString(R.string.world_category)) ){
            final AlertDialog.Builder removeBuilder = new AlertDialog.Builder(this);
            removeBuilder.setTitle(R.string.remove_q);

            // Set up the buttons
            removeBuilder.setPositiveButton(R.string.ok, null);
            removeBuilder.setNegativeButton(R.string.cancel, null);

            final AlertDialog removeCategoryAlert = removeBuilder.create();

            removeCategoryAlert.setOnShowListener(new DialogInterface.OnShowListener() {

                @Override
                public void onShow(final DialogInterface dialog) {
                    Button okButton = removeCategoryAlert.getButton(AlertDialog.BUTTON_POSITIVE);
                    okButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {

                            DatabaseReference publicRecipeDBRef = FirebaseDatabase.getInstance().getReference(InnerIds.PRIVATE);
                            publicRecipeDBRef.child(basePath).child(searchCategoryAndRecipeList.get(position).getName()).removeValue();

                            Toast toast = utils.createBigToast(SubcategoryActivity.this,getResources().getString(R.string.category_remove), Toast.LENGTH_LONG);
                            if(toast != null )
                                toast.show();
//                            ShowRecipeActivity.this.finish();
                            dialog.dismiss();


                        }
                    });
                    Button cancelButton = removeCategoryAlert.getButton(AlertDialog.BUTTON_NEGATIVE);
                    cancelButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            dialog.cancel();
                        }
                    });
                }
            });
            removeCategoryAlert.show();
        }
    }

    private void enterCategory(final String path, String title) {
        categoryRef.child(currentPath).removeEventListener(recyclerlistener);
        searchField.removeTextChangedListener(searchlistener);

        searchField.getText().clear();
        searchField.clearFocus();
        currentPath = path;
        subCategoryTitle.setText(title);
        categoryRef.child(path).addValueEventListener(recyclerlistener);
        searchField.addTextChangedListener(searchlistener);
    }
}
