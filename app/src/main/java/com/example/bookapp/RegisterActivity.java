package com.example.bookapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import com.example.bookapp.databinding.ActivityRegisterBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.regex.Pattern;

public class RegisterActivity extends AppCompatActivity {
    private ActivityRegisterBinding binding;

    private FirebaseAuth firebaseAuth;

    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseAuth=FirebaseAuth.getInstance();

        progressDialog=new ProgressDialog(this);
        progressDialog.setTitle("Vui lòng đợi");
        progressDialog.setCanceledOnTouchOutside(false);


        binding.registerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validateData();
            }


        });
    }

    private String name="",email="",password="";
    private void validateData() {
        name=binding.nameEt.getText().toString().trim();
        email=binding.emailEt.getText().toString().trim();
        password=binding.passwordEt.getText().toString().trim();
        String cpassword=binding.confirmPasswordEt.getText().toString().trim();
        if(TextUtils.isEmpty(name)){
            Toast.makeText(this,"Họ Tên",Toast.LENGTH_SHORT).show();
        }
        else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this,"Email không hợp lệ",Toast.LENGTH_SHORT).show();
        }
        else if(TextUtils.isEmpty(password)){
            Toast.makeText(this,"Nhập mật khẩu",Toast.LENGTH_SHORT).show();
        }
        else if(TextUtils.isEmpty(cpassword)){
            Toast.makeText(this,"Xác nhận lại mật khẩu",Toast.LENGTH_SHORT).show();
        }
        else if(!cpassword.equals(password)){
            Toast.makeText(this,"Mật khẩu không trùng khớp",Toast.LENGTH_SHORT).show();
        }
        else {
            createUserAccount();
        }
    }

    private void createUserAccount() {
        progressDialog.setMessage("Đang tạo tài khoản...");
        progressDialog.show();

        firebaseAuth.createUserWithEmailAndPassword(email,password)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        UpdateUserInfo();
                    }


                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        progressDialog.dismiss();
                        Toast.makeText(RegisterActivity.this,""+e.getMessage(),Toast.LENGTH_SHORT).show();
                    }
                });
    }
    private void UpdateUserInfo() {
        progressDialog.setMessage("Lưu thông tin tài khoản...");
        progressDialog.show();


        long timestamp=System.currentTimeMillis();
        String uid=firebaseAuth.getUid();



        HashMap<String,Object>hashmap=new HashMap<>();
        hashmap.put("uid",uid);
        hashmap.put("email",email);
        hashmap.put("name",name);
        hashmap.put("profileImage","");
        hashmap.put("userType","user");
        hashmap.put("timestamp",timestamp);

        DatabaseReference ref= FirebaseDatabase.getInstance().getReference("Users");


        ref.child(uid)
                .setValue(hashmap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {

                        progressDialog.dismiss();
                        Toast.makeText(RegisterActivity.this,"Tài khoản đã tạo thành công.",Toast.LENGTH_SHORT).show();
                        Intent intent=new Intent(RegisterActivity.this,DashboardUserActivity.class);
                        startActivity(intent);
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        progressDialog.dismiss();
                        Toast.makeText(RegisterActivity.this,"Tài khoản tạo thất bái"+e.getMessage(),Toast.LENGTH_SHORT).show();
                    }
                });
    }
}