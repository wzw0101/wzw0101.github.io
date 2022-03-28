import { Component, OnInit } from '@angular/core';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';

import { PurchaseModalComponent } from '../modals/purchase-modal/purchase-modal.component';
import { CompanyProfile } from '../model/company-profile.model';
import { PortfolioService } from '../portfolio.service';
import { SearchService } from '../search.service';
import { WatchlistService } from '../watchlist.service';

@Component({
  selector: 'app-stock-detail',
  templateUrl: './stock-detail.component.html',
  styleUrls: ['./stock-detail.component.css']
})
export class StockDetailComponent implements OnInit {

  selected = false;
  unwatched = false;
  unwatchedTimeoutID?: number;
  watched = false;
  watchedTimeoutID?: number;

  bought = false;
  boughtTimeoutID?: number;
  sold = false;
  soldTimeoutID?: number;

  constructor(
    public searchService: SearchService,
    public watchlistService: WatchlistService,
    public portfolioService: PortfolioService,
    private modalService: NgbModal,
  ) { }

  ngOnInit(): void { }

  toggle(companyProfile: CompanyProfile) {
    if (this.watchlistService.watchlist[companyProfile.ticker]) {
      this.watchlistService.remove(companyProfile);
      this.watched = false;
      this.unwatched = true;
      if (this.unwatchedTimeoutID) clearTimeout(this.unwatchedTimeoutID);
      this.unwatchedTimeoutID = window.setTimeout((() => { this.unwatched = false; this.unwatchedTimeoutID = undefined }).bind(this), 5000);
    } else {
      this.watchlistService.add(companyProfile);
      this.unwatched = false;
      this.watched = true;
      if (this.watchedTimeoutID) clearTimeout(this.watchedTimeoutID);
      this.watchedTimeoutID = window.setTimeout((() => { this.watched = false; this.watchedTimeoutID = undefined }).bind(this), 5000);
    }
  }

  open(sell?: boolean) {
    const modalRef = this.modalService.open(PurchaseModalComponent);
    modalRef.componentInstance.sell = sell ? true : false;
    modalRef.componentInstance.companyProfile = this.searchService.companyProfile!;
    modalRef.componentInstance.quote = this.searchService.quote!;
    modalRef.closed.subscribe(_ => {
      if (sell) {
        this.sold = true;
        if (this.soldTimeoutID) clearTimeout(this.soldTimeoutID);
        this.soldTimeoutID = window.setTimeout((() => { this.sold = false; this.soldTimeoutID = undefined }).bind(this), 5000);
      } else {
        this.bought = true;
        if (this.boughtTimeoutID) clearTimeout(this.boughtTimeoutID);
        this.boughtTimeoutID = window.setTimeout((() => { this.bought = false; this.boughtTimeoutID = undefined }).bind(this), 5000);
      }
    });
  }
}
