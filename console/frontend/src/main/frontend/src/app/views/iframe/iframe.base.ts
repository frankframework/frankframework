import { Component, HostListener } from '@angular/core';
import { SafeResourceUrl } from '@angular/platform-browser';

@Component({
  template: '',
})
export abstract class BaseIframeComponent {
  url: string = '';
  iframeSrc?: SafeResourceUrl;
  redirectURL?: string;

  private topBarHeightPx = 99;

  @HostListener('window:resize', ['$event'])
  calcTopBarHeight(): void {
    const topinfobarHeight =
      document.querySelector('app-pages-topinfobar')?.getBoundingClientRect()
        .height ?? 0;
    const topnavbarHeight =
      document.querySelector('app-pages-topnavbar')?.getBoundingClientRect()
        .height ?? 0;
    const newTopBarHeight = topinfobarHeight + topnavbarHeight;
    if (newTopBarHeight !== 0) {
      this.topBarHeightPx = Math.round(newTopBarHeight);
    }
  }

  getTopBarHeight(): number {
    return this.topBarHeightPx;
  }

  navigateToUrl(): void {
    window.open(this.url, '_blank');
  }
}
