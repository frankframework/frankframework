import { Component, OnDestroy, OnInit } from '@angular/core';
import { BaseIframeComponent } from '../iframe.base';

@Component({
  selector: 'app-iframe-ladybug',
  templateUrl: '../iframe.component.html',
  styleUrls: ['../iframe.component.scss'],
})
export class IframeLadybugComponent extends BaseIframeComponent implements OnInit, OnDestroy {
  override ngOnInit(): void {
    super.ngOnInit();
    this.setIframeSource('ladybug');
  }
}
