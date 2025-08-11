import { inject, Injectable } from '@angular/core';
import Swal, { SweetAlertOptions, SweetAlertResult } from 'sweetalert2';
import { DebugService } from './debug.service';

@Injectable({
  providedIn: 'root',
})
export class SweetalertService {
  private defaultSettings: SweetAlertOptions = {
    //			confirmButtonColor: "#449d44"
  };

  private readonly Debug: DebugService = inject(DebugService);

  defaults(title: string | SweetAlertOptions, text?: string): SweetAlertOptions {
    // let args = arguments || [];
    const options = this.defaultSettings;

    //expects (String, String) or (JsonObject, Function)
    if (typeof title == 'object') {
      return { ...title };
      // DEPRECATED
      // if (typeof text == "function") {
      //   options.callback = text;
      // }
    } else if (typeof title == 'string') {
      options.title = title;
      if (typeof text == 'string') {
        options.text = text;
      }
    }

    return options; //let [options, callback] = this.defaults.apply(this, arguments);
  }

  input(title: string | SweetAlertOptions, text?: string): Promise<SweetAlertResult<unknown>> {
    const options = this.defaults(title, text);
    if (options.input == undefined) options.input = 'text';
    options.showCancelButton = true;
    return Swal.fire(options);
  }

  confirm(title: string | SweetAlertOptions, text?: string): Promise<SweetAlertResult<unknown>> {
    //(JsonObject, Callback)-> returns boolean
    const options = {
      title: 'Are you sure?',
      showCancelButton: true,
      ...this.defaults(title, text),
    };
    return Swal.fire(options);
  }

  info(title: string | SweetAlertOptions, text?: string): Promise<SweetAlertResult<unknown>> {
    let options: SweetAlertOptions = {};
    options = { icon: 'info', ...this.defaults(title, text) };
    return Swal.fire(options);
  }

  warning(title: string | SweetAlertOptions, text?: string): Promise<SweetAlertResult<unknown>> {
    let options: SweetAlertOptions = {};
    options = { icon: 'warning', ...this.defaults(title, text) };
    return Swal.fire(options);
  }

  error(title: string | SweetAlertOptions, text?: string): Promise<SweetAlertResult<unknown>> {
    let options: SweetAlertOptions = {};
    options = { icon: 'error', ...this.defaults(title, text) };
    return Swal.fire(options);
  }

  success(title: string | SweetAlertOptions, text?: string): Promise<SweetAlertResult<unknown>> {
    let options: SweetAlertOptions = {};
    options = { icon: 'success', ...this.defaults(title, text) };
    return Swal.fire(options);
  }
}
