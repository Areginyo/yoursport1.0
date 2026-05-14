package areg.zakaryan.yoursport.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface ApiService {

    @GET("fixtures")
    Call<Object> getAllMatchesByDate(@Query("date") String date);

    @GET("fixtures")
    Call<Object> getMatchById(@Query("id") String fixtureId);

    @GET("fixtures/headtohead")
    Call<Object> getHeadToHead(@Query("h2h") String h2h, @Query("last") int last);

    @GET("fixtures/lineups")
    Call<Object> getLineups(@Query("fixture") String fixtureId);

    @GET("fixtures/statistics")
    Call<Object> getFixtureStatistics(@Query("fixture") String fixtureId);

    @GET("fixtures/events")
    Call<Object> getFixtureEvents(@Query("fixture") String fixtureId);

    @GET("fixtures")
    Call<Object> getLastFixturesByTeam(@Query("team") int teamId, @Query("last") int last);

    @GET("leagues")
    Call<Object> getCompetitions();

    @GET("teams")
    Call<Object> getTeamsByLeague(@Query("league") int leagueId, @Query("season") int season);

    @GET("teams")
    Call<Object> searchTeamsByName(@Query("search") String name);

    @GET("teams")
    Call<Object> getTeamInformation(@Query("id") int teamId);

    @GET("fixtures")
    Call<Object> getTeamMatches(@Query("team") int teamId, @Query("season") int season);

    @GET("standings")
    Call<Object> getStandings(@Query("league") int leagueId, @Query("season") int season);

    @GET("teams/statistics")
    Call<Object> getTeamStatistics(@Query("league") int leagueId, @Query("season") int season, @Query("team") int teamId);

    @GET("transfers")
    Call<Object> getTransfers(@Query("league") int leagueId, @Query("season") int season, @Query("team") int teamId);

    @GET("players/squads")
    Call<Object> getSquad(@Query("team") int teamId);

    @GET("transfers")
    Call<Object> getTransfersByTeam(@Query("team") int teamId);

    @GET("coachs")
    Call<Object> getCoach(@Query("team") int teamId);

    @GET("leagues")
    Call<Object> getLeaguesByTeam(@Query("team") int teamId, @Query("season") int season);

    // ── League Detail endpoints ──────────────────────────────────────────
    @GET("fixtures/rounds")
    Call<Object> getRounds(@Query("league") int leagueId, @Query("season") int season);

    @GET("fixtures")
    Call<Object> getFixturesByRound(@Query("league") int leagueId, @Query("season") int season, @Query("round") String round);

    @GET("players/topscorers")
    Call<Object> getTopScorers(@Query("league") int leagueId, @Query("season") int season);

    @GET("players/topassists")
    Call<Object> getTopAssists(@Query("league") int leagueId, @Query("season") int season);

    @GET("players/topyellowcards")
    Call<Object> getTopYellowCards(@Query("league") int leagueId, @Query("season") int season);

    @GET("players/topredcards")
    Call<Object> getTopRedCards(@Query("league") int leagueId, @Query("season") int season);

    // ── Player Detail endpoints ──────────────────────────────────────────
    @GET("players")
    Call<Object> getPlayerProfile(@Query("id") int playerId, @Query("season") int season);

    @GET("players/profiles")
    Call<Object> getPlayerProfileBasic(@Query("player") int playerId);

    @GET("players/squads")
    Call<Object> getPlayerSquad(@Query("player") int playerId);

    @GET("transfers")
    Call<Object> getPlayerTransfers(@Query("player") int playerId);
}