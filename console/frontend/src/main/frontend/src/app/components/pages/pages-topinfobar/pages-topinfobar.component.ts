import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Subscription } from 'rxjs';
import { AppService } from 'src/app/app.service';

@Component({
  selector: 'app-pages-topinfobar',
  templateUrl: './pages-topinfobar.component.html',
  styleUrls: ['./pages-topinfobar.component.scss']
})
export class PagesTopinfobarComponent implements OnInit, OnDestroy {
  loading: boolean = true;
  breadcrumbs: string = "Loading";

  private _subscriptions = new Subscription();

  constructor(private route: ActivatedRoute, private appService: AppService) { }

  ngOnInit() {
    this.route.data.subscribe(data => {
      this.breadcrumbs = data['breadcrumbs'] ?? "Error"
    });
    const loadingSubscription = this.appService.loading$.subscribe(loading => this.loading = loading);
    this._subscriptions.add(loadingSubscription);
  }

  ngOnDestroy() {
    this._subscriptions.unsubscribe();
  }
}
