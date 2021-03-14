package com.example.android.parallelproject;

import android.util.Log;

public class ApiThread extends Thread{

    private String url , response;

    public String getResponse() {
        return response;
    }

    public ApiThread(String mUrl)
    {
        this.url = mUrl;
    }

    public void run()
    {
        response = Utilites.fetchEarthquakeData(this.url);
    }
}
