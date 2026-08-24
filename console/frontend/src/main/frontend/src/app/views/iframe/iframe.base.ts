import { Component, inject, OnDestroy, OnInit, ChangeDetectionStrategy, signal } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { HttpClient } from '@angular/common/http';
import { AppService } from '../../app.service';

@Component({
  template: '',
  changeDetection: ChangeDetectionStrategy.Eager,
  host: {
    '(window:resize)': 'calcTopBarHeight()',
  },
})
export abstract class BaseIframeComponent implements OnInit, OnDestroy {
  protected url = signal('');
  protected iframeState: 'loading' | 'show' | 'error' = 'loading';
  protected iframeName = 'custom page';
  protected iframeSrc?: SafeResourceUrl;

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
    const url = `${this.appService.getServerPath()}iaf/${ffPage}`;
    this.url.set(url);
    this.setIframeSource(url, ffPage);
  }

  protected setIframeSource(url: string, pageName: string): void {
    this.appService.iframePopoutUrl.set(url);
    this.iframeSrc = this.sanitizer.bypassSecurityTrustResourceUrl(url);
    this.iframeName = pageName;
    this.checkIframeUrl(url);
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
