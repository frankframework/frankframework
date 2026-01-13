import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { AppService } from 'src/app/app.service';
import { HttpClient } from '@angular/common/http';

@Component({
  template: '',
  host: {
    '(window:resize)': 'calcTopBarHeight()',
  }
})
export abstract class BaseIframeComponent implements OnInit, OnDestroy {
  protected url = '';
  protected iframeState: 'loading' | 'show' | 'error' = 'loading';
  protected iframeName = 'custom page';
  protected iframeSrc?: SafeResourceUrl;
  protected redirectURL?: string;

  protected readonly sanitizer = inject(DomSanitizer);
  protected readonly appService = inject(AppService);
  protected readonly http = inject(HttpClient);

  private topBarHeightPx = 99;

  calcTopBarHeight(): void {
    const topinfobarHeight = document.querySelector('app-pages-topinfobar')?.getBoundingClientRect().height ?? 0;
    const topnavbarHeight = document.querySelector('app-pages-topnavbar')?.getBoundingClientRect().height ?? 0;
    const newTopBarHeight = topinfobarHeight + topnavbarHeight;
    if (newTopBarHeight !== 0) {
      this.topBarHeightPx = Math.round(newTopBarHeight);
    }
  }

  ngOnInit(): void {
    document.body.classList.add('no-scroll');
  }

  ngOnDestroy(): void {
    document.body.classList.remove('no-scroll');
  }

  protected setFFIframeSource(ffPage: string): void {
    this.url = `${this.appService.getServerPath()}iaf/${ffPage}`;
    this.setIframeSource(this.url, ffPage);
  }

  protected setIframeSource(url: string, pageName: string): void {
    this.appService.iframePopoutUrl.set(url);
    this.iframeSrc = this.sanitizer.bypassSecurityTrustResourceUrl(url);
    this.iframeName = pageName;
    this.checkIframeUrl(this.url);
  }

  protected checkIframeUrl(url: string): void {
    this.http.head(url).subscribe({
      next: () => (this.iframeState = 'show'),
      error: () => (this.iframeState = 'error'),
    });
  }

  protected getTopBarHeight(): number {
    return this.topBarHeightPx;
  }
}
