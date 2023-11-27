import { Injectable } from '@angular/core';
import Swal, { SweetAlertOptions } from 'sweetalert2';
import { DebugService } from './debug.service';

@Injectable({
  providedIn: 'root'
})
export class SweetalertService {
  defaultSettings: SweetAlertOptions = {
    //			confirmButtonColor: "#449d44"
  };

  constructor(private Debug: DebugService) { }

  defaults(title: string | SweetAlertOptions, text?: string): SweetAlertOptions {
    // let args = arguments || [];
    let options = this.defaultSettings;

    //expects (String, String) or (JsonObject, Function)
    if (typeof title == "object") {
      return { ...title };
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
    const options = {
      ...{ title: "Are you sure?", showCancelButton: true },
      ...this.defaults(title, text)
    };
    return Swal.fire(options);
  }

  Info(title: string | SweetAlertOptions, text?: string) {
    let options: SweetAlertOptions = {};
    options = { ...{ type: "info" }, ...this.defaults(title, text) };
    return Swal.fire(options);
  }

  Warning(title: string | SweetAlertOptions, text?: string) {
    let options: SweetAlertOptions = {};
    options = { ...{ type: "warning" }, ...this.defaults(title, text) };
    return Swal.fire(options);
  }

  Error(title: string | SweetAlertOptions, text?: string) {
    let options: SweetAlertOptions = {};
    options = { ...{ type: "error" }, ...this.defaults(title, text) };
    return Swal.fire(options);
  }

  Success(title: string | SweetAlertOptions, text?: string) {
    let options: SweetAlertOptions = {};
    options = { ...{ type: "success" }, ...this.defaults(title, text) };
    return Swal.fire(options);
  }
}
