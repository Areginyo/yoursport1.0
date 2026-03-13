package areg.zakaryan.yoursport;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import areg.zakaryan.yoursport.model.SearchItem;

public class SearchAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<SearchItem> items = List.of();
    private final List<SearchItem> selectedItems; // ссылка на список из Activity
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClicked(SearchItem item);
    }

    public SearchAdapter(List<SearchItem> selectedItems, OnItemClickListener listener) {
        this.selectedItems = selectedItems;
        this.listener = listener;
    }

    public void setItems(List<SearchItem> newItems) {
        this.items = newItems != null ? new java.util.ArrayList<>(newItems) : new java.util.ArrayList<>();
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == SearchItem.TYPE_HEADER) {
            View v = inflater.inflate(R.layout.item_search_header, parent, false);
            return new HeaderViewHolder(v);
        } else {
            View v = inflater.inflate(R.layout.item_search, parent, false);
            return new ItemViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        SearchItem item = items.get(position);

        if (holder instanceof HeaderViewHolder vh) {
            vh.txtHeader.setText(item.title);
        } else if (holder instanceof ItemViewHolder vh) {
            vh.txtTitle.setText(item.title);
            vh.txtSubtitle.setText(item.subtitle != null ? item.subtitle : "");

            boolean isSelected = selectedItems.contains(item);
            vh.checkbox.setChecked(isSelected);

            if (item.logoUrl != null && !item.logoUrl.isEmpty()) {
                Glide.with(vh.imgLogo)
                        .load(item.logoUrl)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_menu_close_clear_cancel)
                        .into(vh.imgLogo);
            } else {
                vh.imgLogo.setImageResource(android.R.drawable.ic_menu_gallery);
            }

            vh.itemRoot.setOnClickListener(v -> {
                listener.onItemClicked(item);
                notifyItemChanged(position);
            });
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView txtHeader;

        HeaderViewHolder(View itemView) {
            super(itemView);
            txtHeader = itemView.findViewById(R.id.txtHeader);
        }
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        View itemRoot;
        ImageView imgLogo;
        TextView txtTitle, txtSubtitle;
        CheckBox checkbox;

        ItemViewHolder(View itemView) {
            super(itemView);
            itemRoot    = itemView.findViewById(R.id.itemRoot);
            imgLogo     = itemView.findViewById(R.id.imgLogo);
            txtTitle    = itemView.findViewById(R.id.txtTitle);
            txtSubtitle = itemView.findViewById(R.id.txtSubtitle);
            checkbox    = itemView.findViewById(R.id.checkbox);
        }
    }
}