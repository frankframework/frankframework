import { Component, OnInit } from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';
import { AppService } from 'src/app/app.service';
import { BaseIframeComponent, baseImports } from '../iframe.base';

@Component({
  selector: 'app-iframe-ladybug-legacy',
  imports: baseImports,
  templateUrl: '../iframe.component.html',
  styleUrls: ['../iframe.component.scss'],
})
export class IframeLadybugLegacyComponent extends BaseIframeComponent implements OnInit {
  constructor(sanitizer: DomSanitizer, appService: AppService) {
    super(sanitizer, appService);
  }

  override ngOnInit(): void {
    super.ngOnInit();
    this.setIframeSource('testtool');
  }
}
