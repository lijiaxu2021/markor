package net.gsantner.markor.frontend.blog;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ImageUploader {
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    
    private final String baseUrl;
    private final String token;
    private final OkHttpClient client;
    private final Gson gson;

    public ImageUploader(Context context) {
        SharedPreferencesHelper prefs = new SharedPreferencesHelper(context);
        this.baseUrl = prefs.getBaseUrl();
        this.token = prefs.getToken();
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS) // Longer timeout for uploads
                .build();
        this.gson = new Gson();
    }

    public String uploadImage(Uri imageUri, Context context) throws IOException {
        // 压缩并转换为 Base64
        String base64 = encodeImageToBase64(imageUri, context);
        
        // 生成文件名
        String filename = generateFilename(imageUri);
        
        // 上传
        UploadRequest request = new UploadRequest(filename, base64);
        String body = gson.toJson(request);
        
        okhttp3.Request httpRequest = new Request.Builder()
                .url(baseUrl + "/api/upload")
                .addHeader("Authorization", "Bearer " + token)
                .post(RequestBody.create(body, JSON))
                .build();
        
        try (Response response = client.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Upload failed: HTTP " + response.code());
            }
            
            String responseBody = response.body().string();
            UploadResult result = gson.fromJson(responseBody, UploadResult.class);
            return result.url;
        }
    }

    private String encodeImageToBase64(Uri imageUri, Context context) throws IOException {
        InputStream inputStream = null;
        try {
            inputStream = context.getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                throw new IOException("Cannot open input stream for URI: " + imageUri);
            }
            
            // 先解码获取 Bitmap 用于压缩
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            
            // 重置流
            inputStream.close();
            inputStream = null;
            
            // 压缩图片
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            if (bitmap != null) {
                // 调整大小
                int maxWidth = 1920;
                int maxHeight = 1080;
                float ratio = Math.min((float) maxWidth / bitmap.getWidth(), (float) maxHeight / bitmap.getHeight());
                if (ratio < 1) {
                    int width = Math.round(bitmap.getWidth() * ratio);
                    int height = Math.round(bitmap.getHeight() * ratio);
                    bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
                }
                
                // 压缩为 JPEG
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos);
                bitmap.recycle();
            } else {
                // 如果无法解码，直接读取原始数据
                inputStream = context.getContentResolver().openInputStream(imageUri);
                if (inputStream == null) {
                    throw new IOException("Cannot reopen input stream");
                }
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    baos.write(buffer, 0, bytesRead);
                }
            }
            
            return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }

    private String generateFilename(Uri imageUri) {
        long timestamp = System.currentTimeMillis();
        String randomSuffix = UUID.randomUUID().toString().substring(0, 8);
        
        // 尝试从 URI 获取文件扩展名
        String extension = "jpg"; // 默认
        if (imageUri.getPath() != null) {
            int dotIndex = imageUri.getPath().lastIndexOf('.');
            if (dotIndex > 0 && dotIndex < imageUri.getPath().length() - 1) {
                extension = imageUri.getPath().substring(dotIndex + 1).toLowerCase();
                if (!extension.matches("^(jpg|jpeg|png|gif|webp)$")) {
                    extension = "jpg";
                }
            }
        }
        
        return String.format("image_%d_%s.%s", timestamp, randomSuffix, extension);
    }

    private static class UploadRequest {
        @SerializedName("filename")
        public final String filename;
        
        @SerializedName("content")
        public final String content;

        public UploadRequest(String filename, String content) {
            this.filename = filename;
            this.content = content;
        }
    }

    private static class UploadResult {
        @SerializedName("url")
        public String url;
    }

    // Helper class for SharedPreferences
    private static class SharedPreferencesHelper {
        private final android.content.SharedPreferences prefs;
        private static final String PREFS_NAME = "blog_admin_prefs";
        private static final String KEY_BASE_URL = "base_url";
        private static final String KEY_TOKEN = "token";
        private static final String DEFAULT_URL = "https://edit.upxuu.com";

        public SharedPreferencesHelper(Context context) {
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        }

        public String getBaseUrl() {
            return prefs.getString(KEY_BASE_URL, DEFAULT_URL);
        }

        public String getToken() {
            return prefs.getString(KEY_TOKEN, "");
        }
    }
}
