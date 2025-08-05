import { Component, HostListener, OnInit, OnDestroy } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { AppService } from 'src/app/app.service';

@Component({
  template: '',
})
export abstract class BaseIframeComponent implements OnInit, OnDestroy {
  protected url = '';
  protected iframeSrc?: SafeResourceUrl;
  protected redirectURL?: string;

  private topBarHeightPx = 99;

  protected constructor(
    protected readonly sanitizer: DomSanitizer,
    protected readonly appService: AppService,
  ) {}

  ngOnInit(): void {
    document.body.classList.add('no-scroll');
  }

  ngOnDestroy(): void {
    document.body.classList.remove('no-scroll');
  }

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
    this.appService.iframePopoutUrl.set(this.url);
  }

  getTopBarHeight(): number {
    return this.topBarHeightPx;
  }
}
