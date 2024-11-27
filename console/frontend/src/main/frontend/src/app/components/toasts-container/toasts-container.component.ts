import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { NgbToast } from '@ng-bootstrap/ng-bootstrap';
import { Toast, ToastService, ToastType } from 'src/app/services/toast.service';

@Component({
  selector: 'app-toasts-container',
  templateUrl: './toasts-container.component.html',
  styleUrls: ['./toasts-container.component.scss'],
  standalone: true,
  imports: [CommonModule, NgbToast],
})
export class ToastsContainerComponent {
  constructor(public toastService: ToastService) {}

  remove(toast: Toast): void {
    this.toastService.remove(toast);
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
