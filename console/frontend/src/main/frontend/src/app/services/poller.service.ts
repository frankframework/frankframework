import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { AppConstants, AppService } from '../app.service';
import { DebugService } from './debug.service';

@Injectable({
  providedIn: 'root',
})
export class PollerService {
  data: Record<string, PollerObject> = {};
  list: string[] = [];

  private appConstants: AppConstants;

  constructor(
    private http: HttpClient,
    private appService: AppService,
    private Debug: DebugService,
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

  add(
    uri: string,
    callback?: (data: unknown) => void,
    autoStart?: boolean,
    interval?: number,
  ): PollerObject | void {
    if (!this.data[uri]) {
      this.Debug.log(
        `Adding new poller [${uri}] autoStart [${!!autoStart}] interval [${interval}]`,
      );
      const poller = new this.createPollerObject(
        uri,
        this.Debug,
        this.appService,
        this.http,
        this.appConstants,
        callback,
      );
      this.data[uri] = poller;
      if (!!autoStart) poller.fn();
      if (interval && interval > 1500) poller.setInterval(interval);
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
        Reflect.apply(callback, this, [this.data[x]]);
      }
    }
    return {
      changeInterval: (interval): void => {
        const index =
          interval ?? (this.appConstants['console.pollerInterval'] as number);
        for (const x in this.data) this.data[x].setInterval(index, false);
      },
      start: (): void => {
        this.Debug.info('starting all Pollers');
        for (const x in this.data) this.data[x].fn();
      },
      stop: (): void => {
        this.Debug.info('stopping all Pollers');
        for (const x in this.data) this.data[x].stop();
      },
      remove: (): void => {
        this.Debug.info('removing all Pollers');
        for (const x in this.data) {
          this.data[x].stop();
          delete this.data[x];
        }
        this.data = {};
      },
      list: (): string[] => {
        return Object.keys(this.data);
      },
    };
  }
}

class PollerObject {
  waiting: boolean = true;
  pollerInterval: number = this.appConstants[
    'console.pollerInterval'
  ] as number;
  fired: number = 0;
  errorList: { time: number; fired: number }[] = [];
  ai: {
    list: number[];
    avg: number;
    push: (object: number) => number | undefined;
  } = {
    list: [],
    avg: 0,
    push: (object) => {
      this.ai.list.push(object);
      if (this.ai.list.length == 5) {
        let temporary = 0;
        for (let index = this.ai.list.length - 1; index >= 0; index--) {
          temporary += this.ai.list[index];
        }
        this.ai.avg = Math.round(temporary / this.ai.list.length / 100) * 100;
        this.ai.list = [];
        return this.ai.avg;
      }
      return;
    },
  };
  private poller?: number;
  private lastPolled?: number;

  constructor(
    private uri: string,
    private Debug: DebugService,
    private appService: AppService,
    private http: HttpClient,
    private appConstants: AppConstants,
    private callback?: (data: unknown) => void,
  ) {}

  addError(): void {
    this.errorList.push({
      time: Date.now(),
      fired: this.fired,
    });
    if (this.errorList.length > 10) this.errorList.shift();
  }

  getLastError(): { time: number; fired: number } | null {
    return this.errorList.at(-1) ?? null;
  }

  started(): boolean {
    return this.poller ? true : false;
  }

  stop(): void {
    if (!this.started()) return;

    this.ai.list = [];
    this.ai.avg = 0;
    if (this.waiting) clearTimeout(this.poller);
    else clearInterval(this.poller);
    this.waiting = true;
    delete this.poller;
  }

  fn(runOnce: boolean = false): void {
    this.fired++;
    this.http
      .get<unknown>(this.appService.absoluteApiPath + this.uri)
      .subscribe({
        next: this.callback,
        error: () => {
          this.addError();

          let errors = 0;
          for (const x in this.errorList) {
            const y = this.errorList[x];
            if (
              this.fired == y.fired ||
              this.fired - 1 == y.fired ||
              this.fired - 2 == y.fired
            )
              errors++;
          }
          this.Debug.info(
            `Encountered unhandled exception, poller[${this.uri}] eventId[${this.fired}] retries[${errors}]`,
          );
          if (errors < 3) return;

          this.Debug.warn(
            `Max retries reached. Stopping poller [${this.uri}]`,
            this,
          );

          runOnce = true;
          this.stop();
        },
        complete: () => {
          if (runOnce) return;

          if (this.waiting) this.start();
        },
      });
  }

  run(): void {
    this.fn(true);
  }

  start(): void {
    if (this.started() && !this.waiting) return;

    if (this.waiting) {
      const now = Date.now();
      if (this.lastPolled) {
        const timeBetweenLastPolledAndNow = now - this.lastPolled;
        const interval = this.ai.push(timeBetweenLastPolledAndNow);
        if (interval! > 0 && interval! > this.pollerInterval) {
          this.setInterval(interval!, false);
          this.waitForResponse(false);
          return;
        }
      }
      this.poller = window.setTimeout(() => this.fn(), this.pollerInterval);
      this.lastPolled = now;
    } else
      this.poller = window.setInterval(() => this.fn(), this.pollerInterval);
  }

  setInterval(interval: number, restart?: boolean): void {
    this.Debug.info(
      `Interval for ${this.uri} changed to [${interval}] restart [${restart}]`,
    );
    this.pollerInterval = interval;
    if (restart) this.restart();
  }

  waitForResponse(bool: boolean): void {
    this.stop();
    delete this.lastPolled;
    this.waiting = !!bool;
    if (bool != this.waiting)
      this.Debug.info(`waitForResponse for ${this.uri} changed to: ${bool}`);
    this.start();
  }

  restart(): void {
    this.stop();
    this.start();
  }
}
