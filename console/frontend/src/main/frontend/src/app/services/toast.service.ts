import { Injectable } from '@angular/core';

export type ToastType = 'error' | 'warning' | 'success' | 'info';

export type ToastOptions = {
  timeout?: number;
  clickHandler?: (toast: Toast, event: MouseEvent) => boolean;
  similarCount?: number;
};

export type ToastBody = {
  type: ToastType;
  title: string;
  body?: string;
};

export type Toast = ToastBody & ToastOptions;

export type DuplicateToast = ToastBody & { count: number };

@Injectable({
  providedIn: 'root',
})
export class ToastService {
  toasts: Toast[] = [];
  private duplicates: DuplicateToast[] = [];

  constructor() {}

  error = (title: string, body?: string, options?: ToastOptions): void => this.show('error', title, body, options);
  success = (title: string, body?: string, options?: ToastOptions): void => this.show('success', title, body, options);
  warning = (title: string, body?: string, options?: ToastOptions): void => this.show('warning', title, body, options);

  show(type: ToastType, title: string, body?: string, options?: ToastOptions): void {
    const toast: Toast = { type, title, body, ...options };
    this.removeFromList(toast);
    toast.similarCount = this.duplicateCheck(toast).count;
    this.toasts.push(toast);
  }

  remove(toast: ToastBody): void {
    this.removeFromDuplicates(toast);
    this.removeFromList(toast);
  }

  clear(): void {
    this.toasts = [];
  }

  private duplicateCheck(toast: ToastBody): DuplicateToast {
    const duplicate = this.duplicates.find((t) => t.type === toast.type && t.title === toast.title);
    if (duplicate) {
      duplicate.count += 1;
      return duplicate;
    }
    const newLength = this.duplicates.push({ count: 0, ...toast });
    return this.duplicates[newLength - 1];
  }

  private removeFromDuplicates(toast: ToastBody): void {
    this.duplicates = this.duplicates.filter(
      (duplicate) => !(duplicate.type === toast.type && duplicate.title === toast.title),
    );
  }

  private removeFromList(toast: ToastBody): void {
    this.toasts = this.toasts.filter((t) => !(t.type === toast.type && t.title === toast.title));
  }
}
