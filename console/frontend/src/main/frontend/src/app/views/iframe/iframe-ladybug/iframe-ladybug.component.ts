import { Component, OnDestroy, OnInit } from '@angular/core';
import { BaseIframeComponent } from '../iframe.base';
import { TitleCasePipe } from '@angular/common';

@Component({
  selector: 'app-iframe-ladybug',
  templateUrl: '../iframe.component.html',
  styleUrls: ['../iframe.component.scss'],
  imports: [TitleCasePipe],
})
export class IframeLadybugComponent extends BaseIframeComponent implements OnInit, OnDestroy {
  override ngOnInit(): void {
    super.ngOnInit();
    this.setFFIframeSource('ladybug');
  }
}
