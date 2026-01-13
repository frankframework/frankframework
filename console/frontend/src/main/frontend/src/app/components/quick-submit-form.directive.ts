import { Directive, ElementRef, inject } from '@angular/core';

@Directive({
  selector: '[appQuickSubmitForm]',
  host: {
    '(keydown.control.enter)': 'onControlEnter($event)',
  }
})
export class QuickSubmitFormDirective {
  private element = inject(ElementRef<HTMLFormElement>);

  onControlEnter(event: Event): boolean | void {
    event.preventDefault();
    event.stopPropagation();
    this.element.nativeElement.dispatchEvent(new Event('submit'));
  }
}
