package io.github.wzw0101.android.stockmarket;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.cursoradapter.widget.SimpleCursorAdapter;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.BaseColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class MainActivity extends AppCompatActivity implements Preferences.QuotesRefreshCallback {
    private static final String APIPREFIX = "https://csci571-hw8-343721.wl.r.appspot.com";
    private static final String TAG = MainActivity.class.toString();

    private RequestQueue queue = null;

    private TextView textViewNetWorthValue, textViewCashBalanceValue;
    private PortfolioAdapter portfolioAdapter;
    private FavAdapter favAdapter;
    private ProgressBar progressBar;
    private NestedScrollView mainView;
    private Preferences preferences;

    private Handler handler;
    private Runnable runnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_StockMarket);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setTheme(R.style.Theme_StockMarket);

        this.queue = Volley.newRequestQueue(this);
        this.preferences = Preferences.getInstance(this);

        this.progressBar = findViewById(R.id.progressBar);
        mainView = findViewById(R.id.mainView);

        TextView titleDate = findViewById(R.id.title_date);
        titleDate.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("d LLLL y")));
        initPortfolioSection();
        initFavSection();
        findViewById(R.id.textViewPoweredByFinnhub).setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse("https://finnhub.io/"))));

        // Auto refresher
        handler = new Handler(Looper.getMainLooper());
        runnable = new Runnable() {
            @Override
            public void run() {
                preferences.refreshQuotes(MainActivity.this, MainActivity.this.queue, MainActivity.this);
                handler.postDelayed(this, 15 * 1000);
            }
        };
        handler.postDelayed(runnable, 15 * 1000);

//        preferences.refreshQuotes(this, this.queue, () -> {
//            progressBar.setVisibility(View.GONE);
//            mainView.setVisibility(View.VISIBLE);
//        });

        handler.postDelayed(() -> {
            progressBar.setVisibility(View.GONE);
            mainView.setVisibility(View.VISIBLE);
        }, 3 * 1000);

        Log.d(TAG, "onCreate invoked");
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.d(TAG, "onResume invoked");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause invoked");
    }

    @Override
    protected void onStart() {
        super.onStart();
        favAdapter.notifyItemRangeChanged(0, favAdapter.getItemCount());
        portfolioAdapter.notifyItemRangeChanged(0, portfolioAdapter.getItemCount());
        updatePorfolioBalance();
        Log.d(TAG, "onStart invoked");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(getClass().toString(), "onStop invoked");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(runnable);
        Log.d(getClass().toString(), "onDestroy invoked");
    }

    private void initPortfolioSection() {
        textViewNetWorthValue = findViewById(R.id.textViewNetWorthValue);
        textViewCashBalanceValue = findViewById(R.id.textViewCashBalanceValue);
        updatePorfolioBalance();

        RecyclerView portfolio = findViewById(R.id.portfolioRecyclerView);
        portfolio.setLayoutManager(new LinearLayoutManager(this));
        this.portfolioAdapter = new PortfolioAdapter();
        ItemTouchHelper portfolioHelper = new ItemTouchHelper(new TouchCallback(this, this.portfolioAdapter));
        portfolioHelper.attachToRecyclerView(portfolio);
        portfolio.setAdapter(this.portfolioAdapter);
    }

    private void updatePorfolioBalance() {
        textViewNetWorthValue.setText(String.format("$%.2f", preferences.getNetWorth()));
        textViewCashBalanceValue.setText(String.format("$%.2f", preferences.getBalance()));
    }

    private void initFavSection() {
        RecyclerView fav = findViewById(R.id.favRecyclerView);
        fav.setLayoutManager(new LinearLayoutManager(this));
        this.favAdapter = new FavAdapter();
        ItemTouchHelper favHelper = new ItemTouchHelper(new TouchCallback(this, this.favAdapter));
        favHelper.attachToRecyclerView(fav);
        fav.setAdapter(this.favAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity_options, menu);

        // Associate searchable configuration with the SearchView
        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView =
                (SearchView) menu.findItem(R.id.app_bar_search).getActionView();
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));
        searchView.setSuggestionsAdapter(new SimpleCursorAdapter(this, android.R.layout.simple_list_item_1, null, new String[]{SearchManager.SUGGEST_COLUMN_TEXT_1}, new int[]{android.R.id.text1}, 0));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Log.d(getClass().toString(), "onQueryTextChange: query text changed to " + newText);
                if (newText.length() > 0) {
                    JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET,
                            APIPREFIX + "/api/search/" + newText,
                            null,
                            response -> {
                                try {
                                    String[] autocompleteColNames = new String[]{BaseColumns._ID, SearchManager.SUGGEST_COLUMN_TEXT_1};
                                    MatrixCursor cursor = new MatrixCursor(autocompleteColNames);
                                    JSONArray array = response.getJSONArray("result");
                                    for (int i = 0; i < array.length(); i += 1) {
                                        JSONObject item = array.getJSONObject(i);
                                        if ("Common Stock".equals(item.getString("type")) && !item.getString("symbol").contains(".")) {
                                            String term = String.format("%s | %s", item.getString("symbol"), item.getString("description"));
                                            cursor.addRow(new Object[]{i, term});
                                        }
                                    }
                                    searchView.getSuggestionsAdapter().changeCursor(cursor);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            },
                            Throwable::printStackTrace);
                    request.setRetryPolicy(new DefaultRetryPolicy(15 * 1000, 3, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
                    queue.add(request);
                } else {
                    searchView.getSuggestionsAdapter().changeCursor(null);
                }
                return true;
            }
        });
        searchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
            @Override
            public boolean onSuggestionSelect(int position) {
                Cursor cursor = (Cursor) searchView.getSuggestionsAdapter().getItem(position);
                String term = cursor.getString(cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_TEXT_1));
                cursor.close();
                startActivity(SearchableActivity.newIntent(MainActivity.this, term));
                return true;
            }

            @Override
            public boolean onSuggestionClick(int position) {
                return this.onSuggestionSelect(position);
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onQuotesSynced() {
        this.favAdapter.notifyItemRangeChanged(0, this.favAdapter.getItemCount());
        this.portfolioAdapter.notifyItemRangeChanged(0, this.portfolioAdapter.getItemCount());
        updatePorfolioBalance();
    }

    private class PortfolioHolder extends RecyclerView.ViewHolder {

        private TextView tickerTextView;
        private TextView descTextView;
        private TextView priceTextView;
        private TextView deltaTextView;
        private ImageView trending;
        private ImageButton btnGo;

        public PortfolioHolder(@NonNull View itemView) {
            super(itemView);
            this.tickerTextView = this.itemView.findViewById(R.id.ticker);
            this.descTextView = this.itemView.findViewById(R.id.desc);
            this.priceTextView = this.itemView.findViewById(R.id.price);
            this.deltaTextView = this.itemView.findViewById(R.id.delta);
            btnGo = this.itemView.findViewById(R.id.btnGo);
            trending = this.itemView.findViewById(R.id.imageViewTrending);
        }

        public void bind(StockInfo info) {
            info.d = Math.round(info.d * 100.0d) / 100.0d;
            info.dp = Math.round(info.dp * 100.0d) / 100.0d;

            tickerTextView.setText(info.ticker);
            descTextView.setText(info.desc);
            priceTextView.setText(String.format("$%.2f", info.price));
            deltaTextView.setText(String.format("$%.2f(%.2f%%)", info.d, info.dp));
            btnGo.setOnClickListener(v -> startActivity(SearchableActivity.newIntent(MainActivity.this, info.ticker)));
            if (info.d >= 0.01) {
                deltaTextView.setTextColor(getResources().getColor(R.color.colorSuccess, null));
                trending.setImageResource(R.drawable.trending_up);
            } else if (info.d <= -0.01) {
                deltaTextView.setTextColor(getResources().getColor(R.color.colorDanger, null));
                trending.setImageResource(R.drawable.trending_down);
            } else {
                deltaTextView.setTextColor(getResources().getColor(R.color.black, null));
                trending.setImageResource(0);
            }
        }
    }

    private class PortfolioAdapter extends RecyclerView.Adapter<PortfolioHolder> implements TouchCallback.Helper {

        @NonNull
        @Override
        public PortfolioHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_stock_info, parent, false);
            return new PortfolioHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PortfolioHolder holder, int position) {
            holder.bind(preferences.getPortfolioAt(position));
        }

        @Override
        public int getItemCount() {
            return preferences.getPorfolioLength();
        }

        @Override
        public void onRowMoved(int fromPosition, int toPosition) {
            if (fromPosition < toPosition) {
                for (int i = fromPosition; i < toPosition; i++) {
                    preferences.swapInPortfolio(MainActivity.this, i, i + 1);
                }
            } else {
                for (int i = fromPosition; i > toPosition; i--) {
                    preferences.swapInPortfolio(MainActivity.this, i, i - 1);
                }
            }
            notifyItemMoved(fromPosition, toPosition);
        }

        @Override
        public void onRowSwiped(int position) {

        }

        @Override
        public boolean canSwipe() {
            return false;
        }
    }

    private class FavHolder extends RecyclerView.ViewHolder {

        private final TextView tickerTextView;
        private final TextView descTextView;
        private final TextView priceTextView;
        private final TextView deltaTextView;
        private final ImageButton btnGo;
        private ImageView trending;
        private String ticker;

        public FavHolder(@NonNull View itemView) {
            super(itemView);
            this.tickerTextView = this.itemView.findViewById(R.id.ticker);
            this.descTextView = this.itemView.findViewById(R.id.desc);
            this.priceTextView = this.itemView.findViewById(R.id.price);
            this.deltaTextView = this.itemView.findViewById(R.id.delta);
            this.btnGo = this.itemView.findViewById(R.id.btnGo);
            trending = this.itemView.findViewById(R.id.imageViewTrending);
        }

        public void bind(StockInfo info) {
            info.d = Math.round(info.d * 100.0d) / 100.0d;
            info.dp = Math.round(info.dp * 100.0d) / 100.0d;

            tickerTextView.setText(info.ticker);
            descTextView.setText(info.desc);
            priceTextView.setText(String.format("$%.2f", info.price));
            deltaTextView.setText(String.format("$%.2f(%.2f%%)", info.d, info.dp));
            btnGo.setOnClickListener(v -> startActivity(SearchableActivity.newIntent(MainActivity.this, info.ticker)));
            ticker = info.ticker;
            if (info.d >= 0.01) {
                deltaTextView.setTextColor(getResources().getColor(R.color.colorSuccess, null));
                trending.setImageResource(R.drawable.trending_up);
            } else if (info.d <= -0.01) {
                deltaTextView.setTextColor(getResources().getColor(R.color.colorDanger, null));
                trending.setImageResource(R.drawable.trending_down);
            } else {
                deltaTextView.setTextColor(getResources().getColor(R.color.black, null));
                trending.setImageResource(0);
            }
        }

        public String getTicker() {
            return ticker;
        }
    }

    private class FavAdapter extends RecyclerView.Adapter<FavHolder> implements TouchCallback.Helper {

        private final Preferences preferences = Preferences.getInstance(MainActivity.this);

        @NonNull
        @Override
        public FavHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_stock_info, parent, false);
            return new FavHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull FavHolder holder, int position) {
            StockInfo stockInfo = preferences.getWatchlistAt(position);
            if (stockInfo == null) {
                return;
            }
            holder.bind(stockInfo);
        }

        @Override
        public int getItemCount() {
            return this.preferences.getWatchlistLength();
        }

        @Override
        public void onRowMoved(int fromPosition, int toPosition) {

            if (fromPosition < toPosition) {
                for (int i = fromPosition; i < toPosition; i++) {
                    this.preferences.swapInWatchlist(MainActivity.this, i, i + 1);
                }
            } else {
                for (int i = fromPosition; i > toPosition; i--) {
                    this.preferences.swapInWatchlist(MainActivity.this, i, i - 1);
                }
            }
            notifyItemMoved(fromPosition, toPosition);
        }

        @Override
        public void onRowSwiped(int position) {
            preferences.removeFromWatchlistAt(MainActivity.this, position);
            notifyItemRemoved(position);
        }

        @Override
        public boolean canSwipe() {
            return true;
        }


    }

    private static class TouchCallback extends ItemTouchHelper.Callback {

        private interface Helper {
            void onRowMoved(int fromPosition, int toPosition);

            void onRowSwiped(int position);

            boolean canSwipe();
        }

        private Helper helper;
        private Paint mClearPaint;
        private ColorDrawable mBackground;
        private int backgroundColor;
        private Drawable deleteDrawable;
        private int intrinsicWidth;
        private int intrinsicHeight;

        public TouchCallback(Context context, Helper helper) {
            this.helper = helper;
            mBackground = new ColorDrawable();
            backgroundColor = Color.parseColor("#ff0000");
            mClearPaint = new Paint();
            mClearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
            deleteDrawable = ContextCompat.getDrawable(context, R.drawable.delete);
            intrinsicWidth = deleteDrawable.getIntrinsicWidth();
            intrinsicHeight = deleteDrawable.getIntrinsicHeight();
        }

        @Override
        public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            if (helper.canSwipe())
                return makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN, ItemTouchHelper.LEFT);
            else
                return makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            this.helper.onRowMoved(viewHolder.getAdapterPosition(), target.getAdapterPosition());
            return true;
        }

        @Override
        public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

            View itemView = viewHolder.itemView;
            int itemHeight = itemView.getHeight();

            boolean isCancelled = dX == 0 && !isCurrentlyActive;

            if (isCancelled) {
                clearCanvas(c, itemView.getRight() + dX, (float) itemView.getTop(), (float) itemView.getRight(), (float) itemView.getBottom());
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                return;
            }

            mBackground.setColor(backgroundColor);
            mBackground.setBounds(itemView.getRight() + (int) dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
            mBackground.draw(c);

            int deleteIconTop = itemView.getTop() + (itemHeight - intrinsicHeight) / 2;
            int deleteIconMargin = (itemHeight - intrinsicHeight) / 2;
            int deleteIconLeft = itemView.getRight() - deleteIconMargin - intrinsicWidth;
            int deleteIconRight = itemView.getRight() - deleteIconMargin;
            int deleteIconBottom = deleteIconTop + intrinsicHeight;


            deleteDrawable.setBounds(deleteIconLeft, deleteIconTop, deleteIconRight, deleteIconBottom);
            deleteDrawable.draw(c);

            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }

        private void clearCanvas(Canvas c, Float left, Float top, Float right, Float bottom) {
            c.drawRect(left, top, right, bottom, mClearPaint);

        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            this.helper.onRowSwiped(viewHolder.getAdapterPosition());
        }

        @Override
        public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
            return 0.7f;
        }
    }
}