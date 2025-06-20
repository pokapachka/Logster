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
    private final BottomSheets bottomSheets;
    private final ConfirmationBottomSheet confirmationSheet;

    public Profile(Activity activity, BottomSheets bottomSheet) {
        this.activity = activity;
        this.bottomSheets = bottomSheet;
        this.confirmationSheet = new ConfirmationBottomSheet(activity);
    }

    public void setupProfileSheet(int layoutId, String data) {
        View view = bottomSheets.getContentView();
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
                    Log.e(TAG, "Missing UI elements for R.layout.profile_tag: tag_edit_text=" + (tagEditText == null) +
                            ", save_btn=" + (saveBtn == null) + ", back=" + (back == null));
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
                            bottomSheets.switchSheet(R.layout.profile, null, true, R.anim.slide_out_right, R.anim.slide_in_left);
                        }
                    }
                });
                back.setOnClickListener(v -> bottomSheets.switchSheet(R.layout.profile, null, true, R.anim.slide_out_right, R.anim.slide_in_left));
            } else if (layoutId == R.layout.profile_image) {
                ImageView profileImage = view.findViewById(R.id.image_profile);
                View uploadBtn = view.findViewById(R.id.upload_image);
                View back = view.findViewById(R.id.back);
                if (profileImage == null || uploadBtn == null || back == null) {
                    Log.e(TAG, "Missing UI elements for R.layout.profile_image: image_profile=" + (profileImage == null) +
                            ", upload_btn=" + (uploadBtn == null) + ", back=" + (back == null));
                    Toast.makeText(activity, "Ошибка интерфейса фото", Toast.LENGTH_SHORT).show();
                    return;
                }
                profileImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
                String imageUrl = activity instanceof ChatActivity ? ((ChatActivity) activity).getProfileImageUrl() : null;
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    Glide.with(activity)
                            .load(imageUrl)
                            .placeholder(R.drawable.default_profile)
                            .error(R.drawable.default_profile)
                            .circleCrop()
                            .into(profileImage);
                } else {
                    profileImage.setImageResource(R.drawable.default_profile);
                }
                uploadBtn.setOnClickListener(v -> {
                    if (activity instanceof ChatActivity) {
                        ((ChatActivity) activity).startImagePicker();
                    }
                });
                back.setOnClickListener(v -> bottomSheets.switchSheet(R.layout.profile, null, true, R.anim.slide_out_right, R.anim.slide_in_left));
            } else if (layoutId == R.layout.profile_bio) {
                EditText bioEditText = view.findViewById(R.id.bio_edit_text);
                View saveBtn = view.findViewById(R.id.save_btn);
                View back = view.findViewById(R.id.back);
                if (bioEditText == null || saveBtn == null || back == null) {
                    Log.e(TAG, "Missing UI elements for R.layout.profile_bio: bio_edit_text=" + (bioEditText == null) +
                            ", save_btn=" + (saveBtn == null) + ", back=" + (back == null));
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
                            bottomSheets.switchSheet(R.layout.profile, null, true, R.anim.slide_out_right, R.anim.slide_in_left);
                        }
                    }
                });
                back.setOnClickListener(v -> bottomSheets.switchSheet(R.layout.profile, null, true, R.anim.slide_out_right, R.anim.slide_in_left));
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
                    Log.e(TAG, "Missing UI elements for R.layout.profile: edit_tag=" + (editTagBtn == null) +
                            ", edit_image=" + (editImageBtn == null) + ", edit_bio=" + (editBioBtn == null) +
                            ", close=" + (close == null) + ", logout=" + (logoutBtn == null) +
                            ", delete=" + (deleteBtn == null) + ", continue_profile=" + (continueProfileBtn == null));
                    Toast.makeText(activity, "Ошибка интерфейса профиля", Toast.LENGTH_SHORT).show();
                    return;
                }

                editTagBtn.setOnClickListener(v -> bottomSheets.switchSheet(R.layout.profile_tag, null, true, R.anim.slide_out_left, R.anim.slide_in_right));
                editImageBtn.setOnClickListener(v -> bottomSheets.switchSheet(R.layout.profile_image, null, true, R.anim.slide_out_left, R.anim.slide_in_right));
                editBioBtn.setOnClickListener(v -> bottomSheets.switchSheet(R.layout.profile_bio, null, true, R.anim.slide_out_left, R.anim.slide_in_right));
                close.setOnClickListener(v -> bottomSheets.hide(null));
                logoutBtn.setOnClickListener(v -> {
                    if (confirmationSheet.isShowing()) {
                        Log.w(TAG, "ConfirmationBottomSheet уже отображается, игнорируем повторное нажатие");
                        return;
                    }
                    confirmationSheet.show(
                            "", "", "Logout", "user_logout",
                            () -> {
                                try {
                                    RegisterContext.logout(activity);
                                    if (activity instanceof ChatActivity && !activity.isFinishing()) {
                                        ((ChatActivity) activity).resetProfileData();
                                        confirmationSheet.hide(() -> {
                                            bottomSheets.hide(() -> {
                                                if (!activity.isFinishing()) {
                                                    Toast.makeText(activity, "Выход выполнен", Toast.LENGTH_SHORT).show();
                                                    ((ChatActivity) activity).initializeUI();
                                                    ((ChatActivity) activity).autorizations(null);
                                                    ((ChatActivity) activity).loadMessages();
                                                }
                                            });
                                        });
                                        Log.d(TAG, "Выход из аккаунта подтверждён");
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Logout error: " + e.getMessage(), e);
                                    if (!activity.isFinishing()) {
                                        Toast.makeText(activity, "Ошибка выхода: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                }
                            },
                            () -> {
                                confirmationSheet.hide(null);
                                Log.d(TAG, "Выход из аккаунта отменён");
                            }
                    );
                });
                deleteBtn.setOnClickListener(v -> {
                    confirmationSheet.show(
                            "", // Не используется для DeleteAccount
                            "", // Не используется для DeleteAccount
                            "DeleteAccount",
                            "user_delete",
                            () -> {
                                RegisterContext.deleteAccount(activity, new RegisterContext.Callback<Void>() {
                                    @Override
                                    public void onSuccess(Void result) {
                                        if (activity instanceof ChatActivity) {
                                            ((ChatActivity) activity).resetProfileData();
                                            // Последовательное закрытие листов
                                            confirmationSheet.hide(() -> bottomSheets.hide(() -> {
                                                Toast.makeText(activity, "Аккаунт удалён", Toast.LENGTH_SHORT).show();
                                                ((ChatActivity) activity).initializeUI();
                                                ((ChatActivity) activity).autorizations(null);
                                                ((ChatActivity) activity).loadMessages();
                                            }));
                                            Log.d(TAG, "Удаление аккаунта подтверждено");
                                        }
                                    }

                                    @Override
                                    public void onError(String error) {
                                        Toast.makeText(activity, "Ошибка удаления: " + error, Toast.LENGTH_LONG).show();
                                        Log.e(TAG, "Delete account error: " + error);
                                    }
                                });
                            },
                            () -> {
                                confirmationSheet.hide(null); // Закрываем лист при отмене
                                Log.d(TAG, "Удаление аккаунта отменено");
                            }
                    );
                });
                continueProfileBtn.setOnClickListener(v -> bottomSheets.hide(() -> Toast.makeText(activity, "Профиль сохранён", Toast.LENGTH_SHORT).show()));
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