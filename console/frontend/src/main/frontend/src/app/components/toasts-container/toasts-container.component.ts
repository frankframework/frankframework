import { Component, inject, ChangeDetectionStrategy } from '@angular/core';
import { NgbToast } from '@ng-bootstrap/ng-bootstrap';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faTimes } from '@fortawesome/free-solid-svg-icons';
import { Toast, ToastService, ToastType } from '../../services/toast.service';

@Component({
  selector: 'app-toasts-container',
  templateUrl: './toasts-container.component.html',
  styles: `
    :host {
      padding: 1rem !important;
      right: 0 !important;
      top: 0 !important;
      position: fixed !important;
      z-index: 1200;
    }
  `,
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [NgbToast, FaIconComponent],
})
export class ToastsContainerComponent {
  public toastService: ToastService = inject(ToastService);
  protected readonly faTimes = faTimes;

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
