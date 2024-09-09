import { Component, HostListener } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { AppService } from 'src/app/app.service';

@Component({
  template: '',
})
export abstract class BaseIframeComponent {
  protected url: string = '';
  protected iframeSrc?: SafeResourceUrl;
  protected redirectURL?: string;

  private topBarHeightPx = 99;

  constructor(
    protected sanitizer: DomSanitizer,
    protected appService: AppService,
  ) {}

  @HostListener('window:resize', ['$event'])
  calcTopBarHeight(): void {
    const topinfobarHeight = document.querySelector('app-pages-topinfobar')?.getBoundingClientRect().height ?? 0;
    const topnavbarHeight = document.querySelector('app-pages-topnavbar')?.getBoundingClientRect().height ?? 0;
    const newTopBarHeight = topinfobarHeight + topnavbarHeight;
    if (newTopBarHeight !== 0) {
      this.topBarHeightPx = Math.round(newTopBarHeight);
    }
  }

  protected setIframeSource(ffPage: string): void {
    this.url = `${this.appService.getServerPath()}iaf/${ffPage}`;
    this.iframeSrc = this.sanitizer.bypassSecurityTrustResourceUrl(this.url);
    this.appService.setIframePopoutUrl(this.url);
  }

  getTopBarHeight(): number {
    return this.topBarHeightPx;
  }
}
