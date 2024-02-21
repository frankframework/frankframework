/// <reference types="@angular/localize" />

import { platformBrowserDynamic } from '@angular/platform-browser-dynamic';
import { AppModule } from './app/app.module';
import * as Prism from 'prismjs';
import 'prismjs/plugins/line-numbers/prism-line-numbers';
import 'prismjs/plugins/line-highlight/prism-line-highlight';
import 'prismjs/plugins/custom-class/prism-custom-class';
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
  .catch(err => console.error(err));

declare global {
  var ff_version: string;
  var serverurl: string;
  var server: string;
  // var jQuery: jQuery; already defined in @types/jquery (type import solves this for us?)
  // var $: jQuery;
}

if (location.hostname != "localhost") {
  window.console.log("%cThis is a browser feature intended for developers. Do not paste any code here given to you by someone else. It may compromise your account or have other negative side effects.", "font-weight: bold; font-size: 14px;");
}

console.time("startup");
console.time("documentReady");

/* Main.js */
$(document).ready(function () {
  console.timeEnd("documentReady");
  console.log("Launching GUI!");
  $(".loading").css('display', '');
  // Full height of sidebar
  function fix_height_func() {
    var navbarHeight = $('nav.navbar-default').height()!;
    var wrapperHeight = $('#page-wrapper').height()!;

    if (navbarHeight <= wrapperHeight && $(window).height()! > navbarHeight) {
      $('#page-wrapper').css("min-height", $(window).height() + "px");
    } else {
      $('#page-wrapper').css("min-height", navbarHeight + "px");
    }

  }

  $(window).on("resize scroll", function () {
    if (!$("body").hasClass('body-small')) {
      fix_height();
    }
  });
  $(window).on("load", function () {
    if (!$("body").hasClass('body-small')) {
      fix_height(500);
    }
  });

  function fix_height(time?: number) {
    if (!time) time = 50;
    setTimeout(function () {
      fix_height_func();
    }, time);
  }

  window.addEventListener("keydown", function (e) {
    if (e.which == 70 && (e.ctrlKey || e.metaKey) && e.shiftKey) {
      var searchbar = document.getElementById("searchbar");
      if (searchbar) {
        e.preventDefault();
        searchbar.getElementsByTagName("input")[0].focus();
      }
    }
  });
});

//Foist: To force upon or impose fraudulently or unjustifiably
function foist(callback: () => void) {
  // @ts-ignore
  $(document.body).scope().foist(callback);
}
//Changes the log level to; 0 - error, 1 - warn, 2 - info, 3 - debug
function setLogLevel(level: number) {
  // @ts-ignore
  $(document.body).scope().setLogLevel(level);
}
//Detect if using any (older) version of Internet Explorer
if (navigator.userAgent.indexOf('MSIE') !== -1 || navigator.appVersion.indexOf('Trident/') > -1) {
  $("body").prepend("<h2 style='text-align: center; color: #fdc300;'><strong>Internet Explorer 11 and older do not support XHR requests, the Frank!Console might not load correctly!</strong><br/>Please open this website in MS Edge, Mozilla Firefox or Google Chrome.</h2>");
}

// Automatically minimalize menu when screen is less than 768px
$(function () {
  $(window).on("load resize", function () {
    if ($(document).width()! < 769) {
      $("body").addClass("body-small");
    } else {
      $("body").removeClass("body-small");
    }
  });

  $('body').on("scroll", function (this: JQuery<HTMLElement>) {
    var scroll2top = $(".scroll-to-top").stop(true);
    if ($(this).scrollTop()! > 100) {
      if (parseInt(scroll2top.css("opacity")) === 0) {
        scroll2top.animate({ "opacity": 1, "z-index": 10000 }, 50, "linear");
      }
    } else {
      scroll2top.animate({ "opacity": 0, "z-index": -1 }, 50, "linear");
    }
  });

  Prism.hooks.add('after-highlight', function (env) {
    // works only for <code> wrapped inside <pre data-line-numbers> (not inline)
    var pre = env.element.parentNode as HTMLElement;
    if (!pre || !/pre/i.test(pre.nodeName) || pre.className.indexOf('line-numbers') === -1) {
      return;
    }

    var linesNum = (env.code.split('\n').length);
    var lineNumbersWrapper;

    let lines = new Array(linesNum);
    //See https://stackoverflow.com/questions/1295584/most-efficient-way-to-create-a-zero-filled-javascript-array
    for (let i = 0; i < linesNum; ++i) lines[i] = '<span id="L' + (i + 1) + '"></span>';

    lineNumbersWrapper = document.createElement('span');
    lineNumbersWrapper.className = 'line-numbers-rows';
    lineNumbersWrapper.innerHTML = lines.join("");

    env.element.appendChild(lineNumbersWrapper);
  });

  // this stinks but blame prismjs for the bad support for how its handling / giving content
  const customClassFn = ({ language, type, content }: { language: string, type: string, content: string }): any => {
    if (type === 'tag' && content.endsWith('<span class="token punctuation">></span>') && content.includes('adapter')) {
      const nameRegex = /<span class="token attr-value"><span class="token punctuation attr-equals">=<\/span><span class="token punctuation">"<\/span>(?<value>[^<]+)<span class="token punctuation">"<\/span><\/span>/g.exec(content);
      if(nameRegex?.groups)
        return `adapter-tag ${nameRegex?.groups['value']}`;
      return 'adapter-tag';
    }
  }
  Prism.plugins['customClass'].add(customClassFn);
});
