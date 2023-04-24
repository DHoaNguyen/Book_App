package com.example.bookapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.bookapp.databinding.ActivityPdfAddBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;


public class PdfAddActivity extends AppCompatActivity {

    //cài đặt view binding
    private ActivityPdfAddBinding binding;

    //firebase auth
    private FirebaseAuth firebaseAuth;

    //progress dialog
    private ProgressDialog progressDialog;

    //arraylist để chứa thể loại pdf
    private ArrayList<String> categoryTittleArrayList, categoryIdArrayList;

    //uri của pdf đã chọn
    private Uri pdfUri = null;

    private static final int PDF_PICK_CODE = 1000;
    //Tag debug
    private static final String TAG = "ADD_PDF_TAG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityPdfAddBinding.inflate(getLayoutInflater());

        setContentView(binding.getRoot());

        firebaseAuth = firebaseAuth.getInstance();
        loadPdfCategories();

        //cài đặt progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Vui lòng chờ");
        progressDialog.setCanceledOnTouchOutside(false);

        //Trở về activity trước
        binding.backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        //attach pdf
        binding.attachBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pdfPickIntent();
            }
        });

        //chọn thể loại
        binding.categoryTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                categoryPickDialog();
            }
        });

        //tải lên pdf
        binding.submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //validate dữ liệu
                validateData();
            }
        });
    }

    private String title = "", description = "";

    private void validateData() {
        Log.d(TAG, "validateData: validating...");

        //lấy dữ liệu
        title = binding.titleEt.getText().toString().trim();
        description = binding.descriptionEt.getText().toString().trim();

        //validate
        if (TextUtils.isEmpty(title)) {
            Toast.makeText(this, "Nhập tên sách...", Toast.LENGTH_SHORT).show();
        }
        else if (TextUtils.isEmpty(description)) {
            Toast.makeText(this, "Thêm thông tin mô tả về sách...", Toast.LENGTH_SHORT).show();
        }
        else if (TextUtils.isEmpty(selectedCategoryTittle)) {
            Toast.makeText(this, "Chọn thể loại cho sách...", Toast.LENGTH_SHORT).show();
        }
        else if (pdfUri == null) {
            Toast.makeText(this, "Chọn file pdf cần tải lên...", Toast.LENGTH_SHORT).show();
        }
        else {
            uploadPdfToStorage();
        }

    }

    private void uploadPdfToStorage() {
        Log.d(TAG, "uploadPdfToStorage: Tải lên...");

        //hiển thị tiến trình
        progressDialog.setMessage("Đang tải lên file pdf...");
        progressDialog.show();

        //timestamp
        long timestamp = System.currentTimeMillis();

        //path của file pdf trong storage ở firebase
        String filePathAndName = "Books/" + timestamp;
        //storage reference
        StorageReference storageReference = FirebaseStorage.getInstance().getReference(filePathAndName);
        storageReference.putFile(pdfUri)
            .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Log.d(TAG, "onSuccess: Tải lên file pdf thành công");
                    Log.d(TAG, "onSuccess: Lấy đường dẫn url pdf");

                    Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                    while (!uriTask.isSuccessful());
                    String uploadedPdfUrl = "" + uriTask.getResult();

                    //đăng lên firebase db
                    uploadedPdfInfoToDb(uploadedPdfUrl, timestamp);

                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    progressDialog.dismiss();
                    Log.d(TAG, "onFailure: Tải lên file pdf thất bại vì " + e.getMessage());
                    Toast.makeText(PdfAddActivity.this, "Tải lên file pdf thất bại vì " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void uploadedPdfInfoToDb(String uploadedPdfUrl, long timestamp) {
        Log.d(TAG, "uploadPdfInfoToDb: Tải thông tin pdf lên db firebase...");

        progressDialog.setMessage("Đang tải lên thông tin pdf...");

        String uid = firebaseAuth.getUid();

        //cài đặt dữ liệu tải lên
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("uid", "" + uid);
        hashMap.put("id", "" + timestamp);
        hashMap.put("tittle", "" + title);
        hashMap.put("description", "" + description);
        hashMap.put("categoryId", "" + selectedCategoryId);
        hashMap.put("url", "" + uploadedPdfUrl);
        hashMap.put("timestamp", timestamp);

        //db reference: db > book
        DatabaseReference ref =FirebaseDatabase.getInstance().getReference("Books");
        ref.child("" + timestamp)
                .setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        progressDialog.dismiss();
                        Log.d(TAG, "onSuccess: Tải file pdf lên db thành công ");
                        Toast.makeText(PdfAddActivity.this, "Tải file pdf lên db thành công", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Log.d(TAG, "onFailure: Tải file pdf lên db thất bại vì " + e.getMessage());
                        Toast.makeText(PdfAddActivity.this, "Tải file pdf lên db thất bại vì " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadPdfCategories() {
        Log.d(TAG, "loadPdfCategories: Loading các thể loại pdf...");

        categoryTittleArrayList = new ArrayList<>();
        categoryIdArrayList = new ArrayList<>();


        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Thể loại");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                categoryTittleArrayList.clear(); //clear trước khi thêm dữ liệu
                categoryIdArrayList.clear();
                for (DataSnapshot ds: snapshot.getChildren()) {

                    String categoryId = "" + ds.child("id").getValue();
                    String categoryTittle = "" + ds.child("category").getValue();

                    //thêm vào respective arraylists
                    categoryTittleArrayList.add(categoryTittle);
                    categoryIdArrayList.add(categoryId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
    
    private String selectedCategoryId, selectedCategoryTittle;
    
    private void categoryPickDialog() {
        Log.d(TAG, "categoryPickDialog: hiển thị category pick dialog");

        //lấy string array of categories từ arraylist
        String [] categoriesArray = new String[categoryTittleArrayList.size()];
        for (int i = 0; i < categoryTittleArrayList.size(); i++) {
            categoriesArray[i] = categoryTittleArrayList.get(i);
        }

        //alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Chọn thể loại")
                .setItems(categoriesArray, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        selectedCategoryTittle = categoryTittleArrayList.get(i);
                        selectedCategoryId = categoryIdArrayList.get(i);
                        binding.categoryTv.setText(selectedCategoryTittle);

                        Log.d(TAG, "onClick: Thể loại đã chọn: " + selectedCategoryId + " " + selectedCategoryTittle);
                    }
                })
                .show();
    }

    private void pdfPickIntent() {
        Log.d(TAG, "pdfPickIntent: bắt đầu chạy...");

        Intent intent = new Intent();
        intent.setType("application/pdf");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Chọn file Pdf"), PDF_PICK_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == PDF_PICK_CODE) {
                Log.d(TAG, "onActivityResult: Đã chọn file Pdf");

                pdfUri = data.getData();

                Log.d(TAG, "onActivityResult: URI: " + pdfUri);
            }
        }
        else {
            Log.d(TAG, "onActivityResult: hủy chọn file pdf");
            Toast.makeText(this, "hủy chọn file pdf", Toast.LENGTH_SHORT).show();
        }
    }
}