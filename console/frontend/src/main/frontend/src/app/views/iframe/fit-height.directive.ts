import { Directive, ElementRef, OnInit } from '@angular/core';

@Directive({
  selector: '[appFitHeight]',
})
export class FitHeightDirective implements OnInit {
  private height = {
    topnavbar: 0,
    topinfobar: 0,
    window: 0,
    min: 800,
  };

  constructor(private element: ElementRef<HTMLElement>) {}

  ngOnInit(): void {
    // TODO rewrite since this is only used for iframes (to fill the page properly? maybe a css solution is better)

    window.addEventListener('resize', () => {
      this.height.window = window.innerHeight;
      this.fitHeight();
    });

    document
      .querySelector<HTMLElement>('nav.navbar-default')
      ?.addEventListener('resize', (event: Event): void => {
        this.height.min = (event.currentTarget as HTMLElement).clientHeight;
        this.fitHeight();
      });

    document
      .querySelector<HTMLElement>('.topnavbar')
      ?.addEventListener('resize', (event: Event): void => {
        this.height.topnavbar = (
          event.currentTarget as HTMLElement
        ).clientHeight;
        this.fitHeight();
      });

    document
      .querySelector<HTMLElement>('.topinfobar')
      ?.addEventListener('resize', (event: Event): void => {
        this.height.topinfobar = (
          event.currentTarget as HTMLElement
        ).clientHeight;
        this.fitHeight();
      });

    this.fitHeight();
  }

  fitHeight(): void {
    const offset = this.height.topnavbar + this.height.topinfobar;
    const height =
      (this.height.window > this.height.min
        ? this.height.window
        : this.height.min) - offset;
    this.element.nativeElement.style['height'] = `${height}px`;
    this.element.nativeElement.style['minHeight'] = `${height}px`;
  }
}
