import { Component, OnInit } from '@angular/core';
import { MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { ThemePalette } from '@angular/material/core';
import { ActivatedRoute, Router } from '@angular/router';
import { debounceTime, distinctUntilChanged, Observable, Subject, switchMap } from 'rxjs';

import { SearchResp } from '../model/search.model';
import { SearchService } from '../search.service';

@Component({
  selector: 'app-search-home',
  templateUrl: './search-home.component.html',
  styleUrls: ['./search-home.component.css']
})
export class SearchHomeComponent implements OnInit {
  private searchTerms = new Subject<string>();
  searchResp?: SearchResp;

  color: ThemePalette = 'primary';

  constructor(
    public searchService: SearchService,
    public route: ActivatedRoute,
    private router: Router,
  ) {

  }

  ngOnInit(): void {
    this.searchTerms.pipe(
      // wait 300ms after each keystroke before considering the term
      debounceTime(300),

      // ignore new term if same as previous term
      distinctUntilChanged(),

      // switch to new search observable each time the term changes
      switchMap((term: string) => this.searchService.search(term)),
    ).subscribe(searchResp => {
      searchResp.result = searchResp.result.filter(result =>
        result.type === 'Common Stock' && !result.symbol.includes('.')
      );
      this.searchResp = searchResp;
    });

    this.route.params.subscribe(params => {
      const symbol = params["symbol"];
      if (symbol !== 'home') {
        this.searchService.searchDetail(symbol);
      }
    });


  }

  // Push a search term into the observable stream.
  search(term: string) {
    this.searchResp = undefined;
    if (!term) {
      return;
    }
    this.searchTerms.next(term);
  }

  searchDetail(symbol: string) {
    if (symbol === 'home') {
      this.searchService.searchDetail(symbol);
    }
    if (!symbol && this.route.snapshot.params["symbol"] === '') {
      this.searchService.searchDetail(symbol);
    }
    this.router.navigate(['search', symbol]);
  }

  reset() {
    this.searchService.reset();
    this.searchResp = undefined;
    this.router.navigate(['search', 'home']);
  }

  onOptionSelected(event: MatAutocompleteSelectedEvent) {
    this.router.navigate(['search', event.option.value])
  }
}
