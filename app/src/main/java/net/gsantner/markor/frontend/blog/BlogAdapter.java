package net.gsantner.markor.frontend.blog;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.gsantner.markor.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BlogAdapter extends RecyclerView.Adapter<BlogAdapter.ViewHolder> {
    
    private final List<BlogFragment.BlogPost> _posts;
    private final OnPostClickListener _listener;
    private final SimpleDateFormat _dateFormat;

    public interface OnPostClickListener {
        void onPostClick(BlogFragment.BlogPost post);
    }

    public BlogAdapter(List<BlogFragment.BlogPost> posts, OnPostClickListener listener) {
        _posts = posts;
        _listener = listener;
        _dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.blog_post_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BlogFragment.BlogPost post = _posts.get(position);
        
        holder.title.setText(post.title != null && !post.title.isEmpty() ? post.title : post.name);
        
        if (post.date != null && !post.date.isEmpty()) {
            holder.date.setText(post.date);
            holder.date.setVisibility(View.VISIBLE);
        } else {
            holder.date.setVisibility(View.GONE);
        }
        
        holder.itemView.setOnClickListener(v -> _listener.onPostClick(post));
    }

    @Override
    public int getItemCount() {
        return _posts.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        TextView date;

        ViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.post_title);
            date = itemView.findViewById(R.id.post_date);
        }
    }
}
