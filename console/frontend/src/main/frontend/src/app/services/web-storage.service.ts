import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class WebStorageService {
  private sessionStorage = globalThis.sessionStorage;

  get<T>(key: string): T | null {
    const value = this.sessionStorage.getItem(key);
    return value ? JSON.parse(value) : (value as null);
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
