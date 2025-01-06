import { LocationStrategy } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import { AppService } from 'src/app/app.service';
import { BaseIframeComponent, baseImports } from '../iframe.base';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-iframe-custom-view',
  imports: baseImports,
  templateUrl: '../iframe.component.html',
  styleUrls: ['../iframe.component.scss'],
})
export class IframeCustomViewComponent extends BaseIframeComponent implements OnInit, OnDestroy {
  private routeSubscription?: Subscription;

  constructor(
    protected override sanitizer: DomSanitizer,
    protected override appService: AppService,
    private router: Router,
    private route: ActivatedRoute,
    private location: LocationStrategy,
    private window: Window,
  ) {
    super(sanitizer, appService);
  }

  override ngOnInit(): void {
    super.ngOnInit();
    this.routeSubscription = this.route.url.subscribe((url) => {
      if (url[0].path == 'customView') {
        this.loadPage();
      }
    });
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
      this.appService.setIframePopoutUrl(this.url);
    }, 50);
  }

  override ngOnDestroy(): void {
    super.ngOnDestroy();
    this.routeSubscription?.unsubscribe();
  }
}
