/// <reference types="@angular/localize" />

import { platformBrowserDynamic } from '@angular/platform-browser-dynamic';
import { AppModule } from './app/app.module';
import type * as SockJS from 'sockjs-client';

declare global {
  interface Window {
    server: string;
    SockJS: typeof SockJS; // use premad bundle because sockjs developers don't understand using global might be a bad idea in non-node environments
  }
  // var jQuery: jQuery; already defined in @types/jquery (type import solves this for us?)
  // var $: jQuery;
}

function main(): void {
  platformBrowserDynamic()
    .bootstrapModule(AppModule)
    .catch((error) => console.error(error));

  /* TODO: when all components are standalone, bootstrap using this
  bootstrapApplication(AppCompponent, {
    providers: [
      provideRouter(...),
      importProvidersFrom(
        //  LibraryModule.forRoot()
      ),
      // other providers
    ],
  });
  */
}

if (location.hostname != 'localhost') {
  window.console.log(
    '%cThis is a browser feature intended for developers. Do not paste any code here given to you by someone else. It may compromise your account or have other negative side effects.',
    'font-weight: bold; font-size: 14px;',
  );
}

console.time('startup');
main();
console.time('documentReady');

/* Main.js */
$(document).ready(function () {
  console.timeEnd('documentReady');
  console.log('Launching GUI!');
  $('.loading').css('display', '');
  // Full height of sidebar
  fix_height_function();

  $(window).on('resize scroll', function () {
    if (!$('body').hasClass('body-small')) {
      fix_height();
    }
  });
  $(window).on('load', function () {
    if (!$('body').hasClass('body-small')) {
      fix_height(500);
    }
  });

  function fix_height(time?: number): void {
    if (!time) time = 50;
    setTimeout(function () {
      fix_height_function();
    }, time);
  }

  window.addEventListener('keydown', function (event) {
    if (event.key == 'F' && (event.ctrlKey || event.metaKey) && event.shiftKey) {
      const searchbar = document.querySelector('#searchbar');
      if (searchbar) {
        event.preventDefault();
        searchbar.querySelectorAll('input')[0].focus();
      }
    }
  });
});

function fix_height_function(): void {
  const navbarHeight = $('nav.navbar-default').height()!;
  const wrapperHeight = $('#page-wrapper').height()!;

  if (navbarHeight <= wrapperHeight && $(window).height()! > navbarHeight) {
    $('#page-wrapper').css('min-height', `${$(window).height()}px`);
  } else {
    $('#page-wrapper').css('min-height', `${navbarHeight}px`);
  }
}

//Detect if using any (older) version of Internet Explorer
if (navigator.userAgent.includes('MSIE') || navigator.appVersion.includes('Trident/')) {
  $('body').prepend(
    "<h2 style='text-align: center; color: #fdc300;'><strong>Internet Explorer 11 and older do not support XHR requests, the Frank!Console might not load correctly!</strong><br/>Please open this website in MS Edge, Mozilla Firefox or Google Chrome.</h2>",
  );
}

// Automatically minimalize menu when screen is less than 768px
$(function () {
  $(window).on('load resize', function () {
    if ($(document).width()! < 769) {
      $('body').addClass('body-small');
    } else {
      $('body').removeClass('body-small');
    }
  });

  $('body').on('scroll', function (this: JQuery<HTMLElement>) {
    const scroll2top = $('.scroll-to-top').stop(true);
    if ($(this).scrollTop()! > 100) {
      if (Number.parseInt(scroll2top.css('opacity')) === 0) {
        scroll2top.animate({ opacity: 1, 'z-index': 10_000 }, 50, 'linear');
      }
    } else {
      scroll2top.animate({ opacity: 0, 'z-index': -1 }, 50, 'linear');
    }
  });
});
