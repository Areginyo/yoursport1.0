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

import java.util.ArrayList;
import java.util.List;

import areg.zakaryan.yoursport.model.SearchItem;

public class SearchAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<SearchItem> items = new ArrayList<>();
    private List<SearchItem> selectedItems = new ArrayList<>();
    private OnSelectionChanged listener;

    public interface OnSelectionChanged {
        void onChange(List<SearchItem> selected);
    }

    public SearchAdapter(OnSelectionChanged listener) {
        this.listener = listener;
    }

    public void setItems(List<SearchItem> newItems) {
        this.items = newItems;
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
            return new HeaderVH(v);
        } else {
            View v = inflater.inflate(R.layout.item_search, parent, false);
            return new ItemVH(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        SearchItem item = items.get(position);
        if (holder instanceof HeaderVH) {
            ((HeaderVH) holder).txtHeader.setText(item.title);
        } else {
            ItemVH vh = (ItemVH) holder;
            vh.txtTitle.setText(item.title);
            vh.txtSubtitle.setText(item.subtitle);
            vh.checkbox.setChecked(selectedItems.contains(item));

            if (item.logoUrl != null && !item.logoUrl.isEmpty()) {
                Glide.with(vh.imgLogo.getContext())
                        .load(item.logoUrl)
                        .into(vh.imgLogo);
            }

            vh.itemRoot.setOnClickListener(v -> {
                if (selectedItems.contains(item)) {
                    selectedItems.remove(item);
                } else {
                    selectedItems.add(item);
                }
                vh.checkbox.setChecked(selectedItems.contains(item));
                listener.onChange(selectedItems);
            });
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class HeaderVH extends RecyclerView.ViewHolder {
        TextView txtHeader;
        HeaderVH(View v) {
            super(v);
            txtHeader = v.findViewById(R.id.txtHeader);
        }
    }

    static class ItemVH extends RecyclerView.ViewHolder {
        View      itemRoot;
        ImageView imgLogo;
        TextView  txtTitle, txtSubtitle;
        CheckBox  checkbox;
        ItemVH(View v) {
            super(v);
            itemRoot   = v.findViewById(R.id.itemRoot);
            imgLogo    = v.findViewById(R.id.imgLogo);
            txtTitle   = v.findViewById(R.id.txtTitle);
            txtSubtitle = v.findViewById(R.id.txtSubtitle);
            checkbox   = v.findViewById(R.id.checkbox);
        }
    }
}