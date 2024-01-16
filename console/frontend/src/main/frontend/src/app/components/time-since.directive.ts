import { Directive, ElementRef, Inject, Input, OnChanges, OnDestroy, OnInit, SimpleChanges } from '@angular/core';
import { AppConstants, AppService } from '../app.service';

@Directive({
  selector: '[appTimeSince]'
})
export class TimeSinceDirective implements OnInit, OnChanges, OnDestroy {
  @Input() time!: number;

  private interval?: number;
  private appConstants: AppConstants;

  constructor(
    private element: ElementRef<HTMLElement>,
    private appService: AppService
  ) {
    this.appConstants = this.appService.APP_CONSTANTS;
  }
  ngOnInit() {
    this.interval = window.setInterval(() => this.updateTime(), 300000);
  }

  ngOnChanges(changes: SimpleChanges) {
    this.updateTime();
  }

  ngOnDestroy() {
    window.clearInterval(this.interval);
  }

  updateTime() {
    if (!this.time) return;
    let seconds = Math.round((new Date().getTime() - this.time + this.appConstants['timeOffset']) / 1000);

    let minutes = seconds / 60;
    seconds = Math.floor(seconds % 60);
    if (minutes < 1) {
      return this.element.nativeElement.textContent = seconds + 's';
    }
    let hours = minutes / 60;
    minutes = Math.floor(minutes % 60);
    if (hours < 1) {
      return this.element.nativeElement.textContent = minutes + 'm';
    }
    let days = hours / 24;
    hours = Math.floor(hours % 24);
    if (days < 1) {
      return this.element.nativeElement.textContent = hours + 'h';
    }
    days = Math.floor(days);
    return this.element.nativeElement.textContent = days + 'd';
  }

}
