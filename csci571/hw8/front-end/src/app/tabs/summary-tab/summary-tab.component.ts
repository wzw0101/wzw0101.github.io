import { Component, OnInit } from '@angular/core';

import { SearchService } from 'src/app/search.service';

@Component({
  selector: 'app-summary-tab',
  templateUrl: './summary-tab.component.html',
  styleUrls: ['./summary-tab.component.css']
})
export class SummaryTabComponent implements OnInit {


  constructor(
    public searchService: SearchService,
  ) { }

  ngOnInit(): void { }

}
