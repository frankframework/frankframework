import { inject, Injectable } from '@angular/core';
import { SessionService } from './session.service';

export type Alert = {
  type: string;
  message: string;
  time: number;
  id?: number;
};

@Injectable({
  providedIn: 'root',
})
export class AlertService {
  private readonly Session: SessionService = inject(SessionService);

  add(level: string | number, message: string, non_repeditive: boolean): void {
    if (non_repeditive && this.checkIfExists(message)) return;

    let type;
    switch (level) {
      case 'info':
      case 1: {
        type = 'fa fa-info';
        break;
      }
      case 'warning':
      case 2: {
        type = 'fa fa-warning';
        break;
      }
      case 'severe':
      case 3: {
        type = 'fa fa-times';
        break;
      }
      default: {
        type = 'fa fa-info';
        break;
      }
    }
    const list = this.get(true);
    const object: Alert = {
      type: type,
      message: message,
      time: Date.now(),
    };
    list.unshift(object);
    object.id = list.length;
    this.Session.set('Alert', list);
    //sessionStorage.setItem("Alert", JSON.stringify(list));
  }

  get(preserveList?: boolean): Alert[] {
    //var list = JSON.parse(sessionStorage.getItem("Alert"));
    const list = this.Session.get('Alert');
    if (!preserveList) this.Session.set('Alert', []); //sessionStorage.setItem("Alert", JSON.stringify([])); //Clear after retreival
    return list == null ? [] : (list as Alert[]);
  }

  getCount(): number {
    return this.get(true).length || 0;
  }

  checkIfExists(message: string): boolean {
    const list = this.get(true);
    if (list.length > 0) {
      for (const element of list) {
        if (element.message == message) {
          return true;
        }
      }
    }
    return false;
  }
}
