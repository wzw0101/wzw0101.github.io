const form = document.querySelector('form');
const reset_button = document.querySelector("button.reset");
const tabs = document.querySelector(".results > ul");
const input_history = [];

function formatDate(d) {
    let month = '' + (d.getMonth() + 1),
        day = '' + d.getDate(),
        year = d.getFullYear();

    if (month.length < 2)
        month = '0' + month;
    if (day.length < 2)
        day = '0' + day;

    return [year, month, day].join('-');
}

function hideAllShown() {
    for (let elem of document.querySelectorAll(".show")) {
        elem.classList.remove("show");
    }
    document.querySelector("div.results > ul li.selected").classList.remove("selected")
    document.querySelector("div.results > ul li:first-child").classList.add("selected")
}

function fetchFromCompany(param) {
    fetch("/company?" + param).then(resp => {
        return resp.json();
    }).then(json => {
        if (json.code) {
            document.querySelector("div.no-record").classList.add("show")
            return
        }
        let company = json.results
        let rows = document.querySelectorAll("#company tr");
        rows[0].querySelector("img").src = company.logo;
        rows[1].querySelector("td").innerText = company.name;
        rows[2].querySelector("td").innerText = company.ticker;
        rows[3].querySelector("td").innerText = company.exchange;
        rows[4].querySelector("td").innerText = company.ipo;
        rows[5].querySelector("td").innerText = company.category;

        document.querySelector("div.results").classList.add("show");
        document.querySelector("div.company").classList.add("show");
    })
}

function fetchFromStockSummary(param) {
    fetch("/stocksummary?" + param).then(resp => {
        return resp.json();
    }).then(json => {
        let stockSummary = json.results;
        let rows = document.querySelectorAll("#stock-summary tr");
        rows[0].querySelector("td").innerText = stockSummary.ticker;
        rows[1].querySelector("td").innerText = new Date(stockSummary.t * 1000).toLocaleDateString(undefined, { dateStyle: "medium" });
        rows[2].querySelector("td").innerText = stockSummary.pc;
        rows[3].querySelector("td").innerText = stockSummary.o;
        rows[4].querySelector("td").innerText = stockSummary.h;
        rows[5].querySelector("td").innerText = stockSummary.l;
        rows[6].querySelector("span").innerText = stockSummary.d;
        rows[6].querySelector("img").src = stockSummary.d > 0 ? "/static/img/GreenArrowUp.png" : "/static/img/RedArrowDown.png";
        rows[7].querySelector("span").innerText = stockSummary.dp;
        rows[7].querySelector("img").src = stockSummary.dp > 0 ? "/static/img/GreenArrowUp.png" : "/static/img/RedArrowDown.png";
    })
}

function fetchFromRecommendation(param) {
    fetch("/recommendation?" + param).then(resp => {
        return resp.json();
    }).then(json => {
        let recommendation = json.results;
        boxes = document.querySelector("#stock-summary div.indicator");
        boxes.querySelector("div.strong-sell").innerText = recommendation.strongSell;
        boxes.querySelector("div.sell").innerText = recommendation.sell;
        boxes.querySelector("div.hold").innerText = recommendation.hold;
        boxes.querySelector("div.buy").innerText = recommendation.buy;
        boxes.querySelector("div.strong-buy").innerText = recommendation.strongBuy;
    });
}

function fetchFromCharts(param, symbol) {
    fetch("/charts?" + param).then(resp => {
        return resp.json();
    }).then(data => {
        let date = new Date();
        // Create the chart
        Highcharts.stockChart('charts', {
            yAxis: [{
                title: {
                    text: "Stock Price",
                },
                opposite: false,
            }, {
                title: {
                    text: "Volume",
                },
                opposite: true,
            }],

            rangeSelector: {
                selected: 0,
                buttons: [{
                    type: "day",
                    count: 7,
                    text: "7d",
                    title: "View 7 days"
                }, {
                    type: "day",
                    count: 15,
                    text: "15d",
                    title: "View 15 days"
                }, {
                    type: 'month',
                    count: 1,
                    text: '1m',
                    title: 'View 1 month'
                }, {
                    type: 'month',
                    count: 3,
                    text: '3m',
                    title: 'View 3 months'
                }, {
                    type: 'month',
                    count: 6,
                    text: '6m',
                    title: 'View 6 months'
                }],
                inputEnabled: false,
            },

            title: {
                text: 'Stock Price ' + symbol + " " + formatDate(date),
                style: {
                    padding: "15px"
                },
                useHTML: true,
            },

            subtitle: {
                text: "<a href='https://finnhub.io'>Source: Finnhub</a>",
                useHTML: true,
            },

            series: [{
                name: 'Stock Price',
                data: data.results.tc,
                tooltip: {
                    valueDecimals: 2,
                },
                threshold: null,
                type: 'area',
                yAxis: 0,
                fillColor: {
                    linearGradient: {
                        x1: 0,
                        y1: 0,
                        x2: 0,
                        y2: 1
                    },
                    stops: [
                        [0, Highcharts.getOptions().colors[0]],
                        [1, Highcharts.color(Highcharts.getOptions().colors[0]).setOpacity(0).get('rgba')]
                    ]
                }
            }, {
                name: 'Volume',
                data: data.results.tv,
                type: 'column',
                maxPointWidth: 5,
                yAxis: 1,
            }]
        });
    })
}

function fetchFromNews(param) {
    fetch("/news?" + param).then(resp => {
        return resp.json();
    }).then(json => {
        let news_list = json.results;
        let cards = document.querySelector("#latest-news");
        cards.innerHTML = "";
        for (news of news_list) {
            let card = document.createElement("div");
            card.className = "card";

            let img = document.createElement("img");
            img.src = news.image;

            let textContainer = document.createElement("div");
            let title = document.createElement("p");
            title.className = "title";
            title.innerText = news.title;
            let date = document.createElement("p");
            date.className = "date";
            date.innerText = new Date(news.date * 1000).toLocaleDateString(undefined, { dateStyle: "medium" });
            let pa = document.createElement("p");
            let a = document.createElement("a");
            a.href = news.url;
            a.innerHTML = "See Original Post";
            pa.appendChild(a);
            textContainer.append(title, date, pa);
            card.append(img, textContainer);
            cards.appendChild(card);
        }
    });
}

form.addEventListener('submit', e => {
    // $('form').bind('submit', e => {
    e.preventDefault();
    hideAllShown();

    let searchkey = document.querySelector("input.searchbar").value;
    let param = new URLSearchParams({ "searchkey": searchkey }).toString();

    if (!input_history.includes(searchkey)) {
        input_history.push(searchkey);
    }

    // fetch from the company api
    fetchFromCompany(param);

    // fetch from the stock_summary api
    fetchFromStockSummary(param);

    // fetch from the recommendation api
    fetchFromRecommendation(param);

    // fetch from charts
    fetchFromCharts(param, searchkey);

    fetchFromNews(param);
})

reset_button.addEventListener('click', e => {
    hideAllShown();
})

tabs.addEventListener('click', e => {
    const selected = tabs.querySelector("li.selected");
    if (!(e.target instanceof HTMLLIElement) || e.target === selected) {
        return;
    }
    selected.classList.remove("selected");
    e.target.classList.add("selected");

    document.querySelector(selected.dataset.ref).classList.remove("show");
    document.querySelector(e.target.dataset.ref).classList.add("show");
})

$("input#searchbar").autocomplete({
    source: input_history
});
