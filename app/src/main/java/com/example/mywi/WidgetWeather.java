package com.example.mywi;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.util.TypedValue;
import android.widget.RemoteViews;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import org.json.JSONException;
import org.json.JSONObject;

public class WidgetWeather extends AppWidgetProvider {

    public static String city_name = "Казань";
    private static final String SYNC_CLICKED = "automaticWidgetSyncButtonClick";

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_weather);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    //Используется для обработки нажатия на кнопку обновления
    protected PendingIntent getPendingSelfIntent(Context context, String action) {
        Intent intent = new Intent(context, getClass());
        intent.setAction(action);
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        Log.d("widget_test", "onUpdate");

        RemoteViews remoteViews;
        ComponentName watchWidget;

        remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_weather);
        watchWidget = new ComponentName(context, WidgetWeather.class);

        remoteViews.setOnClickPendingIntent(R.id.update_btn, getPendingSelfIntent(context, SYNC_CLICKED));

        remoteViews.setViewVisibility(R.id.preLoader, View.VISIBLE);
        appWidgetManager.updateAppWidget(watchWidget, remoteViews);

        WeatherData weatherdata = new WeatherData(context);

        for (int appWidgetId : appWidgetIds) {
            weatherdata.setOnDownloadedWeather(new OnDownloadedWeather() {
                @Override
                public void onDownload(String jsonString) throws JSONException {
                    setWeather(jsonString, context, appWidgetManager, appWidgetId);
                }
            });
            weatherdata.getWeather(city_name);
        }
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if (SYNC_CLICKED.equals(intent.getAction())) {
            Log.d("widget_test", "Click");

            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_weather);
            ComponentName watchWidget = new ComponentName(context, WidgetWeather.class);


            remoteViews.setViewVisibility(R.id.preLoader, View.VISIBLE);
            appWidgetManager.updateAppWidget(watchWidget, remoteViews);

            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(watchWidget);
            WeatherData weatherdata = new WeatherData(context);

            weatherdata.setOnDownloadedWeather(new OnDownloadedWeather() {
                @Override
                public void onDownload(String jsonString) throws JSONException {
                    setWeather(jsonString, context.getApplicationContext(), appWidgetManager, appWidgetIds[0]);
                }
            });

            weatherdata.getWeather(city_name);
        }
    }

    @Override
    public void onDisabled(Context context) {
        //вызывается, когда виджет отключается
        //просто заглушка
    }

    public void setWeather(String jsonString, Context context, AppWidgetManager appWidgetManager, int appWidgetId) throws JSONException {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_weather);

        // Отображение прелоадера
        remoteViews.setViewVisibility(R.id.preLoader, View.VISIBLE);
        appWidgetManager.updateAppWidget(appWidgetId, remoteViews);

        Log.d("widget_test", "setWeather");

        JSONObject jsonObject = new JSONObject(jsonString);

        String city = jsonObject.getString("name");
        remoteViews.setTextViewText(R.id.widget_city, city);

        Log.d("widget_test", "city " + city);

        int temp_value = (int) Math.round(jsonObject.getJSONObject("main").getDouble("temp"));
        String temp = (temp_value < 0 ? "-" : "") + temp_value + "°C";
        remoteViews.setTextViewText(R.id.widget_temp, temp);

        String like = jsonObject.getJSONArray("weather").getJSONObject(0).getString("description");
        like = like.substring(0, 1).toUpperCase() + like.substring(1);
        remoteViews.setTextViewText(R.id.widget_like, like);

        String icon = jsonObject.getJSONArray("weather").getJSONObject(0).getString("icon");
        String iconUrl = "https://openweathermap.org/img/wn/" + icon + "@2x.png";


        //appWidgetManager.updateAppWidget(appWidgetId, remoteViews);


        Glide.with(context)
                .asBitmap()
                .load(iconUrl)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {

                        remoteViews.setImageViewBitmap(R.id.image_icon, resource);

                        //скрыть после обновления
                        remoteViews.setViewVisibility(R.id.preLoader, View.GONE);

                        //обновление только после загрузки данных и готовности изображения
                        appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        // Not needed in this case
                    }
                });


        appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
    }

    public static void updateAllWidgets(final Context context,
                                        final int layoutResourceId,
                                        final Class<? extends AppWidgetProvider> appWidgetClass) {
        Log.d("widget_test", "updateAllWidgets");
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), layoutResourceId);

        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        final int[] appWidgetIds = manager.getAppWidgetIds(new ComponentName(context, appWidgetClass));

        for (int i = 0; i < appWidgetIds.length; ++i) {
            manager.updateAppWidget(appWidgetIds[i], remoteViews);
        }
    }
}