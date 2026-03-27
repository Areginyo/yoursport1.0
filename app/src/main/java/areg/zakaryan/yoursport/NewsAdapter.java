package areg.zakaryan.yoursport;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import areg.zakaryan.yoursport.model.NewsItem;

public class NewsAdapter extends ListAdapter<NewsItem, NewsAdapter.NewsViewHolder> {

    public NewsAdapter() {
        super(DIFF_CALLBACK);
    }

    @NonNull
    @Override
    public NewsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_news, parent, false);
        return new NewsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NewsViewHolder holder, int position) {
        NewsItem item = getItem(position);
        holder.bind(item);
    }

    static class NewsViewHolder extends RecyclerView.ViewHolder {

        private final ImageView ivImage;
        private final TextView tvTitle;
        private final TextView tvDescription;
        private final TextView tvDate;
        private final TextView tvSource;

        public NewsViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.iv_news_image);
            tvTitle = itemView.findViewById(R.id.tv_news_title);
            tvDescription = itemView.findViewById(R.id.tv_news_description);
            tvDate = itemView.findViewById(R.id.tv_news_date);
            tvSource = itemView.findViewById(R.id.tv_news_source);
        }

        public void bind(NewsItem item) {
            tvTitle.setText(item.title != null ? item.title : "No title");
            tvDescription.setText(item.description != null ? item.description : "No description");
            tvDate.setText(item.date != null ? item.date : "");
            tvSource.setText(item.source != null ? item.source : "");

            if (item.imageUrl != null && !item.imageUrl.isEmpty()) {
                Glide.with(ivImage.getContext())
                        .load(item.imageUrl)
                        .placeholder(R.drawable.ic_placeholder)
                        .error(R.drawable.ic_error)
                        .centerCrop()
                        .into(ivImage);
            } else {
                ivImage.setImageResource(R.drawable.ic_placeholder);
            }
        }
    }

    private static final DiffUtil.ItemCallback<NewsItem> DIFF_CALLBACK = new DiffUtil.ItemCallback<NewsItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull NewsItem oldItem, @NonNull NewsItem newItem) {
            return oldItem.id.equals(newItem.id);
        }

        @Override
        public boolean areContentsTheSame(@NonNull NewsItem oldItem, @NonNull NewsItem newItem) {
            return oldItem.equals(newItem);
        }
    };
}