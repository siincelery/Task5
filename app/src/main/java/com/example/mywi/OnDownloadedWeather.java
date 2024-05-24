package com.example.mywi;

import org.json.JSONException;

interface OnDownloadedWeather{
    void onDownload(String result) throws JSONException;
}
