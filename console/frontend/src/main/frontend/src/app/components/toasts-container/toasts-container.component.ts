import { Component } from '@angular/core';
import { Toast, ToastService, ToastType } from 'src/app/services/toast.service';

@Component({
  selector: 'app-toasts-container',
  templateUrl: './toasts-container.component.html',
  styleUrls: ['./toasts-container.component.scss'],
})
export class ToastsContainerComponent {
  show: boolean = true;

  constructor(public toastService: ToastService) {}

  remove(toast: Toast | null): void {
    if (toast) {
      this.toastService.remove(toast);
    } else {
      this.show = false;
      window.setTimeout(() => {
        this.show = true;
      }, 3000);
    }
  }

  getClassByType(
    type: ToastType,
  ):
    | 'toast-error text-light'
    | 'toast-success text-light'
    | 'toast-warning'
    | 'toast-info' {
    switch (type) {
      case 'error': {
        return 'toast-error text-light';
      }
      case 'success': {
        return 'toast-success text-light';
      }
      case 'warning': {
        return 'toast-warning';
      }
      // case 'info':
      default: {
        return 'toast-info';
      }
    }
  }
}
