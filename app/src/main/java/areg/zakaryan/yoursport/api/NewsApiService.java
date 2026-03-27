package areg.zakaryan.yoursport.api;

import areg.zakaryan.yoursport.model.NewsResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface NewsApiService {

    @GET("everything")
    Call<NewsResponse> getEverything(
            @Query("q") String query,
            @Query("language") String language,
            @Query("sortBy") String sortBy,
            @Query("apiKey") String apiKey
    );
}