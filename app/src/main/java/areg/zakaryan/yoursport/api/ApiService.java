package areg.zakaryan.yoursport.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface ApiService {

    @GET("leagues")
    Call<Object> getLeagues(@Query("search") String search);

    @GET("teams")
    Call<Object> getTeamsByName(@Query("search") String search);

    @GET("players")
    Call<Object> getPlayersByName(@Query("search") String search,
                                  @Query("season") int season);

    @GET("fixtures")
    Call<Object> getFixtures(@Query("date") String date,
                             @Query("league") int leagueId,
                             @Query("season") int season);
}