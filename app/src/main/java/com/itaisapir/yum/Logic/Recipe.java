package com.itaisapir.yum.Logic;

public class Recipe extends CategoryOrRecipe{
    private String recipeID;
    private String notes;
    private String link;
    private int amountOfPhotos;

    private double lat;
    private double lon;

    public Recipe(){

    }

    public Recipe(String recipeID, String recipeName, String notes, String link, int amountOfPhotos) {
        this.recipeID = recipeID;
        super.setName(recipeName);
        this.notes = notes;
        this.link = link;
        this.amountOfPhotos = amountOfPhotos;
    }

    public String getRecipeID() {
        return recipeID;
    }

    public void setRecipeID(String recipeID) {
        this.recipeID = recipeID;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public int getAmountOfPhotos() {
        return amountOfPhotos;
    }

    public void setAmountOfPhotos(int amountOfPhotos) {
        this.amountOfPhotos = amountOfPhotos;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }


}
