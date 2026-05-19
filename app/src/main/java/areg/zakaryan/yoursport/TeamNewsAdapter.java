package areg.zakaryan.yoursport;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TeamNewsAdapter extends RecyclerView.Adapter<TeamNewsAdapter.NewsViewHolder> {

    private List<TeamNewsFragment.TeamNewsItem> newsItems = new ArrayList<>();
    private OnNewsClickListener listener;

    public interface OnNewsClickListener {
        void onNewsClick(TeamNewsFragment.TeamNewsItem item);
    }

    public void setOnNewsClickListener(OnNewsClickListener listener) {
        this.listener = listener;
    }

    public void setNews(List<TeamNewsFragment.TeamNewsItem> newsItems) {
        this.newsItems = newsItems != null ? newsItems : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NewsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_team_news, parent, false);
        return new NewsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NewsViewHolder holder, int position) {
        TeamNewsFragment.TeamNewsItem item = newsItems.get(position);
        
        holder.txtTitle.setText(item.title);
        holder.txtDescription.setText(item.description);
        holder.txtSource.setText(item.source);

if (item.publishedAt != null && !item.publishedAt.isEmpty()) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                Date date = inputFormat.parse(item.publishedAt);
                holder.txtDate.setText(outputFormat.format(date));
            } catch (Exception e) {
                holder.txtDate.setText(item.publishedAt);
            }
        } else {
            holder.txtDate.setText("");
        }

if (item.imageUrl != null && !item.imageUrl.isEmpty()) {
            holder.imgNews.setVisibility(View.VISIBLE);
            Glide.with(holder.itemView.getContext())
                    .load(item.imageUrl)
                    .placeholder(R.drawable.ic_placeholder)
                    .error(R.drawable.ic_placeholder)
                    .into(holder.imgNews);
        } else {
            holder.imgNews.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onNewsClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return newsItems.size();
    }

    static class NewsViewHolder extends RecyclerView.ViewHolder {
        ImageView imgNews;
        TextView txtTitle;
        TextView txtDescription;
        TextView txtSource;
        TextView txtDate;

        NewsViewHolder(@NonNull View itemView) {
            super(itemView);
            imgNews = itemView.findViewById(R.id.imgNews);
            txtTitle = itemView.findViewById(R.id.txtTitle);
            txtDescription = itemView.findViewById(R.id.txtDescription);
            txtSource = itemView.findViewById(R.id.txtSource);
            txtDate = itemView.findViewById(R.id.txtDate);
        }
    }
}
