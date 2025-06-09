package com.example.logster;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

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

    private List<RegisterContext.Message> messages;
    private String currentUserId;
    private String currentUsername;
    private final Context context;

    public ChatAdapter(Context context, List<RegisterContext.Message> messages) {
        this.context = context;
        this.messages = messages != null ? messages : new ArrayList<>();
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
            Log.d("ChatAdapter", "Обновлён текущий пользователь: " + currentUsername);
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
                        .into(otherHolder.userImage);
            } else {
                otherHolder.userImage.setImageResource(R.drawable.default_profile);
            }
        }
    }

    private String formatTimestamp(String createdAt) {
        try {
            // Проверка, если строка пустая или null
            if (createdAt == null || createdAt.isEmpty()) {
                Log.e("ChatAdapter", "Пустая или null строка времени");
                return "Неизвестное время";
            }

            // Парсинг входной даты с поддержкой микросекунд
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", Locale.getDefault());
            inputFormat.setTimeZone(TimeZone.getTimeZone("UTC")); // Входное время в UTC
            Date messageDate;
            try {
                messageDate = inputFormat.parse(createdAt);
            } catch (Exception e) {
                // Попытка альтернативного формата без микросекунд
                inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault());
                inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                messageDate = inputFormat.parse(createdAt);
            }

            if (messageDate == null) {
                Log.e("ChatAdapter", "Не удалось распарсить дату: " + createdAt);
                return "Неизвестное время";
            }

            // Текущее время в часовом поясе Екатеринбурга
            SimpleDateFormat outputTimeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            outputTimeFormat.setTimeZone(TimeZone.getTimeZone("Asia/Yekaterinburg"));

            // Текущая дата и время
            Date now = new Date();
            long diffInMillis = now.getTime() - messageDate.getTime();
            long diffInSeconds = diffInMillis / 1000;
            long diffInMinutes = diffInSeconds / 60;
            long diffInHours = diffInMinutes / 60;
            long diffInDays = diffInHours / 24;

            // Форматирование времени в Екатеринбурге
            String formattedTime = outputTimeFormat.format(messageDate);

            // Логика отображения
            if (diffInDays < 1 && isSameDay(now, messageDate)) {
                // Сегодня — показываем только время
                Log.d("ChatAdapter", "Форматировано время (сегодня): " + createdAt + " -> " + formattedTime);
                return formattedTime;
            } else if (diffInDays == 1) {
                // Вчера — показываем "вчера" и время
                Log.d("ChatAdapter", "Форматировано время (вчера): " + createdAt + " -> вчера " + formattedTime);
                return "вчера " + formattedTime;
            } else if (diffInDays == 2) {
                // Позавчера — показываем "позавчера" и время
                Log.d("ChatAdapter", "Форматировано время (позавчера): " + createdAt + " -> позавчера " + formattedTime);
                return "позавчера " + formattedTime;
            } else if (diffInDays >= 3 && diffInDays <= 6) {
                // 3–6 дней назад — показываем "X дней назад" и время
                String daysText = getDaysText(diffInDays);
                Log.d("ChatAdapter", "Форматировано время (дни назад): " + createdAt + " -> " + daysText + " " + formattedTime);
                return daysText + " " + formattedTime;
            } else if (diffInDays >= 7 && diffInDays < 14) {
                // Неделю назад
                Log.d("ChatAdapter", "Форматировано время (неделя назад): " + createdAt + " -> неделю назад");
                return "неделю назад";
            } else if (diffInDays >= 14 && diffInDays < 21) {
                // Две недели назад
                Log.d("ChatAdapter", "Форматировано время (две недели назад): " + createdAt + " -> две недели назад");
                return "две недели назад";
            } else if (diffInDays >= 21 && diffInDays < 30) {
                // Три недели назад
                Log.d("ChatAdapter", "Форматировано время (три недели назад): " + createdAt + " -> три недели назад");
                return "три недели назад";
            } else {
                // Месяц назад
                Log.d("ChatAdapter", "Форматировано время (месяц назад): " + createdAt + " -> месяц назад");
                return "месяц назад";
            }
        } catch (Exception e) {
            Log.e("ChatAdapter", "Ошибка парсинга времени: " + e.getMessage() + " для строки: " + createdAt);
            return "Неизвестное время";
        }
    }

    // Вспомогательный метод для проверки, является ли дата сегодняшним днём
    private boolean isSameDay(Date date1, Date date2) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        dateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Yekaterinburg"));
        return dateFormat.format(date1).equals(dateFormat.format(date2));
    }

    // Метод для правильного склонения слова "день"
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
        Log.d("ChatAdapter", "Список сообщений: очищен");
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
        Log.d("ChatAdapter", "Обновлено сообщений: " + newMessages.size());
    }

    public void addMessage(RegisterContext.Message message) {
        this.messages.add(0, message);
        notifyItemInserted(0);
        Log.d("ChatAdapter", "Добавлено сообщение: " + message.content);
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