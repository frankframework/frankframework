import { Component, OnInit } from '@angular/core';
import { BaseIframeComponent } from '../iframe.base';
import { TitleCasePipe } from '@angular/common';

@Component({
  selector: 'app-iframe-ladybug-legacy',
  templateUrl: '../iframe.component.html',
  styleUrls: ['../iframe.component.scss'],
  imports: [TitleCasePipe],
})
export class IframeLadybugLegacyComponent extends BaseIframeComponent implements OnInit {
  override ngOnInit(): void {
    super.ngOnInit();
    this.setFFIframeSource('testtool');
  }
}
