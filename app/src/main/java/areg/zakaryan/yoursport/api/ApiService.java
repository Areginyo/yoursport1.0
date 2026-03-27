package areg.zakaryan.yoursport.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    // Get all competitions/leagues
    @GET("competitions")
    Call<Object> getCompetitions();

    // Get teams by league code (e.g., "PL" for Premier League)
    @GET("competitions/{code}/teams")
    Call<Object> getTeamsByLeague(@Path("code") String leagueCode);

    // Optional: Get matches by league code
    @GET("competitions/{code}/matches")
    Call<Object> getMatches(
            @Path("code") String leagueCode,
            @Query("status") String status
    );
}