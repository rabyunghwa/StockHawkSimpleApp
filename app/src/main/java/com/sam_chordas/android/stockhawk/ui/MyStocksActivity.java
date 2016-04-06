package com.sam_chordas.android.stockhawk.ui;

import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;
import com.melnykov.fab.FloatingActionButton;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.QuoteCursorAdapter;
import com.sam_chordas.android.stockhawk.rest.RecyclerViewItemClickListener;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.service.StockIntentService;
import com.sam_chordas.android.stockhawk.service.StockTaskService;
import com.sam_chordas.android.stockhawk.touch_helper.SimpleItemTouchHelperCallback;
import com.sam_chordas.android.stockhawk.utils.LogUtil;

public class MyStocksActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String LOG_TAG = "MyStocksActivity";

    public static final String STOCK_QUOTE_FETCHED_FAILURE = "com.sam_chordas.android.stockhawk.STOCK_QUOTE_FETCHED_FAILURE";
    public static final String STOCK_QUOTE_FETCHED_SUCCESS = "com.sam_chordas.android.stockhawk.STOCK_QUOTE_FETCHED_SUCCESS";

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;
    private Intent mServiceIntent;
    private ItemTouchHelper mItemTouchHelper;
    private static final int CURSOR_LOADER_ID = 0;
    private QuoteCursorAdapter mCursorAdapter;
    private static Context mContext;
    private Cursor mCursor;
    boolean isConnected;
    private StockInfoFetchedFailureReceiver mStockReceiver;
    private NetworkStateChangeReceiver mNetworkStateChangeReceiver;
    private TextView emptyView;
    private RecyclerView recyclerView;
    private long period;
    private long flex;
    private String periodicTag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        ConnectivityManager cm =
                (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
        setContentView(R.layout.activity_my_stocks);

        registerStockReceiver();
        registerNetworkStateChangeReceiver();

        period = 3600L;
        flex = 10L;
        periodicTag = "periodic";

        // The intent service is for executing immediate pulls from the Yahoo API
        // GCMTaskService can only schedule tasks, they cannot execute immediately
        mServiceIntent = new Intent(this, StockIntentService.class);
        mServiceIntent.putExtra("tag", "init");
        startService(mServiceIntent);
        if (savedInstanceState == null) {
            // Run the initialize task service so that some stocks appear upon an empty database
            mServiceIntent.putExtra("tag", "init");
            if (isConnected) {
                startService(mServiceIntent);
            } else {
                networkToast();
            }
        }
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        getLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);

        // empty view
        emptyView = (TextView) findViewById(R.id.empty_view);

        mCursorAdapter = new QuoteCursorAdapter(this, null);
        recyclerView.addOnItemTouchListener(new RecyclerViewItemClickListener(this,
                new RecyclerViewItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View v, int position) {
                        //TODO:
                        // do something on item click
                        // first get the stock symbol
                        mCursor.moveToPosition(position);
                        String stockSymbolClicked = mCursor.getString(mCursor.getColumnIndex(QuoteColumns.SYMBOL));
                        LogUtil.log_i(LOG_TAG, "clicked stock symbol: " + stockSymbolClicked);
                        Intent intent = new Intent(MyStocksActivity.this, MyStocksLineGraphActivity.class);
                        intent.putExtra("symbol", stockSymbolClicked);
                        MyStocksActivity.this.startActivity(intent);
                 }
                }));
        recyclerView.setAdapter(mCursorAdapter);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.attachToRecyclerView(recyclerView);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isConnected) {
                    new MaterialDialog.Builder(mContext).title(R.string.symbol_search)
                            .content(R.string.content_test)
                            .inputType(InputType.TYPE_CLASS_TEXT)
                            .input(R.string.input_hint, R.string.input_prefill, new MaterialDialog.InputCallback() {
                                @Override
                                public void onInput(MaterialDialog dialog, CharSequence input) {
                                    // On FAB click, receive user input. Make sure the stock doesn't already exist
                                    // in the DB and proceed accordingly
                                    Cursor c = getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                                            new String[]{QuoteColumns.SYMBOL}, QuoteColumns.SYMBOL + "= ?",
                                            new String[]{input.toString()}, null);
                                    if (c.getCount() != 0) {
                                        Toast toast =
                                                Toast.makeText(MyStocksActivity.this, "This stock is already saved!",
                                                        Toast.LENGTH_LONG);
                                        toast.setGravity(Gravity.CENTER, Gravity.CENTER, 0);
                                        toast.show();
                                        return;
                                    } else {
                                        // Add the stock to DB
                                        mServiceIntent.putExtra("tag", "add");
                                        mServiceIntent.putExtra("symbol", input.toString());
                                        startService(mServiceIntent);
                                    }
                                }
                            })
                            .show();
                } else {
                    networkToast();
                }

            }
        });

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(mCursorAdapter);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(recyclerView);

        mTitle = getTitle();
        if (isConnected) {
            // create a periodic task to pull stocks once every hour after the app has been opened. This
            // is so Widget data stays up to date.
            PeriodicTask periodicTask = new PeriodicTask.Builder()
                    .setService(StockTaskService.class)
                    .setPeriod(period)
                    .setFlex(flex)
                    .setTag(periodicTag)
                    .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
                    .setRequiresCharging(false)
                    .build();
            // Schedule task with tag "periodic." This ensure that only the stocks present in the DB
            // are updated.
            GcmNetworkManager.getInstance(this).schedule(periodicTask);
        }
    }

    private void registerNetworkStateChangeReceiver() {
        // register a broadcast receiver to get notified of a connectivity change
        mNetworkStateChangeReceiver = new NetworkStateChangeReceiver();
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
        registerReceiver(mNetworkStateChangeReceiver, filter);
    }

    private void registerStockReceiver() {
        mStockReceiver = new StockInfoFetchedFailureReceiver();
        IntentFilter filter = new IntentFilter(STOCK_QUOTE_FETCHED_FAILURE);
        filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
        registerReceiver(mStockReceiver, filter);
    }

    public static class StockInfoFetchedFailureReceiver extends BroadcastReceiver {

        private static final String LOG_TAG = "StockInfoFetchedFailureReceiver";

        @Override
        public void onReceive(Context context, Intent intent) {
            LogUtil.log_i(LOG_TAG, "stock info fetched failed...");
            // make changes to UI accordingly
            Toast toast =
                    Toast.makeText(mContext, mContext.getResources().getString(R.string.stock_info_fetched_failed),
                            Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, Gravity.CENTER, 0);
            toast.show();
        }
    }

    private class NetworkStateChangeReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager connectivityManager = (ConnectivityManager)
                    context.getSystemService(Context.CONNECTIVITY_SERVICE);
            //NetworkInfo activeNetInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            NetworkInfo netInfo = connectivityManager.getActiveNetworkInfo();
            boolean isConnected = false;
            if (netInfo != null) {
                isConnected = netInfo != null && netInfo.isConnectedOrConnecting();
                if (isConnected) {
                    Log.i("NET", "connected " + isConnected);
                    // if the empty view is displayed, which means that when the app first started, there was no data fetched, then we should start the fetch service
                    // and the GcmNetworkManager service
                    if (emptyView.getVisibility() == View.VISIBLE) {
                        Intent mServiceIntent = new Intent(context, StockIntentService.class);
                        mServiceIntent.putExtra("tag", "init");
                        startService(mServiceIntent);

                        PeriodicTask periodicTask = new PeriodicTask.Builder()
                                .setService(StockTaskService.class)
                                .setPeriod(period)
                                .setFlex(flex)
                                .setTag(periodicTag)
                                .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
                                .setRequiresCharging(false)
                                .build();
                        // Schedule task with tag "periodic." This ensure that only the stocks present in the DB
                        // are updated.
                        GcmNetworkManager.getInstance(context).schedule(periodicTask);
                    }
                }

            } else {
                Log.i("NET", "connected " + isConnected);
                // alert user that internet is disconnected. stock data might not be up to date
                Toast toast =
                        Toast.makeText(context, getResources().getString(R.string.empty_view),
                                Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, Gravity.CENTER, 0);
                toast.show();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getLoaderManager().restartLoader(CURSOR_LOADER_ID, null, this);
    }

    public void networkToast() {
        Toast.makeText(mContext, getString(R.string.network_toast), Toast.LENGTH_SHORT).show();
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.my_stocks, menu);
        restoreActionBar();
        return true;
    }

    @Override
    protected void onDestroy() {
        unregisterBroadcastReceiver();
        super.onDestroy();
    }

    private void unregisterBroadcastReceiver() {
        unregisterReceiver(mStockReceiver);
        unregisterReceiver(mNetworkStateChangeReceiver);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        if (id == R.id.action_change_units) {
            // this is for changing stock changes from percent value to dollar value
            Utils.showPercent = !Utils.showPercent;
            this.getContentResolver().notifyChange(QuoteProvider.Quotes.CONTENT_URI, null);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This narrows the return to only the stocks that are most current.
        return new CursorLoader(this, QuoteProvider.Quotes.CONTENT_URI,
                new String[]{QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                        QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP},
                QuoteColumns.ISCURRENT + " = ?",
                new String[]{"1"},
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // if list is empty, then we should display the empty view instead
        if (data != null) {
            if (data.moveToFirst()) {
                LogUtil.log_i(LOG_TAG, "item count: " + data.getCount());
                emptyView.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            } else {
                emptyView.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            }
        } else {
            emptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        }
        mCursorAdapter.swapCursor(data);
        mCursor = data;
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mCursorAdapter.swapCursor(null);
    }

}
