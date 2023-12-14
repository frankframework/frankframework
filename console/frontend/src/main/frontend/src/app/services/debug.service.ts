import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class DebugService {
  private level = 0; //ERROR
  private levelEnums = ["ERROR", "WARN", "INFO", "DEBUG"];
  private inGroup = false;

  getLevel(): number {
    return this.level;
  }

  setLevel(l: number): void {
    l = Math.min(3, Math.max(0, l));
    if (l == this.level) return;
    console.info(this.head() + " Setting LOG level to [" + this.levelEnums[l] + "]");
    this.level = l;
  }

  head(level?: number): string {
    let d = new Date();
    let date = ('0' + d.getUTCDate()).slice(-2) + "-" + ('0' + d.getUTCMonth()).slice(-2) + "-" + d.getUTCFullYear();
    date += " " + ('0' + d.getSeconds()).slice(-2) + ":" + ('0' + d.getMinutes()).slice(-2) + ":" + ('0' + d.getHours()).slice(-2);
    if (level != undefined)
      return date + " [" + this.levelEnums[level] + "] -";
    else
      return date + " -";
  }

  log(...args: any[]): void {
    if (this.level < 3) return;
    let func = window.console.log;
    if (!this.inGroup)
      Array.prototype.unshift.call(args, this.head(3));
    try {
      func.apply(window.console, args || []);
    } catch (e) {
      for (let a in args)
        console.log(args[a]);
    };
  }

  group(...args: any[]): void {
    let title = Array.prototype.shift.call(args);
    this.inGroup = true;
    window.console.group(this.head() + " " + title);

    if (args.length > 0) { //Loop through args and close group after...
      for (let a in args)
        console.log(args[a]);
      this.groupEnd();
    }
  }

  groupEnd(): void {
    this.inGroup = false;
    window.console.groupEnd();
  }

  info(...args: any[]): void {
    if (this.level < 2) return;
    let func = window.console.info;
    if (!this.inGroup)
      Array.prototype.unshift.call(args, this.head(2));
    try {
      func.apply(window.console, args);
    } catch (e) {
      for (let a in args)
        console.info(args[a]);
    };
  }

  warn(...args: any[]): void {
    if (this.level < 1) return;
    let func = window.console.warn;
    if (!this.inGroup)
      Array.prototype.unshift.call(args, this.head(1));
    try {
      func.apply(window.console, args);
    } catch (e) {
      for (let a in args)
        console.warn(args[a]);
    };
  }

  error(...args: any[]): void {
    let func = window.console.error;
    if (!this.inGroup)
      Array.prototype.unshift.call(args, this.head(0));
    try {
      func.apply(window.console, args);
    } catch (e) {
      for (let a in args)
        console.error(args[a]);
    };
  }
}
