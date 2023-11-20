import { Injectable } from '@angular/core';
import Swal, { SweetAlertOptions } from 'sweetalert2';
import { DebugService } from './debug.service';

type SweetAlertSettings = SweetAlertOptions & { callback?: (value: any) => void };

/* TODO replace with Toastr where possible (warns, info, error, success) */
@Injectable({
  providedIn: 'root'
})
export class SweetalertService {
  defaultSettings: SweetAlertSettings = {
    //			confirmButtonColor: "#449d44"
  };

  constructor(private Debug: DebugService) { }

  defaults(title: string | SweetAlertOptions, text?: string) {
    // let args = arguments || [];
    let options = { ...this.defaultSettings };

    //expects (String, String) or (JsonObject, Function)
    if (typeof title == "object") {
      options = { ...options, ...title };
      // DEPRECATED
      // if (typeof text == "function") {
      //   options.callback = text;
      // }
    } else if (typeof title == "string") {
      options.title = title;
      if (typeof text == "string") {
        options.text = text;
      }
    }

    return options; //let [options, callback] = this.defaults.apply(this, arguments);
  }

  Input(title: string | SweetAlertOptions, text?: string) {
    let options = this.defaults(title, text);
    if (options.input == undefined)
      options.input = "text";
    options.showCancelButton = true;
    return Swal.fire(options)
  }

  Confirm(title: string | SweetAlertOptions, text?: string) { //(JsonObject, Callback)-> returns boolean
    let options: SweetAlertSettings = {
      title: "Are you sure?",
      showCancelButton: true,
    };
    options = { ...options, ...this.defaults(title, text) };
    return Swal.fire(options);
  }

  Info(title: string | SweetAlertOptions, text?: string) {
    let options: SweetAlertSettings = {};
    options = { ...{ type: "info" }, ...this.defaults(title, text) };
    return Swal.fire(options);
  }

  Warning(title: string | SweetAlertOptions, text?: string) {
    let options: SweetAlertSettings = {};
    options = { ...{ type: "warning" }, ...this.defaults(title, text) };
    return Swal.fire(options);
  }

  Error(title: string | SweetAlertOptions, text?: string) {
    let options: SweetAlertSettings = {};
    options = { ...{ type: "error" }, ...this.defaults(title, text) };
    return Swal.fire(options);
  }

  Success(title: string | SweetAlertOptions, text?: string) {
    let options: SweetAlertSettings = {};
    options = { ...{ type: "success" }, ...this.defaults(title, text) };
    return Swal.fire(options);
  }
}
