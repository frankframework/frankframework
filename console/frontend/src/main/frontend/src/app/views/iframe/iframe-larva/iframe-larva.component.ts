import { Component, OnInit } from '@angular/core';
import { BaseIframeComponent } from '../iframe.base';

@Component({
  selector: 'app-iframe-larva',
  templateUrl: '../iframe.component.html',
  styleUrls: ['../iframe.component.scss'],
})
export class IframeLarvaComponent extends BaseIframeComponent implements OnInit {
  constructor() {
    super();
  }

  override ngOnInit(): void {
    super.ngOnInit();
    this.setIframeSource('larva');
  }
}
