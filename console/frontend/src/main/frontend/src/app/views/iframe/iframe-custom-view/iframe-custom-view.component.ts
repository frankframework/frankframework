import { LocationStrategy } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';
import { Router } from '@angular/router';
import { AppService } from 'src/app/app.service';
import { BaseIframeComponent } from '../iframe.base';

@Component({
  selector: 'app-iframe-custom-view',
  templateUrl: '../iframe.component.html',
  styleUrls: ['../iframe.component.scss'],
})
export class IframeCustomViewComponent
  extends BaseIframeComponent
  implements OnInit
{
  constructor(
    sanitizer: DomSanitizer,
    appService: AppService,
    private router: Router,
    private location: LocationStrategy,
    private window: Window,
  ) {
    super(sanitizer, appService);
  }

  ngOnInit(): void {
    const routeState = this.location.getState() as Record<
      string,
      { name: string; url: string }
    >;

    if (!('view' in routeState) || routeState['view'].url == '')
      this.router.navigate(['status']);

    const view = routeState['view'];

    if (view['url'].includes('http')) {
      this.window.open(view['url'], view['name']);
      this.redirectURL = view['url'];
    } else this.url = this.appService.getServerPath() + view['url'];
    this.iframeSrc = this.sanitizer.bypassSecurityTrustResourceUrl(this.url);
    this.appService.setIframePopoutUrl(this.url);
  }
}
