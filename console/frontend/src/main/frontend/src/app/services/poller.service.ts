import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { AppConstants, AppService } from '../app.service';
import { DebugService } from './debug.service';

@Injectable({
  providedIn: 'root'
})
export class PollerService {
  data: Record<string, PollerObject> = {}
  list: string[] = [];

  private appConstants: AppConstants;

  constructor(
    private http: HttpClient,
    private appService: AppService,
    private Debug: DebugService
  ) {
    this.appConstants = this.appService.APP_CONSTANTS;
    this.appService.appConstants$.subscribe(() => {
      this.appConstants = this.appService.APP_CONSTANTS;
    });
  }

  createPollerObject = PollerObject;

  changeInterval(uri: string, interval: number): void {
    this.data[uri].waitForResponse(true);
    this.data[uri].setInterval(interval, false);
  }

  add(uri: string, callback?: (data: any) => void, autoStart?: boolean, interval?: number): PollerObject | void {
    if (!this.data[uri]) {
      this.Debug.log("Adding new poller [" + uri + "] autoStart [" + !!autoStart + "] interval [" + interval + "]");
      let poller = new this.createPollerObject(uri, this.Debug, this.appService, this.http, this.appConstants, callback);
      this.data[uri] = poller;
      if (!!autoStart)
        poller.fn();
      if (interval && interval > 1500)
        poller.setInterval(interval);
      return poller;
    }
  }

  remove(uri: string): void {
    if (this.data[uri]) {
      this.data[uri].stop();
      delete this.data[uri];
    }
  }

  get(uri: string): PollerObject {
    return this.data[uri];
  }

  getAll(callback?: (data: PollerObject) => void): {
    changeInterval: (interval: number) => void;
    start: () => void;
    stop: () => void;
    remove: () => void;
    list: () => string[];
  } {
    if (callback !== undefined) {
      for (const x in this.data) {
        callback.apply(this, [this.data[x]]);
      }
    }
    return {
      changeInterval: (interval) => {
        let i = interval || this.appConstants["console.pollerInterval"];
        for (const x in this.data)
          this.data[x].setInterval(i, false);
      },
      start: () => {
        this.Debug.info("starting all Pollers");
        for (const x in this.data)
          this.data[x].fn();
      },
      stop: () => {
        this.Debug.info("stopping all Pollers");
        for (const x in this.data)
          this.data[x].stop();
      },
      remove: () => {
        this.Debug.info("removing all Pollers");
        for (const x in this.data) {
          this.data[x].stop();
          delete this.data[x];
        }
        this.data = {};
      },
      list: () => {
        return Object.keys(this.data);
      }
    }
  }
}

class PollerObject {
  waiting: boolean = true;
  pollerInterval: number = this.appConstants["console.pollerInterval"];
  fired: number = 0;
  errorList: { time: number, fired: number }[] = [];
  ai: {
    list: any[];
    avg: number;
    push: (obj: any) => number | undefined;
  } = {
      list: [],
      avg: 0,
      push: (obj) => {
        this.ai.list.push(obj);
        if (this.ai.list.length == 5) {
          let tmp = 0;
          for (let i = this.ai.list.length - 1; i >= 0; i--) {
            tmp += this.ai.list[i];
          }
          this.ai.avg = Math.round((tmp / this.ai.list.length) / 100) * 100;
          this.ai.list = [];
          return this.ai.avg;
        }
        return undefined;
      }
    };
  private poller?: number;
  private lastPolled?: number;

  constructor(
    private uri: string,
    private Debug: DebugService,
    private appService: AppService,
    private http: HttpClient,
    private appConstants: AppConstants,
    private callback?: (data: any) => void
  ) { }

  addError(): void {
    this.errorList.push({
      time: (new Date()).getTime(),
      fired: this.fired
    });
    if (this.errorList.length > 10)
      this.errorList.shift();
  }

  getLastError(): { time: number, fired: number } {
    return this.errorList[this.errorList.length - 1];
  }

  started(): boolean { return (this.poller) ? true : false; }

  stop(): void {
    if (!this.started()) return;

    this.ai.list = [];
    this.ai.avg = 0;
    if (this.waiting)
      clearTimeout(this.poller);
    else
      clearInterval(this.poller);
    this.waiting = true;
    delete this.poller;
  }

  fn(runOnce: boolean = false): void {
    this.fired++;
    this.http.get(this.appService.absoluteApiPath + this.uri).subscribe({
      next: this.callback,
      error: () => {
        this.addError();

        let e = 0;
        for (const x in this.errorList) {
          let y = this.errorList[x];
          if (this.fired == y.fired || this.fired - 1 == y.fired || this.fired - 2 == y.fired)
            e++;
        }
        this.Debug.info("Encountered unhandled exception, poller[" + this.uri + "] eventId[" + this.fired + "] retries[" + e + "]");
        if (e < 3) return;

        this.Debug.warn("Max retries reached. Stopping poller [" + this.uri + "]", this);

        runOnce = true;
        this.stop();
      },
      complete: () => {
        if (runOnce) return;

        if (this.waiting)
          this.start();
      }
    })
  }

  run(): void {
    this.fn(true);
  }

  start(): void {
    if (this.started() && !this.waiting) return;

    if (this.waiting) {
      let now = new Date().getTime();
      if (this.lastPolled) {
        let timeBetweenLastPolledAndNow = now - this.lastPolled;
        let interval = this.ai.push(timeBetweenLastPolledAndNow);
        if (interval! > 0 && interval! > this.pollerInterval) {
          this.setInterval(interval!, false);
          this.waitForResponse(false);
          return;
        }
      }
      this.poller = window.setTimeout(() => this.fn(), this.pollerInterval);
      this.lastPolled = now;
    }
    else
      this.poller = window.setInterval(() => this.fn(), this.pollerInterval);
  }

  setInterval(interval: number, restart?: boolean): void {
    this.Debug.info("Interval for " + this.uri + " changed to [" + interval + "] restart [" + restart + "]");
    this.pollerInterval = interval;
    if (restart)
      this.restart();
  }

  waitForResponse(bool: boolean): void {
    this.stop();
    delete this.lastPolled;
    this.waiting = !!bool;
    if (bool != this.waiting)
      this.Debug.info("waitForResponse for " + this.uri + " changed to: " + bool);
    this.start();
  }

  restart(): void {
    this.stop();
    this.start();
  };
}
