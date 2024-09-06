/// <reference types="@angular/localize" />

import { platformBrowserDynamic } from '@angular/platform-browser-dynamic';
import { AppModule } from './app/app.module';
import type * as SockJS from 'sockjs-client';
import { whenElementExists } from './app/utils';

declare global {
  interface Window {
    server: string;
    SockJS: typeof SockJS; // use premade bundle because sockjs developers don't understand using global might be a bad idea in non-node environments
  }
}

let scroll2TopAnimation: Animation | null = null;

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
function onReady(): void {
  console.timeEnd('documentReady');
  console.log('Launching GUI!');
  whenElementExists('.loading', (element) => (element.style.display = ''));

  const bodyElement = document.querySelector<HTMLElement>('body')!;

  window.addEventListener('keydown', function (event) {
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
    window.addEventListener(event, function () {
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
    if (scroll2TopAnimation) scroll2TopAnimation.cancel();
    scroll2TopAnimation =
      this.scrollTop > 100 && Number.parseInt(scroll2top.style.opacity ?? '0') === 0
        ? scroll2top.animate({ opacity: 1, 'z-index': 10_000 }, 50)
        : scroll2top.animate({ opacity: 0, 'z-index': -1 }, 50);
  });
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', onReady);
} else {
  onReady();
}
