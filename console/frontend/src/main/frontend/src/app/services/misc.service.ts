import { Injectable } from '@angular/core';
import { AppConstants, AppService } from '../app.service';
import { Base64Service } from './base64.service';

@Injectable({
  providedIn: 'root'
})
export class MiscService {
  absoluteApiPath = this.getServerPath() + "iaf/api/";

  private appConstants: AppConstants;

    constructor(
      private appService: AppService,
      private Base64: Base64Service
    ) {
      this.appConstants = this.appService.APP_CONSTANTS
      this.appService.appConstants$.subscribe(() => {
        this.appConstants = this.appService.APP_CONSTANTS;
      });
    }

  getServerPath(): string {
    let absolutePath = this.appConstants["server"];
    if (absolutePath && absolutePath.slice(-1) != "/") absolutePath += "/";
    return absolutePath;
  }

  escapeURL(uri: string | number | boolean): string {
    return encodeURIComponent(uri);
  }

  isMobile(): boolean {
    return (navigator.userAgent.match(/Android/i)
      || navigator.userAgent.match(/webOS/i)
      || navigator.userAgent.match(/iPhone/i)
      || navigator.userAgent.match(/iPad/i)
      || navigator.userAgent.match(/iPod/i)
      || navigator.userAgent.match(/BlackBerry/i)
      || navigator.userAgent.match(/Windows Phone/i)
    ) ? true : false;
  }

  getUID(serverInfo: Record<string, any>): string {
    let queryObj = {
      "v": serverInfo["framework"].version,
      "n": serverInfo["instance"].name,
      "s": serverInfo["dtap.stage"],
    };
    let b = this.Base64.encode(JSON.stringify(queryObj));
    const chunks = [];
    let pos = 0
    while (pos < b.length) {
      chunks.push(b.slice(pos, pos += 5));
    }
    return chunks.reverse().join("");
  }

  compare_version(v1: string | number, v2: string | number, operator?: string): boolean | number | null {
    // See for more info: http://locutus.io/php/info/version_compare/

    let i, x, compare = 0;
    let vm = {
      'dev': -6,
      'alpha': -5,
      'a': -5,
      'beta': -4,
      'b': -4,
      'RC': -3,
      'rc': -3,
      '#': -2,
      'p': 1,
      'pl': 1
    };

    let _prepVersion = function (v: string | number) {
      v = ('' + v).replace(/[_\-+]/g, '.');
      v = v.replace(/([^.\d]+)/g, '.$1.').replace(/\.{2,}/g, '.');
      return (!v.length ? [-8] : v.split('.'));
    };
    let _numVersion = function (v: string | number): number {
      return !v ? 0 : (isNaN(v as number) ? vm[v as keyof typeof vm] || -7 : parseInt(v as string, 10));
    };

    let v1Arr = _prepVersion(v1);
    let v2Arr = _prepVersion(v2);
    x = Math.max(v1Arr.length, v2Arr.length);
    for (i = 0; i < x; i++) {
      if (v1Arr[i] === v2Arr[i]) {
        continue;
      }
      v1Arr[i] = _numVersion(v1Arr[i]);
      v2Arr[i] = _numVersion(v2Arr[i]);
      if (v1Arr[i] < v2Arr[i]) {
        compare = -1;
        break;
      } else if (v1Arr[i] > v2Arr[i]) {
        compare = 1;
        break;
      }
    }
    if (!operator) {
      return compare;
    }

    switch (operator) {
      case '>':
      case 'gt':
        return (compare > 0);
      case '>=':
      case 'ge':
        return (compare >= 0);
      case '<=':
      case 'le':
        return (compare <= 0);
      case '===':
      case '=':
      case 'eq':
        return (compare === 0);
      case '<>':
      case '!==':
      case 'ne':
        return (compare !== 0);
      case '':
      case '<':
      case 'lt':
        return (compare < 0);
      default:
        return null;
    }
  }
}
