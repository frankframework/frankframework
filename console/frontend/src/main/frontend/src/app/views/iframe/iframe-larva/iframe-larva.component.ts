import { Component, OnInit } from '@angular/core';
import { BaseIframeComponent } from '../iframe.base';
import { TitleCasePipe } from '@angular/common';

@Component({
  selector: 'app-iframe-larva',
  templateUrl: '../iframe.component.html',
  styleUrls: ['../iframe.component.scss'],
  imports: [TitleCasePipe],
})
export class IframeLarvaComponent extends BaseIframeComponent implements OnInit {
  override ngOnInit(): void {
    super.ngOnInit();
    this.setFFIframeSource('larva');
  }
}
