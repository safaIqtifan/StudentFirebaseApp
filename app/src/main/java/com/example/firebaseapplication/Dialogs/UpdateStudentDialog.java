package com.example.firebaseapplication.Dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.example.firebaseapplication.Constants;
import com.example.firebaseapplication.DataCallBack;
import com.example.firebaseapplication.Model.StudentModel;
import com.example.firebaseapplication.R;
import com.example.firebaseapplication.databinding.DialogUpdateStudentBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UpdateStudentDialog extends Dialog {

    Activity activity;
    Uri updatProfileImgeUri;
    DialogUpdateStudentBinding binding;
    FirebaseFirestore fireStoreDB;
    StorageReference storageRef;

    StudentModel studentModel;
    DataCallBack okCall;

    public UpdateStudentDialog(Activity context, StudentModel student, final DataCallBack okCall) {
        super(context);
        activity = context;

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        binding = DialogUpdateStudentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        studentModel = student;
        this.okCall = okCall;
        fireStoreDB = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();
        binding.loadingLY.setVisibility(View.GONE);

//        progressDialog = new ProgressDialog(activity);
//        progressDialog.setMessage(activity.getString(R.string.edit_student));

        binding.tvUserName.setText(studentModel.name);
        binding.edAvarg.setText(String.valueOf(studentModel.average));

        if (studentModel.photo != null) {
            Glide.with(activity)
                    .asBitmap()
                    .load(studentModel.photo)
                    .into(binding.updatProfileImg);
        }
//            setPhotoUri(Uri.parse(studentModel.photo));

        binding.okBtn.setOnClickListener(view -> {

            String userNameStr = binding.tvUserName.getText().toString().trim();
            String avrgStr = binding.edAvarg.getText().toString().trim();

            // here check all fields that is not null on empty

            boolean hasError = false;
            if (userNameStr.isEmpty()) {
                binding.tvUserName.setError(activity.getString(R.string.invalid_input));
                hasError = true;
            }
            if (avrgStr.isEmpty()) {
                binding.edAvarg.setError(activity.getString(R.string.invalid_input));
                hasError = true;
            }
//            if (updatProfileImgeUri == null) {
//                Toast.makeText(activity, activity.getString(R.string.please_select_photo), Toast.LENGTH_SHORT).show();
//                hasError = true;
//            }
            if (hasError)
                return;

            studentModel.name = userNameStr;
            studentModel.average = Double.parseDouble(avrgStr);
            if (updatProfileImgeUri != null) {
                uploadPhoto(updatProfileImgeUri);
            } else {
                updateStudentData();
            }
//            dataAccess.updateStudent(studentModel.name, studentModel.average, studentModel.photo);


        });

        binding.cancelBtn.setOnClickListener(view -> {

            dismiss();
        });

        binding.updatProfileImg.setOnClickListener(view -> {

            if (okCall != null) {
                okCall.Result(null, Constants.PICK_IMAGE, null);
            }
        });

        try {
            if (activity != null && !activity.isFinishing())
                show();
        } catch (Exception e) {
            dismiss();
        }
    }

    public void setPhotoUri(Uri photoUri) {

        updatProfileImgeUri = photoUri;

        Glide.with(activity)
                .asBitmap()
                .load(updatProfileImgeUri)
                .into(binding.updatProfileImg);
    }


    private void uploadPhoto(Uri photoUri) {

        StorageReference imgRef = storageRef.child(Constants.IMAGES + "/"
                + UUID.randomUUID().toString());
        binding.loadingLY.setVisibility(View.VISIBLE);


        UploadTask uploadTask = imgRef.putFile(photoUri);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.e("aa", exception + "");
//                GlobalHelper.hideProgressDialog();
                // Handle unsuccessful uploads
                Toast.makeText(activity, "ddddd", Toast.LENGTH_SHORT).show();

            }
        }).addOnSuccessListener(taskSnapshot -> {

            imgRef.getDownloadUrl().addOnCompleteListener(task -> {

                studentModel.photo = task.getResult().toString();
                Log.i("s", "Log photo " + studentModel.photo);
                updateStudentData();
//                System.out.println("Log uploaded url " + studentModel.getphoto());
                binding.loadingLY.setVisibility(View.GONE);
            });


        });
    }

    private void updateStudentData() {

        Map<String, Object> studentMap = new HashMap<>();
        studentMap.put("id", studentModel.id);
        studentMap.put("name", studentModel.name);
        studentMap.put("photo", studentModel.photo);
        studentMap.put("average", studentModel.average);


//        progressDialog.show();
        fireStoreDB.collection(Constants.USER).document(studentModel.id)
                .update(studentMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    //                    progressDialog.d
                    @Override

                    public void onSuccess(Void aVoid) {
                        Log.d("TAG", "Log DocumentSnapshot successfully deleted!");
                        dismiss();

                        if (okCall != null) {
                            okCall.Result(studentModel, "", null);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("TAG", "Log Error deleting document", e);

                        Toast.makeText(activity, "Fail edit student", Toast.LENGTH_SHORT).show();
                    }
                });
    }


}