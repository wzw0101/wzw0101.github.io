import { Component, OnInit } from '@angular/core';
import * as Highcharts from 'highcharts/highstock';

import { SearchService } from 'src/app/search.service';

@Component({
  selector: 'app-charts-tab',
  templateUrl: './charts-tab.component.html',
  styleUrls: ['./charts-tab.component.css']
})
export class ChartsTabComponent implements OnInit {

  private chartRef!: Highcharts.Chart;

  constructor(public searchService: SearchService) { }

  ngOnInit(): void {
  }

  chartCallback(): (chart: Highcharts.Chart) => void {
    let self = this;
    return (chart) => {
      self.chartRef = chart;
    }
  };

  reflow() {
    this.chartRef.reflow();
  }
}
