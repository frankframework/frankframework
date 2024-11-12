import { Directive, ElementRef, inject, Input, OnChanges, OnDestroy, OnInit } from '@angular/core';
import { ServerTimeService } from '../services/server-time.service';

@Directive({
  selector: '[appTimeSince]',
  standalone: true,
})
export class TimeSinceDirective implements OnInit, OnChanges, OnDestroy {
  @Input() time!: number;

  private element: ElementRef<HTMLElement> = inject(ElementRef);
  private serverTimeService: ServerTimeService = inject(ServerTimeService);
  private interval?: number;

  ngOnInit(): void {
    this.interval = window.setInterval(() => this.updateTime(), 300_000);
  }

  ngOnChanges(): void {
    this.updateTime();
  }

  ngOnDestroy(): void {
    window.clearInterval(this.interval);
  }

  updateTime(): string {
    if (!this.time) return '';
    let seconds = Math.round((this.serverTimeService.getCurrentTime() - this.time) / 1000);

    let minutes = seconds / 60;
    seconds = Math.floor(seconds % 60);
    if (minutes < 1) {
      return (this.element.nativeElement.textContent = `${seconds}s`);
    }
    let hours = minutes / 60;
    minutes = Math.floor(minutes % 60);
    if (hours < 1) {
      return (this.element.nativeElement.textContent = `${minutes}m`);
    }
    let days = hours / 24;
    hours = Math.floor(hours % 24);
    if (days < 1) {
      return (this.element.nativeElement.textContent = `${hours}h`);
    }
    days = Math.floor(days);
    return (this.element.nativeElement.textContent = `${days}d`);
  }
}
