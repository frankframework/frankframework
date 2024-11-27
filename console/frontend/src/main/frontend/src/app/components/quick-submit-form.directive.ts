import { Directive, ElementRef, HostListener } from '@angular/core';

@Directive({
  selector: '[appQuickSubmitForm]',
  standalone: true,
})
export class QuickSubmitFormDirective {
  constructor(private element: ElementRef<HTMLInputElement>) {}

  // keydown.ctrl.enter doesnt work somehow
  @HostListener('keydown', ['$event'])
  onEnter(event: KeyboardEvent): boolean | void {
    if (event.ctrlKey && event.key === 'Enter') {
      const formElement = this.element.nativeElement.form;
      formElement?.querySelector<HTMLButtonElement>('button[type="submit"]')?.click();
      return false;
    }
  }
}
