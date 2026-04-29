package com.example.deadline_tracker;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class SettingsActivity extends AppCompatActivity {

    private CircleImageView profilePhoto;
    private TextView        tvProfileName, tvProfileRole;
    private EditText        etEditName, etEditDept, etEditBatch;
    private Button          btnSaveProfile;

    private FirebaseAuth      mAuth;
    private FirebaseFirestore db;
    private String            currentUid;

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    profilePhoto.setImageURI(uri);
                    uploadPhotoToCloudinary(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        CloudinaryManager.init(this);

        mAuth      = FirebaseAuth.getInstance();
        db         = FirebaseFirestore.getInstance();
        currentUid = mAuth.getCurrentUser().getUid();

        profilePhoto   = findViewById(R.id.profilePhoto);
        tvProfileName  = findViewById(R.id.tvProfileName);
        tvProfileRole  = findViewById(R.id.tvProfileRole);
        etEditName     = findViewById(R.id.etEditName);
        etEditDept     = findViewById(R.id.etEditDept);
        etEditBatch    = findViewById(R.id.etEditBatch);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);

        findViewById(R.id.tvBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnChangePhoto).setOnClickListener(v ->
                imagePickerLauncher.launch("image/*"));
        btnSaveProfile.setOnClickListener(v -> saveProfile());
        findViewById(R.id.btnLogout).setOnClickListener(v -> showLogoutDialog());

        // ✅ Change password button
        findViewById(R.id.btnChangePassword).setOnClickListener(v ->
                showChangePasswordDialog());

        findViewById(R.id.navHome).setOnClickListener(v -> finish());
        findViewById(R.id.navCalendar).setOnClickListener(v ->
                startActivity(new Intent(this, CalendarActivity.class)));
        findViewById(R.id.navNotif).setOnClickListener(v ->
                startActivity(new Intent(this, NotificationListActivity.class)));

        loadProfile();
    }

    private void loadProfile() {
        db.collection("users").document(currentUid).get()
                .addOnSuccessListener(doc -> {
                    String name  = doc.getString("name");
                    String dept  = doc.getString("department");
                    String batch = doc.getString("batch");
                    String photo = doc.getString("photoUrl");

                    tvProfileName.setText(name != null ? name : "");
                    tvProfileRole.setText((dept != null ? dept : "") +
                            " — Batch " + (batch != null ? batch : ""));
                    etEditName.setText(name);
                    etEditDept.setText(dept);
                    etEditBatch.setText(batch);

                    // ✅ Load profile photo using Glide
                    if (photo != null && !photo.isEmpty()) {
                        Glide.with(this).load(photo).circleCrop()
                                .placeholder(R.drawable.bg_avatar_green)
                                .into(profilePhoto);
                    }
                });
    }

    // ✅ Upload to Cloudinary instead of Firebase Storage
    private void uploadPhotoToCloudinary(Uri uri) {
        Toast.makeText(this, "Uploading photo...", Toast.LENGTH_SHORT).show();

        CloudinaryManager.uploadFile(this, uri,
                "deadline_tracker/profile_photos",
                new CloudinaryManager.UploadListener() {
                    @Override
                    public void onProgress(int percent) {}

                    @Override
                    public void onSuccess(String fileUrl, String publicId) {
                        // Save URL to Firestore
                        db.collection("users").document(currentUid)
                                .update("photoUrl", fileUrl)
                                .addOnSuccessListener(unused -> {
                                    runOnUiThread(() -> {
                                        Toast.makeText(SettingsActivity.this,
                                                "✅ Photo updated!",
                                                Toast.LENGTH_SHORT).show();
                                        // Reload with Glide
                                        Glide.with(SettingsActivity.this)
                                                .load(fileUrl).circleCrop()
                                                .into(profilePhoto);
                                    });
                                });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() ->
                                Toast.makeText(SettingsActivity.this,
                                        "Upload failed: " + error,
                                        Toast.LENGTH_LONG).show());
                    }
                });
    }

    private void saveProfile() {
        String name  = etEditName.getText().toString().trim();
        String dept  = etEditDept.getText().toString().trim();
        String batch = etEditBatch.getText().toString().trim();

        if (TextUtils.isEmpty(name))  { etEditName.setError("Required");  return; }
        if (TextUtils.isEmpty(dept))  { etEditDept.setError("Required");  return; }
        if (TextUtils.isEmpty(batch)) { etEditBatch.setError("Required"); return; }

        btnSaveProfile.setEnabled(false);

        Map<String, Object> updates = new HashMap<>();
        updates.put("name",       name);
        updates.put("department", dept);
        updates.put("batch",      batch);

        db.collection("users").document(currentUid).update(updates)
                .addOnSuccessListener(unused -> {
                    btnSaveProfile.setEnabled(true);
                    tvProfileName.setText(name);
                    tvProfileRole.setText(dept + " — Batch " + batch);
                    Toast.makeText(this, "✅ Profile updated!",
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    btnSaveProfile.setEnabled(true);
                    Toast.makeText(this, "Failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    // ✅ Change password dialog
    private void showChangePasswordDialog() {
        EditText etNewPassword = new EditText(this);
        etNewPassword.setHint("New password (min 6 chars)");
        etNewPassword.setInputType(
                InputType.TYPE_CLASS_TEXT |
                        InputType.TYPE_TEXT_VARIATION_PASSWORD);
        etNewPassword.setPadding(48, 32, 48, 32);

        new AlertDialog.Builder(this)
                .setTitle("Change Password")
                .setView(etNewPassword)
                .setPositiveButton("Update", (dialog, which) -> {
                    String newPass = etNewPassword.getText().toString().trim();
                    if (newPass.length() < 6) {
                        Toast.makeText(this,
                                "Password must be at least 6 characters",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    mAuth.getCurrentUser().updatePassword(newPass)
                            .addOnSuccessListener(unused ->
                                    Toast.makeText(this,
                                            "✅ Password updated!",
                                            Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e ->
                                    Toast.makeText(this,
                                            "Failed (please re-login and try again): "
                                                    + e.getMessage(),
                                            Toast.LENGTH_LONG).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Log out")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Log out", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}