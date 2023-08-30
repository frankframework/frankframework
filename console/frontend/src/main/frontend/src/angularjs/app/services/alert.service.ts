import { appModule } from "../app.module";
import { SessionService } from "./session.service";

export type Alert = {
  type: string,
  message: any,
  time: number
  id?: number,
}

export class AlertService {
  constructor(private Session: SessionService) {}

  add(level: string | number, message: any, non_repeditive: boolean): void {
    if (non_repeditive === true)
      if (this.checkIfExists(message))
        return;

    var type;
    switch (level) {
      case "info":
      case 1:
        type = "fa fa-info";
        break;
      case "warning":
      case 2:
        type = "fa fa-warning";
        break;
      case "severe":
      case 3:
        type = "fa fa-times";
        break;
      default:
        type = "fa fa-info";
        break;
    }
    var list = this.get(true);
    var obj: Alert = {
      type: type,
      message: message,
      time: new Date().getTime()
    };
    list.unshift(obj);
    obj.id = list.length;
    this.Session.set("Alert", list);
    //sessionStorage.setItem("Alert", JSON.stringify(list));
  }

  get(preserveList: boolean): Alert[] {
    //var list = JSON.parse(sessionStorage.getItem("Alert"));
    var list = this.Session.get("Alert");
    if (preserveList == undefined) this.Session.set("Alert", []); //sessionStorage.setItem("Alert", JSON.stringify([])); //Clear after retreival
    return (list != null) ? list : [];
  }

  getCount(): number {
    return this.get(true).length || 0;
  }

  checkIfExists(message: any): boolean {
    var list = this.get(true);
    if (list.length > 0) {
      for (var i = 0; i < list.length; i++) {
        if (list[i].message == message) {
          return true;
        }
      }
    }
    return false;
  };
}

appModule.service('Alert', ['Session', AlertService]);
