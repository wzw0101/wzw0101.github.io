import { Component, OnInit } from '@angular/core';
import { SearchService } from 'src/app/search.service';

@Component({
  selector: 'app-insights-tab',
  templateUrl: './insights-tab.component.html',
  styleUrls: ['./insights-tab.component.css']
})
export class InsightsTabComponent implements OnInit {

  recChartRef?: Highcharts.Chart;
  earningChartRef?: Highcharts.Chart;

  constructor(public searchService: SearchService) { }

  ngOnInit(): void {
  }

  recChartCallback(): (chart: Highcharts.Chart) => void {
    let self = this;
    return (chart) => {
      self.recChartRef = chart;
    }
  }

  earningsChartCallback(): (chart: Highcharts.Chart) => void {
    let self = this;
    return (chart) => {
      self.earningChartRef = chart;
    }
  }

  reflow() {
    this.recChartRef?.reflow();
    this.earningChartRef?.reflow();
  }
}