package com.sam_chordas.android.stockhawk.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Created by ByungHwa on 7/8/2015.
 */
public class NetworkUtil {

    private static Context mContext;

    public static boolean isNetworkAvailable(Context context) {
        context =mContext;
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return (networkInfo != null) && (networkInfo.isConnectedOrConnecting());
    }
}
