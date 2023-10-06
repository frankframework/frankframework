import { Directive, ElementRef, OnInit } from '@angular/core';

@Directive({
  selector: '[appFitHeight]'
})
export class FitHeightDirective implements OnInit {

  height = {
    topnavbar: 0,
    topinfobar: 0,
    window: 0,
    min: 800
  };

  constructor(private element: ElementRef<HTMLElement>) { }

  ngOnInit() {
    window.addEventListener("resize", () => {
      this.height.window = window.innerHeight;
      this.fitHeight();
    });

    const _this = this;

    document.querySelector<HTMLElement>('nav.navbar-default')?.addEventListener("resize", function () {
      _this.height.min = this.clientHeight;
      _this.fitHeight();
    });

    document.querySelector<HTMLElement>('.topnavbar')?.addEventListener("resize", function () {
      _this.height.topnavbar = this.clientHeight;
      _this.fitHeight();
    });

    document.querySelector<HTMLElement>('.topinfobar')?.addEventListener("resize", function () {
      _this.height.topinfobar = this.clientHeight;
      _this.fitHeight();
    });

    this.fitHeight();
  }

  fitHeight() {
    var offset = this.height.topnavbar + this.height.topinfobar;
    var height = (this.height.window > this.height.min ? this.height.window : this.height.min) - offset;
    this.element.nativeElement.style["height"] = height + "px";
    this.element.nativeElement.style["minHeight"] = height + "px";
  }

}
