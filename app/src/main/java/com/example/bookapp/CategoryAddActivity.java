package com.example.bookapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import com.example.bookapp.databinding.ActivityCategoryAddBinding;
import com.example.bookapp.databinding.ActivityMainBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Hashtable;

public class CategoryAddActivity extends AppCompatActivity {

    private @NonNull ActivityCategoryAddBinding binding;


    // firebase auth
    private FirebaseAuth firebaseAuth;

    // dialog

    private ProgressDialog progressDialog;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCategoryAddBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseAuth = FirebaseAuth.getInstance();

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Vui lòng đợi");
        progressDialog.setCanceledOnTouchOutside(false);


        // Xử lý khi bấm nút back quay về..
        binding.backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        // Xử lý click, bắt đầu upload thể loại



        binding.submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                validateData();
            }
        });



    }

    private  String category = "";
    // kiểm tra data có hợp lệ ko
    private void validateData() {
        category = binding.categoryEt.getText().toString().trim();
        if(TextUtils.isEmpty(category)){
            Toast.makeText(this, "Vui lòng nhập thể loại...", Toast.LENGTH_SHORT).show();
        }
        else {
            addCategoryFirebase();
        }


    }

    private void addCategoryFirebase() {
        progressDialog.setMessage("Đang thêm thể loại mới ....");
        progressDialog.show();

        long timestamp = System.currentTimeMillis();

        // setup data đến firebase
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("id",""+timestamp);
        hashMap.put("category",""+category);
        hashMap.put("timestamp", timestamp);
        hashMap.put("uid",""+firebaseAuth.getUid());

        // Thêm data vào firebase

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Thể loại");
        ref.child(""+ timestamp)
                .setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        // thêm thể loại thành công
                        progressDialog.dismiss();
                        Toast.makeText(CategoryAddActivity.this, "Thêm thể loại mới thành công.", Toast.LENGTH_SHORT).show();

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // thêm thất bại
                        progressDialog.dismiss();
                        Toast.makeText(CategoryAddActivity.this, "Thêm thể loại thất bại", Toast.LENGTH_SHORT).show();
                    }
                });

    }

}