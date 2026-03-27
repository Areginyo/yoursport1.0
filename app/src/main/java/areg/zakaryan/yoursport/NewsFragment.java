package areg.zakaryan.yoursport;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

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

    private NewsAdapter newsAdapter;

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

        rvNews.setLayoutManager(new LinearLayoutManager(requireContext()));

        newsAdapter = new NewsAdapter();
        rvNews.setAdapter(newsAdapter);

        loadPersonalizedNews();

        return view;
    }

    private void loadPersonalizedNews() {
        tvEmpty.setText("Loading " + currentSport + " news...");
        tvEmpty.setVisibility(View.VISIBLE);

        String query = getBaseSportQuery(currentSport);

        // Добавляем только выбранные тобой команды, игроков и лиги
        if (selectedItems != null && !selectedItems.isEmpty()) {
            for (SearchItem item : selectedItems) {
                if (item.title != null && !item.title.isEmpty()) {
                    if (query.length() > 0) query += " OR ";
                    query += "\"" + item.title + "\"";
                }
            }
        }

        String apiKey = "e2c5ad86dd95403c8a9f7e535d1f3d56";

        ApiClient.getNewsApiService().getEverything(query, "en", "publishedAt", apiKey)
                .enqueue(new Callback<NewsResponse>() {
                    @Override
                    public void onResponse(Call<NewsResponse> call, Response<NewsResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<NewsItem> newsList = convertToNewsItems(response.body());
                            newsAdapter.submitList(newsList);

                            if (newsList.isEmpty()) {
                                tvEmpty.setText("No news found for your selection");
                            } else {
                                tvEmpty.setVisibility(View.GONE);
                            }
                        } else {
                            tvEmpty.setText("No news found");
                        }
                    }

                    @Override
                    public void onFailure(Call<NewsResponse> call, Throwable t) {
                        tvEmpty.setText("Error loading news");
                    }
                });
    }

    // Правильный базовый запрос для каждого спорта
    private String getBaseSportQuery(String sport) {
        switch (sport) {
            case "Football":
                return "soccer";
            case "UFC":
                return "UFC";
            case "Formula 1":
                return "\"Formula 1\"";
            case "Basketball":
                return "NBA";
            case "Tennis":
                return "tennis";
            default:
                return sport;
        }
    }

    private List<NewsItem> convertToNewsItems(NewsResponse response) {
        List<NewsItem> list = new ArrayList<>();

        for (NewsResponse.Article article : response.articles) {
            NewsItem item = new NewsItem();
            item.id = article.url != null ? article.url : "";
            item.title = article.title;
            item.description = article.description;
            item.date = article.publishedAt;
            item.source = article.source != null ? article.source.name : "News";
            item.imageUrl = article.urlToImage;
            item.url = article.url;
            list.add(item);
        }
        return list;
    }
}