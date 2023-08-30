import { appModule } from "../app.module";

export type ToastObject = {
  title: string,
  type ?: string,
  body ?: string,
  timeout ?: number,
  bodyOutputType ?: string,
  clickHandler ?: (_: any, isCloseButton: boolean) => boolean,
  showCloseButton ?: boolean,
  uid ?: number,
  onHideCallback ?: () => void
}

export class ToastrService {
  constructor(private toaster: any) {}

  error = (title: string | ToastObject, text?: string): void => this.sendToast('error', title, text);
  success = (title: string | ToastObject, text?: string): void => this.sendToast('success', title, text);
  warning = (title: string | ToastObject, text?: string): void => this.sendToast('warning', title, text);

  private sendToast(type: string, title: string | ToastObject, text?: string) {
    const options = { type: type, title: title, body: text };
    if (angular.isObject(title)) {
      angular.merge(options, options, title);
    }
    this.toaster.pop(options);
  }
}

appModule.service('Toastr', ['toaster', ToastrService]);
