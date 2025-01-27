import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, ActivationEnd, NavigationEnd, Router } from '@angular/router';
import { Subscription, filter } from 'rxjs';
import { AppService } from 'src/app/app.service';

@Component({
  selector: 'app-pages-topinfobar',
  templateUrl: './pages-topinfobar.component.html',
  styleUrls: ['./pages-topinfobar.component.scss'],
  imports: [],
})
export class PagesTopinfobarComponent implements OnInit, OnDestroy {
  loading: boolean = true;
  breadcrumbs: string = 'Loading';
  popoutUrl: string | null = null;

  private _subscriptions = new Subscription();

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private appService: AppService,
  ) {}

  ngOnInit(): void {
    this.router.events.pipe(filter((event) => event instanceof ActivationEnd)).subscribe(() => {
      this.popoutUrl = null;
    });
    this.router.events.pipe(filter((event) => event instanceof NavigationEnd)).subscribe(() => {
      const childRoute = this.route.children.pop()!;
      if (!childRoute.snapshot.data['breadcrumbIsCustom']) {
        this.breadcrumbs = childRoute.snapshot.data['breadcrumbs'] ?? 'Error';
      }
    });

    const loadingSubscription = this.appService.loading$.subscribe((loading) => (this.loading = loading));
    this._subscriptions.add(loadingSubscription);

    const customBreadcrumbsSubscription = this.appService.customBreadscrumb$.subscribe(
      (breadcrumbs) => (this.breadcrumbs = breadcrumbs),
    );
    this._subscriptions.add(customBreadcrumbsSubscription);

    const iframePopoutUrlSubscription = this.appService.iframePopoutUrl$.subscribe((url) => (this.popoutUrl = url));
    this._subscriptions.add(iframePopoutUrlSubscription);
  }

  ngOnDestroy(): void {
    this._subscriptions.unsubscribe();
  }

  navigateToUrl(): void {
    if (this.popoutUrl) window.open(this.popoutUrl, '_blank');
  }
}
