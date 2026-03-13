package areg.zakaryan.yoursport.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface ApiServiceSports {

    @GET("searchplayers.php")
    Call<Object> searchPlayers(@Query("p") String playerName);

    @GET("all_leagues.php")
    Call<Object> getAllLeagues(@Query("s") String sport);

    @GET("searchteams.php")
    Call<Object> searchTeams(@Query("l") String leagueName);

    @GET("search_all_teams.php")
    Call<Object> getAllTeams(@Query("l") String leagueName);

    @GET("all_leagues.php")
    Call<Object> getAllLeagues();

    @GET("lookupleague.php")
    Call<Object> getLeague(@Query("id") String leagueId);

}