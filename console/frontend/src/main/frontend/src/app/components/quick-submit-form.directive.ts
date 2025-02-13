import { Directive, ElementRef, HostListener } from '@angular/core';

@Directive({
  selector: '[appQuickSubmitForm]',
})
export class QuickSubmitFormDirective {
  constructor(private element: ElementRef<HTMLFormElement>) {}

  @HostListener('keydown.ctrl.enter', ['$event'])
  onCtrlEnter(): boolean | void {
    this.element.nativeElement.dispatchEvent(new Event('submit'));
  }
}
