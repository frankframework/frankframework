/// <reference types="@angular/localize" />

import { platformBrowserDynamic } from '@angular/platform-browser-dynamic';
import { NgZone } from '@angular/core';
import { AppModule } from './app/app.module';
// import { UIRouter, UrlService } from '@uirouter/core';
// import type * as angularjs from 'angular';
import type * as jQuery from 'jquery';

try {
  //Try and see if serverurl has been defined, if not try to deduct from local url;
  window.server = serverurl;
}
catch (e) {
  var path = window.location.pathname;

  if (path.indexOf("/iaf/gui") >= 0)
    path = path.substr(0, path.indexOf("/iaf/gui") + 1);
  else
    if (path.indexOf("/", 1) >= 0)
      path = path.substr(0, path.indexOf("/", 1) + 1);
  window.server = path;
}

platformBrowserDynamic().bootstrapModule(AppModule)
  /* .then((platformRef) => {
    // Intialize the Angular Module
    // get() the UIRouter instance from DI to initialize the router
    const urlService: UrlService = platformRef.injector.get(UIRouter).urlService;

    // Instruct UIRouter to listen to URL changes
    function startUIRouter() {
      urlService.listen();
      urlService.sync();
    }

    platformRef.injector.get<NgZone>(NgZone).run(startUIRouter);
  }) */
  .catch(err => console.error(err));

declare global {
  var ff_version: string;
  var serverurl: string;
  var server: string;
  // var jQuery: jQuery; already defined in @types/jquery (type import solves this for us?)
  // var $: jQuery;
  // var angular: angularjs.IAngularStatic;
}
