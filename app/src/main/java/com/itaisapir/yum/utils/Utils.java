package com.itaisapir.yum.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.itaisapir.yum.Logic.Category;
import com.itaisapir.yum.Logic.CategoryOrRecipe;
import com.itaisapir.yum.Logic.Recipe;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

public class Utils {
    private static Utils fireBaseDB;
    private Toast lastToast;
    private String language;
    private String country;
    private boolean localNotificationsSwitch;

    private SharedPreferences preferences;
    private boolean isFirstStart;

    private Utils() {
        lastToast = null;
        isFirstStart = true;
    }

    //Singleton
    public static Utils getInstance() {
        if (fireBaseDB == null)
            fireBaseDB = new Utils();
        return fireBaseDB;
    }

    public String getLanguage() {
        return language;
    }

    public String getCountry() {
        return country;
    }

    public boolean isLocalNotificationsSwitch() {
        return localNotificationsSwitch;
    }

    public boolean isFirstStart() {
        return isFirstStart;
    }

    public void setFirstStart(boolean firstStart) {
        isFirstStart = firstStart;
    }

    public Map<String, Object> createCategory(String path, String categoryName) {
        Map<String, Object> generalCategories = new HashMap<>();
        Category category = new Category(categoryName);
        generalCategories.put(path + categoryName + "/", category);
        return generalCategories;
    }

    public List<CategoryOrRecipe> loadCategory(DataSnapshot dataSnapshots) {
        List<CategoryOrRecipe> list = new ArrayList<>();
        List<CategoryOrRecipe> recipeList = new ArrayList<>();
        List<CategoryOrRecipe> categoryList = new ArrayList<>();

        for (DataSnapshot dataSnapshot : dataSnapshots.getChildren()) {
            Log.d("loadCategory", dataSnapshots.getKey());
            Recipe recipe = dataSnapshot.getValue(Recipe.class);
            if (recipe.getRecipeID() != null)
                recipeList.add(recipe);
            else {
                Category category = dataSnapshot.getValue(Category.class);
                Log.d("loadCategory", category.getName());
                categoryList.add(category);
            }
        }

        Collections.sort(categoryList, new LexicographicComparator());
        Collections.sort(recipeList, new LexicographicComparator());

        list.addAll(categoryList);
        list.addAll(recipeList);

        return list;
    }

    private class LexicographicComparator implements Comparator<CategoryOrRecipe> {
        @Override
        public int compare(CategoryOrRecipe o1, CategoryOrRecipe o2) {
            return o1.getName().compareToIgnoreCase(o2.getName());
        }
    }

    public Recipe loadRecipe(DataSnapshot dataSnapshot) {
        return dataSnapshot.getValue(Recipe.class);
    }

    public Toast createBigToast(Context context, String text, int length) {
        if(lastToast== null ||!lastToast.getView().isShown()) {
            lastToast = Toast.makeText(context, text, length);

            TextView messageTextView = lastToast.getView().findViewById(android.R.id.message);
            messageTextView.setTextSize(18);
            messageTextView.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
            return lastToast;
        }
        return null;
    }
    public void disableEnableControls(boolean enable, ViewGroup vg){
        for (int i = 0; i < vg.getChildCount(); i++){
            View child = vg.getChildAt(i);
            child.setEnabled(enable);
            if (child instanceof ViewGroup){
                disableEnableControls(enable, (ViewGroup)child);
            }
        }
    }

    public Toast createBigToastWithColor(Context context, String text, int length, int backColor, int textColor) {
        Toast toast = createBigToast(context, text, length);
        if(toast != null) {
            View view = toast.getView();
            view.getBackground().setColorFilter(backColor, PorterDuff.Mode.SRC_IN);
            TextView v = view.findViewById(android.R.id.message);
            v.setTextColor(textColor);
        }
        return toast;
    }

    public Uri saveBitmapToFile(String dirName, String fileName, Bitmap bm,
                                Bitmap.CompressFormat format, int quality) {
        File dir = new File(dirName);
        boolean doSave = true;
        if (!dir.exists()) {
            doSave = dir.mkdirs();
        }
        if (doSave) {
            File imageFile = new File(dir, fileName);

            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(imageFile);

                bm.compress(format, quality, fos);

                fos.close();

                return Uri.fromFile(imageFile);
            } catch (IOException e) {
                Log.e("app", e.getMessage());
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        } else {
            Log.e("app", "Couldn't create target directory.");
        }
        return null;
    }

    public boolean checkCategoryName(String categoryName) {
        char[] nameChars = categoryName.toCharArray();
        char[] forbiddenChars = InnerIds.FORBIDDEN_CHARS_IN_FIREBASE.toCharArray();
        for (char c : nameChars)
            for(char f : forbiddenChars)
                if(c == f)
                    return false;
        return true;
    }

    public void loadLocaleFromPreferences(Context context) {
        preferences = context.getSharedPreferences("com.itaisapir.yum", Context.MODE_PRIVATE);
        String defaultLanguage = Locale.getDefault().getLanguage();
        String defaultCountry = Locale.getDefault().getCountry();

        language = preferences.getString("Language", defaultLanguage);
        country = preferences.getString("Country", defaultCountry);
        Logger.getLogger("Language").info("Create language:" + language);

        Locale myLocale = new Locale(language, country);
        Resources res = context.getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        Configuration conf = res.getConfiguration();
        conf.setLocale(myLocale);
        res.updateConfiguration(conf, dm);
    }

    public void loadLocalNotificationSwitch() {
        localNotificationsSwitch = preferences.getBoolean("LocalNotificationSwitch", true);
    }

    public void saveLocalNotificationSwitch(Boolean isChecked) {
        SharedPreferences.Editor preferences_editor = preferences.edit();
        this.localNotificationsSwitch = isChecked;
        preferences_editor.putBoolean("LocalNotificationSwitch", isChecked);
        preferences_editor.commit();
    }

    public void setLocale(String language, String country) {
        SharedPreferences.Editor preferences_editor = preferences.edit();
        this.language = language;
        this.country = country;
        preferences_editor.putString("Language", language);
        preferences_editor.putString("Country", country);
        preferences_editor.commit();
    }
}
