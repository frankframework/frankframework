import { Component, OnInit } from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';
import { AppService } from 'src/app/app.service';
import { BaseIframeComponent } from '../iframe.base';
@Component({
  selector: 'app-iframe-larva',
  templateUrl: '../iframe.component.html',
  styleUrls: ['../iframe.component.scss'],
})
export class IframeLarvaComponent extends BaseIframeComponent implements OnInit {
  constructor(
    protected override readonly sanitizer: DomSanitizer,
    protected override readonly appService: AppService,
  ) {
    super(sanitizer, appService);
  }

  override ngOnInit(): void {
    super.ngOnInit();
    this.setIframeSource('larva');
  }
}
