import { Directive, ElementRef, inject, Input, OnChanges } from '@angular/core';
import { ServerTimeService } from '../services/server-time.service';

@Directive({
  selector: '[appToDate]',
})
export class ToDateDirective implements OnChanges {
  @Input() time: string | number = '';

  private element: ElementRef = inject(ElementRef);
  private serverTimeService: ServerTimeService = inject(ServerTimeService);

  ngOnChanges(): void {
    if (this.time === undefined || Number.isNaN(this.time)) return;
    if (Number.isNaN(Number(this.time))) this.time = new Date(this.time).getTime();
    this.element.nativeElement.textContent = this.serverTimeService.toServerTime(this.time as number);
  }
}
