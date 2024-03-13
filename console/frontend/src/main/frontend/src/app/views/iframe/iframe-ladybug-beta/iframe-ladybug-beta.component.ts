import { Component, OnInit } from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';
import { AppService } from 'src/app/app.service';
import { BaseIframeComponent } from '../iframe.base';

@Component({
  selector: 'app-iframe-ladybug-beta',
  templateUrl: '../iframe.component.html',
  styleUrls: ['../iframe.component.scss'],
})
export class IframeLadybugBetaComponent
  extends BaseIframeComponent
  implements OnInit
{
  constructor(
    private sanitizer: DomSanitizer,
    private appService: AppService,
  ) {
    super();
  }

  ngOnInit(): void {
    this.url = `${this.appService.getServerPath()}iaf/ladybug`;
    this.iframeSrc = this.sanitizer.bypassSecurityTrustResourceUrl(this.url);
  }
}
