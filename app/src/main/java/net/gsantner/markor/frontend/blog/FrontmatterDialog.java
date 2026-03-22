package net.gsantner.markor.frontend.blog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

import net.gsantner.markor.R;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FrontmatterDialog {
    
    private static final String PREFS_NAME = "blog_admin_prefs";
    private static final String KEY_BASE_URL = "base_url";
    private static final String KEY_TOKEN = "token";
    private static final String DEFAULT_URL = "https://edit.upxuu.com";
    
    private final Context context;
    private final OnFrontmatterSaveListener listener;
    
    // Frontmatter 字段
    private String title;
    private String description;
    private String category;
    private List<String> tags;
    private String image;
    private boolean draft;
    private String sha;

    public interface OnFrontmatterSaveListener {
        void onFrontmatterSaved(String title, String description, String category, 
                               List<String> tags, String image, boolean draft, String sha);
    }

    public FrontmatterDialog(Context context, OnFrontmatterSaveListener listener) {
        this.context = context;
        this.listener = listener;
    }

    /**
     * 从 Markdown 内容中解析 Frontmatter
     */
    public static FrontmatterData parseFrontmatter(String content) {
        FrontmatterData data = new FrontmatterData();
        data.content = content;
        
        Pattern pattern = Pattern.compile("^---\\n([\\s\\S]*?)\\n---\\n", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(content);
        
        if (!matcher.find()) {
            return data;
        }
        
        String frontmatter = matcher.group(1);
        data.frontmatterEnd = matcher.end();
        
        // 解析各个字段
        data.title = extractValue(frontmatter, "^title:\\s*[\"']?(.*)[\"']?$", Pattern.MULTILINE);
        data.description = extractValue(frontmatter, "^description:\\s*[\"']?(.*)[\"']?$", Pattern.MULTILINE);
        data.category = extractValue(frontmatter, "^category:\\s*[\"']?(.*)[\"']?$", Pattern.MULTILINE);
        data.image = extractValue(frontmatter, "^image:\\s*[\"']?(.*)[\"']?$", Pattern.MULTILINE);
        
        // 解析 tags
        String tagsStr = extractValue(frontmatter, "^tags:\\s*\\[(.*)\\]", Pattern.MULTILINE);
        if (tagsStr != null && !tagsStr.isEmpty()) {
            data.tags = Arrays.asList(tagsStr.split("\\s*,\\s*"));
        } else {
            // 尝试 YAML 列表格式
            tagsStr = extractValue(frontmatter, "^tags:\\s*\\n([\\s\\S]*?)(?=^\\w:|$)", Pattern.MULTILINE);
            if (tagsStr != null) {
                String[] tagLines = tagsStr.split("\\n");
                for (String line : tagLines) {
                    line = line.trim();
                    if (line.startsWith("-")) {
                        data.tags.add(line.substring(1).trim());
                    }
                }
            }
        }
        
        // 解析 draft
        String draftStr = extractValue(frontmatter, "^draft:\\s*(true|false)", Pattern.MULTILINE);
        data.draft = "true".equalsIgnoreCase(draftStr);
        
        return data;
    }
    
    /**
     * 构建带 Frontmatter 的 Markdown 内容
     */
    public static String buildFrontmatter(String title, String description, String category,
                                         List<String> tags, String image, boolean draft,
                                         String body) {
        StringBuilder fm = new StringBuilder();
        fm.append("---\n");
        
        if (title != null && !title.isEmpty()) {
            fm.append("title: ").append(title).append("\n");
        }
        if (description != null && !description.isEmpty()) {
            fm.append("description: ").append(description).append("\n");
        }
        if (category != null && !category.isEmpty()) {
            fm.append("category: ").append(category).append("\n");
        }
        if (tags != null && !tags.isEmpty()) {
            fm.append("tags: [");
            for (int i = 0; i < tags.size(); i++) {
                if (i > 0) fm.append(", ");
                fm.append(tags.get(i));
            }
            fm.append("]\n");
        }
        if (image != null && !image.isEmpty()) {
            fm.append("image: ").append(image).append("\n");
        }
        fm.append("draft: ").append(draft ? "true" : "false").append("\n");
        
        fm.append("---\n\n");
        fm.append(body);
        
        return fm.toString();
    }

    /**
     * 显示编辑对话框
     */
    public void show(String content, String sha) {
        FrontmatterData data = parseFrontmatter(content);
        
        this.title = data.title;
        this.description = data.description;
        this.category = data.category;
        this.tags = data.tags;
        this.image = data.image;
        this.draft = data.draft;
        this.sha = sha;
        
        View dialogView = LayoutInflater.from(context).inflate(R.layout.frontmatter_dialog, null);
        
        TextInputEditText editTitle = dialogView.findViewById(R.id.edit_title);
        TextInputEditText editDescription = dialogView.findViewById(R.id.edit_description);
        TextInputEditText editCategory = dialogView.findViewById(R.id.edit_category);
        TextInputEditText editTags = dialogView.findViewById(R.id.edit_tags);
        TextInputEditText editImage = dialogView.findViewById(R.id.edit_image);
        SwitchMaterial switchDraft = dialogView.findViewById(R.id.switch_draft);
        
        // 填充数据
        editTitle.setText(title);
        editDescription.setText(description);
        editCategory.setText(category);
        editTags.setText(tags != null ? String.join(", ", tags) : "");
        editImage.setText(image);
        switchDraft.setChecked(draft);
        
        AlertDialog dialog = new AlertDialog.Builder(context)
            .setTitle("文章属性")
            .setView(dialogView)
            .setCancelable(false)
            .create();
        
        // 保存按钮
        MaterialButton btnSave = dialogView.findViewById(R.id.btn_save);
        btnSave.setOnClickListener(v -> {
            String newTitle = getString(editTitle);
            String newDesc = getString(editDescription);
            String newCat = getString(editCategory);
            String newTags = getString(editTags);
            String newImage = getString(editImage);
            boolean newDraft = switchDraft.isChecked();
            
            if (newTitle == null || newTitle.isEmpty()) {
                Toast.makeText(context, "标题不能为空", Toast.LENGTH_SHORT).show();
                return;
            }
            
            List<String> tagList = newTags != null && !newTags.isEmpty() ?
                Arrays.asList(newTags.split("\\s*,\\s*")) : null;
            
            listener.onFrontmatterSaved(newTitle, newDesc, newCat, tagList, newImage, newDraft, sha);
            dialog.dismiss();
        });
        
        // 取消按钮
        MaterialButton btnCancel = dialogView.findViewById(R.id.btn_cancel);
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }
    
    private String getString(TextInputEditText editText) {
        if (editText.getText() == null) return null;
        return editText.getText().toString().trim();
    }
    
    private static String extractValue(String frontmatter, String regex, int flags) {
        Pattern pattern = Pattern.compile(regex, flags);
        Matcher matcher = pattern.matcher(frontmatter);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }
    
    public static class FrontmatterData {
        public String title = "";
        public String description = "";
        public String category = "";
        public List<String> tags = new java.util.ArrayList<>();
        public String image = "";
        public boolean draft = false;
        public int frontmatterEnd = 0;
        public String content = "";
    }
}
