package areg.zakaryan.yoursport.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface F1ApiService {

@GET("current.json")
    Call<Object> getCurrentSeasonRaces();

@GET("current/last/results.json")
    Call<Object> getLastRaceResults();

@GET("{season}.json")
    Call<Object> getSeasonRaces(@Path("season") int season);
}