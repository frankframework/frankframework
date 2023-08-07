import { IRequestShortcutConfig } from "angular";
import { SwalParams, SweetAlert } from "sweetalert/typings/core";

export interface AlertService {
  add: (level: string | number, message: any, non_repeditive: boolean) => void;
  get: (preserveList: boolean) => {
    type: string,
    message: any,
    time: number
  }[];
  getCount: () => number;
  checkIfExists: (message: any) => boolean;
}

export interface ApiService {
  Get: (uri: string, callback?: (data: any) => void, error?: (data: any, status: number, statusText: string) => void, httpOptions?: IRequestShortcutConfig, intercept?: boolean) => Promise<void>;
  Post: (uri: string, object: any, callback?: (data: any) => void, error?: (data: any, status: number, statusText: string) => void, intercept?: boolean, responseType?: string | undefined) => Promise<void>;
  Put: (uri: string, object: any, callback?: (data: any) => void, error?: (data: any, status: number, statusText: string) => void, intercept?: boolean) => Promise<void>;
  Delete: (uri: string, object?: any, callback?: (data: any) => void, error?: (data: any, status: number, statusText: string) => void) => Promise<void>;
}

export interface AuthService {
  login: (username: string, password: string) => void;
  loggedin: () => void;
  logout: () => void;
}

export interface Base64Service {
  encode: (input: string) => string;
  decode: (input: string) => string;
}

export interface CookiesService {
  cache: Record<string, any>;
  addToCache: (key: string, value: any) => void;
  flushCache: () => void;
  options: { expires: number, path: string };
  get: (key: string) => any;
  set: (key: string, value: any) => void;
  remove: (key: string) => void;
  clear: () => void;
}

export interface DebugService {
  getLevel: () => number;
  setLevel: (l: number) => void;
  head: (level: number) => string;
  log: (...args: any[]) => void;
  group: (...args: any[]) => void;
  groupEnd: () => void;
  info: (...args: any[]) => void;
  warn: (...args: any[]) => void;
  error: (...args: any[]) => void;
}

export interface GDPRService {
  settings: Record<string, any>;
  defaults: { necessary: boolean, functional: boolean, personalization: boolean };
  cookieName: string;
  options: { expires: number, path: string };
  showCookie: () => boolean;
  getSettings: () => Record<string, any>;
  allowFunctional: () => boolean;
  allowPersonalization: () => boolean;
  setSettings: (settings: Record<string, any>) => void;
}

export interface HooksService {
  call: (...args: any[]) => void;
  register: (...args: any[]) => void;
}

export interface MiscService {
  getServerPath: () => string;
  escapeURL: (uri: string | number | boolean) => string;
  isMobile: () => boolean;
  getUID: (serverInfo: Record<string, any>) => string;
  compare_version: (v1: string | number, v2: string | number, operator: '>' | 'gt' | '>=' | 'ge' | '<=' | 'le' | '===' | '=' | 'eq' | '<>' | '!==' | 'ne' | '' | '<' | 'lt') => boolean;
}

export interface NotificationService {
  list: {
    icon: string,
    title: string,
    message: string | boolean,
    fn: (notification: NotificationService["list"][number]) => void,
    time: number
  }[];
  count: number;
  add: (icon: string, title: string, msg?: string | boolean, fn?: (notification: NotificationService["list"][number]) => void) => void;
  get: (id: number) => NotificationService["list"][number];
  resetCount: () => void;
  getCount: () => number;
  getLatest: (amount: number) => NotificationService["list"];
}

interface PollerObject {
  uri: string;
  waiting: boolean;
  pollerInterval: number;
  fired: number;
  errorList: { time: number, fired: number }[];
  addError: () => void;
  getLastError: () => PollerObject["errorList"][number];
  ai: {
    list: any[];
    avg: number;
    push: (obj: any) => number | undefined;
  };
  started: () => boolean;
  stop: () => void;
  fn: (runOnce: boolean) => void
  run: () => void;
  start: () => void;
  setInterval: (interval: number, restart: boolean) => void;
  waitForResponse: (bool: boolean) => void;
  restart: () => void;
}
export interface PollerService {
  createPollerObject: (uri: string, callback?: (data: any) => void) => void; // callback is never used
  changeInterval: (uri: string, interval: number) => void;
  add: (uri: string, callback?: (data: any) => void, autoStart?: boolean, interval?: number) => PollerObject;
  remove: (uri: string) => void;
  get: (uri: string) => PollerObject;
  getAll: () => {
    changeInterval: (interval: number) => void;
    start: () => void;
    stop: () => void;
    remove: () => void;
    list: () => string[];
  }
}

export interface SessionService {
  get: (key: string) => Record<string, any>;
  set: (key: string, value: Record<string, any>) => void;
  remove: (key: string) => void;
  clear: () => void;
}

export interface SweetAlertService {
  defaultSettings: Record<string, any>;
  defaults: (title: string | SwalParams, text: string | (() => void)) => SwalParams;
  Input: (...args: any[]) => Promise<any>;
  Confirm: (...args: any[]) => Promise<any>;
  Info: (...args: any[]) => Promise<any>;
  Warning: (...args: any[]) => Promise<any>;
  Error: (...args: any[]) => Promise<any>;
  Success: (...args: any[]) => Promise<any>;
}

export interface ToastrService {
  error: (title: string, text: string) => void;
  success: (title: string, text: string) => void;
  warning: (title: string, text: string) => void;
}
