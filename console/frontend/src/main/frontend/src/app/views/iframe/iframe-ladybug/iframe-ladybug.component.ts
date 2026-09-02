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
    const ladybugIframeSource =
      instanceName === '-' ? 'ladybug' : `ladybug?filter-application=${encodeURIComponent(instanceName)}`;
    this.setFFIframeSource(ladybugIframeSource);
  }
}
