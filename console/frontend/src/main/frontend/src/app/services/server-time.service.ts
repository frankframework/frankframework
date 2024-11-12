import { Inject, inject, Injectable, LOCALE_ID } from '@angular/core';
import { formatDate } from '@angular/common';
import { AppConstants, AppService } from '../app.service';

@Injectable({
  providedIn: 'root',
})
export class ServerTimeService {
  public timezone?: string;

  private appService = inject(AppService);
  @Inject(LOCALE_ID) private locale: string = inject(LOCALE_ID);

  private appConstants: AppConstants = this.appService.APP_CONSTANTS;
  private baseTime: Date = new Date();
  private currentTime: Date = new Date();
  private timeUpdateIntervalId = -1;
  private previousLocalTime: number = Date.now();

  getIntialTime(): string {
    return formatDate(this.baseTime, this.appConstants['console.dateFormat'] as string, this.locale, this.timezone);
  }

  getCurrentTime(): number {
    return this.currentTime.getTime();
  }

  getCurrentTimeFormatted(): string {
    if (this.currentTime)
      return formatDate(
        this.currentTime,
        this.appConstants['console.dateFormat'] as string,
        this.locale,
        this.timezone,
      );
    return '';
  }

  toServerTime(value: number | Date): string {
    return formatDate(value, this.appConstants['console.dateFormat'] as string, this.locale, this.timezone);
  }

  setServerTime(serverTime: number, timezone: string): void {
    this.baseTime = new Date(serverTime);
    this.currentTime = new Date(serverTime);
    this.timezone = timezone;
    this.previousLocalTime = Date.now();

    if (this.timeUpdateIntervalId > -1) window.clearInterval(this.timeUpdateIntervalId);
    this.timeUpdateIntervalId = window.setInterval(() => this.updateTime(), 200);
    this.updateTime();
  }

  private updateTime(): void {
    const previousTime = this.previousLocalTime;
    this.previousLocalTime = Date.now();

    const timeDelta = Date.now() - previousTime;
    this.currentTime.setTime(this.currentTime.getTime() + timeDelta);
  }
}
