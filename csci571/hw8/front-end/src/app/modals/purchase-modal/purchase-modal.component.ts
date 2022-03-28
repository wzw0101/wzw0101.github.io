import { Component, Input, OnInit } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { CompanyProfile } from 'src/app/model/company-profile.model';
import { Quote } from 'src/app/model/quote.model';
import { PortfolioService } from 'src/app/portfolio.service';

@Component({
  selector: 'app-purchase-modal',
  templateUrl: './purchase-modal.component.html',
  styleUrls: ['./purchase-modal.component.css']
})
export class PurchaseModalComponent implements OnInit {

  @Input() companyProfile?: CompanyProfile;
  @Input() quote?: Quote;
  @Input() sell = false;
  quantity?: number;

  constructor(
    public activeModal: NgbActiveModal,
    public portfolioService: PortfolioService,
  ) { }

  ngOnInit(): void {
  }

  onSubmit() {
    let quantity = this.sell ? -this.quantity! : this.quantity!;
    let cost = this.quote!.c * quantity;
    this.portfolioService.purchase(this.companyProfile!, quantity, cost)
    this.activeModal.close();
  }
}
