import { Component } from '@angular/core';
import { ToastService, ToastType } from 'src/app/services/toast.service';

@Component({
  selector: 'app-toasts-container',
  templateUrl: './toasts-container.component.html',
  styleUrls: ['./toasts-container.component.scss'],
})
export class ToastsContainerComponent {
  constructor(public toastService: ToastService) {}

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
