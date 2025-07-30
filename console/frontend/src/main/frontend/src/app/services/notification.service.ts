import { Injectable, signal, Signal, WritableSignal } from '@angular/core';
import Tinycon from 'tinycon';

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
  private _list: WritableSignal<Notification[]> = signal([]);
  private _count: WritableSignal<number> = signal(0);

  get list(): Signal<Notification[]> {
    return this._list.asReadonly();
  }

  get count(): Signal<number> {
    return this._count.asReadonly();
  }

  constructor() {
    Tinycon.setOptions({
      background: '#f03d25',
    });
  }

  add(icon: string, title: string, message?: string | boolean, function_?: (notification: Notification) => void): void {
    const newNotification: Notification = {
      icon: icon,
      title: title,
      message: message ?? false,
      fn: function_ ?? false,
      time: Date.now(),
    };
    this._list.set([newNotification, ...this.list()]);
    newNotification.id = this.list.length;
    this._count.set(this.count() + 1);

    Tinycon.setBubble(this.count());
  }

  get(id: number): Notification | null {
    for (let index = 0; index < this.list().length; index++) {
      const notification = this.list()[index];
      if (notification.id == id) {
        if (notification.fn) {
          window.setTimeout(() => {
            if (typeof notification.fn === 'function') Reflect.apply(notification.fn, this, [notification]);
          }, 50);
        }
        return notification;
      }
    }

    return null;
  }

  resetCount(): void {
    Tinycon.setBubble(0);
    this._count.set(0);
  }

  getLatest(amount: number): Notification[] {
    if (amount < 1) amount = 1;
    return this.list().slice(0, amount);
  }
}
