import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { NgbToast } from '@ng-bootstrap/ng-bootstrap';
import { Toast, ToastService, ToastType } from 'src/app/services/toast.service';

@Component({
  selector: 'app-toasts-container',
  templateUrl: './toasts-container.component.html',
  imports: [CommonModule, NgbToast],
})
export class ToastsContainerComponent {
  public toastService: ToastService = inject(ToastService);

  remove(toast: Toast, event: MouseEvent): void {
    this.toastService.remove(toast);
    event.stopPropagation();
  }

  getClassByType(
    type: ToastType,
  ): 'toast-error text-light' | 'toast-success text-light' | 'toast-warning' | 'toast-info' {
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
