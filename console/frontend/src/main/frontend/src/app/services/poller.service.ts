import { HttpClient } from '@angular/common/http';
import { AppService } from '../app.service';
import { DebugService } from './debug.service';
import { Subscription } from 'rxjs';
import { inject, Injectable } from '@angular/core';

type PollerState = 'RUNNING' | 'WAITING' | 'STOPPED';

@Injectable({
  providedIn: 'root',
})
export class PollerService {
  private readonly http: HttpClient = inject(HttpClient);
  private readonly Debug: DebugService = inject(DebugService);
  private readonly appService: AppService = inject(AppService);
  // impossible to keep track of T in Poller<T>, even with a wrapper function
  private pollers: Record<string, Poller<unknown>> = {};

  changeInterval(url: string, intervalTime: number): void {
    this.pollers[url].setInterval(intervalTime, true);
  }

  add<T>(url: string, callback: (data: T) => void, intervalTime?: number, runOnce?: boolean): Poller<T> {
    if (url in this.pollers) {
      return this.pollers[url];
    }

    this.Debug.log(`Adding new poller [${url}] runOnce [${runOnce}] interval [${intervalTime}]`);

    const interval = intervalTime ?? (this.appService.appConstants()['console.pollerInterval'] as number);

    const poller = new Poller<T>(`${this.appService.absoluteApiPath}${url}`, interval, callback, this.http, this.Debug);

    this.pollers[url] = poller as Poller<unknown>; // this hurts yet there is no better solution

    if (runOnce) {
      poller.runOnce();
      return poller;
    }
    poller.start();
    return poller;
  }

  remove(url: string): void {
    if (url in this.pollers) {
      this.pollers[url].stop();
      delete this.pollers[url];
    }
  }

  get(url: string): Poller<unknown> {
    return this.pollers[url];
  }

  getAll(): {
    changeInterval: (interval: number) => void;
    start: () => void;
    stop: () => void;
    remove: () => void;
    list: () => string[];
  } {
    return {
      changeInterval: (interval): void => {
        for (const index in this.pollers) this.pollers[index].setInterval(interval, false);
      },
      start: (): void => {
        this.Debug.info('starting all Pollers');
        for (const index in this.pollers) this.pollers[index].start();
      },
      stop: (): void => {
        this.Debug.info('stopping all Pollers');
        for (const index in this.pollers) this.pollers[index].stop();
      },
      remove: (): void => {
        this.Debug.info('removing all Pollers');
        for (const index in this.pollers) {
          this.pollers[index].stop();
        }
        this.pollers = {};
      },
      list: (): string[] => {
        return Object.keys(this.pollers);
      },
    };
  }
}

export class Poller<T> {
  private timesFired: number = 0;
  private state: PollerState = 'STOPPED';
  private errorCount: number = 0;
  private maxErrorCount: number = 3;

  private windowIntervalId: number | null = null;
  private runningSubscription: Subscription | null = null;

  constructor(
    private url: string,
    private intervalTime: number,
    private callback: (data: T) => void,
    private http: HttpClient,
    private Debug: DebugService,
  ) {}

  getState(): PollerState {
    return this.state;
  }

  setInterval(intervalTime: number, restart?: boolean): void {
    this.Debug.info(`Interval for ${this.url} changed to [${intervalTime}] restart [${restart}]`);
    this.intervalTime = intervalTime;
    if (restart) this.restart();
  }

  start(): void {
    if (this.state !== 'STOPPED') return;

    this.runningSubscription = null;
    this.windowIntervalId = window.setInterval(() => this.run(), this.intervalTime);
    this.state = 'WAITING';

    this.run();
  }

  stop(): void {
    if (this.state === 'STOPPED') return;

    if (this.state === 'RUNNING') this.runningSubscription?.unsubscribe();
    window.clearInterval(this.windowIntervalId!);
    this.windowIntervalId = null;
    this.state = 'STOPPED';
  }

  restart(): void {
    this.stop();
    this.start();
  }

  runOnce(): void {
    this.run(true);
  }

  private run(once?: boolean): void {
    if (this.state === 'RUNNING') return;

    this.errorCount = 0;
    this.runningSubscription = this.http.get<T>(this.url).subscribe({
      next: this.callback,
      error: (error) => {
        this.errorCount += 1;
        this.Debug.info(
          `Encountered unhandled exception, poller [${this.url}] with { runs: ${this.timesFired}, retries: ${this.errorCount} }`,
          error,
        );

        if (this.errorCount < this.maxErrorCount) return;

        this.Debug.warn(`Max retries reached. Stopping poller [${this.url}]`, this);
        this.stop();
      },
      complete: () => {
        this.runningSubscription = null;
        this.state = once ? 'STOPPED' : 'WAITING';
      },
    });
    this.state = 'RUNNING';
    this.timesFired += 1;
  }
}
