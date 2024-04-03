/// <reference types="@angular/localize" />

import { platformBrowserDynamic } from '@angular/platform-browser-dynamic';
import { AppModule } from './app/app.module';
import * as Prism from 'prismjs';
import 'prismjs/plugins/line-numbers/prism-line-numbers';
import 'prismjs/plugins/line-highlight/prism-line-highlight';
import 'prismjs/plugins/custom-class/prism-custom-class';

declare global {
  let ff_version: string;
  interface Window {
    server: string;
  }
  // var jQuery: jQuery; already defined in @types/jquery (type import solves this for us?)
  // var $: jQuery;
}

function main(): void {
  platformBrowserDynamic()
    .bootstrapModule(AppModule)
    .catch((error) => console.error(error));
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
    if (
      event.key == 'F' &&
      (event.ctrlKey || event.metaKey) &&
      event.shiftKey
    ) {
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
if (
  navigator.userAgent.includes('MSIE') ||
  navigator.appVersion.includes('Trident/')
) {
  $('body').prepend(
    "<h2 style='text-align: center; color: #fdc300;'><strong>Internet Explorer 11 and older do not support XHR requests, the Frank!Console might not load correctly!</strong><br/>Please open this website in MS Edge, Mozilla Firefox or Google Chrome.</h2>",
  );
}

// this stinks but blame prismjs for the bad support for how its handling / giving content
function customClassFunction({
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  language,
  type,
  content,
}: {
  language: string;
  type: string;
  content: string;
}): string | void {
  if (
    type === 'tag' &&
    content.endsWith('<span class="token punctuation">></span>') &&
    content.includes('adapter')
  ) {
    const nameRegex =
      /<span class="token attr-value"><span class="token punctuation attr-equals">=<\/span><span class="token punctuation">"<\/span>(?<value>[^<]+)<span class="token punctuation">"<\/span><\/span>/g.exec(
        content,
      );
    if (nameRegex?.groups) return `adapter-tag ${nameRegex?.groups['value']}`;
    return 'adapter-tag';
  }
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

  Prism.hooks.add('after-highlight', function (environment) {
    // works only for <code> wrapped inside <pre data-line-numbers> (not inline)
    const pre = environment.element.parentNode as HTMLElement;
    if (
      !pre ||
      !/pre/i.test(pre.nodeName) ||
      !pre.className.includes('line-numbers')
    ) {
      return;
    }

    const linesNumber = environment.code.split('\n').length;

    const lines = Array.from({ length: linesNumber });
    //See https://stackoverflow.com/questions/1295584/most-efficient-way-to-create-a-zero-filled-javascript-array
    for (let index = 0; index < linesNumber; ++index)
      lines[index] = `<span id="L${index + 1}"></span>`;

    const lineNumbersWrapper = document.createElement('span');
    lineNumbersWrapper.className = 'line-numbers-rows';
    lineNumbersWrapper.innerHTML = lines.join('');

    environment.element.append(lineNumbersWrapper);
  });

  Prism.plugins['customClass'].add(customClassFunction);
});
