import { Component, OnDestroy, OnInit, ChangeDetectionStrategy, effect } from '@angular/core';
import { BaseIframeComponent } from '../iframe.base';
import { TitleCasePipe } from '@angular/common';

@Component({
  selector: 'app-iframe-ladybug',
  templateUrl: '../iframe.component.html',
  styleUrls: ['../iframe.component.scss'],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [TitleCasePipe],
})
export class IframeLadybugComponent extends BaseIframeComponent implements OnInit, OnDestroy {
  constructor() {
    super();
    effect(() => {
      const instanceName = this.appService.instanceName();
      this.setLadybugSource(instanceName);
    });
  }

  private setLadybugSource(instanceName: string): void {
    /* See https://github.com/wearefrank/ladybug/issues/1076 for future changes in exact match expression */
    const encodedExactFilterMatch = encodeURIComponent(`[[[${instanceName}]]]`);
    const ladybugIframeSource =
      instanceName === '-' ? 'ladybug' : `ladybug?filter-application=${encodedExactFilterMatch}`;
    this.setFFIframeSource(ladybugIframeSource);
  }
}
