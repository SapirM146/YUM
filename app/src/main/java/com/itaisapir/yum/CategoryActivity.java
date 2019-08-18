package com.itaisapir.yum;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.itaisapir.yum.Logic.Category;
import com.itaisapir.yum.Logic.CategoryOrRecipe;
import com.itaisapir.yum.Logic.CategoryRecipeAdapter;
import com.itaisapir.yum.utils.InnerIds;
import com.itaisapir.yum.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CategoryActivity extends AppCompatActivity implements CategoryRecipeAdapter.OnItemClicked {
    private Utils utils;
    private DatabaseReference categoryRef;
    private String userID;

    private Button newCategoryButton;
    private Button settingsButton;
    private EditText searchField;

    private RecyclerView categoryRecipeRecyclerView;
    private CategoryRecipeAdapter categoryRecipeRecyclerAdapter;
    private RecyclerView.LayoutManager categoryRecipeLayoutManager;

    private String basePath;

    private List<CategoryOrRecipe> categoryAndRecipeList;
    private List<CategoryOrRecipe> searchCategoryAndRecipeList;
    private boolean doubleBackToExitPressedOnce;
    private boolean splashRun;
    private long currentTime;
    private final int splashMinTime = 3000;

    private LinearLayout searchBarIsEmpty;
    private TextView notFoundText;
    private LinearLayout listOfCategories;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.category_layout);

        final Intent startIntent = new Intent(getApplicationContext(), LocalNotificationService.class);
        startService(startIntent);

        categoryAndRecipeList = new ArrayList<>();
        doubleBackToExitPressedOnce = false;
        Intent intent = getIntent();
        userID = intent.getStringExtra(InnerIds.USER_ID);
        basePath = "/" + userID + "/";
        utils = Utils.getInstance();
        categoryRef = FirebaseDatabase.getInstance().getReference(InnerIds.PRIVATE);

        notFoundText = findViewById(R.id.notFoundText);
        searchBarIsEmpty = findViewById(R.id.searchBarIsEmpty);
        listOfCategories = findViewById(R.id.listOfCategories);

        searchCategoryAndRecipeList = new ArrayList<>();

        newCategoryButton = findViewById(R.id.newCategoryButton);
        settingsButton = findViewById(R.id.settingsButton);

        searchField = findViewById(R.id.searchField);
        categoryRecipeRecyclerView = findViewById(R.id.categoryRecipeList);

        categoryRecipeRecyclerView.setHasFixedSize(true);

        categoryRecipeLayoutManager = new LinearLayoutManager(this);
        categoryRecipeRecyclerView.setLayoutManager(categoryRecipeLayoutManager);

        // Loading the private categeries and recipies of the user and show them
        categoryRef.child(basePath).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                categoryAndRecipeList = utils.loadCategory(dataSnapshot);
                categoryAndRecipeList.add(0, new Category(getResources().getString(R.string.world_category)));
                searchCategoryAndRecipeList.clear();
                searchCategoryAndRecipeList.addAll(categoryAndRecipeList);

                categoryRecipeRecyclerAdapter = new CategoryRecipeAdapter(searchCategoryAndRecipeList);
                categoryRecipeRecyclerAdapter.setOnClick(CategoryActivity.this);
                categoryRecipeRecyclerAdapter.setOnLongClick(CategoryActivity.this);

                categoryRecipeRecyclerView.setAdapter(categoryRecipeRecyclerAdapter);
                if(splashRun){
                    long passedTime = System.currentTimeMillis() - currentTime;
                    if(passedTime < splashMinTime) {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                endSplash();
                            }
                        }, splashMinTime - passedTime);
                    }else{
                        endSplash();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.d("Load Categories", "Failed to read value.", error.toException());
            }
        });
        if(utils.isFirstStart()) {
            SplashScreen();
            splashRun = true;
            currentTime = System.currentTimeMillis();
            utils.setFirstStart(false);
        } else{
            splashRun = false;
        }

        final AlertDialog.Builder newCategoryBuilder = new AlertDialog.Builder(this);
        newCategoryBuilder.setTitle(R.string.add_new_category_title);

        // Set up the input
        final EditText input = new EditText(this);
        final int maxLength = getResources().getInteger(R.integer.maxNameLength);
        final int minLength = getResources().getInteger(R.integer.minNameLength);

        input.setFilters(new InputFilter[] {new InputFilter.LengthFilter(maxLength)});
        newCategoryBuilder.setView(input);

        // Set up the buttons
        newCategoryBuilder.setPositiveButton(R.string.ok, null);
        newCategoryBuilder.setNegativeButton(R.string.cancel, null);

        final AlertDialog newCategoryAlert = newCategoryBuilder.create();

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
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        try{
                                            if (dataSnapshot.hasChild(newCategoryName))
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        Toast toast = utils.createBigToast(getBaseContext(), getResources().getString(R.string.category_exists), Toast.LENGTH_SHORT);
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
                        } else
                            showWhiteAndTopToast(getResources().getString(R.string.category_name_more_then_2));

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

        newCategoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                newCategoryAlert.show();
            }
        });

        final AlertDialog.Builder settingsBuilder = new AlertDialog.Builder(this);
        View settingsView = getLayoutInflater().inflate(R.layout.settings_dialog, null);
        Button signOutButton = settingsView.findViewById(R.id.signOutButton);

        signOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                utils.setFirstStart(true);
                Intent loginIntent = new Intent(getBaseContext(), LoginActivity.class);
                startActivity(loginIntent);
                CategoryActivity.this.finish();
            }
        });

        Switch timeBasedNotificationSwitch = settingsView.findViewById(R.id.timeBasedNotificationSwitch);
        timeBasedNotificationSwitch.setChecked(utils.isLocalNotificationsSwitch());

        timeBasedNotificationSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                LocalNotificationService.toRunService = isChecked;
                utils.saveLocalNotificationSwitch(isChecked);
            }
        });

        final ImageButton languageButton = settingsView.findViewById(R.id.languageButton);

        if(InnerIds.HEBREW.equals(utils.getLanguage())&& InnerIds.ISRAEL.equals(utils.getCountry()))
            languageButton.setImageResource(R.drawable.big_israel_flag);
        else
            languageButton.setImageResource(R.drawable.big_united_states_flag);

        languageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(InnerIds.HEBREW.equals(utils.getLanguage())) {
                    languageButton.setImageResource(R.drawable.big_united_states_flag);
                    utils.setLocale(InnerIds.ENGLISH, InnerIds.US);
                }
                else {
                    languageButton.setImageResource(R.drawable.big_israel_flag);
                    utils.setLocale(InnerIds.HEBREW, InnerIds.ISRAEL);
                }
                Intent changeLanguage = new Intent(getBaseContext(), LoginActivity.class);
                changeLanguage.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(changeLanguage);
            }
        });

        settingsBuilder.setView(settingsView);
        final AlertDialog settingsAlert = settingsBuilder.create();


        settingsButton.setOnClickListener(new View.OnClickListener() { // todo
            @Override
            public void onClick(View view) {
                settingsAlert.show();

            }
        });

        searchField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchCategoryAndRecipeList.clear();

                if(s.length() == 0) {
                    searchCategoryAndRecipeList.addAll(categoryAndRecipeList);
                    searchBarIsEmpty.setVisibility(View.GONE);
                    listOfCategories.setVisibility(View.VISIBLE);
                }
                else{
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
                categoryRecipeRecyclerAdapter.setOnClick(CategoryActivity.this);
                categoryRecipeRecyclerAdapter.setOnLongClick(CategoryActivity.this);
                categoryRecipeRecyclerView.setAdapter(categoryRecipeRecyclerAdapter);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private void SplashScreen() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if(splashRun){
                    Intent intent = new Intent(InnerIds.SPLASH_LONG_TIME);
                    sendBroadcast(intent);
                }
            }
        },10000);
        Intent intent = new Intent(getBaseContext(), SplashActivity.class);
        startActivity(intent);
    }

    private void showWhiteAndTopToast(String message) {
        Toast toast = utils.createBigToastWithColor(getBaseContext(), message, Toast.LENGTH_LONG,Color.WHITE, Color.BLACK);
        if (toast != null) {
            toast.setGravity(Gravity.TOP, 0, 40);
            toast.show();
        }
    }

    private void endSplash(){
        Intent intent = new Intent(InnerIds.SPLASH_END_CODE);
        sendBroadcast(intent);
        splashRun = false;
    }

    @Override
    public void onItemClick(int position, View v) {
        if(searchCategoryAndRecipeList.get(position).getName().equals(getResources().getString(R.string.world_category)) ){
            Intent intent = new Intent(getBaseContext(), WorldCategoryScreen.class);
            startActivity(intent);
        }
        else {
            String path = basePath + searchCategoryAndRecipeList.get(position).getName() + "/";
            Intent intent = new Intent(getBaseContext(), SubcategoryActivity.class);
            intent.putExtra(InnerIds.PATH_CATEGORY, path+InnerIds.IN_CATEGORY);
            startActivity(intent);
            overridePendingTransition(R.anim.fadein, R.anim.fadeout);
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

                            Toast toast = utils.createBigToast(CategoryActivity.this,getResources().getString(R.string.category_remove), Toast.LENGTH_LONG);
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

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast toast = utils.createBigToast(this, getResources().getString(R.string.back_double), Toast.LENGTH_LONG);
        if(toast != null )
            toast.show();
        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce=false;
            }
        }, 3000);
    }
}

