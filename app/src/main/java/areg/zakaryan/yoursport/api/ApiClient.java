package areg.zakaryan.yoursport.api;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    private static final String BASE_URL = "https://v3.football.api-sports.io/";
    private static final String API_KEY = "f297ffaa96f0baa8d6e2c0c730f43e54";

    private static Retrofit retrofitFootball;

    public static Retrofit getClient() {
        if (retrofitFootball == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(chain -> {
                        Request request = chain.request().newBuilder()
                                .addHeader("x-apisports-key", API_KEY)
                                .addHeader("x-rapidapi-key", API_KEY)
                                .addHeader("x-rapidapi-host", "v3.football.api-sports.io")
                                .build();
                        return chain.proceed(request);
                    })
                    .addInterceptor(logging)
                    .build();

            retrofitFootball = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofitFootball;
    }

    public static ApiService getApiService() {
        return getClient().create(ApiService.class);
    }

    private static Retrofit retrofitTheSportsDB;

    public static TheSportsDBService getTheSportsDBService() {
        if (retrofitTheSportsDB == null) {
            retrofitTheSportsDB = new Retrofit.Builder()
                    .baseUrl("https://www.thesportsdb.com/api/v1/json/3/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofitTheSportsDB.create(TheSportsDBService.class);
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

    private static Retrofit retrofitNba;

    public static NbaApiService getNbaApiService() {
        if (retrofitNba == null) {
            retrofitNba = new Retrofit.Builder()
                    .baseUrl("https://v1.basketball.api-sports.io/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofitNba.create(NbaApiService.class);
    }

    private static Retrofit retrofitF1;

    public static F1ApiService getF1ApiService() {
        if (retrofitF1 == null) {
            retrofitF1 = new Retrofit.Builder()
                    .baseUrl("https://v1.formula-1.api-sports.io/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofitF1.create(F1ApiService.class);
    }
}