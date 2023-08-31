import * as Tinycon from 'tinycon';
import { appModule } from "../app.module";
import { Subject } from 'rxjs';

type Notification = {
  icon: string,
  title: string,
  message: string | boolean,
  fn: ((notification: Notification) => void) | boolean,
  time: number,
  id?: number
}

export class NotificationService {
  private onCountUpdateSource = new Subject<void>();

  list: Notification[] = [];
  count: number = 0;
  onCountUpdate$ = this.onCountUpdateSource.asObservable();

  constructor(private $rootScope: angular.IRootScopeService, private $timeout: angular.ITimeoutService){
    Tinycon.setOptions({
      background: '#f03d25'
    });
  }

  add(icon: string, title: string, msg?: string | boolean, fn?: (notification: Notification) => void): void {
    var obj: Notification = {
      icon: icon,
      title: title,
      message: (msg) ? msg : false,
      fn: (fn) ? fn : false,
      time: new Date().getTime()
    };
    this.list.unshift(obj);
    obj.id = this.list.length;
    this.onCountUpdateSource.next();
    this.count++;

    Tinycon.setBubble(this.count);
  }

  get(id: number): Notification | false {
    for (var i = 0; i < this.list.length; i++) {
      var notification = this.list[i];
      if (notification.id == id) {
        if (notification.fn) {
          this.$timeout(() => {
            if(typeof notification.fn === 'function')
              notification.fn.apply(this, [notification]);
          }, 50);
        }
        return notification;
      }
    }

    return false;
  }

  resetCount(): void {
    Tinycon.setBubble(0);
    this.onCountUpdateSource.next();
    this.count = 0;
  }

  getCount(): number {
    return this.count;
  }

  getLatest(amount: number): Notification[] {
    if (amount < 1) amount = 1;
    return this.list.slice(0, amount);
  }
}

appModule.service('Notification', ['$rootScope', '$timeout', NotificationService]);
