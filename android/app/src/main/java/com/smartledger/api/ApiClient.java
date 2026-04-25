package com.smartledger.api;

import com.smartledger.BuildConfig;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    private static SmartLedgerApi api;

    private ApiClient() {
    }

    public static SmartLedgerApi getApi(SessionManager sessionManager) {
        if (api == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(BuildConfig.DEBUG
                    ? HttpLoggingInterceptor.Level.BASIC
                    : HttpLoggingInterceptor.Level.NONE);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(chain -> {
                        okhttp3.Request original = chain.request();
                        String token = sessionManager.getToken();
                        okhttp3.Request.Builder builder = original.newBuilder()
                                .header("apikey", BuildConfig.SUPABASE_ANON_KEY);
                        if (token == null) {
                            return chain.proceed(builder.build());
                        }

                        okhttp3.Request authenticated = builder
                                .header("Authorization", "Bearer " + token)
                                .build();
                        return chain.proceed(authenticated);
                    })
                    .addInterceptor(logging)
                    .build();

            api = new Retrofit.Builder()
                    .baseUrl(BuildConfig.SUPABASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(SmartLedgerApi.class);
        }
        return api;
    }
}
