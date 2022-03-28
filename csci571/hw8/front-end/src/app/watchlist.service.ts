import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, share } from 'rxjs';
import { CompanyProfile } from './model/company-profile.model';
import { Quote } from './model/quote.model';

const WATCHLIST_KEY = 'watchlist'

@Injectable({
  providedIn: 'root'
})
export class WatchlistService {

  watchlist: { [key: string]: CompanyProfile } = JSON.parse(localStorage.getItem(WATCHLIST_KEY) || '{}');
  watchQuote: { [key: string]: Observable<Quote> };

  constructor(public http: HttpClient) {
    this.watchQuote = {}
    for (const symbol in this.watchlist) this.watchQuote[symbol] = this.http.get<Quote>(`/api/quote/${symbol}`).pipe(share());
  }

  add(companyProfile: CompanyProfile) {
    this.watchlist[companyProfile.ticker] = companyProfile;
    this.watchQuote[companyProfile.ticker] = this.http.get<Quote>(`/api/quote/${companyProfile.ticker}`).pipe(share());
    localStorage.setItem(WATCHLIST_KEY, JSON.stringify(this.watchlist));
  }

  remove(companyProfile: CompanyProfile) {
    delete this.watchlist[companyProfile.ticker];
    delete this.watchQuote[companyProfile.ticker];
    localStorage.setItem(WATCHLIST_KEY, JSON.stringify(this.watchlist));
  }

  has(companyProfile: CompanyProfile) {
    return undefined !== this.watchlist[companyProfile.ticker];
  }

  toList = () => Object.entries(this.watchlist).map(([_, value]) => value);
}
