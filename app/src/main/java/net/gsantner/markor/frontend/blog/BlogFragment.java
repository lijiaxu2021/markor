package net.gsantner.markor.frontend.blog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import net.gsantner.markor.R;
import net.gsantner.markor.activity.DocumentActivity;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class BlogFragment extends Fragment {
    private static final String PREFS_NAME = "blog_admin_prefs";
    private static final String KEY_BASE_URL = "base_url";
    private static final String KEY_TOKEN = "token";
    private static final String DEFAULT_URL = "https://edit.upxuu.com";

    private SwipeRefreshLayout _swipeRefreshLayout;
    private RecyclerView _recyclerView;
    private BlogAdapter _adapter;
    private WorkerApi _api;
    private SharedPreferences _prefs;
    private Gson _gson;

    private List<BlogPost> _posts = new ArrayList<>();
    private boolean _isLoading = false;

    public static BlogFragment newInstance() {
        return new BlogFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        _gson = new Gson();
        
        String baseUrl = _prefs.getString(KEY_BASE_URL, DEFAULT_URL);
        String token = _prefs.getString(KEY_TOKEN, "");
        _api = new WorkerApi(baseUrl, token);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.blog_fragment, container, false);
        
        _swipeRefreshLayout = root.findViewById(R.id.swipe_refresh);
        _recyclerView = root.findViewById(R.id.recycler_view);
        FloatingActionButton fab = root.findViewById(R.id.fab);
        
        _recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        _adapter = new BlogAdapter(_posts, this::onPostClick);
        _recyclerView.setAdapter(_adapter);
        
        _swipeRefreshLayout.setOnRefreshListener(this::loadPosts);
        _swipeRefreshLayout.setColorSchemeResources(R.color.primary);
        
        fab.setOnClickListener(v -> {
            // 新建文章
            DocumentActivity.launch(requireContext(), null, true, null);
        });
        
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        String token = _prefs.getString(KEY_TOKEN, "");
        if (token.isEmpty()) {
            showLoginDialog();
        } else if (_posts.isEmpty()) {
            loadPosts();
        }
    }

    private void loadPosts() {
        if (_isLoading) return;
        _isLoading = true;
        _swipeRefreshLayout.setRefreshing(true);
        
        new Thread(() -> {
            try {
                List<BlogPost> posts = _api.listPosts();
                requireActivity().runOnUiThread(() -> {
                    _posts.clear();
                    _posts.addAll(posts);
                    _adapter.notifyDataSetChanged();
                    _swipeRefreshLayout.setRefreshing(false);
                    _isLoading = false;
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "加载失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    _swipeRefreshLayout.setRefreshing(false);
                    _isLoading = false;
                });
            }
        }).start();
    }

    private void onPostClick(BlogPost post) {
        // 打开文章编辑
        File file = new File(post.path);
        DocumentActivity.launch(requireContext(), file, true, post.sha);
    }

    private void showLoginDialog() {
        String savedUrl = _prefs.getString(KEY_BASE_URL, DEFAULT_URL);
        String savedToken = _prefs.getString(KEY_TOKEN, "");
        
        EditText urlInput = new EditText(requireContext());
        urlInput.setText(savedUrl);
        urlInput.setHint("API 地址");
        
        EditText tokenInput = new EditText(requireContext());
        tokenInput.setText(savedToken);
        tokenInput.setHint("访问令牌");
        tokenInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        
        new AlertDialog.Builder(requireContext())
            .setTitle("博客设置")
            .setMessage("请输入 Worker API 地址和访问令牌")
            .setView(urlInput)
            .setPositiveButton("保存", (dialog, which) -> {
                String url = urlInput.getText().toString().trim();
                String token = tokenInput.getText().toString().trim();
                
                if (url.isEmpty() || token.isEmpty()) {
                    Toast.makeText(requireContext(), "请填写完整信息", Toast.LENGTH_SHORT).show();
                    showLoginDialog();
                    return;
                }
                
                _prefs.edit()
                    .putString(KEY_BASE_URL, url)
                    .putString(KEY_TOKEN, token)
                    .apply();
                
                _api = new WorkerApi(url, token);
                loadPosts();
            })
            .setNegativeButton("取消", (dialog, which) -> {
                if (_posts.isEmpty()) {
                    showLoginDialog();
                }
            })
            .setCancelable(false)
            .show();
    }

    // Worker API 类
    public static class WorkerApi {
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

        public List<BlogPost> listPosts() throws IOException {
            Request request = new Request.Builder()
                    .url(baseUrl + "/api/posts")
                    .addHeader("Authorization", "Bearer " + token)
                    .get()
                    .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("HTTP " + response.code());
                }
                String body = response.body().string();
                BlogPost[] posts = gson.fromJson(body, BlogPost[].class);
                List<BlogPost> result = new ArrayList<>();
                for (BlogPost post : posts) {
                    if (post != null) result.add(post);
                }
                return result;
            }
        }
    }

    public static class BlogPost {
        @SerializedName("name")
        public String name;
        
        @SerializedName("path")
        public String path;
        
        @SerializedName("sha")
        public String sha;
        
        @SerializedName("title")
        public String title;
        
        @SerializedName("date")
        public String date;
        
        @SerializedName("type")
        public String type;
    }
}
