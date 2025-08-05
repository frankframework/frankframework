import { LocationStrategy } from '@angular/common';
import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { BaseIframeComponent } from '../iframe.base';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-iframe-custom-view',
  templateUrl: '../iframe.component.html',
  styleUrls: ['../iframe.component.scss'],
})
export class IframeCustomViewComponent extends BaseIframeComponent implements OnInit, OnDestroy {
  private readonly router: Router = inject(Router);
  private readonly route: ActivatedRoute = inject(ActivatedRoute);
  private readonly location: LocationStrategy = inject(LocationStrategy);
  private readonly window: Window = inject(Window);
  private routeSubscription: Subscription | null = null;

  constructor() {
    super();
  }

  override ngOnInit(): void {
    super.ngOnInit();
    this.routeSubscription = this.route.url.subscribe((url) => {
      if (url[0].path == 'customView') {
        this.loadPage();
      }
    });
  }

  override ngOnDestroy(): void {
    super.ngOnDestroy();
    this.routeSubscription?.unsubscribe();
  }

  loadPage(): void {
    const routeState = this.location.getState() as Record<string, { name: string; url: string }>;

    if (!('view' in routeState) || routeState['view'].url == '') {
      this.router.navigate(['status']);
    }

    const view = routeState['view'];

    if (view['url'].includes('http')) {
      this.window.open(view['url'], view['name']);
      this.redirectURL = view['url'];
      this.url = '';
    } else {
      this.url = this.appService.getServerPath() + view['url'];
    }

    this.iframeSrc = this.sanitizer.bypassSecurityTrustResourceUrl(this.url);
    setTimeout(() => {
      // run after router events have passed
      this.appService.iframePopoutUrl.set(this.url);
    }, 50);
  }
}
