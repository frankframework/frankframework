import {
  Directive,
  Output,
  EventEmitter,
  ElementRef,
  HostListener,
} from '@angular/core';

@Directive({
  selector: '[appQuickSubmitForm]',
})
export class QuickSubmitFormDirective {
  @Output() quickSubmit = new EventEmitter<void>();

  constructor(private element: ElementRef) {}

  // keydown.ctrl.enter doesnt work somehow
  @HostListener('keydown', ['$event'])
  onEnter(event: KeyboardEvent): boolean | void {
    if (event.ctrlKey && event.key === 'Enter') {
      this.quickSubmit.emit();
      return false;
    }
  }
}
