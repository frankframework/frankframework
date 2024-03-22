import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class SessionService {
  constructor() {}

  get<T>(key: string): T {
    //Debug.log(key, sessionStorage.getItem(key), sessionStorage.getItem(key) == null, sessionStorage.getItem(key) == "null");
    return JSON.parse(sessionStorage.getItem(key)!);
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
