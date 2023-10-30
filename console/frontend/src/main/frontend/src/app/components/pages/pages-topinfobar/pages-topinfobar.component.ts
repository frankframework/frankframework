import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, NavigationEnd, Router } from '@angular/router';
import { Subscription, filter, map } from 'rxjs';
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

  constructor(private router: Router, private route: ActivatedRoute, private appService: AppService) { }

  ngOnInit() {
    /* this.route.data.pipe(map(data => data['breadcrumbs'])).subscribe(breadcrumbs => {
      this.breadcrumbs = breadcrumbs ?? "Error"
    }); */
    this.router.events.pipe(
      filter((e) => e instanceof NavigationEnd)
    ).subscribe((e) => {
      const event: NavigationEnd = e as any;
      const childRoute = this.route.children.pop()!;
      this.breadcrumbs = childRoute.snapshot.data['breadcrumbs'] ?? "Error"
    });
    const loadingSubscription = this.appService.loading$.subscribe(loading => this.loading = loading);
    this._subscriptions.add(loadingSubscription);
  }

  ngOnDestroy() {
    this._subscriptions.unsubscribe();
  }
}
