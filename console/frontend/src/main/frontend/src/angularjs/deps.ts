// @ts-ignore pace-js does not have types
import * as Pace from 'pace-js';
Pace.start({
  ajax: false
});

import * as jQuery from 'jquery';
// import './plugins/iCheck/icheck.min';
// import 'datatables.net/js/jquery.dataTables.min';
// import 'chart.js';
// import 'mermaid';

import 'metismenu';

import * as angular from 'angular';
import 'angular-animate';
import 'angular-aria';
import 'angular-cookies';
import 'angular-loader';
import 'angular-messages';
import 'angular-mocks';
import 'angular-parse-ext';
import 'angular-resource';
import 'angular-route';
import 'angular-sanitize';
import 'angular-touch';
import 'angular-ui-bootstrap';
import 'angular-ui-router';
import 'ng-idle/angular-idle.js';
import 'angular-ladda';
import 'angularjs-toaster';
import 'angular-datatables/dist/angular-datatables';
import 'angular-datatables/dist/plugins/buttons/angular-datatables.buttons';
import 'angular-chart.js';
import './plugins/mermaid/ng-mermaid';

const $ = jQuery;

export {
  jQuery, $,
  angular,
  Pace
};

// because @types/ng-idle does not work for some weird ts reason (import name mismatch)
declare module 'angular' {
  export namespace idle {

    /**
         * Used to configure the Keepalive service.
         */
    interface IKeepAliveProvider extends IServiceProvider {

      /**
       * If configured, options will be used to issue a request using $http.
       * If the value is null, no HTTP request will be issued.
       * You can specify a string, which it will assume to be a URL to a simple GET request.
       * Otherwise, you can use the same options $http takes. However, cache will always be false.
       *
       * @param value May be string or IRequestConfig, default is null.
       */
      http(value: string | IRequestConfig): void;

      /**
       * This specifies how often the keepalive event is triggered and the
       * HTTP request is issued.
       *
       * @param seconds Integer, default is 10 minutes. Must be greater than 0.
       */
      interval(seconds: number): void;
    }

    /**
         * Used to configure the Idle service.
         */
    interface IIdleProvider extends IServiceProvider {
      /**
       * Specifies the DOM events the service will watch to reset the idle timeout.
       * Multiple events should be separated by a space.
       *
       * @param events string, default 'mousemove keydown DOMMouseScroll mousewheel mousedown'
       */
      interrupt(events: string): void;

      /**
       * The idle timeout duration in seconds. After this amount of time passes without the user
       * performing an action that triggers one of the watched DOM events, the user is considered
       * idle.
       *
       * @param seconds integer, default is 20min
       */
      idle(seconds: number): void;

      /**
       * The amount of time the user has to respond (in seconds) before they have been considered
       * timed out.
       *
       * @param seconds integer, default is 30s
       */
      timeout(seconds: number): void;

      /**
       * When true or idle, user activity will automatically interrupt the warning countdown
       * and reset the idle state. If false or off, you will need to manually call watch()
       * when you want to start watching for idleness again. If notIdle, user activity will
       * only automatically interrupt if the user is not yet idle.
       *
       * @param enabled boolean or string, possible values: off/false, idle/true, or notIdle
       */
      autoResume(enabled: boolean | string): void;

      /**
       * When true, the Keepalive service is automatically stopped and started as needed.
       *
       * @param enabled boolean, default is true
       */
      keepalive(enabled: boolean): void;
    }

    /**
     * Idle, once watch() is called, will start a timeout which if expires, will enter a warning state
     * countdown. Once the countdown reaches zero, idle will broadcast a timeout event indicating the
     * user has timed out (where your app should log them out or whatever you like). If the user performs
     * an action that triggers a watched DOM event that bubbles up to document.body, this will reset the
     * idle/warning state and start the process over again.
     */
    interface IIdleService {
      /**
       * Gets the current idle value
       */
      getIdle(): number;

      /**
       * Gets the current timeout value
       */
      getTimeout(): number;

      /**
       * Updates the idle value (see IdleProvider.idle()) and
       * restarts the watch if its running.
       */
      setIdle(idle: number): void;

      /**
       * Updates the timeout value (see IdleProvider.timeout()) and
       * restarts the watch if its running.
       */
      setTimeout(timeout: number): void;

      /**
       * Whether user has timed out (meaning idleDuration + timeout has passed without any activity)
       */
      isExpired(): boolean;

      /**
       * Whether or not the watch() has been called and it is watching for idleness.
       */
      running(): boolean;

      /**
       * Whether or not the user appears to be idle.
       */
      idling(): boolean;

      /**
       * Starts watching for idleness, or resets the idle/warning state and continues watching.
       */
      watch(): void;

      /**
       * Stops watching for idleness, and resets the idle/warning state.
       */
      unwatch(): void;

      /**
       * Manually trigger the idle interrupt that normally occurs during user activity.
       */
      interrupt(): any;
    }
  }
}

