import { AfterViewInit, Directive, ElementRef, OnInit } from '@angular/core';

@Directive({
  selector: '[appSideNavigation]'
})
export class SideNavigationDirective implements AfterViewInit {

  constructor(
    private element: ElementRef<HTMLElement>
  ) { }

  ngAfterViewInit() {
    // @ts-expect-error metisMenu is not a function
    $(this.element.nativeElement).metisMenu();
  }

}
