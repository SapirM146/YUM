package com.itaisapir.yum;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.StrictMode;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.itaisapir.yum.Logic.Recipe;
import com.itaisapir.yum.Logic.ShowImagesAdapter;
import com.itaisapir.yum.utils.InnerIds;
import com.itaisapir.yum.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ShowRecipeActivity extends AppCompatActivity implements ShowImagesAdapter.OnItemClicked,  BoundService.RotationAlertListener {

    private Utils utils;
    private DatabaseReference recipeDBRef;
    private StorageReference storageRef ;
    private StorageReference recipeStorageRef;
    private String basePath;
    private String privateOrPublic;
    private String recipeID;
    private String categoryName;
    private Recipe theRecipe;
    private TextView NotesText;
    private TextView recipeTitle;
    private TextView linkText;
    private int currentPic;
    private Button backButton;
    private Button shareButton;
    private Button settingsButton;

    private final String yumTempDirectory = Environment.getExternalStorageDirectory().toString() + "/yumPictures/sent";
    private final int REQUEST_READ_WRITE = 310;

    private BoundService.SensorBinder mRotationBinder;
    private ServiceConnection mRotationService;
    private boolean isRotationServiceBound;

    private List<Uri> photosUri;
    private RecyclerView showPhotos;
    private LinearLayoutManager Manager;
    private ShowImagesAdapter imagesAdapter;
    private int amountOfPhotos;
    private AlertDialog settingsAlert;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.show_recipe);

        photosUri = new ArrayList<>();
        amountOfPhotos = 0;
        currentPic = 0;
        Intent intent = getIntent();

        basePath = intent.getStringExtra(InnerIds.PATH_CATEGORY);

        privateOrPublic = intent.getStringExtra(InnerIds.PRIVATE_OR_PUBLIC);


        utils = Utils.getInstance();
        recipeDBRef = FirebaseDatabase.getInstance().getReference(privateOrPublic);

        String[] temp = basePath.split("/");
        recipeID = temp[temp.length-1];
        categoryName = temp[temp.length-3];

        storageRef = FirebaseStorage.getInstance().getReference();
        recipeStorageRef = storageRef.child(recipeID);

        NotesText = findViewById(R.id.NotesText);
        recipeTitle =  findViewById(R.id.recipeTitle);
        linkText =  findViewById(R.id.linkText);

        settingsButton =  findViewById(R.id.settingsButton);
        shareButton =  findViewById(R.id.shareButton);

        if(InnerIds.PRIVATE.equals(privateOrPublic))
            setSettings();
        else{
            settingsButton.setVisibility(View.GONE);
            shareButton.setVisibility(View.VISIBLE);
        }

        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(settingsAlert!=null)
                    settingsAlert.dismiss();
                if (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                        || ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(ShowRecipeActivity.this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            REQUEST_READ_WRITE);
                } else {
                    getImagesAndShare();
                }
            }
        });

        backButton = findViewById(R.id.backButton);
        showPhotos = findViewById(R.id.showPhotos);
        Manager = new GridLayoutManager(getBaseContext(),4);

        showPhotos.setLayoutManager(Manager);
        loadRecipe();

        mRotationService = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                mRotationBinder = (BoundService.SensorBinder) iBinder;
                mRotationBinder.registerListener(ShowRecipeActivity.this);
                isRotationServiceBound = true;
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                isRotationServiceBound = false;
            }
        };

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
    }

    private void setSettings() {
        settingsButton.setVisibility(View.VISIBLE);
        shareButton.setVisibility(View.GONE);
        final AlertDialog.Builder settingsBuilder = new AlertDialog.Builder(this);
        View settingsView = getLayoutInflater().inflate(R.layout.show_settings_dialog, null);
        shareButton = settingsView.findViewById(R.id.shareButton);
        Button removeButton = settingsView.findViewById(R.id.removeButton);

        removeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final AlertDialog.Builder removeAlertBuilder = new AlertDialog.Builder(ShowRecipeActivity.this);
                removeAlertBuilder.setTitle(R.string.remove);

                // Set up the input
                final TextView removeText = new TextView(ShowRecipeActivity.this);
                removeText.setText(getResources().getString(R.string.check_recipe_remove));
                removeText.setTextSize(17);
                removeText.setPadding(0,10,0,0);
                removeText.setGravity(Gravity.CENTER);
                removeAlertBuilder.setView(removeText);

                // Set up the buttons
                removeAlertBuilder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(amountOfPhotos > 0)
                            removeImages();

                        DatabaseReference publicRecipeDBRef = FirebaseDatabase.getInstance().getReference(InnerIds.PUBLIC);
                        publicRecipeDBRef.child(categoryName).child(InnerIds.IN_CATEGORY).child(recipeID).removeValue();

                        recipeDBRef.child(basePath).removeValue();
                        Toast toast = utils.createBigToast(ShowRecipeActivity.this,getResources().getString(R.string.recipe_remove), Toast.LENGTH_LONG);
                        if(toast != null )
                            toast.show();
                        ShowRecipeActivity.this.finish();
                    }
                });

                removeAlertBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                final AlertDialog removeAlert = removeAlertBuilder.create();

                removeAlert.show();

            }
        });

        settingsBuilder.setView(settingsView);
        settingsAlert = settingsBuilder.create();

        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                settingsAlert.show();
            }
        });
    }


    private void getImagesAndShare() {

        final LinearLayout progressLayout = findViewById(R.id.progressLayout);
        progressLayout.setVisibility(View.VISIBLE);
        ImageView progressImage = findViewById(R.id.shareProgress);
        Glide.with(getBaseContext())
                .asGif()
                .load(R.drawable.loading_tost_trans)
                .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true))
                .into(progressImage);
        shareButton.setEnabled(false);
        showPhotos.setEnabled(false);
        stopRotationService();
        if(amountOfPhotos == 0) {
            shareRecipe(null);
            progressLayout.setVisibility(View.GONE);
        }else {
            final ArrayList<Uri> imageUris = new ArrayList<>();
            for (int i = 0; i < amountOfPhotos; i++) {
                ImageView content = new ImageView(getBaseContext());
                final int imageIndex = i;
                RequestBuilder<Drawable> req = Glide.with(getBaseContext())
                        .load(photosUri.get(i));
                req.addListener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        Toast toast = utils.createBigToast(getBaseContext(), getResources().getString(R.string.load_error), Toast.LENGTH_LONG);
                        if(toast != null)
                            toast.show();
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        Bitmap imageBM = ((BitmapDrawable) resource).getBitmap();
                        saveImageAndSaveUriInList(imageUris, imageIndex, imageBM);

                        if (imageUris.size() == amountOfPhotos) {
                            shareRecipe(imageUris);
                            progressLayout.setVisibility(View.GONE);
                        }
                        return true;
                    }
                }).submit();

            }
        }
    }

    private void saveImageAndSaveUriInList(ArrayList<Uri> imageUris, int imageIndex, Bitmap imageBM) {
        Uri uri = utils.saveBitmapToFile(yumTempDirectory, recipeID + "_" + imageIndex + ".jpg", imageBM, Bitmap.CompressFormat.JPEG, 100);
        if (uri != null)
            imageUris.add(uri);
    }

    private void shareRecipe(ArrayList<Uri> imageUris) {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
        String shareRecipe =  getRecipeDetails();
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareRecipe);

        if(imageUris == null){
            shareIntent.setType("text/plain");

        } else {
            Collections.sort(imageUris, new ImageComparator());
            StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
            StrictMode.setVmPolicy(builder.build());
            shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, imageUris);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            shareIntent.setType("*/*");
        }

        startActivity(Intent.createChooser(shareIntent, getResources().getString(R.string.share_recipe)));
        shareButton.setEnabled(true);
        showPhotos.setEnabled(true);
        startRotationService();
    }

    private String getRecipeDetails() {
        StringBuilder recipe = new StringBuilder(getResources().getString(R.string.recipe_name) +"\n"+ theRecipe.getName() + "\n\n");
        if(!"".equals(theRecipe.getNotes()))
            recipe.append(getResources().getString(R.string.notes) +"\n"+ theRecipe.getNotes() + "\n\n");
        if(!"".equals(theRecipe.getLink()))
            recipe.append(getResources().getString(R.string.link) +"\n"+ theRecipe.getLink() + "\n\n");
        return recipe.toString();
    }

    @Override
    protected void onStart() {
        super.onStart();
        startRotationService();
    }

    private void loadRecipe() {
        recipeDBRef.child(basePath).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                theRecipe = utils.loadRecipe(dataSnapshot);
                amountOfPhotos = theRecipe.getAmountOfPhotos();
                startRotationService();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        NotesText.setText(theRecipe.getNotes());
                        recipeTitle.setText(theRecipe.getName());
                        linkText.setText(theRecipe.getLink());
                    }
                });
                if(amountOfPhotos > 0) {
                    LinearLayout tiltImageMessage = findViewById(R.id.tiltImageMessage);
                    tiltImageMessage.setVisibility(View.VISIBLE);
                    loadImages();
                }
                else
                    shareButton.setEnabled(true);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Toast toast = utils.createBigToast(getBaseContext(), getResources().getString(R.string.error_recipe), Toast.LENGTH_LONG);
                if(toast != null)
                    toast.show();            }
        });
    }

    private void loadImages(){
        for(currentPic = amountOfPhotos-1; currentPic >= 0; --currentPic) {
            recipeStorageRef.child(theRecipe.getRecipeID()+"_"+currentPic+".jpg").getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) {
                    photosUri.add(uri);

                    if(photosUri.size() == amountOfPhotos){
                        sortUriArray();
                        imagesAdapter = new ShowImagesAdapter(photosUri, getBaseContext());
                        imagesAdapter.setOnClick(ShowRecipeActivity.this);
                        showPhotos.setAdapter(imagesAdapter);
                        shareButton.setEnabled(true);
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    // Handle any errors
                }
            });
        }

    }

    private void removeImages() {
        for(currentPic = amountOfPhotos-1; currentPic >=0; --currentPic) {
            recipeStorageRef.child(theRecipe.getRecipeID() + "_" + currentPic + ".jpg").delete();
        }
    }

    public void sortUriArray() {
        Collections.sort(photosUri, new ImageComparator());
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        stopRotationService();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.v("resume","onResume!!!!");
        startRotationService();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopRotationService();
    }

    private void stopRotationService(){
        if(isRotationServiceBound){
            unbindService(mRotationService);
            isRotationServiceBound = false;
        }
    }

    private void startRotationService(){
        if(!isRotationServiceBound && amountOfPhotos>0) {
            Intent intentRotate = new Intent(ShowRecipeActivity.this, BoundService.class);
            bindService(intentRotate, mRotationService, Context.BIND_AUTO_CREATE);
            isRotationServiceBound = true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        boolean allFine = true;
        for (int i = 0; i < grantResults.length && allFine; ++i) {
            if(grantResults[i] != PackageManager.PERMISSION_GRANTED)
                allFine = false;
        }

        if (allFine) {
            if(requestCode== REQUEST_READ_WRITE)
                getImagesAndShare();
        }
    }

    @Override
    public void onItemClick(int position) {
        Intent intent = new Intent(this, ImagesSliderActivity.class);
        ArrayList<String> urisToStringArray = new ArrayList<>();
        for (Uri uri:photosUri) {
            urisToStringArray.add(uri.toString());
        }
        intent.putExtra(InnerIds.IMAGES_ARRAY, urisToStringArray);
        intent.putExtra(InnerIds.CURRENT_IMAGE, position);

        stopRotationService();
        startActivity(intent);
    }

    @Override
    public void rotationAlert() {
        boolean hasAccelerometer = android.provider.Settings.System.getInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0) == 1;
        if(hasAccelerometer)
            onItemClick(0);
    }

    private class ImageComparator implements Comparator<Uri> {
        @Override
        public int compare(Uri o1, Uri o2) {
            String[] temp1 = o1.toString().split(".jpg")[0].split("_");
            String[] temp2 = o2.toString().split(".jpg")[0].split("_");
            return Integer.parseInt(temp1[temp1.length-1]) - Integer.parseInt(temp2[temp2.length-1]);
        }
    }
}
