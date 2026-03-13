package areg.zakaryan.yoursport.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    // Все лиги
    @GET("competitions")
    Call<Object> getCompetitions();

    // Команды по лиге (по коду лиги, например "PL", "BL1")
    @GET("competitions/{code}/teams")
    Call<Object> getTeamsByLeague(@Path("code") String leagueCode);

    // Матчи (если понадобится позже)
    @GET("competitions/{code}/matches")
    Call<Object> getMatches(
            @Path("code") String leagueCode,
            @Query("status") String status
    );
}