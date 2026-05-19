package areg.zakaryan.yoursport.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface NbaApiService {

@GET("games")
    Call<Object> getGamesByDate(
            @Query("dates[]") String date,
            @Query("per_page") int perPage
    );
}