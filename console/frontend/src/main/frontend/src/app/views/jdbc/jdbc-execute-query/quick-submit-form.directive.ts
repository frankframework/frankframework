import { Directive, Output, EventEmitter, ElementRef, OnInit } from '@angular/core';

@Directive({
  selector: '[appQuickSubmitForm]'
})
export class QuickSubmitFormDirective implements OnInit {
  @Output() quickSubmit = new EventEmitter<void>();

  constructor(
    private element: ElementRef
  ) { }

  ngOnInit() {
    this.element.nativeElement.addEventListener('keydown', (event: KeyboardEvent) => {
      if (event.ctrlKey && event.key === 'Enter') {
        this.quickSubmit.emit();
      }
    });
  }

}
