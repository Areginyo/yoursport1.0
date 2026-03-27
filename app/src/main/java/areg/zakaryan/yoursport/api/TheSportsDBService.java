package areg.zakaryan.yoursport.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface TheSportsDBService {

    @GET("searchevents.php")
    Call<Object> searchEvents(@Query("e") String eventName);

    @GET("lookuphonours.php")
    Call<Object> getTeamHonours(@Query("id") int id);

    @GET("lookuphonours.php")
    Call<Object> getPlayerHonours(@Query("id") int id);
}