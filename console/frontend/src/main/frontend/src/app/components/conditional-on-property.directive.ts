import { Directive, ElementRef, inject, Input, OnInit } from '@angular/core';
import { AppService } from '../app.service';

@Directive({
  selector: '[appConditionalOnProperty]',
})
export class ConditionalOnPropertyDirective implements OnInit {
  @Input({ required: true }) appConditionalOnProperty!: string;
  @Input() onFalseConditionClassName: string = 'disabled';

  private readonly elementRef: ElementRef<HTMLElement> = inject(ElementRef);
  private readonly appService: AppService = inject(AppService);

  ngOnInit(): void {
    this.appService.appConstants$.subscribe(() => this.checkCondition());
    this.checkCondition();
  }

  checkCondition(): void {
    if (this.appService.APP_CONSTANTS[this.appConditionalOnProperty] === 'true') {
      this.elementRef.nativeElement.classList.remove(this.onFalseConditionClassName);
      this.elementRef.nativeElement.style.removeProperty('pointer-events');
      return;
    }
    this.elementRef.nativeElement.classList.add(this.onFalseConditionClassName);
    this.elementRef.nativeElement.style.pointerEvents = 'none';
  }
}
