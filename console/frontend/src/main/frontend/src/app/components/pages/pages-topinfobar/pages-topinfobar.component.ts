import { Component, OnInit } from '@angular/core';
import { StateService, TransitionService } from '@uirouter/angularjs';
import { AppService } from 'src/angularjs/app/app.service';

@Component({
  selector: 'app-pages-topinfobar',
  templateUrl: './pages-topinfobar.component.html',
  styleUrls: ['./pages-topinfobar.component.scss']
})
export class PagesTopinfobarComponent implements OnInit {
  loading = true;
  currRoute = this.$state.current;

  constructor(private appService: AppService, private $state: StateService, private transition: TransitionService) { }

  ngOnInit() {
    this.currRoute = this.$state.current;
    this.transition.onSuccess({}, () => {
      this.currRoute = this.$state.current;
    });
    this.appService.loading$.subscribe(loading => this.loading = loading);
  }
}
