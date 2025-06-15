package com.example.logster;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class FileUtils {
    private static final String TAG = "FileUtils";

    public static String getPath(Context context, Uri uri) {
        try {
            Log.d(TAG, "Processing Uri: " + uri.toString());
            if ("content".equalsIgnoreCase(uri.getScheme())) {
                File tempFile = createTempFile(context);
                if (copyUriToFile(context, uri, tempFile)) {
                    String path = tempFile.getAbsolutePath();
                    Log.d(TAG, "Successfully copied file to: " + path);
                    return path;
                } else {
                    Log.e(TAG, "Failed to copy Uri to file");
                    return null;
                }
            } else if ("file".equalsIgnoreCase(uri.getScheme())) {
                String path = uri.getPath();
                Log.d(TAG, "Direct file path: " + path);
                return path;
            } else {
                Log.e(TAG, "Unsupported Uri scheme: " + uri.getScheme());
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting path from Uri: " + e.getMessage(), e);
            return null;
        }
    }

    private static File createTempFile(Context context) throws Exception {
        String fileName = "temp_image_" + System.currentTimeMillis() + ".jpg";
        File tempFile = new File(context.getCacheDir(), fileName);
        Log.d(TAG, "Created temp file: " + tempFile.getAbsolutePath());
        return tempFile;
    }

    private static boolean copyUriToFile(Context context, Uri uri, File destination) {
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
             OutputStream outputStream = new FileOutputStream(destination)) {
            if (inputStream == null) {
                Log.e(TAG, "InputStream is null for Uri: " + uri);
                return false;
            }
            String mimeType = context.getContentResolver().getType(uri);
            Log.d(TAG, "MIME type: " + mimeType);
            if (mimeType == null || !(mimeType.equals("image/jpeg") || mimeType.equals("image/png"))) {
                Log.e(TAG, "Unsupported file type: " + mimeType);
                return false;
            }
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            Log.d(TAG, "File copied successfully to: " + destination.getAbsolutePath());
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error copying Uri to file: " + e.getMessage(), e);
            return false;
        }
    }

    // Новый метод для сжатия изображения
    public static String compressImage(Context context, String inputPath) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(inputPath, options);
            int scale = 1;
            if (options.outHeight > 1024 || options.outWidth > 1024) {
                scale = (int) Math.pow(2, (int) Math.round(Math.log(1024 / (double) Math.max(options.outHeight, options.outWidth)) / Math.log(0.5)));
            }
            options.inJustDecodeBounds = false;
            options.inSampleSize = scale;
            Bitmap bitmap = BitmapFactory.decodeFile(inputPath, options);
            if (bitmap == null) {
                Log.e(TAG, "Failed to decode image: " + inputPath);
                return inputPath;
            }

            File compressedFile = createTempFile(context);
            FileOutputStream fos = new FileOutputStream(compressedFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);
            fos.flush();
            fos.close();
            bitmap.recycle();
            Log.d(TAG, "Compressed image: " + compressedFile.getAbsolutePath() + ", size: " + (compressedFile.length() / 1024) + " KB");
            return compressedFile.getAbsolutePath();
        } catch (Exception e) {
            Log.e(TAG, "Image compression error: " + e.getMessage(), e);
            return inputPath;
        }
    }
}