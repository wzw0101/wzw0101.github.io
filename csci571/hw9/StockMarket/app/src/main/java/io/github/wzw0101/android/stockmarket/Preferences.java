package io.github.wzw0101.android.stockmarket;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Preferences {
    private static final String APIPREFIX = "https://csci571-hw8-343721.wl.r.appspot.com";
    private static final String TAG = Preferences.class.toString();

    private static final String KEY_QUOTES = "QUOTES";
    private static final String KEY_WATCHLIST = "WATCHLIST";
    private static final String KEY_PORTFOLIO = "PORTFOLIO";
    private static final String KEY_BALANCE = "BALANCE";

    private static final String WATCHLIST_KEY_SYMBOL = "symbol";
    private static final String WATCHLIST_KEY_NAME = "name";
    private static final String PORTFOLIO_KEY_SYMBOL = "symbol";
    private static final String PORTFOLIO_KEY_AVG = "avg";
    private static final String PORTFOLIO_KEY_SHARES = "shares";
    private static final String QUOTES_KEY_C = "c";
    private static final String QUOTES_KEY_D = "d";
    private static final String QUOTES_KEY_DP = "dp";
    private static final String QUOTES_KEY_REFCNT = "refcnt";

    private static Preferences preferences = null;

    private float balance;
    private JSONObject quotes;
    private JSONArray watchlist;
    private JSONArray portfolio;

    private Preferences(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        try {
            watchlist = new JSONArray(preferences.getString(KEY_WATCHLIST, new JSONArray().toString()));
            quotes = new JSONObject(preferences.getString(KEY_QUOTES, new JSONObject().toString()));
            portfolio = new JSONArray(preferences.getString(KEY_PORTFOLIO, new JSONArray().toString()));
            balance = preferences.getFloat(KEY_BALANCE, 25000F);
        } catch (JSONException e) {
            e.printStackTrace();
            watchlist = new JSONArray();
            portfolio = new JSONArray();
            quotes = new JSONObject();
            balance = 0F;
        }
    }

    public static Preferences getInstance(Context context) {
        if (preferences == null) {
            preferences = new Preferences(context);
        }
        return preferences;
    }

    public PortfolioItem getPortfolioItem(String symbol) {
        if (symbol == null) return null;
        int i = findInPortfolio(symbol);
        if (i < 0) return null;
        try {
            JSONObject obj = portfolio.getJSONObject(i);
            PortfolioItem ret = new PortfolioItem();
            ret.symbol = obj.getString(PORTFOLIO_KEY_SYMBOL);
            ret.avg = obj.getDouble(PORTFOLIO_KEY_AVG);
            ret.shares = obj.getInt(PORTFOLIO_KEY_SHARES);
            return ret;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public StockInfo getPortfolioAt(int i) {
        try {
            JSONObject obj = portfolio.getJSONObject(i);
            StockInfo ret = new StockInfo();
            ret.ticker = obj.getString(PORTFOLIO_KEY_SYMBOL);
            ret.desc = String.format("%d shares", obj.getInt(PORTFOLIO_KEY_SHARES));
            JSONObject quote = quotes.getJSONObject(ret.ticker);
            ret.price = quote.getDouble(QUOTES_KEY_C) * obj.getInt(PORTFOLIO_KEY_SHARES);
            ret.d = (quote.getDouble(QUOTES_KEY_C) - obj.getDouble(PORTFOLIO_KEY_AVG)) * obj.getInt(PORTFOLIO_KEY_SHARES);
            ret.dp = (ret.d * 100) / (obj.getDouble(PORTFOLIO_KEY_AVG) * obj.getInt(PORTFOLIO_KEY_SHARES));
            return ret;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public double getNetWorth() {
        double ret = 0D;
        for (int i = 0; i < portfolio.length(); i += 1) {
            ret += getPortfolioAt(i).price;
        }
        return ret + balance;
    }

    public void swapInPortfolio(Context context, int i, int j) {
        if (i < 0 || j < 0 || i >= getPorfolioLength() || j >= getPorfolioLength()) return;
        Object o = this.portfolio.opt(i);
        try {
            this.portfolio.put(i, this.portfolio.opt(j));
            this.portfolio.put(j, o);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        writePortfolio(context);
    }

    public int getPorfolioLength() {
        return this.portfolio.length();
    }

    public boolean canBuy(double unitPrice, int toBuy) {
        return balance >= unitPrice * toBuy;
    }

    public boolean canSell(String symbol, int toSell) {
        try {
            int i = findInPortfolio(symbol);
            if (i < 0) {
                return false;
            }
            return portfolio.getJSONObject(i).getInt(PORTFOLIO_KEY_SHARES) >= toSell;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void sellStock(Context context, String symbol, double unitPrice, int toSell) {
        if (!canSell(symbol, toSell)) return;
        int i = findInPortfolio(symbol);
        if (i < 0) return;
        try {
            JSONObject obj = portfolio.getJSONObject(i);
            int shares = obj.getInt(PORTFOLIO_KEY_SHARES);
            if (shares > toSell) {
                obj.put(PORTFOLIO_KEY_SHARES, shares - toSell);
                portfolio.put(i, obj);
            } else {
                portfolio.remove(i);
                releaseQuote(context, symbol);
            }
            writePortfolio(context);
            updateBalance(context, toSell * unitPrice);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void buyStock(Context context, String symbol, double unitPrice, int toBuy, double d, double dp){
        if (!canBuy(unitPrice, toBuy)){
            return;
        }
        int i = findInPortfolio(symbol);
        if (i >= 0) {
            try {
                JSONObject obj = portfolio.getJSONObject(i);
                double avg = obj.getDouble(PORTFOLIO_KEY_AVG);
                int shares = obj.getInt(PORTFOLIO_KEY_SHARES);
                avg = ((avg * shares) + (toBuy * unitPrice)) / (toBuy + shares);
                obj.put(PORTFOLIO_KEY_SHARES, shares + toBuy);
                obj.put(PORTFOLIO_KEY_AVG, avg);
                portfolio.put(i, obj);
                writePortfolio(context);
                updateBalance(context, -(toBuy * unitPrice));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            try {
                JSONObject obj = new JSONObject()
                        .put(PORTFOLIO_KEY_SYMBOL, symbol)
                        .put(PORTFOLIO_KEY_SHARES, toBuy)
                        .put(PORTFOLIO_KEY_AVG, unitPrice);
                portfolio.put(obj);
                writePortfolio(context);
                updateBalance(context, -(toBuy * unitPrice));
                refQuote(context, symbol, unitPrice, d, dp);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private int findInPortfolio(String symbol) {
        if (symbol == null) {
            return -1;
        }
        for (int i = 0; i < this.portfolio.length(); i += 1) {
            try {
                if (symbol.equals(this.portfolio.getJSONObject(i).getString(PORTFOLIO_KEY_SYMBOL))) {
                    return i;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return -1;
    }

    public float getBalance() {
        return balance;
    }

    public boolean isInWatchlist(String symbol) {
        return findInWatchlist(symbol) >= 0;
    }

    public StockInfo getWatchlistAt(int index) {
        StockInfo ret = new StockInfo();
        try {
            JSONObject watchlistItem = this.watchlist.getJSONObject(index);
            ret.ticker = watchlistItem.getString(WATCHLIST_KEY_SYMBOL);
            ret.desc = watchlistItem.getString(WATCHLIST_KEY_NAME);
            JSONObject quote = this.quotes.getJSONObject(ret.ticker);
            ret.price = quote.getDouble(QUOTES_KEY_C);
            ret.d = quote.getDouble(QUOTES_KEY_D);
            ret.dp = quote.getDouble(QUOTES_KEY_DP);
            return ret;
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    public int getWatchlistLength() {
        return this.watchlist.length();
    }

    public void swapInWatchlist(Context context, int i, int j) {
        if (i < 0 || j < 0 || i >= getWatchlistLength() || j >= getWatchlistLength()) return;
        Object o = this.watchlist.opt(i);
        try {
            this.watchlist.put(i, this.watchlist.opt(j));
            this.watchlist.put(j, o);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        writeWatchlist(context);
    }

    public void removeFromWatchlist(Context context, String symbol) {
        int i = findInWatchlist(symbol);
        if (i < 0) {
            return;
        }
        removeFromWatchlistAt(context, i);
    }

    public void removeFromWatchlistAt(Context context, int i) {
        try {
            Object obj = this.watchlist.remove(i);
            if (obj == null) {
                return;
            }
            String symbol = ((JSONObject) obj).getString(WATCHLIST_KEY_SYMBOL);
            this.writeWatchlist(context);
            releaseQuote(context, symbol);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private int findInWatchlist(String symbol) {
        if (symbol == null) {
            return -1;
        }
        for (int i = 0; i < this.watchlist.length(); i += 1) {
            try {
                if (symbol.equals(this.watchlist.getJSONObject(i).getString(WATCHLIST_KEY_SYMBOL))) {
                    return i;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return -1;
    }

    public void addToWatchlist(Context context, String symbol, String name, double c, double d, double dp) {
        try {
            this.watchlist.put(new JSONObject().put(WATCHLIST_KEY_SYMBOL, symbol).put(WATCHLIST_KEY_NAME, name));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        this.writeWatchlist(context);
        refQuote(context, symbol, c, d, dp);
    }

    private void refQuote(Context context, String symbol, double c, double d, double dp) {
        try {
            int refcnt = this.quotes.has(symbol) ? this.quotes.getJSONObject(symbol).getInt(QUOTES_KEY_REFCNT) : 0;
            this.quotes.put(symbol,
                    new JSONObject()
                            .put(QUOTES_KEY_C, c)
                            .put(QUOTES_KEY_D, d)
                            .put(QUOTES_KEY_DP, dp)
                            .put(QUOTES_KEY_REFCNT, refcnt + 1));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        this.writeQuotes(context);
    }

    private void releaseQuote(Context context, String symbol) {
        JSONObject quote = this.quotes.optJSONObject(symbol);
        if (quote == null) {
            return;
        }
        int refcnt = quote.optInt(QUOTES_KEY_REFCNT);
        if (refcnt < 2) {
            this.quotes.remove(symbol);
        } else {
            try {
                quote.put(QUOTES_KEY_REFCNT, refcnt - 1);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        this.writeQuotes(context);
    }

    public void refreshQuotes(Context context, RequestQueue queue, QuotesRefreshCallback callback) {
        new Runnable() {
            int cnt = Preferences.this.quotes.length();

            @Override
            public void run() {
                JSONArray jsonArray = Preferences.this.quotes.names();
                if (jsonArray != null) {
                    for (int i = 0; i < jsonArray.length(); i += 1) {
                        try {
                            String symbol = jsonArray.getString(i);
                            queue.add(new JsonObjectRequest(
                                    Request.Method.GET,
                                    APIPREFIX + "/api/quote/" + jsonArray.getString(i),
                                    null,
                                    response -> {
                                        JSONObject obj = Preferences.this.quotes.optJSONObject(symbol);
                                        if (obj != null) {
                                            try {
                                                obj.put(QUOTES_KEY_C, response.getDouble("c"))
                                                        .put(QUOTES_KEY_D, response.getDouble("d"))
                                                        .put(QUOTES_KEY_DP, response.getDouble("dp"));
                                                Preferences.this.writeQuotes(context);
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                        cnt -= 1;
                                        if (cnt <= 0) {
                                            callback.onQuotesSynced();
                                        }
                                    },
                                    error -> {
                                        cnt -= 1;
                                        if (cnt <= 0) {
                                            callback.onQuotesSynced();
                                        }
                                    }
                            ));
                        } catch (JSONException e) {
                            e.printStackTrace();
                            cnt -= 1;
                            if (cnt <= 0) {
                                callback.onQuotesSynced();
                            }
                        }
                    }
                } else {
                    callback.onQuotesSynced();
                }
            }
        }.run();
    }

    private void writeWatchlist(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putString(KEY_WATCHLIST, this.watchlist.toString()).apply();
    }

    private void writePortfolio(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putString(KEY_PORTFOLIO, this.portfolio.toString()).apply();
    }

    private void writeQuotes(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putString(KEY_QUOTES, this.quotes.toString()).apply();
    }

    private void updateBalance(Context context, double delta) {
        balance += (float) delta;
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putFloat(KEY_BALANCE, balance).apply();
    }

    public interface QuotesRefreshCallback {
        void onQuotesSynced();
    }

    public void logdAll(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        final String TAG = getClass().toString();
        Log.d(TAG, "quotes json: " + sp.getString(KEY_QUOTES, "EMPTY"));
        Log.d(TAG, "watchlist json: " + sp.getString(KEY_WATCHLIST, "EMPTY"));
        Log.d(TAG, "portfolio json: " + sp.getString(KEY_PORTFOLIO, "EMPTY"));
        Log.d(TAG, "balance: " + sp.getFloat(KEY_BALANCE, 0));
    }
}
