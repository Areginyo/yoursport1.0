package areg.zakaryan.yoursport.api;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    private static final String BASE_URL = "https://api.football-data.org/v4/";
    private static final String API_KEY  = "e9992eda34d4471b954b26f991aed4af";

    private static Retrofit retrofit = null;

    public static Retrofit getClient() {
        if (retrofit == null) {

            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(chain -> {
                        Request request = chain.request().newBuilder()
                                .addHeader("X-Auth-Token", API_KEY)
                                .build();
                        return chain.proceed(request);
                    })
                    .addInterceptor(logging)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    private static Retrofit retrofitTheSportsDB;

    private static final String THESPORTSDB_BASE_URL = "https://www.thesportsdb.com/api/v1/json/3/";

    public static TheSportsDBService getTheSportsDBService() {
        if (retrofitTheSportsDB == null) {
            retrofitTheSportsDB = new Retrofit.Builder()
                    .baseUrl(THESPORTSDB_BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofitTheSportsDB.create(TheSportsDBService.class);
    }

    public static ApiService getApiService() {
        return getClient().create(ApiService.class);
    }

    private static Retrofit retrofitNews;

    public static NewsApiService getNewsApiService() {
        if (retrofitNews == null) {
            retrofitNews = new Retrofit.Builder()
                    .baseUrl("https://newsapi.org/v2/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofitNews.create(NewsApiService.class);
    }
}