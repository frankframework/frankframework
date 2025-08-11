/// <reference types="@angular/localize" />

import { bootstrapApplication } from '@angular/platform-browser';
import type SockJS from 'sockjs-client';
import { AppComponent } from './app/app.component';
import { appConfig } from './app/app.config';
import { whenElementExists } from './app/utilities';

declare global {
  // eslint-disable-next-line @typescript-eslint/consistent-type-definitions
  interface Window {
    server: string;
    SockJS: typeof SockJS; // use premade bundle because sockjs developers don't understand using global might be a bad idea in non-node environments
  }
}

function main(): void {
  bootstrapApplication(AppComponent, appConfig).catch((error) => console.error(error));
}

if (location.hostname != 'localhost') {
  globalThis.console.log(
    '%cThis is a browser feature intended for developers. Do not paste any code here given to you by someone else. It may compromise your account or have other negative side effects.',
    'font-weight: bold; font-size: 14px;',
  );
}

console.time('startup');
main();
console.time('documentReady');

/* Main.js */
function onReady(): void {
  console.timeEnd('documentReady');
  console.log('Launching GUI!');
  whenElementExists('.loading', (element) => (element.style.display = ''));

  const bodyElement = document.querySelector<HTMLElement>('body')!;

  globalThis.addEventListener('keydown', function (event) {
    if (event.key == 'F' && (event.ctrlKey || event.metaKey) && event.shiftKey) {
      const searchbar = document.querySelector('#searchbar');
      if (searchbar) {
        event.preventDefault();
        searchbar.querySelectorAll('input')[0].focus();
      }
    }
  });

  // Automatically minimalize menu when screen is less than 768px
  for (const event of ['resize', 'load']) {
    globalThis.addEventListener(event, function () {
      if (bodyElement.clientWidth < 769) {
        bodyElement.classList.add('body-small');
      } else {
        bodyElement.classList.remove('body-small');
      }
    });
  }
  bodyElement.addEventListener('scroll', function (this: HTMLElement) {
    const scroll2top = this.querySelector<HTMLElement>('.scroll-to-top');
    if (!scroll2top) return;
    if (scroll2top.classList.contains('hidden-scroll') && this.scrollTop > 100) {
      scroll2top.classList.remove('hidden-scroll');
    } else if (!scroll2top.classList.contains('hidden-scroll') && this.scrollTop <= 100) {
      scroll2top.classList.add('hidden-scroll');
    }
  });
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', onReady);
} else {
  onReady();
}
