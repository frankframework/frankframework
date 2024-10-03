import { Component, OnDestroy, OnInit } from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';
import { AppService } from 'src/app/app.service';
import { BaseIframeComponent } from '../iframe.base';

@Component({
  selector: 'app-iframe-ladybug',
  templateUrl: '../iframe.component.html',
  styleUrls: ['../iframe.component.scss'],
})
export class IframeLadybugComponent extends BaseIframeComponent implements OnInit, OnDestroy {
  constructor(sanitizer: DomSanitizer, appService: AppService) {
    super(sanitizer, appService);
  }

  override ngOnInit(): void {
    super.ngOnInit();
    this.setIframeSource('ladybug');
  }
}
