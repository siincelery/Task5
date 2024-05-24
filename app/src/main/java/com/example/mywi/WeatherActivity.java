package com.example.mywi;

import android.Manifest;
import android.annotation.SuppressLint;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import com.bumptech.glide.Glide;

public class WeatherActivity extends AppCompatActivity {


    private EditText ed_city;
    private Button btn_def_location;
    private Button btn_find;
    private Button btn_exit;
    private TextView city_name;
    private TextView temp;
    private TextView humidity;
    private TextView feels_like;

    private ImageView image_icon;
    private final String key = "49446b6b3e00afaeb808d4ad364351c8";


    LocationManager locationManager;
    LocationListener locationListener;
    WeatherData weatherData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);
        Bundle args = getIntent().getExtras();

        ed_city = findViewById(R.id.ed_city);
        btn_find = findViewById(R.id.btn_find);
        btn_def_location = findViewById(R.id.btn_def_location);
        city_name = findViewById(R.id.city_name);
        temp = findViewById(R.id.temp);
        humidity = findViewById(R.id.humidity);
        feels_like = findViewById(R.id.feel);
        image_icon = findViewById(R.id.image_icon);


        weatherData = new WeatherData(this);

        weatherData.setOnDownloadedWeather(new OnDownloadedWeather() {
            @Override
            public void onDownload(String result) throws JSONException {
                try {
                    onDownloadWeather(result);
                } catch (Exception ex) {
                    Toast.makeText(getBaseContext(), "Ð ÐµÐ¿Ñ€Ð°Ð²Ð¸Ð»ÑŒÐ½Ñ‹Ð¹ Ð²Ð²Ð¾Ð´", Toast.LENGTH_SHORT).show();
                    Log.d("widget_test", ex.toString());
                }
            }
        });

        if (checkPermission()) {
            // Запускаем получение данных о погоде по GPS
            weatherData.getWeatherByGPS();
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        btn_find.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    clickFind();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }

            }
        });



        btn_def_location.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("my_location", "click");
                weatherData.getWeatherByGPS();
            }
        });

    }

    private boolean checkPermission(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            Log.d("my_location", "checkSelfPermission is false in checkPermission");
            return false;
        } else {
            return true;
        }
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Запускаем получение данных о погоде по GPS, если разрешения получены
                weatherData.getWeatherByGPS();
            } else {

                Log.d("my_location", "Permission denied in onRequestPermissionsResult");
            }
        }
    }




    private void clickFind() throws IOException, JSONException {

        if (ed_city.getText().toString().trim().equals("")) {
            Toast.makeText(this, "Ошибка", Toast.LENGTH_SHORT).show();
        } else {
            Log.d("weather", "Ошибка" + ed_city.getText().toString());
            getWeather(ed_city.getText().toString(), key);
        }

    }

    private  void getWeather(double latitudem, double longitude, String key){
        String url = "https://api.openweathermap.org/data/2.5/weather?lat="+ latitudem+"&lon="+longitude+"&appid=" + key + "&units=metric&lang=ru";
        new DownloadWeatherTask().execute(url);
    }
    private  void getWeather(String city, String key){
        String url = "https://api.openweathermap.org/data/2.5/weather?q=" + city + "&appid=" + key + "&units=metric&lang=ru";
        new DownloadWeatherTask().execute(url);
    }

    @SuppressLint("SetTextI18n")
    private void onDownloadWeather(String result) throws JSONException {
        JSONObject jsonObject = new JSONObject(result);
        String value_city = jsonObject.getString("name");
        city_name.setText(value_city);
        WidgetWeather.city_name = value_city;

        int temp_value = (int)Math.round(jsonObject.getJSONObject("main").getDouble("temp"));
        temp.setText( (temp_value < 0 ? "-" : "+") + temp_value + "°С");
        String humidity_value = Integer.toString(jsonObject.getJSONObject("main").getInt("humidity"));
        humidity.setText("Влажность воздуха: "+humidity_value + "%");
        int feels_like_value = (int)Math.round(jsonObject.getJSONObject("main").getDouble("feels_like"));
        feels_like.setText("Ощущается как: "+feels_like_value + "°С");


        String icon = jsonObject.getJSONArray("weather").getJSONObject(0).getString("icon");

        String iconUrl = "https://openweathermap.org/img/wn/" + icon +"@4x.png";

        Glide.with(this)
                .load(iconUrl)
                .into(image_icon);

    }

    private class DownloadWeatherTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {

            String surl = params[0];
            URL url;
            StringBuilder builder = new StringBuilder();

            try {
                url = new URL(surl);
                try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
                    String str;
                    while ((str = bufferedReader.readLine()) != null) {
                        builder.append(str);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            return builder.toString();
        }

        @Override
        protected void onPostExecute(String result) {
            try{
                onDownloadWeather(result);
            } catch (Exception ex){
                Toast.makeText(getBaseContext(), "Ð ÐµÐ¿Ñ€Ð°Ð²Ð¸Ð»ÑŒÐ½Ñ‹Ð¹ Ð²Ð²Ð¾Ð´", Toast.LENGTH_SHORT).show();
                Log.d("widget_test", ex.toString());
            }
        }
    }

    public void onClickExit(){
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}