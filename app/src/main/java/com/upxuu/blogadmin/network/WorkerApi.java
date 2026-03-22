package com.upxuu.blogadmin.network;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class WorkerApi {
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    
    private final String baseUrl;
    private final String token;
    private final OkHttpClient client;
    private final Gson gson;

    public WorkerApi(String baseUrl, String token) {
        this.baseUrl = baseUrl;
        this.token = token;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
    }

    public List<Post> listPosts() throws IOException {
        Request request = buildRequest("/api/posts", "GET", null);
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code());
            }
            String body = response.body().string();
            return gson.fromJson(body, Post[].class);
        }
    }

    public PostContent getPost(String filename) throws IOException {
        String encodedFilename = java.net.URLEncoder.encode(filename, "UTF-8");
        Request request = buildRequest("/api/post/" + encodedFilename, "GET", null);
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code());
            }
            String body = response.body().string();
            return gson.fromJson(body, PostContent.class);
        }
    }

    public String savePost(String filename, String content, String sha) throws IOException {
        String encodedFilename = java.net.URLEncoder.encode(filename, "UTF-8");
        String body = gson.toJson(new SavePostRequest(content, sha));
        Request request = buildRequest("/api/post/" + encodedFilename, "PUT", body);
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code());
            }
            return "Saved successfully";
        }
    }

    public String deletePost(String filename, String sha) throws IOException {
        String encodedFilename = java.net.URLEncoder.encode(filename, "UTF-8");
        String body = gson.toJson(new DeleteRequest(sha));
        Request request = buildRequest("/api/post/" + encodedFilename, "DELETE", body);
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code());
            }
            return "Deleted successfully";
        }
    }

    public String uploadImage(String filename, String base64Content) throws IOException {
        String body = gson.toJson(new UploadRequest(filename, base64Content));
        Request request = buildRequest("/api/upload", "POST", body);
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code());
            }
            String responseBody = response.body().string();
            UploadResult result = gson.fromJson(responseBody, UploadResult.class);
            return result.url;
        }
    }

    public List<ImageInfo> listImages() throws IOException {
        Request request = buildRequest("/api/images", "GET", null);
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code());
            }
            String body = response.body().string();
            return gson.fromJson(body, ImageInfo[].class);
        }
    }

    private Request buildRequest(String path, String method, String body) {
        Request.Builder builder = new Request.Builder()
                .url(baseUrl + path)
                .addHeader("Authorization", "Bearer " + token);
        
        if ("GET".equals(method)) {
            builder.get();
        } else if ("POST".equals(method)) {
            builder.post(body != null ? RequestBody.create(body, JSON) : RequestBody.create("", JSON));
        } else if ("PUT".equals(method)) {
            builder.put(RequestBody.create(body, JSON));
        } else if ("DELETE".equals(method)) {
            builder.delete(body != null ? RequestBody.create(body, JSON) : null);
        }
        
        return builder.build();
    }

    // Data Classes
    public static class Post {
        public String name;
        public String path;
        public String sha;
        public String title;
        public String date;
        public String type;
    }

    public static class PostContent {
        public String content;
        public String sha;
    }

    public static class ImageInfo {
        public String name;
        public String path;
        public String sha;
    }

    private static class SavePostRequest {
        @SerializedName("content")
        public String content;
        
        @SerializedName("sha")
        public String sha;

        public SavePostRequest(String content, String sha) {
            this.content = content;
            this.sha = sha;
        }
    }

    private static class DeleteRequest {
        @SerializedName("sha")
        public String sha;

        public DeleteRequest(String sha) {
            this.sha = sha;
        }
    }

    private static class UploadRequest {
        @SerializedName("filename")
        public String filename;
        
        @SerializedName("content")
        public String content;

        public UploadRequest(String filename, String content) {
            this.filename = filename;
            this.content = content;
        }
    }

    private static class UploadResult {
        @SerializedName("url")
        public String url;
    }
}
