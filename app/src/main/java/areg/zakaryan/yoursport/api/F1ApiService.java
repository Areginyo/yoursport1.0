package areg.zakaryan.yoursport.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface F1ApiService {

    // Расписание текущего сезона (все гонки)
    // Пример: https://ergast.com/api/f1/current.json
    @GET("current.json")
    Call<Object> getCurrentSeasonRaces();

    // Результаты последней гонки
    @GET("current/last/results.json")
    Call<Object> getLastRaceResults();

    // Расписание конкретного года
    @GET("{season}.json")
    Call<Object> getSeasonRaces(@Path("season") int season);
}