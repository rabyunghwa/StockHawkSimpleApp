package com.sam_chordas.android.stockhawk.rest;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.text.format.Time;
import android.util.Log;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.entity.Stock;
import com.sam_chordas.android.stockhawk.utils.LogUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by sam_chordas on 10/8/15.
 */
public class Utils {

    private static String LOG_TAG = Utils.class.getSimpleName();

    public static boolean showPercent = true;

    public static ArrayList quoteJsonToContentVals(String JSON) {
        ArrayList<ContentProviderOperation> batchOperations = new ArrayList<>();
        JSONObject jsonObject = null;
        JSONArray resultsArray = null;
        try {
            jsonObject = new JSONObject(JSON);
            if (jsonObject != null && jsonObject.length() != 0) {
                jsonObject = jsonObject.getJSONObject("query");
                int count = Integer.parseInt(jsonObject.getString("count"));
                LogUtil.log_i(LOG_TAG, "result count: " + count);// whether or not the Stock Quote is valid, there will still be a result sent back, except all the values are "null" strings
                if (count == 1) {
                    jsonObject = jsonObject.getJSONObject("results")
                            .getJSONObject("quote");
                    // as there is always a result sent back, we have to verify if its really valid data
                    if (!jsonObject.getString("Change").equals("null") && !jsonObject.getString("symbol").equals("null") && !jsonObject.getString("Bid").equals("null") && !jsonObject.getString("ChangeinPercent").equals("null")) {
                        batchOperations.add(buildBatchOperation(jsonObject));
                    }

                } else {
                    resultsArray = jsonObject.getJSONObject("results").getJSONArray("quote");

                    if (resultsArray != null && resultsArray.length() != 0) {
                        for (int i = 0; i < resultsArray.length(); i++) {
                            jsonObject = resultsArray.getJSONObject(i);
                            batchOperations.add(buildBatchOperation(jsonObject));
                        }
                    }
                }
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "String to JSON failed: " + e);
        }
        return batchOperations;
    }

    public static ArrayList<Stock> quoteJsonToArrayList(String json) {
        ArrayList<Stock> results = new ArrayList<>();
        JSONObject jsonObject = null;
        JSONArray resultsArray = null;
        try {
            jsonObject = new JSONObject(json);
            if (jsonObject != null && jsonObject.length() != 0) {
                jsonObject = jsonObject.getJSONObject("query");
                int count = Integer.parseInt(jsonObject.getString("count"));
                LogUtil.log_i(LOG_TAG, "result count: " + count);// whether or not the Stock Quote is valid, there will still be a result sent back, except all the values are "null" strings
                if (count == 1) {
                    jsonObject = jsonObject.getJSONObject("results")
                            .getJSONObject("quote");
                    // as there is always a result sent back, we have to verify if its really valid data
                    if (!jsonObject.getString("Change").equals("null") && !jsonObject.getString("symbol").equals("null") && !jsonObject.getString("Bid").equals("null") && !jsonObject.getString("ChangeinPercent").equals("null")) {

                    }

                } else {
                    resultsArray = jsonObject.getJSONObject("results").getJSONArray("quote");

                    if (resultsArray != null && resultsArray.length() != 0) {
                        for (int i = 0; i < resultsArray.length(); i++) {
                            jsonObject = resultsArray.getJSONObject(i);
                            results.add(buildBidPrices(jsonObject));
                        }
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return results;
    }

    private static Stock buildBidPrices(JSONObject jsonObject) {
        try {
            Stock stock = new Stock();
            stock.setBidPrice(jsonObject.getString("High"));
            stock.setDateString(jsonObject.getString("Date"));
            return stock;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String truncateBidPrice(String bidPrice) {
        LogUtil.log_i(LOG_TAG, "bid price: " + bidPrice);
        if (!bidPrice.equals("null")) {
            bidPrice = String.format("%.2f", Float.parseFloat(bidPrice));
        }
        return bidPrice;
    }

    public static String truncateChange(String change, boolean isPercentChange) {
        if (!change.equals("null")) {
            String weight = change.substring(0, 1);
            String ampersand = "";
            if (isPercentChange) {
                ampersand = change.substring(change.length() - 1, change.length());
                change = change.substring(0, change.length() - 1);
            }
            change = change.substring(1, change.length());
            double round = (double) Math.round(Double.parseDouble(change) * 100) / 100;
            change = String.format("%.2f", round);
            StringBuffer changeBuffer = new StringBuffer(change);
            changeBuffer.insert(0, weight);
            changeBuffer.append(ampersand);
            change = changeBuffer.toString();
        }
        return change;
    }

    public static ContentProviderOperation buildBatchOperation(JSONObject jsonObject) {
        ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(
                QuoteProvider.Quotes.CONTENT_URI);
        try {
            String change = jsonObject.getString("Change");
            builder.withValue(QuoteColumns.SYMBOL, jsonObject.getString("symbol"));
            builder.withValue(QuoteColumns.BIDPRICE, truncateBidPrice(jsonObject.getString("Bid")));
            builder.withValue(QuoteColumns.PERCENT_CHANGE, truncateChange(
                    jsonObject.getString("ChangeinPercent"), true));
            builder.withValue(QuoteColumns.CHANGE, truncateChange(change, false));
            builder.withValue(QuoteColumns.ISCURRENT, 1);
            if (change.charAt(0) == '-') {
                builder.withValue(QuoteColumns.ISUP, 0);
            } else {
                builder.withValue(QuoteColumns.ISUP, 1);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return builder.build();
    }

    public static String getDayName(Context context, long dateInMillis) {
        // If the date is today, return the localized version of "Today" instead of the actual
        // day name.

        Time t = new Time();
        t.setToNow();
        int julianDay = Time.getJulianDay(dateInMillis, t.gmtoff);
        int currentJulianDay = Time.getJulianDay(System.currentTimeMillis(), t.gmtoff);
        if (julianDay == currentJulianDay) {
            return context.getString(R.string.today);
        } else if (julianDay == currentJulianDay - 1) {
            return context.getString(R.string.yesterday);
        } else {
            Time time = new Time();
            time.setToNow();
            // Otherwise, the format is just the day of the week (e.g "Wednesday".
            SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE");
            return dayFormat.format(dateInMillis);
        }
    }

    public static long dateStringToTimeInMillis(String dateString) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        try {
            Date mDate = sdf.parse(dateString);
            long timeInMilliseconds = mDate.getTime();
            return timeInMilliseconds;
        } catch (ParseException e) {
            e.printStackTrace();
            return -1;
        }
    }
}
