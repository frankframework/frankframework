import { Directive, ElementRef, inject, Input, OnDestroy, OnInit } from '@angular/core';
import { AppService } from '../app.service';
import { toObservable } from '@angular/core/rxjs-interop';
import { Subscription } from 'rxjs';

@Directive({
  selector: '[appConditionalOnProperty]',
})
export class ConditionalOnPropertyDirective implements OnInit, OnDestroy {
  @Input({ required: true }) appConditionalOnProperty!: string;
  @Input() onFalseConditionClassName: string = 'disabled';

  private readonly elementRef: ElementRef<HTMLElement> = inject(ElementRef);
  private readonly appService: AppService = inject(AppService);
  private subscription: Subscription | null = null;

  ngOnInit(): void {
    this.subscription = toObservable(this.appService.appConstants).subscribe(() => this.checkCondition());
    this.checkCondition();
  }

  ngOnDestroy(): void {
    this.subscription?.unsubscribe();
  }

  checkCondition(): void {
    if (this.appService.appConstants()[this.appConditionalOnProperty] === 'true') {
      this.elementRef.nativeElement.classList.remove(this.onFalseConditionClassName);
      this.elementRef.nativeElement.style.removeProperty('pointer-events');
      return;
    }
    this.elementRef.nativeElement.classList.add(this.onFalseConditionClassName);
    this.elementRef.nativeElement.style.pointerEvents = 'none';
  }
}
