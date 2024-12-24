import { Directive, ElementRef, HostListener } from '@angular/core';

@Directive({
  selector: '[appQuickSubmitForm]',
  standalone: true,
})
export class QuickSubmitFormDirective {
  constructor(private element: ElementRef<HTMLInputElement>) {}

  @HostListener('keydown', ['$event'])
  onEnter(event: KeyboardEvent): boolean | void {
    if (event.ctrlKey && event.key === 'Enter') {
      this.element.nativeElement.form?.submit();
    } else if (event.key === 'Enter') {
      event.preventDefault();
      event.stopPropagation();
    }
  }
}
