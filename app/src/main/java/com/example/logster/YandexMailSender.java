package com.example.logster;

import android.os.AsyncTask;
import android.util.Log;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class YandexMailSender extends AsyncTask<String, Void, String> {
    private static final String SMTP_HOST = "smtp.yandex.ru";
    private static final int SMTP_PORT = 465;
    private static final String USERNAME = "logster1@yandex.ru"; // Замените на ваш email
    private static final String PASSWORD = "fdfrbhuqaxybhlei"; // Замените на пароль приложения

    public interface MailCallback {
        void onSuccess();
        void onError(String error);
    }

    private final MailCallback callback;

    public YandexMailSender(MailCallback callback) {
        this.callback = callback;
    }

    @Override
    protected String doInBackground(String... params) {
        String recipient = params[0];
        String subject = params[1];
        String text = params[2];

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.ssl.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);
        props.put("mail.smtp.ssl.trust", SMTP_HOST);

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(USERNAME, PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(USERNAME));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
            message.setSubject(subject);
            message.setText(text);
            Transport.send(message);
            return "Email sent successfully";
        } catch (MessagingException e) {
            Log.e("YandexMailSender", "Error sending email", e);
            return "Error: " + e.getMessage();
        }
    }

    @Override
    protected void onPostExecute(String result) {
        if (result.startsWith("Error")) {
            callback.onError(result);
        } else {
            callback.onSuccess();
        }
    }
}