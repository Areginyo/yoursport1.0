package areg.zakaryan.yoursport;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import areg.zakaryan.yoursport.api.ApiClient;
import areg.zakaryan.yoursport.model.NewsItem;
import areg.zakaryan.yoursport.model.NewsResponse;
import areg.zakaryan.yoursport.model.SearchItem;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NewsFragment extends Fragment {

    private static final String ARG_SELECTED_ITEMS = "selected_items";
    private static final String ARG_CURRENT_SPORT = "current_sport";

    private ArrayList<SearchItem> selectedItems;
    private String currentSport;

    private RecyclerView rvNews;
    private TextView tvEmpty;
    private LinearLayout btnFavoritesToggle;
    private TextView tvFavoritesLabel;
    private ImageView ivStar;

    private NewsAdapter newsAdapter;
    private boolean isFavoritesMode = false;

    public static NewsFragment newInstance(ArrayList<SearchItem> selectedItems, String currentSport) {
        NewsFragment fragment = new NewsFragment();
        Bundle args = new Bundle();
        args.putParcelableArrayList(ARG_SELECTED_ITEMS, selectedItems);
        args.putString(ARG_CURRENT_SPORT, currentSport);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            selectedItems = getArguments().getParcelableArrayList(ARG_SELECTED_ITEMS);
            currentSport = getArguments().getString(ARG_CURRENT_SPORT);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_news, container, false);

        rvNews = view.findViewById(R.id.rv_news);
        tvEmpty = view.findViewById(R.id.tv_empty);
        btnFavoritesToggle = view.findViewById(R.id.btn_favorites_toggle);
        tvFavoritesLabel = view.findViewById(R.id.tv_favorites_label);
        ivStar = view.findViewById(R.id.iv_star);

        rvNews.setLayoutManager(new LinearLayoutManager(requireContext()));

        newsAdapter = new NewsAdapter();
        rvNews.setAdapter(newsAdapter);

        updateFavoritesToggleUI();

        btnFavoritesToggle.setOnClickListener(v -> {
            isFavoritesMode = !isFavoritesMode;
            updateFavoritesToggleUI();
            loadNews();
        });

        loadNews();

        return view;
    }

    private void updateFavoritesToggleUI() {
        btnFavoritesToggle.setSelected(isFavoritesMode);
        if (isFavoritesMode) {
            tvFavoritesLabel.setTextColor(0xFF181818);
            ivStar.setColorFilter(0xFF181818);
        } else {
            tvFavoritesLabel.setTextColor(0xFFFFFFFF);
            ivStar.setColorFilter(0xFFAAFF00);
        }
    }

    private void loadNews() {
        tvEmpty.setText("Loading " + currentSport + " news...");
        tvEmpty.setVisibility(View.VISIBLE);

        String query;
        if (isFavoritesMode && selectedItems != null && !selectedItems.isEmpty()) {
            query = buildFavoritesQuery();
        } else {
            query = getQueryForSport(currentSport);
        }

        if (query == null || query.trim().isEmpty()) {
            tvEmpty.setText("No favorites selected");
            newsAdapter.submitList(new ArrayList<>());
            return;
        }

        String apiKey = "e2c5ad86dd95403c8a9f7e535d1f3d56";

        ApiClient.getNewsApiService().getEverything(query, "en", "publishedAt", apiKey)
                .enqueue(new Callback<NewsResponse>() {
                    @Override
                    public void onResponse(Call<NewsResponse> call, Response<NewsResponse> response) {
                        if (!isAdded()) return;
                        if (response.isSuccessful() && response.body() != null) {
                            List<NewsItem> newsList = convertToNewsItems(response.body());

if (isFavoritesMode) {
                                newsList = filterByFavorites(newsList);
                            }

                            newsAdapter.submitList(newsList);

                            if (newsList.isEmpty()) {
                                tvEmpty.setText(isFavoritesMode
                                        ? "No news found for your favorites"
                                        : "No news found for " + currentSport);
                            } else {
                                tvEmpty.setVisibility(View.GONE);
                            }
                        } else {
                            tvEmpty.setText("No news found");
                        }
                    }

                    @Override
                    public void onFailure(Call<NewsResponse> call, Throwable t) {
                        if (!isAdded()) return;
                        tvEmpty.setText("Error loading news");
                    }
                });
    }

    private String buildFavoritesQuery() {
        if (selectedItems == null || selectedItems.isEmpty()) return null;

        List<String> names = new ArrayList<>();
        for (SearchItem item : selectedItems) {
            if (item.type == SearchItem.TYPE_ITEM && item.title != null && !item.title.trim().isEmpty()) {
                names.add("\"" + item.title.trim() + "\"");
            }
        }

        if (names.isEmpty()) return null;

int maxTerms = Math.min(names.size(), 10);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < maxTerms; i++) {
            if (i > 0) sb.append(" OR ");
            sb.append(names.get(i));
        }
        return sb.toString();
    }

    private List<NewsItem> filterByFavorites(List<NewsItem> items) {
        if (selectedItems == null || selectedItems.isEmpty()) return items;

Set<String> favoriteNames = new HashSet<>();
        for (SearchItem item : selectedItems) {
            if (item.type == SearchItem.TYPE_ITEM && item.title != null) {
                favoriteNames.add(item.title.trim().toLowerCase(Locale.ENGLISH));
            }
        }

        List<NewsItem> filtered = new ArrayList<>();
        for (NewsItem news : items) {
            String text = ((news.title != null ? news.title : "") + " "
                    + (news.description != null ? news.description : "")).toLowerCase(Locale.ENGLISH);

            for (String name : favoriteNames) {
                if (text.contains(name)) {
                    filtered.add(news);
                    break;
                }
            }
        }
        return filtered;
    }

    private String getQueryForSport(String sport) {
        if (sport == null) return "sports";
        switch (sport) {
            case "Football": return "(FIFA)";
            default:         return sport;
        }
    }

    private List<NewsItem> convertToNewsItems(NewsResponse response) {
        List<NewsItem> list = new ArrayList<>();
        if (response.articles == null) return list;

        Set<String> seen = new HashSet<>();

        for (NewsResponse.Article article : response.articles) {
            if (article == null) continue;
            if (!isFavoritesMode && !isRelevantForCurrentSport(article)) continue;

            NewsItem item = new NewsItem();
            item.id = article.url != null ? article.url : "";
            if (item.id.isEmpty()) item.id = (article.title != null ? article.title : "") + "_" + (article.publishedAt != null ? article.publishedAt : "");
            if (seen.contains(item.id)) continue;
            seen.add(item.id);

            item.title = article.title != null ? article.title : "No title";
            item.description = article.description != null ? article.description : "";
            item.date = article.publishedAt;
            item.source = article.source != null ? article.source.name : "News";
            item.imageUrl = article.urlToImage;
            item.url = article.url;
            list.add(item);
        }
        return list;
    }

    private boolean isRelevantForCurrentSport(NewsResponse.Article article) {
        String text = ((article.title != null ? article.title : "") + " "
                + (article.description != null ? article.description : "") + " "
                + (article.content != null ? article.content : "")).toLowerCase(Locale.ENGLISH);

        if (currentSport == null) return true;
        switch (currentSport) {
            case "Football": return containsAny(text, "fifa");
            default:         return true;
        }
    }

    private boolean containsAny(String text, String... keys) {
        for (String key : keys) if (text.contains(key)) return true;
        return false;
    }
}