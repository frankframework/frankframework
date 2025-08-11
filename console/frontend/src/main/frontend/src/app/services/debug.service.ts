import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class DebugService {
  private level = 0; //ERROR
  private levelEnums = ['ERROR', 'WARN', 'INFO', 'DEBUG'];
  private inGroup = false;

  getLevel(): number {
    return this.level;
  }

  setLevel(l: number): void {
    l = Math.min(3, Math.max(0, l));
    if (l == this.level) return;
    console.info(`${this.head()} Setting LOG level to [${this.levelEnums[l]}]`);
    this.level = l;
  }

  head(level?: number): string {
    const d = new Date();
    let date = `${`0${d.getUTCDate()}`.slice(-2)}-${`0${d.getUTCMonth()}`.slice(-2)}-${d.getUTCFullYear()}`;
    date += ` ${`0${d.getSeconds()}`.slice(-2)}:${`0${d.getMinutes()}`.slice(-2)}:${`0${d.getHours()}`.slice(-2)}`;
    return level == undefined ? `${date} -` : `${date} [${this.levelEnums[level]}] -`;
  }

  log(...arguments_: unknown[]): void {
    if (this.level < 3) return;
    const function_ = globalThis.console.log;
    if (!this.inGroup) Array.prototype.unshift.call(arguments_, this.head(3));
    try {
      function_.apply(globalThis.console, arguments_ || []);
    } catch {
      for (const a in arguments_) console.log(arguments_[a]);
    }
  }

  group(...arguments_: unknown[]): void {
    const title = Array.prototype.shift.call(arguments_);
    this.inGroup = true;
    globalThis.console.group(`${this.head()} ${title}`);

    if (arguments_.length > 0) {
      //Loop through args and close group after...
      for (const a in arguments_) console.log(arguments_[a]);
      this.groupEnd();
    }
  }

  groupEnd(): void {
    this.inGroup = false;
    globalThis.console.groupEnd();
  }

  info(...arguments_: unknown[]): void {
    if (this.level < 2) return;
    const function_ = globalThis.console.info;
    if (!this.inGroup) Array.prototype.unshift.call(arguments_, this.head(2));
    try {
      function_.apply(globalThis.console, arguments_);
    } catch {
      for (const a in arguments_) console.info(arguments_[a]);
    }
  }

  warn(...arguments_: unknown[]): void {
    if (this.level < 1) return;
    const function_ = globalThis.console.warn;
    if (!this.inGroup) Array.prototype.unshift.call(arguments_, this.head(1));
    try {
      function_.apply(globalThis.console, arguments_);
    } catch {
      for (const a in arguments_) console.warn(arguments_[a]);
    }
  }

  error(...arguments_: unknown[]): void {
    const function_ = globalThis.console.error;
    if (!this.inGroup) Array.prototype.unshift.call(arguments_, this.head(0));
    try {
      function_.apply(globalThis.console, arguments_);
    } catch {
      for (const a in arguments_) console.error(arguments_[a]);
    }
  }
}
