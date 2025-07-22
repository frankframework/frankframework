import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class SessionService {
  get<T>(key: string): T | null {
    try {
      return JSON.parse(sessionStorage.getItem(key)!);
    } catch (error) {
      console.error('Failed to retrieve session item', error);
      return null;
    }
  }

  set<T>(key: string, value: T): void {
    sessionStorage.setItem(key, JSON.stringify(value));
  }

  remove(key: string): void {
    sessionStorage.removeItem(key);
  }

  clear(): void {
    sessionStorage.clear();
  }
}
