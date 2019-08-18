package com.itaisapir.yum;

import android.Manifest;
import android.content.ClipData;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StrictMode;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.itaisapir.yum.Logic.Category;
import com.itaisapir.yum.Logic.Recipe;
import com.itaisapir.yum.utils.InnerIds;
import com.itaisapir.yum.utils.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddRecipeActivity extends AppCompatActivity implements com.google.android.gms.location.LocationListener,  GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{
    private final int REQUEST_IMAGE_CAPTURE = 100;
    private final int SELECT_PICTURE = 200;
    private final int REQUEST_READ_WRITE_CAMERA = 300;
    private final int REQUEST_READ_WRITE = 310;

    private final int REQUEST_LOCATION = 400;

    String mCurrentPhotoPath;
    private Utils utils;
    private DatabaseReference categoryPrivateRef;
    private DatabaseReference categoryPublicRef;
    private final String yumDirectory = Environment.getExternalStorageDirectory().toString() + "/yumPictures";
    private String basePath;
    private String recipeID;
    private EditText notesField;
    private EditText recipeNameField;
    private EditText linkField;
    private TextView numberOfFiles;
    private LinearLayout recipeInputsLayout;

    private int amountOfPhotos;
    private int count;
    private boolean doubleBackToExitPressedOnce;
    private Button importButton;
    private Button takeAPicButton;
    private Button saveButton;
    private Button backButtonAddNewRecipe;
    private CheckBox publicCheckBox;
    private String imageEncoded;
    private List<String> imagesEncodedList;

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;
    private Location mLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_recipe);

        utils = Utils.getInstance();
        categoryPrivateRef = FirebaseDatabase.getInstance().getReference(InnerIds.PRIVATE);
        Intent intent = getIntent();
        basePath = intent.getStringExtra(InnerIds.PATH_CATEGORY);
        recipeID = categoryPrivateRef.child(basePath).push().getKey();

        notesField = findViewById(R.id.notesField);
        recipeNameField = findViewById(R.id.recipeNameField);
        linkField = findViewById(R.id.linkField);

        numberOfFiles = findViewById(R.id.numberOfFiles);
        amountOfPhotos = 0;
        count = 0;
        doubleBackToExitPressedOnce = false;

        importButton = findViewById(R.id.importButton);
        takeAPicButton = findViewById(R.id.takeAPicButton);
        saveButton = findViewById(R.id.saveButton);
        backButtonAddNewRecipe = findViewById(R.id.backButtonAddNewRecipe);
        publicCheckBox = findViewById(R.id.publicCheckBox);
        recipeInputsLayout = findViewById(R.id.recipeInputs);

        publicCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked && (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED
                        || ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED)) {
                    utils.disableEnableControls(false, recipeInputsLayout);
                    ActivityCompat.requestPermissions(AddRecipeActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                            REQUEST_LOCATION);
                }
            }
        });

        importButton.setOnClickListener(new View.OnClickListener() { // todo
            @Override
            public void onClick(View view) {
                utils.disableEnableControls(false, recipeInputsLayout);
                if(ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                        || ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(AddRecipeActivity.this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            REQUEST_READ_WRITE);
                }else {
                    dispatchImportPicturesIntent();
                }
            }
        });

        takeAPicButton.setOnClickListener(new View.OnClickListener() { // todo
            @Override
            public void onClick(View view) {
                utils.disableEnableControls(false, recipeInputsLayout);
                if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
                    Toast toast = utils.createBigToast(getBaseContext(), getResources().getString(R.string.no_camera), Toast.LENGTH_LONG);
                    if(toast != null )
                        toast.show();
                } else {
                    if ((ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
                            || (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                            || (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)){
                        ActivityCompat.requestPermissions(AddRecipeActivity.this,
                                new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE},
                                REQUEST_READ_WRITE_CAMERA);
                    } else {
                        dispatchTakePictureIntent();
                    }
                }
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String name = recipeNameField.getText().toString();
                if(name.length() < 2){
                    Toast toast = utils.createBigToast(getBaseContext(), getResources().getString(R.string.recipe_name_more_then_2), Toast.LENGTH_SHORT);
                    if(toast != null )
                        toast.show();
                    return ;
                }
                //save images to storage
                StorageReference storageRef = FirebaseStorage.getInstance().getReference();
                StorageReference recipeRef = storageRef.child(recipeID);

                if(amountOfPhotos == 0) {
                    uploadRecipeDetailsToDB();
                    AddRecipeActivity.super.onBackPressed();

                }else {
                    utils.disableEnableControls(false, recipeInputsLayout);

                    final LinearLayout progressLayout = findViewById(R.id.progressLayout);
                    progressLayout.setVisibility(View.VISIBLE);
                    ImageView progressImage = findViewById(R.id.progress);
                    Glide.with(getBaseContext()).asGif().load(R.drawable.piza3).into(progressImage);

                    for (int i = 0; i < amountOfPhotos; ++i) {
                        String fileName = recipeID + "_" + i + ".jpg";
                        File imageFile = new File(yumDirectory,fileName);
                        final BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inJustDecodeBounds = true;
                        options.inSampleSize = 2;
                        options.inJustDecodeBounds = false;
                        options.inTempStorage = new byte[16 * 1024];
                        Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getPath(),options );
                        Uri file = utils.saveBitmapToFile(yumDirectory, fileName, bitmap, Bitmap.CompressFormat.JPEG, 50);

                        recipeRef.child(fileName).putFile(file)
                                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                    @Override
                                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                        ++count;
                                        if (count == amountOfPhotos) {
                                            uploadRecipeDetailsToDB();
                                            Toast toast = utils.createBigToast(getBaseContext(), getResources().getString(R.string.upload_message), Toast.LENGTH_LONG);
                                            if (toast != null)
                                                toast.show();
                                            finish();
                                        }
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception exception) {
                                        Toast toast = utils.createBigToast(getBaseContext(), getResources().getString(R.string.recipe_error_message), Toast.LENGTH_LONG);
                                        if(toast != null )
                                            toast.show();
                                        progressLayout.setVisibility(View.GONE);
                                        utils.disableEnableControls(true, recipeInputsLayout);
                                    }
                                });
                    }
                }

            }
        });

        backButtonAddNewRecipe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(5000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        mLocationCallback =  new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult locationResult) {
                onLocationChanged(locationResult.getLastLocation());
            }
        };
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }


    @Override
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocationServices.getFusedLocationProviderClient(this).removeLocationUpdates(mLocationCallback);
    }
    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    private void uploadRecipeDetailsToDB() {
        String name = recipeNameField.getText().toString();
        String notes = notesField.getText().toString();
        String link = linkField.getText().toString();

        final Recipe recipe = new Recipe(recipeID, name, notes, link, amountOfPhotos);
        final Map<String, Object> recipes = new HashMap<>();

        if(publicCheckBox.isChecked()){
            double lat = mLocation != null ? mLocation.getLatitude() : 0;
            double lon = mLocation != null ? mLocation.getLongitude() : 0;
            recipe.setLat(lat);
            recipe.setLon(lon);
        }

        recipes.put(recipeID, recipe);
        categoryPrivateRef.child(basePath).updateChildren(recipes);

        if (publicCheckBox.isChecked()) {
            categoryPublicRef = FirebaseDatabase.getInstance().getReference(InnerIds.PUBLIC);
            String[] temp = basePath.split("/");
            final String categoryName = temp[temp.length - 2];

            categoryPublicRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.hasChild(categoryName))
                        categoryPublicRef.child(categoryName).child(InnerIds.IN_CATEGORY).updateChildren(recipes);

                    else {
                        Map<String, Object> categories = new HashMap<>();
                        Category category = new Category(categoryName);
                        category.addRecipe(recipeID, recipe);
                        categories.put(categoryName + "/", category);
                        categoryPublicRef.updateChildren(categories);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
    }


    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            for (int i = 0; i < amountOfPhotos; ++i) {
                File file = new File(yumDirectory, recipeID + "_" + i + ".jpg");
                file.delete();
            }
            super.onBackPressed();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast toast = utils.createBigToast(this, getResources().getString(R.string.back_double_add), Toast.LENGTH_LONG);
        if(toast != null )
            toast.show();
        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce=false;
            }
        }, 3000);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        utils.disableEnableControls(true, recipeInputsLayout);

        boolean allFine = true;
        for (int i = 0; i < grantResults.length && allFine; ++i) {
            if(grantResults[i] != PackageManager.PERMISSION_GRANTED)
                allFine = false;
        }

        if (allFine) {
            if (requestCode== REQUEST_READ_WRITE_CAMERA)
                dispatchTakePictureIntent();
            else if(requestCode== REQUEST_READ_WRITE)
                dispatchImportPicturesIntent();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        utils.disableEnableControls(true, recipeInputsLayout);
        try {
            switch (requestCode){
                case REQUEST_IMAGE_CAPTURE:
                    if (resultCode == RESULT_OK) {
                        galleryAddPic(mCurrentPhotoPath);
                        increaseNumOfPhotosAndShow();
                    } else{
                        File file = new File(mCurrentPhotoPath);
                        file.delete();
                    }
                    break;

                case SELECT_PICTURE:
                    if (resultCode == RESULT_OK && data != null) {
                        if (data.getData() != null) {
                            Uri selectedImageUri = data.getData();
                            imageEncoded = getRealPathFromURI(this, selectedImageUri);
                            Log.v("LOG_TAG", imageEncoded + "1");

                            File originalPhoto = new File(imageEncoded);
                            copyFile(originalPhoto, createImageFile());
                            increaseNumOfPhotosAndShow();

                        } else if (data.getClipData() != null) {

                            ClipData mClipData = data.getClipData();
                            for (int i = 0; i < mClipData.getItemCount(); i++) {
                                ClipData.Item item = mClipData.getItemAt(i);
                                Uri uri = item.getUri();
                                Log.e("hello",uri.toString());
                                Log.e("hello",uri.getPath());

                                imageEncoded = getRealPathFromURI(this, uri);
                                Log.v("LOG_TAG", imageEncoded + "2");
                                File originalPhoto = null;

                                if (imageEncoded != null)
                                    originalPhoto = new File(imageEncoded);
                                else
                                    throw new NullPointerException();

                                copyFile(originalPhoto, createImageFile());
                                increaseNumOfPhotosAndShow();
                            }
                        }
                        Log.e("hello","3");
                    }
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            for (int i = 0; i < e.getStackTrace().length; i++) {
                Log.e("hell",e.getStackTrace()[i].toString());
            }

//            throw e
//            Toast toast = utils.createBigToast(getBaseContext(), getResources().getString(R.string.error_message), Toast.LENGTH_LONG);
//            Toast toast = utils.createBigToast(getBaseContext(), e.getStackTrace(), Toast.LENGTH_LONG);
//            if(toast != null)
//                toast.show();

        }
    }

    public void increaseNumOfPhotosAndShow() {
        ++amountOfPhotos;
        String displayNumber = amountOfPhotos + "";
        numberOfFiles.setText(displayNumber);
    }

//    public String getRealPathFromURI(Uri contentUri) {
//        String[] projection = {MediaStore.Images.Media.DATA};
//        Cursor cursor = null;
//        try {
//            String wholeID = DocumentsContract.getDocumentId(contentUri);
//            String[] split = wholeID.split(":");
//            String id = split[1];
//            String sel = MediaStore.Images.Media._ID + "=?";
//            String type = split[0];
//
//            if ("primary".equalsIgnoreCase(type)) {
//                return Environment.getExternalStorageDirectory() + "/" + split[1];
//            }
//            cursor = getBaseContext().getContentResolver().query(
//                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
//                    projection, sel, new String[]{id}, null);
//
//        } catch (Exception e) {
//            for (int i = 0; i < e.getStackTrace().length; i++) {
//                Log.d("hello",e.getStackTrace()[i].toString());
//            }
//
//            e.printStackTrace();
//        }
//
//        String path = null;
//        try {
//            int column_index = cursor
//                    .getColumnIndex(projection[0]);
//            if (cursor.moveToFirst()) {
//                path = cursor.getString(column_index);
//                cursor.close();
//            }
//        } catch (NullPointerException e) {
//            Log.d("hello","2");
//
//            e.printStackTrace();
//        }
//        return path;
//    }

    public static String getRealPathFromURI(final Context context, final Uri uri) {
        // DocumentProvider
        if (DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {

            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    private File createImageFile() throws IOException {
        File storageDir = new File(yumDirectory);
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }
        String imageFileName = recipeID + "_" + amountOfPhotos + ".jpg";
        File image = new File(storageDir, imageFileName);

        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void dispatchImportPicturesIntent() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent,
                "Select Pictures"), SELECT_PICTURE);
    }

    private void dispatchTakePictureIntent() {
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast toast = utils.createBigToast(getBaseContext(), getResources().getString(R.string.error_message), Toast.LENGTH_LONG);
                if(toast != null)
                    toast.show();
            }
            if (photoFile != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private void galleryAddPic(String path) {
        File f = new File(path);
        Uri contentUri = Uri.fromFile(f);
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    public static void copyFile(File src, File dst) throws IOException {
        try (InputStream in = new FileInputStream(src)) {
            try (OutputStream out = new FileOutputStream(dst)) {
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            mLocation = location;
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.getFusedLocationProviderClient(this).requestLocationUpdates(mLocationRequest, mLocationCallback, null);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e("yumLocation", "Connection suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e("google_maps","onConnectionFailed: \n" + connectionResult.toString());
    }
}
