import datetime

from flask import Flask
from flask import request
from dateutil.relativedelta import relativedelta
import finnhub

app = Flask(__name__)
FINNHUB_API_KEY = "c824evqad3i9d12p6sog"
client = finnhub.Client(api_key=FINNHUB_API_KEY)


@app.route("/wzw")
def hello_world():
    return {
        "wzw": "123",
        "ly": "321"
    }


@app.route("/company")
def company():
    symbol = request.args.get('searchkey', '')
    resp = client.company_profile2(symbol=symbol.upper())
    if not resp:
        return {
            "results": None,
            "code": 1
        }
    return {
        "results": {
            "logo": resp.get("logo", "?"),
            "name": resp.get("name", "?"),
            "ticker": resp.get("ticker", "?"),
            "exchange": resp.get("exchange", "?"),
            "ipo": resp.get("ipo", "?"),
            "category": resp.get("finnhubIndustry", "?")
        },
        "code": 0
    }


@app.route("/stocksummary")
def stock_summary():
    symbol = request.args.get('searchkey', '')
    resp = client.quote(symbol.upper())
    return {
        "results": {
            "ticker": symbol,
            "t": resp.get("t", "?"),
            "pc": resp.get("pc", "?"),
            "o": resp.get("o", "?"),
            "h": resp.get("h", "?"),
            "l": resp.get("l", "?"),
            "d": resp.get("d", "?"),
            "dp": resp.get("dp", "?")
        }
    }


@app.route("/recommendation")
def recommendation():
    symbol = request.args.get('searchkey', '')
    resp = client.recommendation_trends(symbol.upper())
    resp = resp[0] if len(resp) > 0 else {}
    return {
        "results": {
            "strongBuy": resp.get("strongBuy", "?"),
            "buy": resp.get("buy", "?"),
            "hold": resp.get("hold", "?"),
            "sell": resp.get("sell", "?"),
            "strongSell": resp.get("strongSell", "?")
        }
    }


@app.route("/charts")
def charts():
    symbol = request.args.get('searchkey', '')
    to = datetime.datetime.now()
    _from = to - relativedelta(months=6, days=1)
    resp = client.stock_candles(symbol.upper(), "D", int(
        _from.timestamp()), int(to.timestamp()))
    return {
        "results": {
            "tc": [[1000 * t, c] for t, c in zip(resp.get("t", []), resp.get("c", []))],
            "tv": [[1000 * t, v] for t, v in zip(resp.get("t", []), resp.get("v", []))]
        }
    }


@app.route("/news")
def news():
    symbol = request.args.get('searchkey', '')
    to = datetime.date.today()
    _from = to - datetime.timedelta(days=30)
    news_list = client.company_news(symbol.upper(), str(_from), str(to))
    ret = []
    count = 0
    for news in news_list:
        if count >= 5:
            break
        if news.get("image", "") and news.get("headline", "") and news.get("datetime", "") and news.get("url", ""):
            ret.append({
                "image": news["image"],
                "title": news["headline"],
                "date": news["datetime"],
                "url": news["url"]
            })
            count += 1
    return {
        "results": ret
    }


@app.route("/")
def hw6():
    return app.send_static_file("hw6.html")
