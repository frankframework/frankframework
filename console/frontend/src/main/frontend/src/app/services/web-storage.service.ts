import { Injectable } from '@angular/core';
import { DebugService } from './debug.service';
import { AppService } from '../app.service';

@Injectable({
  providedIn: 'root'
})
export class WebStorageService {
  // cache: Record<string, any> | null = null;
  // options: { expires: Date, path: string };

  private sessionStorage = this.window.sessionStorage;

  constructor(
    private window: Window
  ) {
    /* const date = new Date();
    date.setDate(date.getDate() + 7);
    this.options = {
      expires: date,
      path: '/'
    }; */
  }

  get(key: string): any {
    const value = this.sessionStorage.getItem(key);
    return value ? JSON.parse(value) : value;
  }

  set(key: string, value: any): void {
    this.sessionStorage.setItem(key, JSON.stringify(value));
  }

  remove(key: string): void {
    this.sessionStorage.removeItem(key);
  }

  clear(): void {
    this.sessionStorage.clear();
  };
}
