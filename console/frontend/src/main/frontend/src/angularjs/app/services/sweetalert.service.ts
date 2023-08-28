import Swal, { SweetAlertOptions } from 'sweetalert2';
import { appModule } from '../app.module';
import { DebugService } from './debug.service';

type SweetAlertSettings = SweetAlertOptions & { callback?: (value: any) => void };

/* TODO replace with Toastr where possible (warns, info, error, success) */
export class SweetAlertService {
  defaultSettings: SweetAlertSettings = {
    //			confirmButtonColor: "#449d44"
  };

  constructor(private Debug: DebugService) {}

  defaults(title: string | SweetAlertOptions, text?: string | (() => void)) {
    // var args = arguments || [];
    var options = angular.copy(this.defaultSettings);

    //expects (String, String) or (JsonObject, Function)
    if (typeof title == "object") {
      angular.merge(options, options, title);
      if (typeof text == "function") {
        options.callback = text;
      }
    } else if (typeof title == "string") {
      options.title = title;
      if (typeof text == "string") {
        options.text = text;
      }
    }

    return options; //var [options, callback] = this.defaults.apply(this, arguments);
  }

  Input(title: string | SweetAlertOptions, text?: string | (() => void)) {
    var options = this.defaults(title, text);
    if (options.input == undefined)
      options.input = "text";
    options.showCancelButton = true;
    return Swal.fire(options)
  }

  Confirm(title: string | SweetAlertOptions, text?: string | (() => void)) { //(JsonObject, Callback)-> returns boolean
    var options: SweetAlertSettings = {
      title: "Are you sure?",
      showCancelButton: true,
    };
    angular.merge(options, options, this.defaults(title, text));
    if (!options.callback)
      return Swal.fire(options);
    return Swal.fire(options).then(result => options.callback ? options.callback(result.value) : result);
  }

  Info(title: string | SweetAlertOptions, text?: string | (() => void)) {
    var options: SweetAlertSettings = {};
    angular.merge(options, { type: "info" }, this.defaults(title, text));
    if (!options.callback)
      return Swal.fire(options);
    return Swal.fire(options).then(result => options.callback ? options.callback(result.value) : result);
  }

  Warning(title: string | SweetAlertOptions, text?: string | (() => void)) {
    var options: SweetAlertSettings  = {};
    angular.merge(options, { type: "warning" }, this.defaults(title, text));
    if (!options.callback)
      return Swal.fire(options);
    return Swal.fire(options).then(result => options.callback ? options.callback(result.value) : result);
  }

  Error(title: string | SweetAlertOptions, text?: string | (() => void)) {
    var options: SweetAlertSettings = {};
    angular.merge(options, { type: "error" }, this.defaults(title, text));
    if (!options.callback)
      return Swal.fire(options);
    return Swal.fire(options).then(result => options.callback ? options.callback(result.value) : result);
  }

  Success(title: string | SweetAlertOptions, text?: string | (() => void)) {
    var options: SweetAlertSettings = {};
    angular.merge(options, { type: "success" }, this.defaults(title, text));
    if (!options.callback)
      return Swal.fire(options);
    return Swal.fire(options).then(result => options.callback ? options.callback(result.value) : result);
  }
}

appModule.service('SweetAlert', ['Debug', SweetAlertService]);
