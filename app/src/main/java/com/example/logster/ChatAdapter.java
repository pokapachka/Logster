package com.example.logster;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int SELF_MESSAGE = 1;
    private static final int OTHER_MESSAGE = 2;
    private static final String TAG = "ChatAdapter";

    private List<RegisterContext.Message> messages;
    private String currentUserId;
    private String currentUsername;
    private final Context context;
    private final BottomSheets bottomSheets; // Для открытия профиля

    public ChatAdapter(Context context, List<RegisterContext.Message> messages) {
        this.context = context;
        this.messages = messages != null ? messages : new ArrayList<>();
        this.bottomSheets = new BottomSheets((Activity) context); // Инициализация BottomSheets
        updateCurrentUser();
    }

    public void updateCurrentUser() {
        SharedPreferences prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
        String newUserId = prefs.getString("user_id", "");
        String newUsername = prefs.getString("username", "User");
        if (!newUserId.equals(currentUserId) || !newUsername.equals(currentUsername)) {
            currentUserId = newUserId;
            currentUsername = newUsername;
            notifyDataSetChanged();
            Log.d(TAG, "Обновлён текущий пользователь: " + currentUsername);
        }
    }

    @Override
    public int getItemViewType(int position) {
        RegisterContext.Message message = messages.get(position);
        return (message.userId != null && message.userId.equals(currentUserId)) ? SELF_MESSAGE : OTHER_MESSAGE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == SELF_MESSAGE) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.self_message_item, parent, false);
            return new SelfMessageViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.other_message_item, parent, false);
            return new OtherMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        RegisterContext.Message message = messages.get(position);
        if (holder instanceof SelfMessageViewHolder) {
            SelfMessageViewHolder selfHolder = (SelfMessageViewHolder) holder;
            selfHolder.contentTextView.setText(message.content);
            selfHolder.usernameTextView.setText("@" + currentUsername);
            selfHolder.timestampTextView.setText(formatTimestamp(message.createdAt));

            // Обработчик долгого нажатия для своих сообщений
            selfHolder.itemView.setOnLongClickListener(v -> {
                ConfirmationBottomSheet confirmationSheet = new ConfirmationBottomSheet(context);
                confirmationSheet.show(
                        "Удалить сообщение",
                        message.content.length() > 20 ? message.content.substring(0, 20) + "..." : message.content,
                        "Message",
                        message.id,
                        () -> {
                            // Действие при подтверждении удаления
                            int currentPosition = holder.getAdapterPosition();
                            if (currentPosition == RecyclerView.NO_POSITION) {
                                Log.w(TAG, "Позиция недействительна, удаление отменено: id=" + message.id);
                                return;
                            }
                            RegisterContext.deleteMessage(context, message.id, new RegisterContext.Callback<Void>() {
                                @Override
                                public void onSuccess(Void result) {
                                    ((Activity) context).runOnUiThread(() -> {
                                        // Удаляем сообщение из списка
                                        if (currentPosition < messages.size() && messages.get(currentPosition).id.equals(message.id)) {
                                            messages.remove(currentPosition);
                                            notifyItemRemoved(currentPosition);
                                            notifyItemRangeChanged(currentPosition, messages.size());
                                            Log.d(TAG, "Сообщение удалено: id=" + message.id);
                                            // Обновляем список сообщений с сервера
                                            if (context instanceof ChatActivity) {
                                                ((ChatActivity) context).loadMessages();
                                            }
                                        } else {
                                            Log.w(TAG, "Сообщение на позиции не соответствует ID, обновление UI пропущено");
                                            // Обновляем список для синхронизации
                                            if (context instanceof ChatActivity) {
                                                ((ChatActivity) context).loadMessages();
                                            }
                                        }
                                    });
                                }

                                @Override
                                public void onError(String error) {
                                    ((Activity) context).runOnUiThread(() -> {
                                        Toast.makeText(context, "Ошибка удаления: " + error, Toast.LENGTH_SHORT).show();
                                        Log.e(TAG, "Ошибка удаления сообщения: " + error);
                                    });
                                }
                            });
                        },
                        () -> Log.d(TAG, "Удаление сообщения отменено: id=" + message.id)
                );
                return true;
            });
        } else {
            OtherMessageViewHolder otherHolder = (OtherMessageViewHolder) holder;
            otherHolder.usernameTextView.setText("@" + message.sender_login);
            otherHolder.contentTextView.setText(message.content);
            otherHolder.timestampTextView.setText(formatTimestamp(message.createdAt));
            if (message.user_image != null && !message.user_image.isEmpty()) {
                Glide.with(otherHolder.itemView.getContext())
                        .load(message.user_image)
                        .placeholder(R.drawable.default_profile)
                        .error(R.drawable.default_profile)
                        .circleCrop()
                        .into(otherHolder.userImage);
            } else {
                otherHolder.userImage.setImageResource(R.drawable.default_profile);
            }
            // Обработчик клика по изображению пользователя
            otherHolder.userImage.setOnClickListener(v -> {
                if (context instanceof ChatActivity) {
                    bottomSheets.switchSheet(R.layout.profile_user, message.userId, false, 0, 0);
                    Log.d(TAG, "Открыт профиль пользователя: userId=" + message.userId);
                }
            });
        }
    }

    private String formatTimestamp(String createdAt) {
        try {
            if (createdAt == null || createdAt.isEmpty()) {
                Log.e(TAG, "Пустая или null строка времени");
                return "Неизвестное время";
            }

            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", Locale.getDefault());
            inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date messageDate;
            try {
                messageDate = inputFormat.parse(createdAt);
            } catch (Exception e) {
                inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault());
                inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                messageDate = inputFormat.parse(createdAt);
            }

            if (messageDate == null) {
                Log.e(TAG, "Не удалось распарсить дату: " + createdAt);
                return "Неизвестное время";
            }

            SimpleDateFormat outputTimeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            outputTimeFormat.setTimeZone(TimeZone.getTimeZone("Asia/Yekaterinburg"));

            Date now = new Date();
            long diffInMillis = now.getTime() - messageDate.getTime();
            long diffInSeconds = diffInMillis / 1000;
            long diffInMinutes = diffInSeconds / 60;
            long diffInHours = diffInMinutes / 60;
            long diffInDays = diffInHours / 24;

            String formattedTime = outputTimeFormat.format(messageDate);

            if (diffInDays < 1 && isSameDay(now, messageDate)) {
                Log.d(TAG, "Форматировано время (сегодня): " + createdAt + " -> " + formattedTime);
                return formattedTime;
            } else if (diffInDays == 1) {
                Log.d(TAG, "Форматировано время (вчера): " + createdAt + " -> вчера " + formattedTime);
                return "вчера " + formattedTime;
            } else if (diffInDays == 2) {
                Log.d(TAG, "Форматировано время (позавчера): " + createdAt + " -> позавчера " + formattedTime);
                return "позавчера " + formattedTime;
            } else if (diffInDays >= 3 && diffInDays <= 6) {
                String daysText = getDaysText(diffInDays);
                Log.d(TAG, "Форматировано время (дни назад): " + createdAt + " -> " + daysText + " " + formattedTime);
                return daysText + " " + formattedTime;
            } else if (diffInDays >= 7 && diffInDays < 14) {
                Log.d(TAG, "Форматировано время (неделя назад): " + createdAt + " -> неделю назад");
                return "неделю назад";
            } else if (diffInDays >= 14 && diffInDays < 21) {
                Log.d(TAG, "Форматировано время (две недели назад): " + createdAt + " -> две недели назад");
                return "две недели назад";
            } else if (diffInDays >= 21 && diffInDays < 30) {
                Log.d(TAG, "Форматировано время (три недели назад): " + createdAt + " -> три недели назад");
                return "три недели назад";
            } else {
                Log.d(TAG, "Форматировано время (месяц назад): " + createdAt + " -> месяц назад");
                return "месяц назад";
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка парсинга времени: " + e.getMessage() + " для строки: " + createdAt);
            return "Неизвестное время";
        }
    }

    private boolean isSameDay(Date date1, Date date2) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        dateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Yekaterinburg"));
        return dateFormat.format(date1).equals(dateFormat.format(date2));
    }

    private String getDaysText(long days) {
        if (days == 3) {
            return "3 дня назад";
        } else if (days == 4) {
            return "4 дня назад";
        } else {
            return days + " дней назад";
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void clear() {
        messages.clear();
        notifyDataSetChanged();
        Log.d(TAG, "Список сообщений: очищен");
    }

    public void updateMessages(List<RegisterContext.Message> newMessages) {
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return messages.size();
            }

            @Override
            public int getNewListSize() {
                return newMessages.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return messages.get(oldItemPosition).id.equals(newMessages.get(newItemPosition).id);
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                return messages.get(oldItemPosition).equals(newMessages.get(newItemPosition));
            }
        });
        messages.clear();
        messages.addAll(newMessages);
        result.dispatchUpdatesTo(this);
        Log.d(TAG, "Обновлено сообщений: " + newMessages.size());
    }

    public void addMessage(RegisterContext.Message message) {
        this.messages.add(0, message);
        notifyItemInserted(0);
        Log.d(TAG, "Добавлено сообщение: " + message.content);
    }

    static class SelfMessageViewHolder extends RecyclerView.ViewHolder {
        TextView contentTextView;
        TextView usernameTextView;
        TextView timestampTextView;

        SelfMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            contentTextView = itemView.findViewById(R.id.self_content);
            usernameTextView = itemView.findViewById(R.id.self_username);
            timestampTextView = itemView.findViewById(R.id.self_timestamp);
        }
    }

    static class OtherMessageViewHolder extends RecyclerView.ViewHolder {
        TextView usernameTextView;
        TextView contentTextView;
        TextView timestampTextView;
        ImageView userImage;

        OtherMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            usernameTextView = itemView.findViewById(R.id.username);
            contentTextView = itemView.findViewById(R.id.content);
            timestampTextView = itemView.findViewById(R.id.timestamp);
            userImage = itemView.findViewById(R.id.user_image);
        }
    }


}