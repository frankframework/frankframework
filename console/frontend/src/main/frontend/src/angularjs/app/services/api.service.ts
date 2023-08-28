import { AppConstants, appModule } from "../app.module";
import { DebugService } from "./debug.service";
import { MiscService } from "./misc.service";
import { SessionService } from "./session.service";

export type IAFHttpOptions = angular.IRequestShortcutConfig & { intercept?: boolean, poller?: boolean }; // would any of this even work in runtime?
type IAFRequestConfig = angular.IRequestConfig & { intercept?: boolean }; // would any of this even work in runtime?
type ErrorCallback = (data: any, status: number, statusText: string) => void;

export class ApiService {

  public absolutePath = this.Misc.getServerPath() + "iaf/api/";
  public etags: Record<string, string> = {};
  private allowed: Record<string, string> = {};
  private defaultTimeout = this.appConstants["console.pollerInterval"] - 1000;

  constructor(
    private $http: angular.IHttpService,
    private appConstants: AppConstants,
    private Misc: MiscService,
    private Session: SessionService,
    private Debug: DebugService
  ){
    this.$http.defaults.headers!.post["Content-Type"] = "application/json";
  }


  Get(uri: string, callback?: (data: any) => void, error?: ErrorCallback, httpOptions?: IAFHttpOptions, intercept?: boolean): angular.IPromise<void> {
    const defaultHttpOptions: IAFHttpOptions = { headers: {}, timeout: this.defaultTimeout, intercept: intercept };

    if (httpOptions) {
      //If httpOptions is TRUE, skip additional/custom settings, if it's an object, merge both objects
      if (typeof httpOptions == "object") {
        angular.merge(defaultHttpOptions, defaultHttpOptions, httpOptions);
        if (!httpOptions.poller) {
          this.Debug.log("Sending request to uri [" + uri + "] using HttpOptions ", defaultHttpOptions);
        }
      }
    }
    if (this.etags.hasOwnProperty(uri)) { //If not explicitly disabled (httpOptions==false), check eTag
      var tag = this.etags[uri];
      defaultHttpOptions.headers!['If-None-Match'] = tag;
    }

    return this.$http.get(this.buildURI(uri), defaultHttpOptions).then((response) => {
      if (callback && typeof callback === 'function') {
        if (response.headers("etag")) {
          this.etags[uri] = response.headers("etag");
        }
        if (response.headers("allow")) {
          this.allowed[uri] = response.headers("allow");
        }
        callback(response.data);
      }
    }).catch((response) => { this.errorException(response, error); });
  };

  Post(uri: string, object: any, callback?: (data: any) => void, error?: ErrorCallback, intercept?: boolean, responseType?: string | undefined): angular.IPromise<void> { // uri, object, callback, error, intercept 4xx errors
    var headers = {};
    if (object instanceof FormData) {
      headers = { 'Content-Type': undefined }; //Unset default contentType when posting formdata
    }

    return this.$http.post(this.buildURI(uri), object, {
      headers: headers,
      responseType: responseType,
      transformRequest: angular.identity,
      timeout: this.defaultTimeout,
      intercept: intercept,
    } as angular.IRequestShortcutConfig).then((response) => {
      if (callback && typeof callback === 'function') {
        this.etags[uri] = response.headers("etag");
        callback(response.data);
      }
    }).catch((response) => { this.errorException(response, error); });
  };

  Put(uri: string, object: Record<string, any> | FormData | null, callback?: (data: any) => void, error?: ErrorCallback, intercept?: boolean): angular.IPromise<void> {
    var headers: angular.IHttpRequestConfigHeaders = {};
    var data = {};
    if (object != null) {
      if (object instanceof FormData) {
        data = object;
        headers["Content-Type"] = undefined;
      } else {
        data = JSON.stringify(object);
        headers["Content-Type"] = "application/json";
      }
    }

    return this.$http.put(this.buildURI(uri), data, {
      headers: headers,
      transformRequest: angular.identity,
      timeout: this.defaultTimeout,
      intercept: intercept,
    } as angular.IRequestShortcutConfig).then((response) => {
      if (callback && typeof callback === 'function') {
        this.etags[uri] = response.headers("etag");
        callback(response.data);
      }
    }).catch((response) => { this.errorException(response, error); });
  };

  Delete(uri: string, object?: any, callback?: (data: any) => void, error?: ErrorCallback, intercept?: boolean): angular.IPromise<void> { // uri, callback, error || uri, object, callback, error
    var request: IAFRequestConfig = { url: this.buildURI(uri), method: "delete", headers: {}, timeout: this.defaultTimeout, intercept };

    if (object instanceof Function) { //we have a callback function, that means no object is present!
      callback = object; // set the callback method accordingly
    } else {
      if (object instanceof FormData) {
        request.data = object;
        request.headers!["Content-Type"] = undefined;
      } else {
        request.data = JSON.stringify(object);
        request.headers!["Content-Type"] = "application/json";
      }
    }

    return this.$http(request).then((response) => {
      if (callback && typeof callback === 'function') {
        this.etags[uri] = response.headers("etag");
        callback(response.data);
      }
    }).catch((response) => { this.errorException(response, error); });
  };

  errorException(response: angular.IHttpResponse<unknown>, callback?: ErrorCallback) {
    if (response.status != 304) {
      var status = (response.status > 0) ? " " + response.status + " error" : "n unknown error";
      if (response.status == 404 || response.status == 500) {
        var config = response.config;
        var debug = " url[" + config.url + "] method[" + config.method + "]";
        if (config.data && config.data != "") debug += " data[" + config.data + "]";
        this.Debug.warn("A" + status + " occurred, please notify a system administrator!" + '\n' + debug);
      }
      else {
        this.Debug.info("A" + status + " occured.", response);
      }

      if ((response.status != 304) && (callback && typeof callback === 'function')) {
        callback(response.data, response.status, response.statusText);
      }
    }
  };

  flushCache() {
    this.etags = {};
  };

  private buildURI(uri: string) {
    return this.absolutePath + uri;
  }
}

appModule.service('Api', ['$http', 'appConstants', 'Misc', 'Session', 'Debug', ApiService]);
