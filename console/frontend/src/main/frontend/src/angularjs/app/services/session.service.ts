import { DebugService } from "src/app/services.types";
import { appModule } from "../app.module";

export class SessionService {
  constructor(private Debug: DebugService){}

  get(key: string): any {
    //Debug.log(key, sessionStorage.getItem(key), sessionStorage.getItem(key) == null, sessionStorage.getItem(key) == "null");
    return JSON.parse(sessionStorage.getItem(key)!);
  }

  set(key: string, value: any): void {
    sessionStorage.setItem(key, JSON.stringify(value));
  }

  remove(key: string): void {
    sessionStorage.removeItem(key);
  }

  clear(): void {
    sessionStorage.clear();
  }
}

appModule.service('Session', ['Debug', SessionService]);
