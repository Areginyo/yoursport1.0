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

import areg.zakaryan.yoursport.model.SearchItem;
import areg.zakaryan.yoursport.model.NewsItem;

// Предполагаем, что у тебя есть класс NewsItem и адаптер
// public class NewsItem { String title, description, url, imageUrl, source; ... }

public class NewsFragment extends Fragment {

    private static final String ARG_SELECTED_ITEMS = "selected_items";

    private RecyclerView rvNews;
    private TextView tvEmptyOrLoading;

    private List<NewsItem> displayedNews = new ArrayList<>();
    // private NewsAdapter adapter;  // ← твой адаптер

    public static NewsFragment newInstance(ArrayList<SearchItem> selectedItems) {
        NewsFragment fragment = new NewsFragment();
        Bundle args = new Bundle();
        args.putParcelableArrayList(ARG_SELECTED_ITEMS, selectedItems);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_news, container, false);

        rvNews = view.findViewById(R.id.rv_news);
        tvEmptyOrLoading = view.findViewById(R.id.tv_empty);

        rvNews.setLayoutManager(new LinearLayoutManager(requireContext()));
        // rvNews.setAdapter(adapter = new NewsAdapter());

        loadNews();

        return view;
    }

    private void loadNews() {
        ArrayList<SearchItem> favorites = getArguments() != null ?
                getArguments().getParcelableArrayList(ARG_SELECTED_ITEMS) : new ArrayList<>();

        tvEmptyOrLoading.setText("Загрузка новостей...");
        tvEmptyOrLoading.setVisibility(View.VISIBLE);

        // 1. Здесь должен быть реальный fetch (Retrofit / Volley)
        List<NewsItem> allNews = fetchAllNews(); // ← заглушка или API

        // 2. Фильтрация + приоритет
        List<NewsItem> priority = new ArrayList<>();
        List<NewsItem> fallback = new ArrayList<>();

        for (NewsItem news : allNews) {
            boolean isRelevant = false;
            String text = (news.title + " " + news.description).toLowerCase();

            for (SearchItem item : favorites) {
                String key = item.title.toLowerCase();
                if (text.contains(key)) {
                    isRelevant = true;
                    break;
                }
            }

            if (isRelevant) {
                priority.add(news);
            } else {
                fallback.add(news);
            }
        }

        displayedNews.clear();
        displayedNews.addAll(priority);
        displayedNews.addAll(fallback);

        // adapter.submitList(displayedNews);  // или notifyDataSetChanged()

        if (displayedNews.isEmpty()) {
            tvEmptyOrLoading.setText("No news yet");
        } else {
            tvEmptyOrLoading.setVisibility(View.GONE);
        }
    }

    // Заглушка — замени на реальный запрос
    private List<NewsItem> fetchAllNews() {
        // В реальности: Retrofit -> TheSportsDB / RSS / ESPN API
        return new ArrayList<>(); // пусто для примера
    }
}