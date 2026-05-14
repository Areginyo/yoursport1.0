package areg.zakaryan.yoursport;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import areg.zakaryan.yoursport.api.ApiClient;
import areg.zakaryan.yoursport.model.NewsResponse;
import areg.zakaryan.yoursport.model.SearchItem;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TeamNewsFragment extends Fragment {

    private static final String ARG_TEAM_ITEM = "team_item";
    private static final String NEWS_API_KEY = "e2c5ad86dd95403c8a9f7e535d1f3d56";

    private SearchItem teamItem;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView txtNoNews;
    private TeamNewsAdapter adapter;

    public static TeamNewsFragment newInstance(SearchItem teamItem) {
        TeamNewsFragment fragment = new TeamNewsFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_TEAM_ITEM, teamItem);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            teamItem = getArguments().getParcelable(ARG_TEAM_ITEM);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_team_news, container, false);
        initViews(view);
        setupRecyclerView();
        loadTeamNews();
        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerViewNews);
        progressBar = view.findViewById(R.id.progressBar);
        txtNoNews = view.findViewById(R.id.txtNoNews);
    }

    private void setupRecyclerView() {
        adapter = new TeamNewsAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        adapter.setOnNewsClickListener(item -> {
            if (item.url != null && !item.url.isEmpty()) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(item.url)));
            }
        });
    }

    private void loadTeamNews() {
        if (teamItem == null) { showNoNews(); return; }

        progressBar.setVisibility(View.VISIBLE);
        txtNoNews.setVisibility(View.GONE);

        String query = "\"" + teamItem.title + "\" football";
        ApiClient.getNewsApiService().getEverything(query, "en", "publishedAt", NEWS_API_KEY)
                .enqueue(new Callback<NewsResponse>() {
                    @Override
                    public void onResponse(Call<NewsResponse> call, Response<NewsResponse> response) {
                        if (!isAdded()) return;
                        progressBar.setVisibility(View.GONE);

                        List<TeamNewsItem> newsItems = new ArrayList<>();
                        if (response.isSuccessful() && response.body() != null
                                && response.body().articles != null) {
                            for (NewsResponse.Article a : response.body().articles) {
                                if (a == null || a.title == null || a.title.trim().isEmpty()) continue;
                                newsItems.add(new TeamNewsItem(
                                        a.title,
                                        a.description != null ? a.description : "",
                                        a.urlToImage,
                                        a.publishedAt != null ? a.publishedAt : "",
                                        a.source != null ? a.source.name : "",
                                        a.url
                                ));
                                if (newsItems.size() >= 20) break;
                            }
                        }

                        if (!newsItems.isEmpty()) {
                            adapter.setNews(newsItems);
                        } else {
                            showNoNews();
                        }
                    }

                    @Override
                    public void onFailure(Call<NewsResponse> call, Throwable t) {
                        if (!isAdded()) return;
                        progressBar.setVisibility(View.GONE);
                        showNoNews();
                    }
                });
    }

    private void showNoNews() {
        txtNoNews.setVisibility(View.VISIBLE);
        txtNoNews.setText("No news available for " + (teamItem != null ? teamItem.title : "this team"));
    }

    // News item data class
    public static class TeamNewsItem {
        public String title;
        public String description;
        public String imageUrl;
        public String publishedAt;
        public String source;
        public String url;

        public TeamNewsItem(String title, String description, String imageUrl,
                           String publishedAt, String source, String url) {
            this.title = title;
            this.description = description;
            this.imageUrl = imageUrl;
            this.publishedAt = publishedAt;
            this.source = source;
            this.url = url;
        }
    }
}
