import { Component, OnInit } from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';
import { AppService } from 'src/app/app.service';
import { BaseIframeComponent } from '../iframe.base';

@Component({
  selector: 'app-iframe-ladybug-legacy',
  templateUrl: '../iframe.component.html',
  styleUrls: ['../iframe.component.scss'],
})
export class IframeLadybugLegacyComponent extends BaseIframeComponent implements OnInit {
  constructor(sanitizer: DomSanitizer, appService: AppService) {
    super(sanitizer, appService);
  }

  ngOnInit(): void {
    this.setIframeSource('testtool');
  }
}
