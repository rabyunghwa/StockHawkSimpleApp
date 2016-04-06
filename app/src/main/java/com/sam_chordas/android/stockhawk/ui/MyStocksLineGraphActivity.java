package com.sam_chordas.android.stockhawk.ui;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.db.chart.model.LineSet;
import com.db.chart.view.ChartView;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.entity.Stock;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.utils.LogUtil;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by ByungHwa on 4/4/2016.
 */
public class MyStocksLineGraphActivity extends AppCompatActivity {

    private static final String LOG_TAG = "MyStocksLineGraphActivity";
    private ChartView chartView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_line_graph);

        String clickedSymbol = getIntent().getStringExtra("symbol");
        LogUtil.log_i(LOG_TAG, "clicked stock symbol: " + clickedSymbol);

        // start fetch stock data asynctask
        FetchHistoricalAsyncTask asyncTask = new FetchHistoricalAsyncTask();
        asyncTask.execute(clickedSymbol);

        chartView = (ChartView) findViewById(R.id.linechart);
    }

    class FetchHistoricalAsyncTask extends AsyncTask<String, Void, ArrayList<Stock>> {

        private OkHttpClient client = new OkHttpClient();

        @Override
        protected ArrayList<Stock> doInBackground(String... params) {
            String symbol = params[0];
            StringBuilder urlStringBuilder = new StringBuilder();
            // http://query.yahooapis.com/v1/public/yql?q=select%20*%20from%20yahoo.finance.historicaldata%20where%20symbol%20in%20%28%27YHOO%27%29%20and%20startDate%20=%20%272016-03-30%27%20and%20endDate%20=%20%272016-04-05%27&diagnostics=true&env=store://datatables.org/alltableswithkeys
            try {
                // Base URL for the Yahoo query
                urlStringBuilder.append("https://query.yahooapis.com/v1/public/yql?q=");
                urlStringBuilder.append(URLEncoder.encode("select * from yahoo.finance.historicaldata where symbol "
                        + "in (", "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            try {
                // get start date string and end date string
                Date date = new Date(System.currentTimeMillis());
                SimpleDateFormat mformat = new SimpleDateFormat("yyyy-MM-dd");
                String endDateString = mformat.format(date);

                date = new Date(System.currentTimeMillis() + (( - 6) * 86400000));
                String startDateString = mformat.format(date);

                urlStringBuilder.append(
                        URLEncoder.encode("\"" + symbol + "\")", "UTF-8"))
                .append(" and startDate = ")
                .append("\"" + URLEncoder.encode(startDateString, "UTF-8") + "\"")
                .append(" and endDate = ")
                .append("\"" + URLEncoder.encode(endDateString, "UTF-8") + "\"");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            urlStringBuilder.append("&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables."
                    + "org%2Falltableswithkeys&callback=");

            String urlString;
            String getResponse = null;
            int result = GcmNetworkManager.RESULT_FAILURE;

            if (urlStringBuilder != null) {
                urlString = urlStringBuilder.toString();
                LogUtil.log_i(LOG_TAG, "full url: " + urlString);
                // http://query.yahooapis.com/v1/public/yql?q=select%20*%20from%20yahoo.finance.historicaldata%20where%20symbol%20in%20%28%27YHOO%27%29%20and%20startDate%20=%20%272016-03-30%27%20and%20endDate%20=%20%272016-04-05%27&diagnostics=true&env=store://datatables.org/alltableswithkeys
                try {
                    getResponse = fetchData(urlString); // fetched stock data: {"query":{"count":1,"created":"2016-04-03T06:27:56Z","lang":"en-US","diagnostics":{"url":[{"execution-start-time":"0","execution-stop-time":"1","execution-time":"1","content":"http://www.datatables.org/yahoo/finance/yahoo.finance.quotes.xml"},{"execution-start-time":"5","execution-stop-time":"19","execution-time":"14","content":"http://download.finance.yahoo.com/d/quotes.csv?f=aa2bb2b3b4cc1c3c4c6c8dd1d2ee1e7e8e9ghjkg1g3g4g5g6ii5j1j3j4j5j6k1k2k4k5ll1l2l3mm2m3m4m5m6m7m8nn4opp1p2p5p6qrr1r2r5r6r7ss1s7t1t7t8vv1v7ww1w4xy&s=fgdgfdg"}],"publiclyCallable":"true","cache":{"execution-start-time":"4","execution-stop-time":"4","execution-time":"0","method":"GET","type":"MEMCACHED","content":"5d1e1de680846a307c9874dc3d6878dc"},"query":{"execution-start-time":"5","execution-stop-time":"19","execution-time":"14","params":"{url=[http://download.finance.yahoo.com/d/quotes.csv?f=aa2bb2b3b4cc1c3c4c6c8dd1d2ee1e7e8e9ghjkg1g3g4g5g6ii5j1j3j4j5j6k1k2k4k5ll1l2l3mm2m3m4m5m6m7m8nn4opp1p2p5p6qrr1r2r5r6r7ss1s7t1t7t8vv1v7ww1w4xy&s=fgdgfdg]}","content":"select * from csv where url=@url and columns='Ask,AverageDailyVolume,Bid,AskRealtime,BidRealtime,BookValue,Change&PercentChange,Change,Commission,Currency,ChangeRealtime,AfterHoursChangeRealtime,DividendShare,LastTradeDate,TradeDate,EarningsShare,ErrorIndicationreturnedforsymbolchangedinvalid,EPSEstimateCurrentYear,EPSEstimateNextYear,EPSEstimateNextQuarter,DaysLow,DaysHigh,YearLow,YearHigh,HoldingsGainPercent,AnnualizedGain,HoldingsGain,HoldingsGainPercentRealtime,HoldingsGainRealtime,MoreInfo,OrderBookRealtime,MarketCapitalization,MarketCapRealtime,EBITDA,ChangeFromYearLow,PercentChangeFromYearLow,LastTradeRealtimeWithTime,ChangePercentRealtime,ChangeFromYearHigh,PercebtChangeFromYearHigh,LastTradeWithTime,LastTradePriceOnly,HighLimit,LowLimit,DaysRange,DaysRangeRealtime,FiftydayMovingAverage,TwoHundreddayMovingAverage,ChangeFromTwoHundreddayMovingAverage,PercentChangeFromTwoHundreddayMovingAverage,ChangeFromFiftydayMovingAverage,PercentChangeFromFiftydayMovingAverage,Name,Notes,Open,PreviousClose,PricePaid,ChangeinPercent,PriceSales,PriceBook,ExDividendDate,PERatio,DividendPayDate,PERatioRealtime,PEGRatio,PriceEPSEstimateCurrentYear,PriceEPSEstimateNextYear,Symbol,SharesOwned,ShortRatio,LastTradeTime,TickerTrend,OneyrTargetPrice,Volume,HoldingsValue,HoldingsValueRealtime,YearRange,DaysValueChange,DaysValueChangeRealtime,StockExchange,DividendYield'"},"javascript":{"execution-start-time":"3","execution-stop-time":"26","execution-time":"23","instructions-used":"53664","table-name":"yahoo.finance.quotes"},"user-time":"27","service-time":"15","build-version":"0.2.430"},"results":{"quote":{"symbol":"fgdgfdg","Ask":null,"AverageDailyVolume":null,"Bid":null,"AskRealtime":null,"BidRealtime":null,"BookValue":null,"Change_PercentChange":null,"Change":null,"Commission":null,"Currency":null,"ChangeRealtime":null,"AfterHoursChangeRealtime":null,"DividendShare":null,"LastTradeDate":null,"TradeDate":null,"EarningsShare":null,"ErrorIndicationreturnedforsymbolchangedinvalid":null,"EPSEstimateCurrentYear":null,"EPSEstimateNextYear":null,"EPSEstimateNextQuarter":null,"DaysLow":null,"DaysHigh":null,"YearLow":null,"YearHigh":null,"HoldingsGainPercent":null,"AnnualizedGain":null,"HoldingsGain":null,"HoldingsGainPercentRealtime":null,"HoldingsGainRealtime":null,"MoreInfo":null,"OrderBookRealtime":null,"MarketCapitalization":null,"MarketCapRealtime":null,"EBITDA":null,"ChangeFromYearLow":null,"PercentChangeFromYearLow":null,"LastTradeRealtimeWithTime":null,"ChangePercentRealtime":null,"ChangeFromYearHigh":null,"PercebtChangeFromYearHigh":null,"LastTradeWithTime":null,"LastTradePriceOnly":null,"HighLimit":null,"LowLimit":null,"DaysRange":null,"DaysRangeRealtime":null,"FiftydayMovingAverage":null,"TwoHundreddayMovingAverage":null,"ChangeFromTwoHundreddayMovingAverage":null,"PercentChangeFromTwoHundreddayMovingAverage":null,"ChangeFromFiftydayMovingAverage":null,"PercentChangeFromFiftydayMovingAverage":null,"Name":null,"Notes":null,"Open":null,"PreviousClose":null,"Pr
                    LogUtil.log_i(LOG_TAG, "fetched stock data: " + getResponse);
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
//        result = GcmNetworkManager.RESULT_SUCCESS;

            return Utils.quoteJsonToArrayList(getResponse);
        }

        @Override
        protected void onPostExecute(ArrayList<Stock> results) {
            ArrayList<String> dateStringArrayList = new ArrayList<>(results.size());
            ArrayList<Float> bidFloatArrayList = new ArrayList<>(results.size());

            if (results.size() > 0) {
                //for (int i=results.size()-1;i>=0;i--) {
                    for (int i=results.size()-1;i>=0;i--) {
                    LogUtil.log_i(LOG_TAG, "stock bid price: " + results.get(i).getBidPrice());
                    LogUtil.log_i(LOG_TAG, "stock date: " + Utils.getDayName(MyStocksLineGraphActivity.this, Utils.dateStringToTimeInMillis(results.get(i).getDateString())));
                    LogUtil.log_i(LOG_TAG, "-------------------");
                    dateStringArrayList.add(Utils.getDayName(MyStocksLineGraphActivity.this, Utils.dateStringToTimeInMillis(results.get(i).getDateString())));
                    bidFloatArrayList.add(Float.valueOf(results.get(i).getBidPrice()));
                }
                String[] dateArray = dateStringArrayList.toArray(new String[0]);
//            String[] dateArray = ArrayUtils.toPrimitive(dateStringArrayList.toArray(new String[0]));
                float[] bidArray = ArrayUtils.toPrimitive(bidFloatArrayList.toArray(new Float[0]), 0.0F);
                // Data
                LineSet dataset = new LineSet(dateArray, bidArray);
                dataset.setColor(getResources().getColor(android.R.color.black))
                        .setDotsColor(getResources().getColor(android.R.color.black))
                        .setThickness(4)
                        .setDashed(new float[]{10f,10f})
                        .beginAt(0);
                chartView.addData(dataset);
                chartView.show();
            }
        }

        String fetchData(String url) throws IOException {
            Request request = new Request.Builder()
                    .url(url)
                    .build();

            Response response = client.newCall(request).execute();
            return response.body().string();
        }
    }


}
