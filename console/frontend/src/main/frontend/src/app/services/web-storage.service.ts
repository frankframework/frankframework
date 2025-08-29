import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class WebStorageService {
  // cache: Record<string, any> | null = null;
  // options: { expires: Date, path: string };

  private sessionStorage = globalThis.sessionStorage;

  constructor() {
    /* const date = new Date();
    date.setDate(date.getDate() + 7);
    this.options = {
      expires: date,
      path: '/'
    }; */
  }

  get<T>(key: string): T {
    const value = this.sessionStorage.getItem(key);
    return (value ? JSON.parse(value) : value) as T;
  }

  set<T>(key: string, value: T): void {
    this.sessionStorage.setItem(key, JSON.stringify(value));
  }

  remove(key: string): void {
    this.sessionStorage.removeItem(key);
  }

  clear(): void {
    this.sessionStorage.clear();
  }
}
