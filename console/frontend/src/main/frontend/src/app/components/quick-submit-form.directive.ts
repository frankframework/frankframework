import { Directive, ElementRef, HostListener } from '@angular/core';

@Directive({
  selector: '[appQuickSubmitForm]',
})
export class QuickSubmitFormDirective {
  constructor(private element: ElementRef<HTMLFormElement>) {}

  @HostListener('keydown', ['$event'])
  onEnter(event: KeyboardEvent): boolean | void {
    if (event.key === 'Enter') {
      event.preventDefault();
      event.stopPropagation();
      if (event.ctrlKey) {
        this.element.nativeElement.dispatchEvent(new Event('submit'));
      }
    }
  }
}
