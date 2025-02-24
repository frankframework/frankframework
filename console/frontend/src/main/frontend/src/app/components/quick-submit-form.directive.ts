import { Directive, ElementRef, HostListener } from '@angular/core';

@Directive({
  selector: '[appQuickSubmitForm]',
})
export class QuickSubmitFormDirective {
  constructor(private element: ElementRef<HTMLFormElement>) {}

  @HostListener('keydown.control.enter', ['$event'])
  onControlEnter(event: KeyboardEvent): boolean | void {
    event.preventDefault();
    event.stopPropagation();
    this.element.nativeElement.dispatchEvent(new Event('submit'));
  }
}
