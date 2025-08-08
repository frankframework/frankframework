import { Component, inject, OnDestroy, OnInit, Signal } from '@angular/core';
import { ActivatedRoute, ActivationEnd, NavigationEnd, Router } from '@angular/router';
import { filter, Subscription } from 'rxjs';
import { AppService } from 'src/app/app.service';

@Component({
  selector: 'app-pages-topinfobar',
  templateUrl: './pages-topinfobar.component.html',
  styleUrls: ['./pages-topinfobar.component.scss'],
  imports: [],
})
export class PagesTopinfobarComponent implements OnInit, OnDestroy {
  protected breadcrumbs = 'Loading';
  protected loading: Signal<boolean>;
  protected popoutUrl: Signal<string | null>;

  private _subscriptions: Subscription = new Subscription();
  private readonly router: Router = inject(Router);
  private readonly route: ActivatedRoute = inject(ActivatedRoute);
  private readonly appService: AppService = inject(AppService);

  constructor() {
    this.loading = this.appService.loading;
    this.popoutUrl = this.appService.iframePopoutUrl;
  }

  ngOnInit(): void {
    const navigationActivationSubscription = this.router.events
      .pipe(filter((event) => event instanceof ActivationEnd))
      .subscribe(() => {
        this.appService.iframePopoutUrl.set(null);
      });
    const navigationEndSubscription = this.router.events
      .pipe(filter((event) => event instanceof NavigationEnd))
      .subscribe(() => {
        const childRoute = this.route.children.pop()!;
        if (!childRoute.snapshot.data['breadcrumbIsCustom']) {
          this.breadcrumbs = childRoute.snapshot.data['breadcrumbs'] ?? 'Error';
        }
      });
    const customBreadcrumbsSubscription = this.appService.customBreadcrumbs$.subscribe(
      (breadcrumbs) => (this.breadcrumbs = breadcrumbs),
    );

    this._subscriptions.add(navigationActivationSubscription);
    this._subscriptions.add(navigationEndSubscription);
    this._subscriptions.add(customBreadcrumbsSubscription);
  }

  ngOnDestroy(): void {
    this._subscriptions.unsubscribe();
  }

  navigateToUrl(): void {
    const popoutUrl = this.popoutUrl();
    if (popoutUrl) window.open(popoutUrl, '_blank');
  }
}
