package com.itaisapir.yum;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.itaisapir.yum.utils.InnerIds;
import com.itaisapir.yum.utils.Utils;

import java.util.Arrays;
import java.util.List;

import static android.content.ContentValues.TAG;

public class LoginActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private FirebaseUser user;
    private ImageView googleSignIn;
    private LinearLayout progressLayout;
    private LinearLayout loginLayout;
    private Utils utils;

    private final int RC_SIGN_IN = 150;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        utils = Utils.getInstance();
        utils.loadLocaleFromPreferences(this);
        utils.loadLocalNotificationSwitch();
        super.onCreate(savedInstanceState);

        setContentView(R.layout.login);
        auth = FirebaseAuth.getInstance();
        googleSignIn = findViewById(R.id.googleSignIn);
        Button signInButton = findViewById(R.id.signInButton);
        Button signUpButton = findViewById(R.id.signUpButton);
        Button forgotPasswordButton = findViewById(R.id.forgotPasswordButton);
        progressLayout = findViewById(R.id.progressLayout);
        loginLayout = findViewById(R.id.loginLayout);

        googleSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                utils.disableEnableControls(false, loginLayout);
                List<AuthUI.IdpConfig> providers = Arrays.asList(
                        new AuthUI.IdpConfig.GoogleBuilder().build());

                // Create and launch sign-in intent
                startActivityForResult(AuthUI.getInstance()
                                .createSignInIntentBuilder()
                                .setAvailableProviders(providers)
                                .build(),
                        RC_SIGN_IN);
            }
        });

        signInButton.setOnClickListener(new View.OnClickListener() { // todo
            @Override
            public void onClick(View view) {
                EditText userEmailField = (EditText) findViewById(R.id.userEmailField);
                EditText passwordField = (EditText) findViewById(R.id.passwordField);
                String email = userEmailField.getText().toString();
                String password = passwordField.getText().toString();
                if(email.length()>0 && password.length()>0)
                    signIn(email, password);
                else {
                    Toast toast = utils.createBigToast(getBaseContext(), getResources().getString(R.string.sign_in_incorrect), Toast.LENGTH_LONG);
                    if(toast != null )
                        toast.show();
                }
            }
        });


        AlertDialog.Builder signUpDialogBuilder = new AlertDialog.Builder(this);
        View mView = getLayoutInflater().inflate(R.layout.sign_up_dialog, null);

        // Set up the input
        final EditText emailInput = mView.findViewById(R.id.userEmailInput);
        final EditText passwordInput = mView.findViewById(R.id.userPasswordInput);

        signUpDialogBuilder.setView(mView);

        // Set up the buttons
        signUpDialogBuilder.setPositiveButton(R.string.ok, null);
        signUpDialogBuilder.setNegativeButton(R.string.cancel, null);

        final android.app.AlertDialog signUpDialog = signUpDialogBuilder.create();
        signUpDialog.setOnShowListener(new DialogInterface.OnShowListener() {

            @Override
            public void onShow(final DialogInterface dialog) {
                final Button okButton = signUpDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                okButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        okButton.setEnabled(false);
                        InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        mgr.hideSoftInputFromWindow(emailInput.getWindowToken(), 0);
                        String email = emailInput.getText().toString();
                        String password = passwordInput.getText().toString();

                        if(!checkEmail(email)){
                            Toast toast = utils.createBigToast(getBaseContext(), getResources().getString(R.string.email_check), Toast.LENGTH_LONG);
                            if(toast != null )
                                toast.show();
                        }else if(password.length()<6){
                            Toast toast = utils.createBigToast(getBaseContext(), getResources().getString(R.string.password_check), Toast.LENGTH_LONG);
                            if(toast != null )
                                toast.show();
                        }else{
                            writeNewUser(email, password);
                            emailInput.getText().clear();
                            passwordInput.getText().clear();
                            dialog.dismiss();
                        }
                        okButton.setEnabled(true);
                    }
                });
                Button cancelButton = signUpDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                cancelButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        emailInput.getText().clear();
                        passwordInput.getText().clear();
                        dialog.cancel();
                    }
                });
            }
        });

        signUpButton.setOnClickListener(new View.OnClickListener() { // todo
            @Override
            public void onClick(View view) {
                signUpDialog.show();
            }
        });

        final AlertDialog.Builder forgotPasswordDialog = new AlertDialog.Builder(this);
        forgotPasswordDialog.setTitle(R.string.user_email);

        // Set up the input
        final EditText input = new EditText(this);
        forgotPasswordDialog.setView(input);

        // Set up the buttons
        forgotPasswordDialog.setPositiveButton(R.string.ok, null);
        forgotPasswordDialog.setNegativeButton(R.string.cancel, null);

        final AlertDialog forgotPasswordAlert = forgotPasswordDialog.create();
        forgotPasswordAlert.setOnShowListener(new DialogInterface.OnShowListener() {

            @Override
            public void onShow(final DialogInterface dialog) {
                final Button okButton = forgotPasswordAlert.getButton(AlertDialog.BUTTON_POSITIVE);
                okButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        okButton.setEnabled(false);
                        final String email = input.getText().toString();
                        if(!checkEmail(email)){
                            Toast toast = utils
                                    .createBigToastWithColor(getBaseContext(), getResources().getString(R.string.email_check)
                                            + " " + email, Toast.LENGTH_LONG, Color.WHITE,Color.BLACK);
                            if(toast != null) {
                                toast.setGravity(Gravity.TOP, 0, 40);
                                toast.show();
                            }
                        } else {
                            auth.sendPasswordResetEmail(email)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                Toast toast = utils
                                                        .createBigToastWithColor(getBaseContext(), getResources().getString(R.string.reset_password_email_sent)
                                                                + " " + email, Toast.LENGTH_LONG, Color.WHITE,Color.BLACK);
                                                if(toast != null) {
                                                    toast.setGravity(Gravity.TOP, 0, 40);
                                                    toast.show();
                                                }
                                                input.getText().clear();
                                                dialog.dismiss();
                                            }
                                            else {
                                                Toast toast = utils
                                                        .createBigToastWithColor(getBaseContext(), getResources().getString(R.string.email_check)
                                                                + " " + email, Toast.LENGTH_LONG, Color.WHITE,Color.BLACK);
                                                if(toast != null) {
                                                    toast.setGravity(Gravity.TOP, 0, 40);
                                                    toast.show();
                                                }
                                            }
                                        }
                                    });
                        }
                        okButton.setEnabled(true);
                    }
                });
                Button cancelButton = forgotPasswordAlert.getButton(AlertDialog.BUTTON_NEGATIVE);
                cancelButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        input.getText().clear();
                        dialog.cancel();
                    }
                });
            }
        });
        forgotPasswordButton.setOnClickListener(new View.OnClickListener() { // todo
            @Override
            public void onClick(View view) {
                forgotPasswordAlert.show();
            }
        });
        loadUser();
    }

    public void signIn(String email, String password) {
        progressLayout.setVisibility(View.VISIBLE);
        utils.disableEnableControls(false, loginLayout);
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithEmail:success");
                            user = auth.getCurrentUser();
                            goToCategoryActivity();
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            progressLayout.setVisibility(View.GONE);
                            utils.disableEnableControls(true, loginLayout);
                            Toast toast = utils.createBigToast(getBaseContext(), getResources().getString(R.string.sign_in_incorrect), Toast.LENGTH_SHORT);
                            if(toast != null)
                                toast.show();
                        }
                    }
                });
    }

    public void writeNewUser(String email, String password) {
        progressLayout.setVisibility(View.VISIBLE);
        utils.disableEnableControls(false, loginLayout);
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "createUserWithEmail:success");
                            Toast toast = utils.createBigToast(getBaseContext(), getResources().getString(R.string.registration_success), Toast.LENGTH_SHORT);
                            if(toast != null)
                                toast.show();
                            loadUser();

                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            Toast toast = utils.createBigToast(getBaseContext(), getResources().getString(R.string.registration_failed), Toast.LENGTH_SHORT);
                            if(toast != null)
                                toast.show();                        }
                        progressLayout.setVisibility(View.GONE);
                        utils.disableEnableControls(true, loginLayout);
                    }
                });
    }

    public void loadUser(){
        user = auth.getCurrentUser();
        if(user != null)
            goToCategoryActivity();
    }

    public void goToCategoryActivity(){
        Intent intent = new Intent(getBaseContext(), CategoryActivity.class);
        intent.putExtra(InnerIds.USER_ID, user.getUid());
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        utils.disableEnableControls(true, loginLayout);

        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK) {
                loadUser();
            } else {
                if(response != null) {
                    Toast toast = utils.createBigToast(this, getResources().getString(R.string.error_message), Toast.LENGTH_LONG);
                    if(toast != null)
                        toast.show();
                    Log.e("Error_Google",response.getError().getErrorCode()+"");
                    Log.e("Error_Google",response.getError().toString());
                }
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
            }
        }
    }

    public boolean checkEmail(String email){
        String[] check = email.split("@"); //to check that {mail} @ [domain]

        if(check.length == 2 && check[0].length() > 0 && check[1].length() > 0){
            String[] check2 = check[1].split("\\.");
            return check2.length > 0; // check that has at least one dot
        }
        return false;
    }
}

