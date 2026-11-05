import { Component, OnDestroy, OnInit, effect } from '@angular/core';
import { BaseIframeComponent } from '../iframe.base';
import { TitleCasePipe } from '@angular/common';

@Component({
  selector: 'app-iframe-ladybug',
  templateUrl: '../iframe.component.html',
  styleUrls: ['../iframe.component.scss'],
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
    // InstanceName is between "[[[" and "]]]" to trigger Ladybug's feature to do an exact match.
    // Without, Ladybug would filter as "contains string" and show too many reports.
    // TODO Ladybug issue https://github.com/wearefrank/ladybug/issues/1076. When Ladybug
    // supports a URL that expresses this intend better, then make the URL more clear.
    const ladybugIframeSource =
      instanceName === '-' ? 'ladybug' : `ladybug?filter-application=%5B%5B%5B${encodeURIComponent(instanceName)}%5D%5D%5D`;
    this.setFFIframeSource(ladybugIframeSource);
  }
}
