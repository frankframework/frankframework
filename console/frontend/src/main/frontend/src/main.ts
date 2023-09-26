/// <reference types="@angular/localize" />

import { platformBrowserDynamic } from '@angular/platform-browser-dynamic';
import { NgZone } from '@angular/core';
import { AppModule } from './app/app.module';
import { UIRouter, UrlService } from '@uirouter/core';
import type * as angularjs from 'angular';
import type * as jQuery from 'jquery';

platformBrowserDynamic().bootstrapModule(AppModule)
  .then((platformRef) => {
    // Intialize the Angular Module
    // get() the UIRouter instance from DI to initialize the router
    const urlService: UrlService = platformRef.injector.get(UIRouter).urlService;

    // Instruct UIRouter to listen to URL changes
    function startUIRouter() {
      urlService.listen();
      urlService.sync();
    }

    platformRef.injector.get<NgZone>(NgZone).run(startUIRouter);
  })
  .catch(err => console.error(err));

declare global {
  var ff_version: string;
  var serverurl: string;
  // var jQuery: jQuery; already defined in @types/jquery (type import solves this for us?)
  // var $: jQuery;
  var angular: angularjs.IAngularStatic;
}
