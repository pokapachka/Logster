package com.example.logster;

import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

public class Profile {
    private static final String TAG = "Profile";
    private final Activity activity;
    private final BottomSheets bottomSheet;

    public Profile(Activity activity, BottomSheets bottomSheet) {
        this.activity = activity;
        this.bottomSheet = bottomSheet;
    }

    public void setupProfileSheet(int layoutId, String data) {
        View view = bottomSheet.getContentView();
        if (view == null) {
            Log.e(TAG, "Ошибка: content view is null for layoutId=" + layoutId);
            Toast.makeText(activity, "Ошибка загрузки интерфейса", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "setupProfileSheet called with layoutId=" + layoutId);

        try {
            if (layoutId == R.layout.profile_tag) {
                EditText tagEditText = view.findViewById(R.id.tag_edit_text);
                View saveBtn = view.findViewById(R.id.save_btn);
                View back = view.findViewById(R.id.back);
                if (tagEditText == null || saveBtn == null || back == null) {
                    Log.e(TAG, "Missing UI elements for R.layout.profile_tag");
                    Toast.makeText(activity, "Ошибка интерфейса тега", Toast.LENGTH_SHORT).show();
                    return;
                }
                String currentTag = activity instanceof ChatActivity ? ((ChatActivity) activity).getProfileTag() : "@user";
                tagEditText.setText(currentTag);
                saveBtn.setOnClickListener(v -> {
                    String tag = tagEditText.getText().toString().trim();
                    if (tag.isEmpty()) {
                        Toast.makeText(activity, "Введите тег", Toast.LENGTH_SHORT).show();
                    } else {
                        if (activity instanceof ChatActivity) {
                            ((ChatActivity) activity).updateProfileTag(tag);
                            bottomSheet.switchSheet(R.layout.profile, null, true, R.anim.slide_out_right, R.anim.slide_in_left);
                        }
                    }
                });
                back.setOnClickListener(v -> bottomSheet.switchSheet(R.layout.profile, null, true, R.anim.slide_out_right, R.anim.slide_in_left));
            } else if (layoutId == R.layout.profile_image) {
                ImageView profileImage = view.findViewById(R.id.image_profile);
                View uploadBtn = view.findViewById(R.id.upload_image);
                View back = view.findViewById(R.id.back);
                if (profileImage == null || uploadBtn == null || back == null) {
                    Log.e(TAG, "Missing UI elements for R.layout.profile_image");
                    Toast.makeText(activity, "Ошибка интерфейса фото", Toast.LENGTH_SHORT).show();
                    return;
                }
                String imageUrl = activity instanceof ChatActivity ? ((ChatActivity) activity).getProfileImageUrl() : null;
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    Glide.with(activity)
                            .load(imageUrl)
                            .placeholder(R.drawable.default_profile)
                            .error(R.drawable.default_profile)
                            .into(profileImage);
                } else {
                    profileImage.setImageResource(R.drawable.default_profile);
                }
                uploadBtn.setOnClickListener(v -> {
                    if (activity instanceof ChatActivity) {
                        ((ChatActivity) activity).startImagePicker();
                        bottomSheet.switchSheet(R.layout.profile, null, true, R.anim.slide_out_right, R.anim.slide_in_left);
                    }
                });
                back.setOnClickListener(v -> bottomSheet.switchSheet(R.layout.profile, null, true, R.anim.slide_out_right, R.anim.slide_in_left));
            } else if (layoutId == R.layout.profile_bio) {
                EditText bioEditText = view.findViewById(R.id.bio_edit_text);
                View saveBtn = view.findViewById(R.id.save_btn);
                View back = view.findViewById(R.id.back);
                if (bioEditText == null || saveBtn == null || back == null) {
                    Log.e(TAG, "Missing UI elements for R.layout.profile_bio");
                    Toast.makeText(activity, "Ошибка интерфейса био", Toast.LENGTH_SHORT).show();
                    return;
                }
                String currentBio = activity instanceof ChatActivity ? ((ChatActivity) activity).getProfileBio() : "";
                bioEditText.setText(currentBio);
                saveBtn.setOnClickListener(v -> {
                    String bio = bioEditText.getText().toString().trim();
                    if (bio.isEmpty()) {
                        Toast.makeText(activity, "Введите био", Toast.LENGTH_SHORT).show();
                    } else {
                        if (activity instanceof ChatActivity) {
                            ((ChatActivity) activity).updateProfileBio(bio);
                            bottomSheet.switchSheet(R.layout.profile, null, true, R.anim.slide_out_right, R.anim.slide_in_left);
                        }
                    }
                });
                back.setOnClickListener(v -> bottomSheet.switchSheet(R.layout.profile, null, true, R.anim.slide_out_right, R.anim.slide_in_left));
            } else if (layoutId == R.layout.profile) {
                View editTagBtn = view.findViewById(R.id.edit_tag);
                View editImageBtn = view.findViewById(R.id.edit_image);
                View editBioBtn = view.findViewById(R.id.edit_bio);
                View close = view.findViewById(R.id.close);
                ImageView logoutBtn = view.findViewById(R.id.logout);
                ImageView deleteBtn = view.findViewById(R.id.delete);
                View continueProfileBtn = view.findViewById(R.id.continue_profile);

                if (editTagBtn == null || editImageBtn == null || editBioBtn == null || close == null ||
                        logoutBtn == null || deleteBtn == null || continueProfileBtn == null) {
                    Log.e(TAG, "Missing UI elements for R.layout.profile");
                    Toast.makeText(activity, "Ошибка интерфейса профиля", Toast.LENGTH_SHORT).show();
                    return;
                }

                editTagBtn.setOnClickListener(v -> bottomSheet.switchSheet(R.layout.profile_tag, null, true, R.anim.slide_out_left, R.anim.slide_in_right));
                editImageBtn.setOnClickListener(v -> bottomSheet.switchSheet(R.layout.profile_image, null, true, R.anim.slide_out_left, R.anim.slide_in_right));
                editBioBtn.setOnClickListener(v -> bottomSheet.switchSheet(R.layout.profile_bio, null, true, R.anim.slide_out_left, R.anim.slide_in_right));
                close.setOnClickListener(v -> bottomSheet.hide(null));
                logoutBtn.setOnClickListener(v -> {
                    RegisterContext.logout(activity);
                    if (activity instanceof ChatActivity) {
                        ((ChatActivity) activity).resetProfileData();
                        bottomSheet.hide(() -> {
                            Toast.makeText(activity, "Выход выполнен", Toast.LENGTH_SHORT).show();
                            ((ChatActivity) activity).initializeUI();
                            ((ChatActivity) activity).autorizations(null);
                            ((ChatActivity) activity).loadMessages(); // Принудительное обновление
                        });
                    }
                });
                deleteBtn.setOnClickListener(v -> {
                    RegisterContext.deleteAccount(activity, new RegisterContext.Callback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            if (activity instanceof ChatActivity) {
                                ((ChatActivity) activity).resetProfileData();
                                bottomSheet.hide(() -> {
                                    Toast.makeText(activity, "Аккаунт удалён", Toast.LENGTH_SHORT).show();
                                    ((ChatActivity) activity).initializeUI();
                                    ((ChatActivity) activity).autorizations(null);
                                    ((ChatActivity) activity).loadMessages(); // Принудительное обновление
                                });
                            }
                        }

                        @Override
                        public void onError(String error) {
                            Toast.makeText(activity, "Ошибка удаления: " + error, Toast.LENGTH_LONG).show();
                            Log.e(TAG, "Delete account error: " + error);
                        }
                    });
                });
                continueProfileBtn.setOnClickListener(v -> bottomSheet.hide(() -> Toast.makeText(activity, "Профиль сохранён", Toast.LENGTH_SHORT).show()));
            } else {
                Log.w(TAG, "Unknown layoutId: " + layoutId);
                Toast.makeText(activity, "Неизвестный экран профиля", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка в setupProfileSheet: " + e.getMessage(), e);
            Toast.makeText(activity, "Ошибка профиля: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}