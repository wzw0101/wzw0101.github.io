package io.github.wzw0101.android.stockmarket;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.core.text.HtmlCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewClientCompat;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class SearchableActivity extends AppCompatActivity implements TradeDialog.NoticeDialogListener {
    private static final String APIPREFIX = "https://csci571-hw8-343721.wl.r.appspot.com";
    private static final String TAG = SearchableActivity.class.toString();

    private int cnt;
    private String query, name;
    private double c, d, dp;
    private RequestQueue queue = null;
    private Preferences preferences = null;

    private NestedScrollView detailHolder = null;
    private ProgressBar progressBar = null;
    private TextView textViewSharesOwnedValue;
    private TextView textViewAvgCostPerShareValue;
    private TextView textViewTotalCostValue;
    private TextView textViewChangesValue;
    private TextView textViewMarketValueValue;

    public static Intent newIntent(Context context, String query) {
        return new Intent(context, SearchableActivity.class)
                .setAction(Intent.ACTION_SEARCH)
                .putExtra(SearchManager.QUERY, query);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_searchable);

        this.queue = Volley.newRequestQueue(this);
        this.preferences = Preferences.getInstance(this);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        this.detailHolder = findViewById(R.id.detail_holder);
        this.progressBar = findViewById(R.id.progressBar2);

        // Get the intent, verify the action and get the query
        Intent intent = getIntent();
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String[] terms = intent.getStringExtra(SearchManager.QUERY).split(" \\| ");
            this.query = terms[0];
            actionBar.setTitle(this.query);
            initCompanyProfileInfo();

            initViewPager();
            initAboutSectionCompanyPeers();
            initInsightsSection();
            initNewsSection();
        } else {
            this.progressBar.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options_searchable, menu);
        MenuItem menuItem = menu.findItem(R.id.action_favorite);
        if (this.preferences.isInWatchlist(this.query)) {
            menuItem.setIcon(R.drawable.ic_fav);
        } else {
            menuItem.setIcon(R.drawable.ic_not_fav);
        }
        return super.onCreateOptionsMenu(menu);
    }

    private void initCompanyProfileInfo() {
        String url = APIPREFIX + "/api/company/" + this.query;
        requestBegin();
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        this.name = response.getString("name");
                        String logo = response.getString("logo");
                        if (!"".equals(logo))
                            Picasso.get()
                                    .load(logo)
                                    .into((ImageView) findViewById(R.id.company_image_image_view));
                        ((TextView) findViewById(R.id.company_ticker_text_view)).setText(response.getString("ticker"));
                        ((TextView) findViewById(R.id.company_name_text_view)).setText(response.getString("name"));
                        initCompanyQuote();
                        initAboutSection(response.getString("ipo"), response.getString("finnhubIndustry"), response.getString("weburl"));
                        initSocialSentimentsTableComapanyName();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    } finally {
                        requestEnd();
                    }
                },
                error -> {
                    error.printStackTrace();
                    requestEnd();
                });
        request.setRetryPolicy(new DefaultRetryPolicy(15 * 1000, 3, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(request);
    }

    private void initCompanyQuote() {
        String url = APIPREFIX + "/api/quote/" + this.query;
        requestBegin();
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        c = response.getDouble("c");
                        dp = response.getDouble("dp");
                        d = response.getDouble("d");

                        ((TextView) findViewById(R.id.current_price_text_view))
                                .setText(String.format("$%.2f", c));
                        TextView textViewDeltaPrice = findViewById(R.id.delta_price_text_view);
                        textViewDeltaPrice.setText(String.format("$%.2f(%.2f%%)", d, dp));

                        ImageView trending = ((ImageView) findViewById(R.id.trending_image_view));
                        if (d > 0) {
                            textViewDeltaPrice.setTextColor(getResources().getColor(R.color.colorSuccess, null));
                            trending.setImageResource(R.drawable.trending_up);
                        } else if (d < 0) {
                            textViewDeltaPrice.setTextColor(getResources().getColor(R.color.colorDanger, null));
                            trending.setImageResource(R.drawable.trending_down);
                        } else {
                            textViewDeltaPrice.setTextColor(getResources().getColor(R.color.black, null));
                            trending.setImageResource(0);
                        }

                        initPortfolioSection();

                        double o = response.getDouble("o"),
                                h = response.getDouble("h"),
                                l = response.getDouble("l"),
                                pc = response.getDouble("pc");
                        initStatsSection(o, h, l, pc);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    } finally {
                        requestEnd();
                    }
                },
                error -> {
                    error.printStackTrace();
                    requestEnd();
                });
        request.setRetryPolicy(new DefaultRetryPolicy(15 * 1000, 3, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(request);
    }

    private void initViewPager() {
        ViewPager2 pager = findViewById(R.id.pager);
        pager.setAdapter(new ViewPager2Adapter(this, this.query));
        TabLayout tabLayout = findViewById(R.id.tab_layout);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {

            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    tab.setIcon(R.drawable.chart_line_purple);
                } else {
                    tab.setIcon(R.drawable.clock_time_three_purple);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    tab.setIcon(R.drawable.chart_line);
                } else {
                    tab.setIcon(R.drawable.clock_time_three);
                }
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
        new TabLayoutMediator(tabLayout, pager,
                (tab, position) -> {
                    if (position > 0) tab.setIcon(R.drawable.clock_time_three);
                    else tab.setIcon(R.drawable.chart_line);
                }
        ).attach();
    }

    private void initPortfolioSection() {
        findViewById(R.id.tradeButton).setOnClickListener(v -> TradeDialog.getInstance(query, name, c, d, dp).show(getSupportFragmentManager(), "trade"));
        textViewSharesOwnedValue = findViewById(R.id.textViewSharesOwnedValue);
        textViewAvgCostPerShareValue = findViewById(R.id.textViewAvgCostPerShareValue);
        textViewTotalCostValue = findViewById(R.id.textViewTotalCostValue);
        textViewChangesValue = findViewById(R.id.textViewChangesValue);
        textViewMarketValueValue = findViewById(R.id.textViewMarketValueValue);
        updatePortfolioSectionData();
    }

    private void updatePortfolioSectionData() {
        PortfolioItem item = preferences.getPortfolioItem(query);
        if (item == null) {
            textViewSharesOwnedValue.setText(getString(R.string.default_int_value));
            textViewAvgCostPerShareValue.setText(getString(R.string.default_empty_price));
            textViewTotalCostValue.setText(getString(R.string.default_empty_price));
            textViewChangesValue.setText(R.string.default_empty_price);
            textViewMarketValueValue.setText(R.string.default_empty_price);
            textViewChangesValue.setTextColor(getResources().getColor(R.color.black, null));
            textViewMarketValueValue.setTextColor(getResources().getColor(R.color.black, null));
        } else {
            textViewSharesOwnedValue.setText(String.valueOf(item.shares));
            textViewAvgCostPerShareValue.setText(String.format("$%.2f", item.avg));
            textViewTotalCostValue.setText(String.format("$%.2f", item.shares * item.avg));
            textViewChangesValue.setText(String.format("$%.2f", (c - item.avg) * item.shares));
            textViewMarketValueValue.setText(String.format("$%.2f", item.shares * c));
            if (c - item.avg >= 0.01d) {
                textViewChangesValue.setTextColor(getResources().getColor(R.color.colorSuccess, null));
                textViewMarketValueValue.setTextColor(getResources().getColor(R.color.colorSuccess, null));
            } else if (c - item.avg <= -0.01d) {
                textViewChangesValue.setTextColor(getResources().getColor(R.color.colorDanger, null));
                textViewMarketValueValue.setTextColor(getResources().getColor(R.color.colorDanger, null));
            } else {
                textViewChangesValue.setTextColor(getResources().getColor(R.color.black, null));
                textViewMarketValueValue.setTextColor(getResources().getColor(R.color.black, null));
            }
        }
    }

    private void initStatsSection(double o, double h, double l, double pc) {
        ((TextView) findViewById(R.id.textViewOpenPriceValue)).setText(String.format("$%.2f", o));
        ((TextView) findViewById(R.id.textViewHighPriceValue)).setText(String.format("$%.2f", h));
        ((TextView) findViewById(R.id.textViewLowPriceValue)).setText(String.format("$%.2f", l));
        ((TextView) findViewById(R.id.textViewPrevCloseValue)).setText(String.format("$%.2f", pc));
    }

    private void initAboutSection(String ipoStartDate, String industry, String webpage) {
        ((TextView) findViewById(R.id.textViewIPOStartDateValue)).setText(ipoStartDate);
        ((TextView) findViewById(R.id.textViewIndustryValue)).setText(industry);
        TextView textView = findViewById(R.id.textViewWebpageValue);
        textView.setText(HtmlCompat.fromHtml(String.format("<a href='#'>%s</a>", webpage), HtmlCompat.FROM_HTML_MODE_LEGACY));
        textView.setOnClickListener(v -> startActivity(new Intent().setAction(Intent.ACTION_VIEW).setData(Uri.parse(webpage))));
    }

    private void initAboutSectionCompanyPeers() {
        String url = APIPREFIX + "/api/peers/" + this.query;
        requestBegin();
        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    LinearLayoutCompat linearLayout = findViewById(R.id.peersGroup);
                    for (int i = 0; i < response.length(); i += 1) {
                        try {
                            TextView textView = new TextView(this);
                            String peer = response.getString(i);
                            textView.setText(HtmlCompat.fromHtml(
                                    String.format("<a href='#'>%s</a>", peer),
                                    HtmlCompat.FROM_HTML_MODE_LEGACY
                            ));

                            textView.setOnClickListener(v -> startActivity(newIntent(this, peer)));
                            LinearLayoutCompat.LayoutParams layoutParams = new LinearLayoutCompat.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                            layoutParams.setMarginEnd(10);
                            linearLayout.addView(textView, layoutParams);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    requestEnd();
                },
                error -> {
                    error.printStackTrace();
                    requestEnd();
                }
        );
        request.setRetryPolicy(new DefaultRetryPolicy(15 * 1000, 3, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(request);
    }

    private void initInsightsSection() {
        initSocialSentimentsTable();
        initWebViewRecommendationTrends();
        initWebViewHistoricalEPSSurprises();
    }

    private void initSocialSentimentsTable() {
        String url = APIPREFIX + "/api/social/" + query;
        requestBegin();
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray reddit = response.getJSONArray("reddit");
                        int redditTotle = 0, redditPositiveTotal = 0, redditNegativeTotal = 0;
                        for (int i = 0; i < reddit.length(); i += 1) {
                            JSONObject obj = reddit.getJSONObject(i);
                            redditTotle += obj.getInt("mention");
                            redditPositiveTotal += obj.getInt("positiveMention");
                            redditNegativeTotal += obj.getInt("negativeMention");
                        }
                        ((TextView) findViewById(R.id.textViewTotalMentionsRedditValue)).setText(String.valueOf(redditTotle));
                        ((TextView) findViewById(R.id.textViewPositiveMentionsRedditValue)).setText(String.valueOf(redditPositiveTotal));
                        ((TextView) findViewById(R.id.textViewNegativeMentionsRedditValue)).setText(String.valueOf(redditNegativeTotal));

                        JSONArray twitter = response.getJSONArray("twitter");
                        int twitterTotal = 0, twitterPositiveTotal = 0, twitterNegativeTotal = 0;
                        for (int i = 0; i < twitter.length(); i += 1) {
                            JSONObject obj = twitter.getJSONObject(i);
                            twitterTotal += obj.getInt("mention");
                            twitterPositiveTotal += obj.getInt("positiveMention");
                            twitterNegativeTotal += obj.getInt("negativeMention");
                        }
                        ((TextView) findViewById(R.id.textViewTotalMentionsTwitterValue)).setText(String.valueOf(twitterTotal));
                        ((TextView) findViewById(R.id.textViewPositiveMentionsTwitterValue)).setText(String.valueOf(twitterPositiveTotal));
                        ((TextView) findViewById(R.id.textViewNegativeMentionsTwitterValue)).setText(String.valueOf(twitterNegativeTotal));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    } finally {
                        requestEnd();
                    }
                },
                error -> {
                    error.printStackTrace();
                    requestEnd();
                });
        request.setRetryPolicy(new DefaultRetryPolicy(15 * 1000, 3, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(request);

    }

    private void initSocialSentimentsTableComapanyName() {
        ((TextView) findViewById(R.id.textViewSocialSentimentsCompanyName)).setText(name);
    }

    private void initWebViewRecommendationTrends() {
        WebView webView = findViewById(R.id.webViewRecommendationTrends);
        webView.setBackgroundColor(Color.TRANSPARENT);
        webView.getSettings().setJavaScriptEnabled(true);

        WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                .setDomain("csci571-hw8-343721.wl.r.appspot.com")
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();
        webView.setWebViewClient(new LocalContentWebViewClient(assetLoader));
        webView.addJavascriptInterface(new IMessenger() {
            @NonNull
            @Override
            @JavascriptInterface
            public String getSymbol() {
                return query;
            }
        }, "query");
        webView.loadUrl("https://csci571-hw8-343721.wl.r.appspot.com/assets/recommendation-trends-chart.html");
    }

    private void initWebViewHistoricalEPSSurprises() {
        WebView webView = findViewById(R.id.webViewHistoricalEPSSurprises);
        webView.setBackgroundColor(Color.TRANSPARENT);
        webView.getSettings().setJavaScriptEnabled(true);

        WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                .setDomain("csci571-hw8-343721.wl.r.appspot.com")
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();
        webView.setWebViewClient(new LocalContentWebViewClient(assetLoader));
        webView.addJavascriptInterface(new IMessenger() {
            @NonNull
            @Override
            @JavascriptInterface
            public String getSymbol() {
                return query;
            }
        }, "query");
        webView.loadUrl("https://csci571-hw8-343721.wl.r.appspot.com/assets/historical-eps-surprise-chart.html");
    }

    private void initNewsSection() {
        RecyclerView recyclerView = findViewById(R.id.recyclerViewNewsList);
        NewsAdapter adapter = new NewsAdapter(null);
        recyclerView.setAdapter(adapter);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(recyclerView.getContext());
        recyclerView.setLayoutManager(layoutManager);

        LocalDate now = LocalDate.now(), before = now.minusMonths(1);
        String url = String.format("%s/api/news/%s?from=%s&to=%s", APIPREFIX, query, before, now);
        Log.d(TAG, "initNewsSection: " + url);
        requestBegin();
        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    Log.d(TAG, "initNewsSection: " + response.toString());
                    List<NewsInfo> newsInfoArr = new ArrayList<>();
                    for (int i = 0; i < response.length(); i += 1) {
                        try {
                            JSONObject obj = response.getJSONObject(i);
                            NewsInfo info = new NewsInfo();
                            info.datetime = obj.getLong("datetime");
                            info.headline = obj.getString("headline");
                            info.image = obj.getString("image");
                            info.source = obj.getString("source");
                            info.summary = obj.getString("summary");
                            info.url = obj.getString("url");
                            if (info.datetime > 0
                                    && !"".equals(info.headline)
                                    && !"".equals(info.image)
                                    && !"".equals(info.source)
                                    && !"".equals(info.summary)
                                    && !"".equals(info.url)) {
                                newsInfoArr.add(info);
                            }
                            if (newsInfoArr.size() >= 20)
                                break;
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    adapter.setNewsInfoList(newsInfoArr);
                    adapter.notifyItemRangeChanged(0, adapter.getItemCount());
                    requestEnd();
                },
                error -> {
                    error.printStackTrace();
                    requestEnd();
                });
        request.setRetryPolicy(new DefaultRetryPolicy(15 * 1000, 3, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(request);
    }

    private void requestBegin() {
        cnt += 1;
    }

    private void requestEnd() {
        cnt -= 1;
        if (cnt <= 0) {
            this.progressBar.setVisibility(View.GONE);
            this.detailHolder.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_favorite:
                if (this.preferences.isInWatchlist(this.query)) {
                    this.preferences.removeFromWatchlist(this, this.query);
                    item.setIcon(R.drawable.ic_not_fav);
                    Toast.makeText(this, query + " is removed from favorites", Toast.LENGTH_SHORT).show();
                } else {
                    this.preferences.addToWatchlist(this, this.query, this.name, this.c, this.d, this.dp);
                    item.setIcon(R.drawable.ic_fav);
                    Toast.makeText(this, query + " is added to favorites", Toast.LENGTH_SHORT).show();
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBtnClicked() {
        updatePortfolioSectionData();
    }

    private class NewsViewHolder extends RecyclerView.ViewHolder {

        private TextView textViewArticleSrc, textViewArticleTitle, textViewElapsedTime;
        private ImageView imageViewArticleImage;

        public NewsViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewArticleSrc = itemView.findViewById(R.id.textViewArticleSrc);
            textViewArticleTitle = itemView.findViewById(R.id.textViewArticleTitle);
            textViewElapsedTime = itemView.findViewById(R.id.textViewElapsedTime);
            imageViewArticleImage = itemView.findViewById(R.id.imageViewArticleImage);
            imageViewArticleImage.setClipToOutline(true);
        }

        public void bind(NewsInfo info) {
            textViewArticleSrc.setText(info.source);
            textViewArticleTitle.setText(info.headline);
            Instant now = Instant.now(), then = Instant.ofEpochSecond(info.datetime);
            Duration duration = Duration.between(then, now);
            if (duration.toHours() > 0) {
                textViewElapsedTime.setText(String.format("%d hours ago", duration.toHours()));
            } else if (duration.toMinutes() > 0) {
                textViewElapsedTime.setText(String.format("%d minutes ago", duration.toMinutes()));
            } else {
                textViewElapsedTime.setText(String.format("%d seconds ago", duration.getSeconds()));
            }
            Glide.with(itemView).load(info.image).centerCrop().into(imageViewArticleImage);

            itemView.setOnClickListener(v -> {
                NewsDialog.getInstance(info).show(getSupportFragmentManager(), "news");
            });
        }

    }

    private class NewsAdapter extends RecyclerView.Adapter<NewsViewHolder> {

        private List<NewsInfo> list;

        public NewsAdapter(List<NewsInfo> list) {
            setNewsInfoList(list);
        }

        @NonNull
        @Override
        public NewsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            int rid = viewType > 0 ? R.layout.item_top_news : R.layout.item_regular_news;
            View view = LayoutInflater.from(parent.getContext()).inflate(rid, parent, false);
            return new NewsViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull NewsViewHolder holder, int position) {
            holder.bind(list.get(position));
        }

        @Override
        public int getItemViewType(int position) {
            return position > 0 ? 0 : 1;
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        public void setNewsInfoList(List<NewsInfo> list) {
            this.list = list == null ? new ArrayList<>() : list;
        }
    }

    private static class ViewPager2Adapter extends FragmentStateAdapter {
        private final String query;

        public ViewPager2Adapter(FragmentActivity activity, String query) {
            super(activity);
            this.query = query;
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            Fragment fragment = new ViewPager2Fragment();
            Bundle args = new Bundle();
            args.putInt(ViewPager2Fragment.ARG_POS, position);
            args.putString(ViewPager2Fragment.ARG_QUERY, this.query);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }

    // Instances of this class are fragments representing a single
    // object in our collection.
    public static class ViewPager2Fragment extends Fragment {
        public static final String ARG_POS = "pos";
        public static final String ARG_QUERY = "query";

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_viewpager_object, container, false);
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            Bundle args = requireArguments();
            String query = args.getString(ARG_QUERY);
            WebView webView = view.findViewById(R.id.web_view);
            webView.setBackgroundColor(Color.TRANSPARENT);
            webView.getSettings().setJavaScriptEnabled(true);

            WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                    .setDomain("csci571-hw8-343721.wl.r.appspot.com")
                    .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(requireContext()))
                    .build();
            webView.setWebViewClient(new LocalContentWebViewClient(assetLoader));
            webView.addJavascriptInterface(new IMessenger() {
                @NonNull
                @Override
                @JavascriptInterface
                public String getSymbol() {
                    return query;
                }
            }, "query");
            if (args.getInt(ARG_POS) > 0) {
                webView.loadUrl("https://csci571-hw8-343721.wl.r.appspot.com/assets/historical-chart.html");
            } else {
                webView.loadUrl("https://csci571-hw8-343721.wl.r.appspot.com/assets/hourly-chart.html");
            }
        }
    }

    private interface IMessenger {
        String getSymbol();
    }

    private static class LocalContentWebViewClient extends WebViewClientCompat {

        private final WebViewAssetLoader mAssetLoader;

        LocalContentWebViewClient(WebViewAssetLoader assetLoader) {
            mAssetLoader = assetLoader;
        }

        @Override
        @RequiresApi(21)
        public WebResourceResponse shouldInterceptRequest(WebView view,
                                                          WebResourceRequest request) {
            return mAssetLoader.shouldInterceptRequest(request.getUrl());
        }

        @Override
        @SuppressWarnings("deprecation") // to support API < 21
        public WebResourceResponse shouldInterceptRequest(WebView view,
                                                          String url) {
            return mAssetLoader.shouldInterceptRequest(Uri.parse(url));
        }
    }
}

