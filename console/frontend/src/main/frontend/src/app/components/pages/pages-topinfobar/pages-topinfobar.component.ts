import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, NavigationEnd, Router } from '@angular/router';
import { Subscription, filter } from 'rxjs';
import { AppService } from 'src/app/app.service';

@Component({
  selector: 'app-pages-topinfobar',
  templateUrl: './pages-topinfobar.component.html',
  styleUrls: ['./pages-topinfobar.component.scss'],
})
export class PagesTopinfobarComponent implements OnInit, OnDestroy {
  loading: boolean = true;
  breadcrumbs: string = 'Loading';

  private _subscriptions = new Subscription();

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private appService: AppService,
  ) {}

  ngOnInit(): void {
    this.router.events
      .pipe(filter((event) => event instanceof NavigationEnd))
      .subscribe(() => {
        const childRoute = this.route.children.pop()!;
        this.breadcrumbs = childRoute.snapshot.data['breadcrumbs'] ?? 'Error';
      });
    const loadingSubscription = this.appService.loading$.subscribe(
      (loading) => (this.loading = loading),
    );
    this._subscriptions.add(loadingSubscription);
    const customBreadcrumbsSubscription =
      this.appService.customBreadscrumb$.subscribe(
        (breadcrumbs) => (this.breadcrumbs = breadcrumbs),
      );
    this._subscriptions.add(customBreadcrumbsSubscription);
  }

  ngOnDestroy(): void {
    this._subscriptions.unsubscribe();
  }
}
