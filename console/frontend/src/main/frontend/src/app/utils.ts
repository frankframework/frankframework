// @ts-expect-error _.merge does not have correct types
import _merge from 'lodash.merge';
import type { merge } from 'lodash';

export const deepMerge: typeof merge = _merge;

export function whenElementExists<T extends HTMLElement>(selector: string, callback: (element: T) => void): void {
  const element = document.querySelector<T>(selector);
  if (element) callback(element);
}

export function computeServerPath(): string {
  let path = window.location.pathname;

  if (path.includes('/iaf/gui')) path = path.slice(0, Math.max(0, path.indexOf('/iaf/gui') + 1));
  else if (path.includes('/', 1)) path = path.slice(0, Math.max(0, path.indexOf('/', 1) + 1));
  return path;
}

export function getProcessStateIcon(
  processState: string,
): 'fa-server' | 'fa-gears' | 'fa-sign-in' | 'fa-pause-circle' | 'fa-times-circle' {
  switch (processState) {
    case 'Available': {
      return 'fa-server';
    }
    case 'InProcess': {
      return 'fa-gears';
    }
    case 'Done': {
      return 'fa-sign-in';
    }
    case 'Hold': {
      return 'fa-pause-circle';
    }
    // case 'Error':
    default: {
      return 'fa-times-circle';
    }
  }
}

export function getProcessStateIconColor(processState: string): 'success' | 'warning' | 'danger' {
  switch (processState) {
    case 'Available':
    case 'InProcess':
    case 'Done': {
      return 'success';
    }
    case 'Hold': {
      return 'warning';
    }
    // case 'Error':
    default: {
      return 'danger';
    }
  }
}

export function copyToClipboard(text: string): void {
  const element = document.createElement('textarea');
  element.value = text;
  element.setAttribute('readonly', '');
  element.style.position = 'absolute';
  element.style.left = '-9999px';
  document.body.append(element);
  element.select();
  document.execCommand('copy'); // TODO: soon deprecated but no real solution yet
  element.remove();
}

export function fixedPointFloat(value: number): number {
  return +value.toFixed(3);
}

export const compare = (v1: string | number, v2: string | number): 1 | -1 | 0 => (v1 < v2 ? -1 : v1 > v2 ? 1 : 0);

/** This is pretty bad, non primitive types won't be covered correctly (null, undefined, object, etc) */
export const anyCompare = <T>(v1: T, v2: T): 1 | -1 | 0 => (v1 < v2 ? -1 : v1 > v2 ? 1 : 0);
