import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, share } from 'rxjs';
import { CompanyProfile } from './model/company-profile.model';
import { PurchasedStock } from './model/purchased-stock.model';
import { Quote } from './model/quote.model';

const BALANCE = 'balance';
const STOCKS = 'stocks'

@Injectable({
  providedIn: 'root'
})
export class PortfolioService {

  balance: number = parseFloat(localStorage.getItem(BALANCE) || '25000');
  stocks: { [key: string]: PurchasedStock } = JSON.parse(localStorage.getItem(STOCKS) || '{}');
  stockQuoteObserverable: { [key: string]: Observable<Quote> } = {};

  constructor(private http: HttpClient) {
    for (const symbol in this.stocks) {
      this.stockQuoteObserverable[symbol] = this.http.get<Quote>(`/api/quote/${symbol}`).pipe(share());
    }
  }

  purchase(companyProfile: CompanyProfile, cnt: number, cost: number) {
    if (!this.stocks[companyProfile.ticker]) {
      this.stocks[companyProfile.ticker] = {
        symbol: companyProfile.ticker,
        name: companyProfile.name,
        count: 0,
        totalCost: 0
      }
      this.stockQuoteObserverable[companyProfile.ticker] = this.http.get<Quote>(`/api/quote/${companyProfile.ticker}`).pipe(share());
    }

    let stock = this.stocks[companyProfile.ticker];
    stock.count += cnt;
    if (cnt < 0) {
      stock.totalCost += stock.totalCost / stock.count * cnt;
    } else {
      stock.totalCost += cost;
    }

    this.balance -= cost;

    if (stock.count <= 0) {
      delete this.stocks[stock.symbol];
      delete this.stockQuoteObserverable[stock.symbol];
    } else {
      this.stocks[stock.symbol] = stock;
    }

    localStorage.setItem(STOCKS, JSON.stringify(this.stocks));
    localStorage.setItem(BALANCE, this.balance.toString())
  }

  getPurchasedStocks = () => Object.entries(this.stocks).map(([_, stock]) => stock);
}
