import { Directive, ElementRef, HostListener, inject } from '@angular/core';

@Directive({
  selector: '[appQuickSubmitForm]',
})
export class QuickSubmitFormDirective {
  private element = inject(ElementRef<HTMLFormElement>);

  @HostListener('keydown.control.enter', ['$event'])
  onControlEnter(event: KeyboardEvent): boolean | void {
    event.preventDefault();
    event.stopPropagation();
    this.element.nativeElement.dispatchEvent(new Event('submit'));
  }
}
