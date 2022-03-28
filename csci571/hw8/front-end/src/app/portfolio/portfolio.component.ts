import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { PurchaseModalComponent } from '../modals/purchase-modal/purchase-modal.component';
import { Quote } from '../model/quote.model';
import { PortfolioService } from '../portfolio.service';

@Component({
  selector: 'app-portfolio',
  templateUrl: './portfolio.component.html',
  styleUrls: ['./portfolio.component.css']
})
export class PortfolioComponent implements OnInit {

  changedSymbol?: string;
  sold = false;
  soldTimeoutID?: number;
  bought = false;
  boughtTimeoutID?: number;

  stockQuote: { [key: string]: Quote } = {};

  constructor(
    public portfolioService: PortfolioService,
    public modalService: NgbModal,
    public router: Router,
  ) { }

  ngOnInit(): void {
    Object.entries(this.portfolioService.stockQuoteObserverable).forEach(([symbol, ob]) => {
      ob.subscribe(quote => {
        this.stockQuote[symbol] = quote;
      });
    });
  }

  open(symbol: string, quote: Quote, sell?: boolean) {
    const modalRef = this.modalService.open(PurchaseModalComponent);
    modalRef.componentInstance.sell = sell ? true : false;
    modalRef.componentInstance.companyProfile = { ticker: symbol };
    modalRef.componentInstance.quote = quote;

    modalRef.closed.subscribe(_ => {
      this.changedSymbol = symbol;
      if (sell) {
        this.sold = true;
        clearTimeout(this.soldTimeoutID);
        this.soldTimeoutID = window.setTimeout((() => { this.sold = false; this.changedSymbol = undefined }).bind(this), 5000);
      } else {
        this.bought = true;
        clearTimeout(this.boughtTimeoutID);
        this.boughtTimeoutID = window.setTimeout((() => { this.bought = false; this.changedSymbol = undefined }).bind(this), 5000);
      }
    })

  }

}
