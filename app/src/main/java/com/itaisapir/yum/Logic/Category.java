package com.itaisapir.yum.Logic;

import java.util.HashMap;
import java.util.Map;

public class Category extends CategoryOrRecipe{
    private Map<String ,Object> categoryAndRecipes;
    private Map<String ,Object> tokens;

    public Category() {
        categoryAndRecipes = new HashMap<>();
    }
    public Category(String categoryName) {
        super.setName(categoryName);
        categoryAndRecipes = new HashMap<>();
        tokens = new HashMap<>();
    }

    public Map<String, Object> getCategoryAndRecipes() {
        return categoryAndRecipes;
    }

    public void setName(String name) {
        super.setName(name);
    }

    public String getName(){
        return super.getName();
    }

    public void setCategoryAndRecipes(Map<String, Object> categoryAndRecipes) {
        this.categoryAndRecipes = categoryAndRecipes;
    }

    public void addRecipe(String key, Recipe recipe){
        categoryAndRecipes.put(key,recipe);
    }
}
