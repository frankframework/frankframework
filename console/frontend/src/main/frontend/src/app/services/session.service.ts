import { Injectable } from '@angular/core';
import { DebugService } from './debug.service';

@Injectable({
  providedIn: 'root',
})
export class SessionService {
  constructor(private Debug: DebugService) {}

  get(key: string): NonNullable<unknown> {
    //Debug.log(key, sessionStorage.getItem(key), sessionStorage.getItem(key) == null, sessionStorage.getItem(key) == "null");
    return JSON.parse(sessionStorage.getItem(key)!);
  }

  set(key: string, value: NonNullable<unknown>): void {
    sessionStorage.setItem(key, JSON.stringify(value));
  }

  remove(key: string): void {
    sessionStorage.removeItem(key);
  }

  clear(): void {
    sessionStorage.clear();
  }
}
