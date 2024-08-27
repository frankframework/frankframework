import { Injectable } from '@angular/core';

export type ToastType = 'error' | 'warning' | 'success' | 'info';

export type ToastOptions = {
  timeout?: number;
  clickHandler?: (toast: Toast, event: MouseEvent) => boolean;
};

export type Toast = {
  type: ToastType;
  title: string;
  body?: string;
} & ToastOptions;

@Injectable({
  providedIn: 'root',
})
export class ToastService {
  toasts: Toast[] = [];

  constructor() {}

  error = (title: string, body?: string, options?: ToastOptions): void => this.show('error', title, body, options);
  success = (title: string, body?: string, options?: ToastOptions): void => this.show('success', title, body, options);
  warning = (title: string, body?: string, options?: ToastOptions): void => this.show('warning', title, body, options);

  show(type: ToastType, title: string, body?: string, options?: ToastOptions): void {
    this.toasts.push({ type, title, body, ...options });
  }

  remove(toast: Toast): void {
    this.toasts = this.toasts.filter((t) => t !== toast);
  }

  clear(): void {
    this.toasts = [];
  }
}
