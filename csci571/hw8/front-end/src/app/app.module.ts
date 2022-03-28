import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { FormsModule } from '@angular/forms';
import { HttpClientModule } from '@angular/common/http';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { MatAutocompleteModule } from '@angular/material/autocomplete'
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTabsModule } from '@angular/material/tabs';
import { HighchartsChartModule } from 'highcharts-angular';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { NavbarComponent } from './navbar/navbar.component';
import { SearchHomeComponent } from './search-home/search-home.component';
import { SearchResultComponent } from './search-result/search-result.component';
import { StockDetailComponent } from './stock-detail/stock-detail.component';
import { SummaryTabComponent } from './tabs/summary-tab/summary-tab.component';
import { WatchlistComponent } from './watchlist/watchlist.component';
import { TopNewsComponent } from './tabs/top-news/top-news.component';
import { NewsModalComponent } from './modals/news-modal/news-modal.component';
import { ChartsTabComponent } from './tabs/charts-tab/charts-tab.component';
import { InsightsTabComponent } from './tabs/insights-tab/insights-tab.component';
import { PortfolioComponent } from './portfolio/portfolio.component';
import { PurchaseModalComponent } from './modals/purchase-modal/purchase-modal.component';


@NgModule({
  declarations: [
    AppComponent,
    NavbarComponent,
    SearchHomeComponent,
    SearchResultComponent,
    StockDetailComponent,
    SummaryTabComponent,
    WatchlistComponent,
    TopNewsComponent,
    NewsModalComponent,
    ChartsTabComponent,
    InsightsTabComponent,
    PortfolioComponent,
    PurchaseModalComponent
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    NgbModule,
    FormsModule,
    HttpClientModule,
    BrowserAnimationsModule,
    MatAutocompleteModule,
    MatProgressSpinnerModule,
    MatTabsModule,
    HighchartsChartModule,
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
