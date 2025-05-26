import { Inject, inject, Injectable, LOCALE_ID } from '@angular/core';
import { formatDate } from '@angular/common';

@Injectable({
  providedIn: 'root',
})
export class ServerTimeService {
  public timezone?: string;

  @Inject(LOCALE_ID) private locale: string = inject(LOCALE_ID);
  private baseTime: Date = new Date();
  private currentTime: Date = new Date();
  private timeUpdateIntervalId = -1;
  private previousLocalTime: number = Date.now();
  private dateFormat: string = 'yyyy-MM-dd HH:mm:ss';
  private serverTimezoneOffset: number = 0;

  getIntialTime(): string {
    return formatDate(this.baseTime, this.dateFormat, this.locale, this.timezone);
  }

  // doesnt contain timezone data so will return everything in locale time
  getCurrentTime(): number {
    return this.currentTime.getTime();
  }

  // does use timezone so will show in server timezone
  getCurrentTimeFormatted(): string {
    const zonedTime = this.currentTime.toLocaleString(this.locale, { timeZone: this.timezone });
    return formatDate(zonedTime, this.dateFormat, this.locale, this.timezone);
  }

  getDateWithOffset(): Date {
    const totalOffset = this.serverTimezoneOffset + this.getTimezoneOffsetToUTCInSeconds();
    const totalOffsetInMiliseconds = totalOffset * 1000;
    return new Date(this.currentTime.getTime() + totalOffsetInMiliseconds);
  }

  toServerTime(value: number | Date): string {
    const zonedTime = new Date(value).toLocaleString(this.locale, { timeZone: this.timezone });
    return formatDate(zonedTime, this.dateFormat, this.locale, this.timezone);
  }

  setServerTime(serverTime: number, timezone?: string, timezoneOffset?: number): void {
    this.baseTime = new Date(serverTime);
    this.currentTime = new Date(serverTime);
    this.timezone = timezone;
    this.serverTimezoneOffset = timezoneOffset ?? 0;
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

  private getTimezoneOffsetToUTCInSeconds(): number {
    return this.currentTime.getTimezoneOffset() * 60;
  }
}
