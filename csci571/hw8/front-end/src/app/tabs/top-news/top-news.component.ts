import { Component, OnInit } from '@angular/core';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { NewsModalComponent } from 'src/app/modals/news-modal/news-modal.component';
import { News } from 'src/app/model/news.model';

import { SearchService } from 'src/app/search.service';

@Component({
  selector: 'app-top-news',
  templateUrl: './top-news.component.html',
  styleUrls: ['./top-news.component.css']
})
export class TopNewsComponent implements OnInit {

  constructor(
    public searchService: SearchService,
    private modalService: NgbModal
  ) { }

  ngOnInit(): void {
  }

  open(news: News) {
    const modalRef = this.modalService.open(NewsModalComponent);
    modalRef.componentInstance.news = news;
  }
}
