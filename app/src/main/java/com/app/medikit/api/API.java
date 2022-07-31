package com.app.medikit.api;

public class API {

    public static String CHANNEL_ID = "1811137";
    public static String READ_API_KEY = "CRJVGC27S9JP8HHI";

    public static String getFeedURL(int limit){
        return "https://api.thingspeak.com/channels/" + CHANNEL_ID + "/feeds.json?api_key=" + READ_API_KEY + "&results="+limit;
    }
}
