import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { of as Of, interval, startWith, Subscription } from 'rxjs'
import { Router } from '@angular/router';
import * as Highcharts from 'highcharts/highstock';
import IndicatorsCore from "highcharts/indicators/indicators";
import IndicatorVbp from "highcharts/indicators/volume-by-price";
IndicatorsCore(Highcharts);
IndicatorVbp(Highcharts);

import { CompanyProfile } from './model/company-profile.model';
import { SearchResp } from './model/search.model';
import { Quote } from './model/quote.model';
import { Candle } from './model/candle.model';
import { News } from './model/news.model';
import { Mention } from './model/mention.model';
import { Recommendation } from './model/recommendation.model';
import { Earnings } from './model/earnings.model';

@Injectable({
  providedIn: 'root'
})
export class SearchService {
  prevSymbol = '';

  searching = false;
  failed = false;
  failOnEmpty = false;

  companyProfile?: CompanyProfile;
  quote?: Quote;
  peers?: string[];

  Highcharts = Highcharts;
  summaryChartOptions: Highcharts.Options = {
    rangeSelector: {
      enabled: false,
    },

    navigator: {
      enabled: false,
    },

    time: {
      useUTC: false,
    },

    series: [{
      type: 'line',
      data: []
    }],
  }
  summaryChartUpdate = false;

  closed = false;
  intervalSubscription?: Subscription;

  chartsOptions: Highcharts.Options = {

    subtitle: {
      text: 'With SMA and Volume by Price technical indicators'
    },

    yAxis: [{
      startOnTick: false,
      endOnTick: false,
      labels: {
        align: 'right',
        x: -3
      },
      title: {
        text: 'OHLC'
      },
      height: '60%',
      lineWidth: 2,
      resize: {
        enabled: true
      }
    }, {
      labels: {
        align: 'right',
        x: -3
      },
      title: {
        text: 'Volume'
      },
      top: '65%',
      height: '35%',
      offset: 0,
      lineWidth: 2
    }],

    tooltip: {
      split: true
    },


    series: [{
      type: 'candlestick',
      id: 'ohlc',
      data: []
    }, {
      type: 'column',
      id: 'volume',
      data: []
    }, {
      type: 'vbp',
      linkedTo: 'ohlc',
      params: {
        volumeSeriesID: 'volume'
      },
    }, {
      type: 'sma',
      linkedTo: 'ohlc',
    }]
  };
  chartsUpdate = false;

  redditMention?: Mention;
  twitterMention?: Mention;

  newsList?: News[];

  recommendationsChartOption: Highcharts.Options = {

    title: {
      text: 'Recommendation Trends'
    },

    plotOptions: {
      column: {
        stacking: 'normal',
        dataLabels: {
          enabled: true
        }
      }
    },

    yAxis: {
      title: {
        text: '#Analysis'
      }
    },

    series: [{
      type: 'column',
      name: 'Strong Sell',
    }, {
      type: 'column',
      name: 'Sell',
    }, {
      type: 'column',
      name: 'Hold',
    }, {
      type: 'column',
      name: 'Buy',
    }, {
      type: 'column',
      name: 'Strong Buy',
    }]
  };
  recommendationsChartUpdate = false;

  earningsChartOption: Highcharts.Options = {
    title: {
      text: 'Historical EPS Surprises',
    },
    yAxis: {
      title: {
        text: 'Quarterly EPS',
      }
    },
    series: [{
      type: 'spline',
    }, {
      type: 'spline',
    }],
    tooltip: {
      shared: true,
    }
  };
  earningsChartUpdate = false;

  constructor(
    private http: HttpClient,
  ) {
    console.log('searchService initing!');
  }

  getCompanyProfile(symbol: string) {
    this.http.get<CompanyProfile>(`/api/company/${symbol}`).subscribe(companyProfile => {
      if (!companyProfile.ticker) {
        this.failed = true;
      }
      this.companyProfile = companyProfile
      this.searching = false;
    });
  }

  getQuote(symbol: string) {
    this.http.get<Quote>(`/api/quote/${symbol}`).subscribe(quote => {
      this.quote = quote;
      let curTime = Math.floor(new Date().getTime() / 1000)
      this.closed = curTime - quote.t >= 5 * 60;

      if (this.closed) {
        this.getChartsForLastWorkingDay(symbol, quote.t);
      } else {
        this.getChartsForLastWorkingDay(symbol, curTime);
      }
    });
  }

  getPeers(symbol: string) {
    this.http.get<string[]>(`/api/peers/${symbol}`).subscribe(peers => {
      this.peers = peers.filter(peer => peer);
    });
  }

  getChartsForLastWorkingDay(symbol: string, _to: number) {
    let _from = _to - 6 * 3600;
    this.http.get<Candle>(`/api/candle/${symbol}?resolution=5&from=${_from}&to=${_to}`).subscribe(candle => {
      if (!candle.t) {
        return;
      }

      this.summaryChartOptions.series = [{
        type: 'line',
        name: this.companyProfile?.ticker,
        data: candle.t.map((e, i) => {
          return [e * 1000, candle.c[i]];
        }),
        color: this.quote && this.quote.d >= 0 ? '#198754' : '#dc3545',
      }];

      this.summaryChartOptions.title = {
        text: `${symbol.toUpperCase()} Hourly Price Variation`,
      };

      this.summaryChartUpdate = true;
    });
  }

  getChartsData(symbol: string) {
    let now = new Date(), before = new Date();
    before.setFullYear(now.getFullYear() - 2);
    this.http.get<Candle>(`/api/candle/${symbol}?resolution=D&from=${Math.floor(before.getTime() / 1000)}&to=${Math.floor(now.getTime() / 1000)}`)
      .subscribe(candle => {
        if (!candle.t) {
          return;
        }
        let ohlc = candle.t.map((e, i) => [e * 1000, candle.o[i], candle.h[i], candle.l[i], candle.c[i]]);
        let volume = candle.t.map((e, i) => [e * 1000, candle.v[i]]);
        this.chartsOptions.series = [{
          type: 'candlestick',
          name: symbol.toUpperCase(),
          id: 'ohlc',
          zIndex: 2,
          data: ohlc
        }, {
          type: 'column',
          name: 'Volume',
          id: 'volume',
          data: volume,
          yAxis: 1
        }, {
          type: 'vbp',
          linkedTo: 'ohlc',
          params: {
            volumeSeriesID: 'volume'
          },
          dataLabels: {
            enabled: false
          },
          zoneLines: {
            enabled: false
          }
        }, {
          type: 'sma',
          linkedTo: 'ohlc',
          zIndex: 1,
          marker: {
            enabled: false
          }
        }]
        this.chartsOptions.title = {
          text: `${symbol.toUpperCase()} Historical`
        }
        this.chartsUpdate = true;
      });
  }

  getNews(symbol: string) {
    let now = new Date(), before = new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000);
    this.http.get<News[]>(`/api/news/${symbol}?from=${this.formatDate(before)}&to=${this.formatDate(now)}`)
      .subscribe(newsList => {
        this.newsList = [];
        let count = 0
        for (let news of newsList) {
          if (news.image && news.source && news.datetime && news.headline && news.summary && news.url) {
            this.newsList.push(news);
            count += 1;
          }
          if (count >= 20) {
            break;
          }
        }
      });
  }

  getSocial(symbol: string) {
    this.http.get<{ reddit: Mention[], symbol: string, twitter: Mention[] }>(`/api/social/${symbol}`)
      .subscribe(resp => {
        const cb = (prev: Mention, cur: Mention) => {
          return {
            mention: prev.mention + cur.mention,
            positiveMention: prev.positiveMention + cur.positiveMention,
            negativeMention: prev.negativeMention + cur.negativeMention,
          }
        }
        this.redditMention = resp.reddit.reduce(cb, { mention: 0, positiveMention: 0, negativeMention: 0 });
        this.twitterMention = resp.twitter.reduce(cb, { mention: 0, positiveMention: 0, negativeMention: 0 });
      });
  }

  getRecommendation(symbol: string) {
    this.http.get<Recommendation[]>(`/api/recommendation/${symbol}`).subscribe(recommendations => {
      this.recommendationsChartOption.xAxis = {
        categories: recommendations.map(recommendation => recommendation.period.substring(0, 7)),
      };
      this.recommendationsChartOption.series = [{
        type: 'column',
        name: 'Strong Buy',
        data: recommendations.map(item => item.strongBuy),
        color: 'forestgreen'
      }, {
        type: 'column',
        name: 'Buy',
        data: recommendations.map(item => item.buy),
        color: 'mediumseagreen',
      }, {
        type: 'column',
        name: 'Hold',
        data: recommendations.map(item => item.hold),
        color: 'darkgoldenrod'
      }, {
        type: 'column',
        name: 'Sell',
        data: recommendations.map(item => item.sell),
        color: 'tomato',
      }, {
        type: 'column',
        name: 'Strong Sell',
        data: recommendations.map(item => item.strongSell),
        color: 'brown'
      }];
      this.recommendationsChartUpdate = true;
    });
  }

  getEarnings(symbol: string) {
    this.http.get<Earnings[]>(`/api/earnings/${symbol}`).subscribe(earningsList => {
      this.earningsChartOption.xAxis = {
        categories: earningsList.map(item => `<div style='text-align: center'>${item.period}<br>Surpirse: ${item.surprise || 0}<br><\div>`),
        labels: {
          useHTML: true,
        }
      }
      this.earningsChartOption.series = [{
        type: 'spline',
        data: earningsList.map(item => item.actual || 0),
        name: 'Actual',
      }, {
        type: 'spline',
        data: earningsList.map(item => item.estimate || 0),
        name: 'Estimate',
      }];
      this.earningsChartUpdate = true;
    });
  }

  search(symbol: string) {
    return symbol ? this.http.get<SearchResp>(`/api/search/${symbol}`) : Of({ count: 0, result: [] });
  }

  searchDetail(symbol: string): void {

    if (!symbol) {
      this.reset();
      this.failed = true;
      this.failOnEmpty = true;
      return;
    }

    if (this.prevSymbol === symbol) {
      return;
    }

    this.reset();
    this.prevSymbol = symbol;
    this.searching = true;


    this.intervalSubscription = interval(15000).pipe(startWith(0)).subscribe(_ => {
      this.getCompanyProfile(this.prevSymbol);
      this.getQuote(this.prevSymbol);
      this.getPeers(this.prevSymbol);
    });

    this.getNews(this.prevSymbol);

    this.getChartsData(this.prevSymbol);

    this.getSocial(this.prevSymbol);

    this.getRecommendation(this.prevSymbol);

    this.getEarnings(this.prevSymbol);

  }

  reset() {
    this.prevSymbol = '';
    this.searching = false;
    this.failed = false;
    this.failOnEmpty = false;

    this.companyProfile = undefined;
    this.quote = undefined;
    this.peers = undefined;
    this.closed = false;
    if (this.intervalSubscription) {
      this.intervalSubscription.unsubscribe();
      this.intervalSubscription = undefined;
    }

    this.summaryChartOptions.series = [{
      type: 'line',
      data: []
    }];
    this.chartsOptions.series = [{
      type: 'candlestick',
      id: 'ohlc',
      data: []
    }, {
      type: 'column',
      id: 'volume',
      data: []
    }, {
      type: 'vbp',
      linkedTo: 'ohlc',
      params: {
        volumeSeriesID: 'volume'
      },
    }, {
      type: 'sma',
      linkedTo: 'ohlc',
    }];
    this.redditMention = undefined;
    this.twitterMention = undefined;
    this.newsList = undefined;
    this.recommendationsChartOption.series = [{
      type: 'column',
      name: 'Strong Sell',
    }, {
      type: 'column',
      name: 'Sell',
    }, {
      type: 'column',
      name: 'Hold',
    }, {
      type: 'column',
      name: 'Buy',
    }, {
      type: 'column',
      name: 'Strong Buy',
    }];
    this.earningsChartOption.series = [{
      type: 'spline',
    }, {
      type: 'spline',
    }];

  }

  chartCallback: Highcharts.ChartCallbackFunction = (chart) => {
    setTimeout(() => {
      chart.reflow();
    }, 0);
  };

  private formatDate(date: Date) {
    let d = new Date(date),
      month = '' + (d.getMonth() + 1),
      day = '' + d.getDate(),
      year = d.getFullYear();

    if (month.length < 2)
      month = '0' + month;
    if (day.length < 2)
      day = '0' + day;

    return [year, month, day].join('-');
  }
}
