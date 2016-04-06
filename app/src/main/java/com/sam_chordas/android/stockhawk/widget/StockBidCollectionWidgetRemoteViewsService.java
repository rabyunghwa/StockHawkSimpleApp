package com.sam_chordas.android.stockhawk.widget;

import android.annotation.TargetApi;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.utils.LogUtil;


/**
 * Created by ByungHwa on 7/9/2015.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class StockBidCollectionWidgetRemoteViewsService extends RemoteViewsService {

    public static final String EXTRA_LIST_VIEW_SYMBOL_CLICKED = "symbol";

    private static final String[] STOCK_COLUMNS = {
            QuoteColumns.SYMBOL,
            QuoteColumns.PERCENT_CHANGE,
    };

    // these indices must match the projection
    private static final int INDEX_SYMBOL = 0;
    private static final int INDEX_PERCENT_CHANGE = 1;
    private RemoteViews views;

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new RemoteViewsFactory() {
            // this "data" contains 5 days of football score data
            private Cursor data = null;

            @Override
            public void onCreate() {
                // Nothing to do
            }

            @Override
            public void onDataSetChanged() {
                if (data != null) {
                    data.close();
                }
                // This method is called by the app hosting the widget (e.g., the launcher)
                // However, our ContentProvider is not exported so it doesn't have access to the
                // data. Therefore we need to clear (and finally restore) the calling identity so
                // that calls use our process and permission
                final long identityToken = Binder.clearCallingIdentity();
                Uri footballForDateUri = QuoteProvider.Quotes.CONTENT_URI;
                data = getContentResolver().query(footballForDateUri,
                        STOCK_COLUMNS,
                        null,
                        null,
                        null);
                LogUtil.log_i("info", "Detail Widget Cursor is empty: " + !data.moveToFirst());
                Binder.restoreCallingIdentity(identityToken);
            }

            @Override
            public void onDestroy() {
                if (data != null) {
                    data.close();
                    data = null;
                }
            }

            @Override
            public int getCount() {
                int count = data == null ? 0 : data.getCount();
                LogUtil.log_i("Stock Widget", "item count: " + count);
                return count;
            }

            @Override
            public RemoteViews getViewAt(int position) {
                if (position == AdapterView.INVALID_POSITION ||
                        data == null || !data.moveToPosition(position)) {
                    return null;
                }
                views = new RemoteViews(getPackageName(),
                        R.layout.widget_collection_item);

                // fill views with real data

                // data.moveToPosition(position) declared above has already moved data to position. thus here "data"
                // only contains one day of data
                // Extract the weather data from the Cursor
                String symbol = data.getString(INDEX_SYMBOL);
                String percentchange = data.getString(INDEX_PERCENT_CHANGE);

                views.setTextViewText(R.id.stock_symbol, symbol);
                views.setTextViewText(R.id.change, percentchange);

                // launch detail activity on click of an item in the list
                Intent fillInIntent = new Intent();
                fillInIntent.putExtra(EXTRA_LIST_VIEW_SYMBOL_CLICKED, data.getString(INDEX_SYMBOL));
                views.setOnClickFillInIntent(R.id.widget_list_item, fillInIntent);

                return views;
            }

            @Override
            public RemoteViews getLoadingView() {
                return new RemoteViews(getPackageName(), R.layout.widget_collection_item);
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }
        };
    }

}
