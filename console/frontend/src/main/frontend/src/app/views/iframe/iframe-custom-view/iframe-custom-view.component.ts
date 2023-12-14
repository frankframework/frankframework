import { LocationStrategy } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import { AppService } from 'src/app/app.service';

@Component({
  selector: 'app-iframe-custom-view',
  templateUrl: '../iframe.component.html',
  styleUrls: ['../iframe.component.scss']
})
export class IframeCustomViewComponent implements OnInit {
  url: string = "";
  iframeSrc?: SafeResourceUrl;
  redirectURL: string = "";

  constructor(
    private sanitizer: DomSanitizer,
    private router: Router,
    private location: LocationStrategy,
    private appService: AppService,
    private window: Window
  ) { };

  ngOnInit() {
    const routeState = this.location.getState() as Record<string, any>;

    if (!('view' in routeState) || routeState['view'].url == "")
      this.router.navigate(['status']);

    const view = routeState['view'];

    if (view["url"].indexOf("http") > -1) {
      this.window.open(view["url"], view["name"]);
      this.redirectURL = view["url"];
    } else
      this.url = this.appService.getServerPath() + view["url"];
    this.iframeSrc = this.sanitizer.bypassSecurityTrustResourceUrl(this.url);
  }
}
