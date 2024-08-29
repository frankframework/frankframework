import { LocationStrategy } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import { AppService } from 'src/app/app.service';
import { BaseIframeComponent } from '../iframe.base';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-iframe-custom-view',
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

  ngOnInit(): void {
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
    this.appService.setIframePopoutUrl(this.url);
  }

  ngOnDestroy(): void {
    this.routeSubscription?.unsubscribe();
  }
}
