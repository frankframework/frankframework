import { inject, Injectable } from '@angular/core';
import { Base64Service } from './base64.service';
import { ServerInfo } from './server-info.service';

@Injectable({
  providedIn: 'root',
})
export class MiscService {
  private readonly Base64: Base64Service = inject(Base64Service);

  escapeURL(uri: string | number | boolean): string {
    return encodeURIComponent(uri);
  }

  isMobile(): boolean {
    return (
      /android/i.test(navigator.userAgent) ||
      /webos/i.test(navigator.userAgent) ||
      /iphone/i.test(navigator.userAgent) ||
      /ipad/i.test(navigator.userAgent) ||
      /ipod/i.test(navigator.userAgent) ||
      /blackberry/i.test(navigator.userAgent) ||
      /windows phone/i.test(navigator.userAgent)
    );
  }

  getUID(serverInfo: ServerInfo): string {
    const queryObject = {
      v: serverInfo['framework'].version,
      n: serverInfo['instance'].name,
      s: serverInfo['dtap.stage'],
    };
    const b = this.Base64.encode(JSON.stringify(queryObject));
    const chunks = [];
    let pos = 0;
    while (pos < b.length) {
      chunks.push(b.slice(pos, (pos += 5)));
    }
    return chunks.reverse().join('');
  }

  compare_version(v1: string | number, v2: string | number, operator?: string): boolean | number | null {
    // See for more info: http://locutus.io/php/info/version_compare/

    let index,
      compare = 0;
    const vm = {
      dev: -6,
      alpha: -5,
      a: -5,
      beta: -4,
      b: -4,
      RC: -3,
      rc: -3,
      '#': -2,
      p: 1,
      pl: 1,
    };

    const _numberVersion = function (v: string | number): number {
      return v ? (Number.isNaN(Number(v)) ? vm[v as keyof typeof vm] || -7 : Number.parseInt(v as string, 10)) : 0;
    };

    const v1Array = this._prepVersion(v1);
    const v2Array = this._prepVersion(v2);
    const x = Math.max(v1Array.length, v2Array.length);
    for (index = 0; index < x; index++) {
      if (v1Array[index] === v2Array[index]) {
        continue;
      }
      v1Array[index] = _numberVersion(v1Array[index]);
      v2Array[index] = _numberVersion(v2Array[index]);
      if (v1Array[index] < v2Array[index]) {
        compare = -1;
        break;
      } else if (v1Array[index] > v2Array[index]) {
        compare = 1;
        break;
      }
    }
    if (!operator) {
      return compare;
    }

    switch (operator) {
      case '>':
      case 'gt': {
        return compare > 0;
      }
      case '>=':
      case 'ge': {
        return compare >= 0;
      }
      case '<=':
      case 'le': {
        return compare <= 0;
      }
      case '===':
      case '=':
      case 'eq': {
        return compare === 0;
      }
      case '<>':
      case '!==':
      case 'ne': {
        return compare !== 0;
      }
      case '':
      case '<':
      case 'lt': {
        return compare < 0;
      }
      default: {
        return null;
      }
    }
  }

  private _prepVersion(v: string | number): string[] | number[] {
    v = `${v}`.replaceAll(/[+_-]/g, '.');
    v = v.replaceAll(/([^\d.]+)/g, '.$1.').replaceAll(/\.{2,}/g, '.');
    return v.length === 0 ? [-8] : v.split('.');
  }
}
