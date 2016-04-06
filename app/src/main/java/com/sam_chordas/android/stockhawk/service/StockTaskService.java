package com.sam_chordas.android.stockhawk.service;

import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.utils.LogUtil;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Created by sam_chordas on 9/30/15.
 * The GCMTask service is primarily for periodic tasks. However, OnRunTask can be called directly
 * and is used for the initialization and adding task as well.
 */
public class StockTaskService extends GcmTaskService{
  private String LOG_TAG = StockTaskService.class.getSimpleName();

  private OkHttpClient client = new OkHttpClient();
  private Context mContext;
  private StringBuilder mStoredSymbols = new StringBuilder();
  private boolean isUpdate;

  public StockTaskService(){}

  public StockTaskService(Context context){
    mContext = context;
  }

  String fetchData(String url) throws IOException{
    Request request = new Request.Builder()
        .url(url)
        .build();

    Response response = client.newCall(request).execute();
    return response.body().string();
  }

  @Override
  public int onRunTask(TaskParams params){
    Cursor initQueryCursor;
    if (mContext == null){
      mContext = this;
    }
    StringBuilder urlStringBuilder = new StringBuilder();
    try{
      // Base URL for the Yahoo query
      urlStringBuilder.append("https://query.yahooapis.com/v1/public/yql?q=");
      urlStringBuilder.append(URLEncoder.encode("select * from yahoo.finance.quotes where symbol "
        + "in (", "UTF-8"));
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    if (params.getTag().equals("init") || params.getTag().equals("periodic")){
      isUpdate = true;
      initQueryCursor = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
          new String[] { "Distinct " + QuoteColumns.SYMBOL }, null,
          null, null);
      if (initQueryCursor.getCount() == 0 || initQueryCursor == null){
        // Init task. Populates DB with quotes for the symbols seen below
        try {
          urlStringBuilder.append(
              URLEncoder.encode("\"YHOO\",\"AAPL\",\"GOOG\",\"MSFT\")", "UTF-8"));
        } catch (UnsupportedEncodingException e) {
          e.printStackTrace();
        }
      } else if (initQueryCursor != null){
        DatabaseUtils.dumpCursor(initQueryCursor);
        initQueryCursor.moveToFirst();
        for (int i = 0; i < initQueryCursor.getCount(); i++){
          mStoredSymbols.append("\""+
              initQueryCursor.getString(initQueryCursor.getColumnIndex("symbol"))+"\",");
          initQueryCursor.moveToNext();
        }
        mStoredSymbols.replace(mStoredSymbols.length() - 1, mStoredSymbols.length(), ")");
        try {
          urlStringBuilder.append(URLEncoder.encode(mStoredSymbols.toString(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
          e.printStackTrace();
        }
      }
    } else if (params.getTag().equals("add")){
      isUpdate = false;
      // get symbol from params.getExtra and build query
      String stockInput = params.getExtras().getString("symbol");
      try {
        urlStringBuilder.append(URLEncoder.encode("\""+stockInput+"\")", "UTF-8"));
      } catch (UnsupportedEncodingException e){
        e.printStackTrace();
      }
    }
    // finalize the URL for the API query.
    urlStringBuilder.append("&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables."
        + "org%2Falltableswithkeys&callback=");

    String urlString;
    String getResponse;
    int result = GcmNetworkManager.RESULT_FAILURE;

    if (urlStringBuilder != null){
      urlString = urlStringBuilder.toString();
      LogUtil.log_i(LOG_TAG, "full url: " + urlString);
      // http://query.yahooapis.com/v1/public/yql?q=select%20*%20from%20yahoo.finance.historicaldata%20where%20symbol%20in%20%28%27YHOO%27%29%20and%20startDate%20=%20%272016-03-30%27%20and%20endDate%20=%20%272016-04-05%27&diagnostics=true&env=store://datatables.org/alltableswithkeys
      try{
        getResponse = fetchData(urlString); // fetched stock data: {"query":{"count":1,"created":"2016-04-03T06:27:56Z","lang":"en-US","diagnostics":{"url":[{"execution-start-time":"0","execution-stop-time":"1","execution-time":"1","content":"http://www.datatables.org/yahoo/finance/yahoo.finance.quotes.xml"},{"execution-start-time":"5","execution-stop-time":"19","execution-time":"14","content":"http://download.finance.yahoo.com/d/quotes.csv?f=aa2bb2b3b4cc1c3c4c6c8dd1d2ee1e7e8e9ghjkg1g3g4g5g6ii5j1j3j4j5j6k1k2k4k5ll1l2l3mm2m3m4m5m6m7m8nn4opp1p2p5p6qrr1r2r5r6r7ss1s7t1t7t8vv1v7ww1w4xy&s=fgdgfdg"}],"publiclyCallable":"true","cache":{"execution-start-time":"4","execution-stop-time":"4","execution-time":"0","method":"GET","type":"MEMCACHED","content":"5d1e1de680846a307c9874dc3d6878dc"},"query":{"execution-start-time":"5","execution-stop-time":"19","execution-time":"14","params":"{url=[http://download.finance.yahoo.com/d/quotes.csv?f=aa2bb2b3b4cc1c3c4c6c8dd1d2ee1e7e8e9ghjkg1g3g4g5g6ii5j1j3j4j5j6k1k2k4k5ll1l2l3mm2m3m4m5m6m7m8nn4opp1p2p5p6qrr1r2r5r6r7ss1s7t1t7t8vv1v7ww1w4xy&s=fgdgfdg]}","content":"select * from csv where url=@url and columns='Ask,AverageDailyVolume,Bid,AskRealtime,BidRealtime,BookValue,Change&PercentChange,Change,Commission,Currency,ChangeRealtime,AfterHoursChangeRealtime,DividendShare,LastTradeDate,TradeDate,EarningsShare,ErrorIndicationreturnedforsymbolchangedinvalid,EPSEstimateCurrentYear,EPSEstimateNextYear,EPSEstimateNextQuarter,DaysLow,DaysHigh,YearLow,YearHigh,HoldingsGainPercent,AnnualizedGain,HoldingsGain,HoldingsGainPercentRealtime,HoldingsGainRealtime,MoreInfo,OrderBookRealtime,MarketCapitalization,MarketCapRealtime,EBITDA,ChangeFromYearLow,PercentChangeFromYearLow,LastTradeRealtimeWithTime,ChangePercentRealtime,ChangeFromYearHigh,PercebtChangeFromYearHigh,LastTradeWithTime,LastTradePriceOnly,HighLimit,LowLimit,DaysRange,DaysRangeRealtime,FiftydayMovingAverage,TwoHundreddayMovingAverage,ChangeFromTwoHundreddayMovingAverage,PercentChangeFromTwoHundreddayMovingAverage,ChangeFromFiftydayMovingAverage,PercentChangeFromFiftydayMovingAverage,Name,Notes,Open,PreviousClose,PricePaid,ChangeinPercent,PriceSales,PriceBook,ExDividendDate,PERatio,DividendPayDate,PERatioRealtime,PEGRatio,PriceEPSEstimateCurrentYear,PriceEPSEstimateNextYear,Symbol,SharesOwned,ShortRatio,LastTradeTime,TickerTrend,OneyrTargetPrice,Volume,HoldingsValue,HoldingsValueRealtime,YearRange,DaysValueChange,DaysValueChangeRealtime,StockExchange,DividendYield'"},"javascript":{"execution-start-time":"3","execution-stop-time":"26","execution-time":"23","instructions-used":"53664","table-name":"yahoo.finance.quotes"},"user-time":"27","service-time":"15","build-version":"0.2.430"},"results":{"quote":{"symbol":"fgdgfdg","Ask":null,"AverageDailyVolume":null,"Bid":null,"AskRealtime":null,"BidRealtime":null,"BookValue":null,"Change_PercentChange":null,"Change":null,"Commission":null,"Currency":null,"ChangeRealtime":null,"AfterHoursChangeRealtime":null,"DividendShare":null,"LastTradeDate":null,"TradeDate":null,"EarningsShare":null,"ErrorIndicationreturnedforsymbolchangedinvalid":null,"EPSEstimateCurrentYear":null,"EPSEstimateNextYear":null,"EPSEstimateNextQuarter":null,"DaysLow":null,"DaysHigh":null,"YearLow":null,"YearHigh":null,"HoldingsGainPercent":null,"AnnualizedGain":null,"HoldingsGain":null,"HoldingsGainPercentRealtime":null,"HoldingsGainRealtime":null,"MoreInfo":null,"OrderBookRealtime":null,"MarketCapitalization":null,"MarketCapRealtime":null,"EBITDA":null,"ChangeFromYearLow":null,"PercentChangeFromYearLow":null,"LastTradeRealtimeWithTime":null,"ChangePercentRealtime":null,"ChangeFromYearHigh":null,"PercebtChangeFromYearHigh":null,"LastTradeWithTime":null,"LastTradePriceOnly":null,"HighLimit":null,"LowLimit":null,"DaysRange":null,"DaysRangeRealtime":null,"FiftydayMovingAverage":null,"TwoHundreddayMovingAverage":null,"ChangeFromTwoHundreddayMovingAverage":null,"PercentChangeFromTwoHundreddayMovingAverage":null,"ChangeFromFiftydayMovingAverage":null,"PercentChangeFromFiftydayMovingAverage":null,"Name":null,"Notes":null,"Open":null,"PreviousClose":null,"Pr
        LogUtil.log_i(LOG_TAG, "fetched stock data: " + getResponse);
//        result = GcmNetworkManager.RESULT_SUCCESS;
        try {
          ContentValues contentValues = new ContentValues();
          // update ISCURRENT to 0 (false) so new data is current
          if (isUpdate){
            contentValues.put(QuoteColumns.ISCURRENT, 0);
            mContext.getContentResolver().update(QuoteProvider.Quotes.CONTENT_URI, contentValues,
                null, null);
          }

          // see if there is need to insert data into content provider, if returned data is not valid, then we shouldnt perform the transaction
          if (Utils.quoteJsonToContentVals(getResponse).size() > 0) {
            // first delete old data
            // Delete old items so that we dont overload the layout with same data
            if (isUpdate) {
              Uri dirUriItemsContract = QuoteProvider.Quotes.CONTENT_URI;
              mContext.getContentResolver().delete(dirUriItemsContract, null, null);
            }

            mContext.getContentResolver().applyBatch(QuoteProvider.AUTHORITY,
                    Utils.quoteJsonToContentVals(getResponse));
            // we only set result to RESULT_SUCCESS if we successfully fetched and inserted data
            result = GcmNetworkManager.RESULT_SUCCESS;
          }

        }catch (RemoteException | OperationApplicationException e){
          Log.e(LOG_TAG, "Error applying batch insert", e);
        }
      } catch (IOException e){
        e.printStackTrace();
      }
    }

    return result;
  }

}
