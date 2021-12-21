package com.example.firebaseapplication;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.firebaseapplication.Adapter.StudentAdapter;
import com.example.firebaseapplication.Dialogs.UpdateStudentDialog;
import com.example.firebaseapplication.Model.StudentModel;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.kcode.permissionslib.main.OnRequestPermissionsCallBack;
import com.kcode.permissionslib.main.PermissionCompat;

import java.util.ArrayList;


public class AllStudentActivity extends AppCompatActivity {

    ProgressBar loadingLY;
    RecyclerView rv;
    ArrayList<StudentModel> studentModelsList;
    StudentAdapter adapter;
    StudentModel studentModel;

    FirebaseFirestore fireStoreDB;

    UpdateStudentDialog updateStudentDialog;
    ActivityResultLauncher<Intent> pickImageLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_student);

        fireStoreDB = FirebaseFirestore.getInstance();

        rv = findViewById(R.id.recyclerView);
        loadingLY = findViewById(R.id.loadingLY);
        studentModelsList = new ArrayList<>();

        rv.setLayoutManager(new LinearLayoutManager(this));

        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == RESULT_OK) {
                            try {
                                Intent intent = result.getData();
                                Uri selectedImageUri = intent.getData();

                                if (updateStudentDialog != null) {
                                    updateStudentDialog.setPhotoUri(selectedImageUri);
                                }

                            } catch (Exception e) {
                                Log.e("FileSelectorActivity", "File select error", e);
                            }

                        }
                    }
                });

        adapter = new StudentAdapter(this, studentModelsList, new DataCallBack() {
            @Override
            public void Result(Object obj, String type, Object otherData) {
                studentModel = (StudentModel) obj;
                int position = (int) otherData;

                if (updateStudentDialog == null) {
                    updateStudentDialog = new UpdateStudentDialog(AllStudentActivity.this, studentModel, new DataCallBack() {
                        @Override
                        public void Result(Object obj, String type, Object otherData) {

                            if (type.equals(Constants.PICK_IMAGE)) {
                                checkPermission();
                            } else {
                                studentModel = (StudentModel) obj;
                                adapter.dataList.set(position, studentModel);
                                adapter.notifyItemChanged(position);
                            }
                        }
                    });
                    updateStudentDialog.setOnDismissListener(dialog -> updateStudentDialog = null);
                }
            }

        });

        rv.setAdapter(adapter);

        getStudentData();

    }

    public void getStudentData() {

        loadingLY.setVisibility(View.VISIBLE);

        fireStoreDB.collection(Constants.USER)
                .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {

                loadingLY.setVisibility(View.GONE);

                if (task.isSuccessful()) {

                    studentModelsList.clear();

                    for (DocumentSnapshot document : task.getResult().getDocuments()) {
                        StudentModel studentModel = document.toObject(StudentModel.class);
                        studentModelsList.add(studentModel);
                    }
                    adapter.dataList = studentModelsList;
                    adapter.notifyDataSetChanged();

                } else {
                    Toast.makeText(getApplicationContext(), getString(R.string.fail_get_data), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void checkPermission() {

        try {
            PermissionCompat.Builder builder = new PermissionCompat.Builder(this);
            builder.addPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE});
            builder.addPermissionRationale(getString(R.string.should_allow_permission));
            builder.addRequestPermissionsCallBack(new OnRequestPermissionsCallBack() {
                @Override
                public void onGrant() {
                    Intent intent = new Intent();
                    intent.setType("image/*");
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    pickImageLauncher.launch(Intent.createChooser(intent, ""));
                }
                @Override
                public void onDenied(String permission) {
                    Toast.makeText(AllStudentActivity.this, getString(R.string.some_permission_denied), Toast.LENGTH_SHORT).show();
                }
            });
            builder.build().request();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
