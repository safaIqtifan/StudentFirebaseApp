package com.example.firebaseapplication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.firebaseapplication.Model.StudentModel;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    Button addbtn, getbtn, clerbtn;
    TextView userName, avgEd;
    ProgressBar loadingLY;
    ImageView profileImag;

    Uri userPhotoUri;
    StudentModel studentModel;
    private static final int IMAGE_PICK_CODE = 1000;
    private static final int PERMISSION_CODE = 1001;

    FirebaseFirestore fireStoreDB;
    StorageReference storageRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fireStoreDB = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();
        studentModel = new StudentModel();

        userName = findViewById(R.id.tvUserName);
        avgEd = findViewById(R.id.edAvarg);
        addbtn = findViewById(R.id.addBtn);
        getbtn = findViewById(R.id.getBtn);
        clerbtn = findViewById(R.id.clerBtn);
        profileImag = findViewById(R.id.profileImag);
        loadingLY = findViewById(R.id.loadingLY);

        loadingLY.setVisibility(View.GONE);

        addbtn.setOnClickListener(v -> {
            checkData();
        });

        getbtn.setOnClickListener(v -> {
            Intent i = new Intent(this, AllStudentActivity.class);
            startActivity(i);
        });

        clerbtn.setOnClickListener(v -> {
            userName.setText("");
            avgEd.setText("");
            profileImag.setImageResource(R.drawable.profile);
        });

        profileImag.setOnClickListener(v -> {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (this.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_DENIED) {
                    String[] permission = {Manifest.permission.READ_EXTERNAL_STORAGE};
                    requestPermissions(permission, PERMISSION_CODE);
                } else {
                    pickImageFromGallery();
                }
            } else {
                pickImageFromGallery();
            }
        });

        addbtn.setOnClickListener(v -> {
            checkData();
        });

    }

    private void checkData() {
        String name = userName.getText().toString();
        String avg = avgEd.getText().toString();

        loadingLY.setVisibility(View.VISIBLE);
        boolean hasError = false;

        if (name.isEmpty()) {
            userName.setError(this.getString(R.string.invalid_input));
            hasError = true;
        }
        if (avg.isEmpty()) {
            avgEd.setError(this.getString(R.string.invalid_input));
            hasError = true;
        }
        if (userPhotoUri == null) {
            hasError = true;
        }
        if (hasError)
            return;

        studentModel.name = name;
        studentModel.average = Double.parseDouble(avg);
        uploadPhoto(userPhotoUri);

    }

    private void uploadPhoto(Uri photoUri) {

        StorageReference imgRef = storageRef.child(Constants.IMAGES + "/"
                + UUID.randomUUID().toString());

        UploadTask uploadTask = imgRef.putFile(photoUri);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.e("aa", exception + "");

                Toast.makeText(MainActivity.this, "ddddd", Toast.LENGTH_SHORT).show();

            }
        }).addOnSuccessListener(taskSnapshot -> {

            imgRef.getDownloadUrl().addOnCompleteListener(task -> {

                studentModel.photo = task.getResult().toString();
                Log.e("s",studentModel.photo);
//                System.out.println("Log uploaded url " + studentModel.getphoto());
                addUserToFirebase();
            });


        });
    }

    private void addUserToFirebase() {

        String userId = fireStoreDB.collection(Constants.USER).document().getId(); // this is auto genrat

        Map<String, Object> studentModelMap = new HashMap<>();
        studentModelMap.put("id", userId);
        studentModelMap.put("name", studentModel.name);
        studentModelMap.put("average", studentModel.average);
        studentModelMap.put("photo", studentModel.photo);

        fireStoreDB.collection(Constants.USER).document(userId).set(studentModelMap, SetOptions.merge())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(MainActivity.this, "user Added", Toast.LENGTH_SHORT).show();
                        loadingLY.setVisibility(View.GONE);
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void pickImageFromGallery() {

        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, IMAGE_PICK_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    pickImageFromGallery();
                } else {
                    Toast.makeText(this, "Permission denied...!", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == IMAGE_PICK_CODE) {

            userPhotoUri = data.getData();
            profileImag.setImageURI(userPhotoUri);
        }
    }
}