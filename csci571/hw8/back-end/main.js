const express = require('express');
const axios = require('axios').default;

const app = express();
const PORT = parseInt(process.env.PORT) || 3000;
const FINNKEY = 'c824evqad3i9d12p6sog';

app.get('/api/ping', (req, res) => {
  res.send('pong!');
});

app.get('/api/error', (req, res) => {
  setTimeout(() => {
    throw new Error('Broken');
  }, 100);
});

app.get('/api/company/:symbol', (req, res, next) => {
  axios.get(`https://finnhub.io/api/v1/stock/profile2?symbol=${req.params.symbol.toUpperCase()}&token=${FINNKEY}`)
    .then((resp) => {
      res.json(resp.data);
    }).catch(next);
});

app.get('/api/candle/:symbol', (req, res, next) => {
  axios.get(`https://finnhub.io/api/v1/stock/candle?symbol=${req.params.symbol.toUpperCase()}&resolution=${req.query.resolution}&from=${req.query.from}&to=${req.query.to}&token=${FINNKEY}`)
    .then((resp) => {
      res.json(resp.data);
    }).catch(next);
});

app.get('/api/quote/:symbol', (req, res, next) => {
  axios.get(`https://finnhub.io/api/v1/quote?symbol=${req.params.symbol.toUpperCase()}&token=${FINNKEY}`)
    .then((resp) => {
      res.json(resp.data);
    }).catch(next);
});

app.get('/api/search/:symbol', (req, res, next) => {
  axios.get(`https://finnhub.io/api/v1/search?q=${req.params.symbol.toUpperCase()}&token=${FINNKEY}`)
    .then((resp) => {
      res.json(resp.data);
    }).catch(next);
});

app.get('/api/news/:symbol', (req, res, next) => {
  axios.get(`https://finnhub.io/api/v1/company-news?symbol=${req.params.symbol}&from=${req.query.from}&to=${req.query.to}&token=${FINNKEY}`)
    .then((resp) => {
      res.json(resp.data);
    }).catch(next);
});

app.get('/api/recommendation/:symbol', (req, res, next) => {
  axios.get(`https://finnhub.io/api/v1/stock/recommendation?symbol=${req.params.symbol}&token=${FINNKEY}`)
    .then((resp) => {
      res.json(resp.data);
    }).catch(next);
});

app.get('/api/social/:symbol', (req, res, next) => {
  axios.get(`https://finnhub.io/api/v1/stock/social-sentiment?symbol=${req.params.symbol}&from=2022-01-01&token=${FINNKEY}`)
    .then((resp) => {
      res.json(resp.data);
    }).catch(next);
});

app.get('/api/peers/:symbol', (req, res, next) => {
  axios.get(`https://finnhub.io/api/v1//stock/peers?symbol=${req.params.symbol}&token=${FINNKEY}`)
    .then((resp) => {
      res.json(resp.data);
    }).catch(next);
});

app.get('/api/earnings/:symbol', (req, res, next) => {
  axios.get(`https://finnhub.io/api/v1//stock/earnings?symbol=${req.params.symbol}&token=${FINNKEY}`)
    .then((resp) => {
      res.json(resp.data);
    }).catch(next);
});

app.use(express.static('public'));

app.use((err, req, res, next) => {
  if (err.response) {
    // The request was made and the server responded with a status code
    // that falls out of the range of 2xx
    // console.log('err.response.data: ', err.response.data);
    // console.log('err.response.status: ', err.response.status);
    // console.log('err.response.headers: ', err.response.headers);
    res.status(err.response.status).send(err.response.data);
    return;
  } else if (err.request) {
    // The request was made but no response was received
    // `error.request` is an instance of XMLHttpRequest in the browser and an instance of
    // http.ClientRequest in node.js
    console.log(err.request);
  } else {
    // Something happened in setting up the request that triggered an Error
    console.log('Error', err.message);
  }
  console.log(err.config);
  res.status(500).send(err);
})

app.listen(PORT, () => {
  console.log(`Server listening on port ${PORT}...`);
});