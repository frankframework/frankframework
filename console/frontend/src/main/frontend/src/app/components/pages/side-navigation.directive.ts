import { AfterViewInit, Directive, ElementRef } from '@angular/core';

@Directive({
  selector: '[appSideNavigation]',
})
export class SideNavigationDirective implements AfterViewInit {
  constructor(private element: ElementRef<HTMLElement>) {}

  ngAfterViewInit(): void {
    // @ts-expect-error metisMenu is not a function
    $(this.element.nativeElement).metisMenu();
  }
}
