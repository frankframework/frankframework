import { Component, OnInit, OnDestroy } from '@angular/core';
import { StateService, TransitionService } from '@uirouter/angularjs';
import { Subscription } from 'rxjs';
import { AppService } from 'src/angularjs/app/app.service';

@Component({
  selector: 'app-pages-topinfobar',
  templateUrl: './pages-topinfobar.component.html',
  styleUrls: ['./pages-topinfobar.component.scss']
})
export class PagesTopinfobarComponent implements OnInit, OnDestroy {
  loading = true;
  currRoute = this.$state.current;

  private _subscriptions = new Subscription();

  constructor(private appService: AppService, private $state: StateService, private transition: TransitionService) { }

  ngOnInit() {
    this.currRoute = this.$state.current;
    this.transition.onSuccess({}, () => {
      this.currRoute = this.$state.current;
    });
    const loadingSubscription = this.appService.loading$.subscribe(loading => this.loading = loading);
    this._subscriptions.add(loadingSubscription);
  }

  ngOnDestroy() {
    this._subscriptions.unsubscribe();
  }
}
