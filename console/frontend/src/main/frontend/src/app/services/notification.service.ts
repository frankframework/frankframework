import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';
import * as Tinycon from 'tinycon';

export type Notification = {
  icon: string;
  title: string;
  message: string | boolean;
  fn: ((notification: Notification) => void) | boolean;
  time: number;
  id?: number;
};

@Injectable({
  providedIn: 'root',
})
export class NotificationService {
  list: Notification[] = [];
  count: number = 0;
  private onCountUpdateSource = new Subject<void>();
  onCountUpdate$ = this.onCountUpdateSource.asObservable();

  constructor() {
    Tinycon.setOptions({
      background: '#f03d25',
    });
  }

  add(icon: string, title: string, message?: string | boolean, function_?: (notification: Notification) => void): void {
    const object: Notification = {
      icon: icon,
      title: title,
      message: message ?? false,
      fn: function_ ?? false,
      time: Date.now(),
    };
    this.list.unshift(object);
    object.id = this.list.length;
    this.count++;
    this.onCountUpdateSource.next();

    Tinycon.setBubble(this.count);
  }

  get(id: number): Notification | false {
    for (let index = 0; index < this.list.length; index++) {
      const notification = this.list[index];
      if (notification.id == id) {
        if (notification.fn) {
          window.setTimeout(() => {
            if (typeof notification.fn === 'function') Reflect.apply(notification.fn, this, [notification]);
          }, 50);
        }
        return notification;
      }
    }

    return false;
  }

  resetCount(): void {
    Tinycon.setBubble(0);
    this.count = 0;
    this.onCountUpdateSource.next();
  }

  getCount(): number {
    return this.count;
  }

  getLatest(amount: number): Notification[] {
    if (amount < 1) amount = 1;
    return this.list.slice(0, amount);
  }
}
