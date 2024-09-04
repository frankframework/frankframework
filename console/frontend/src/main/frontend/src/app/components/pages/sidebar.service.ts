import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class SidebarService {
  private bodyElement = document.querySelector<HTMLElement>('body')!;
  private sideMenuElement = document.querySelector<HTMLElement>('#side-menu');

  toggle(): void {
    this.bodyElement.classList.toggle('mini-navbar');
    this.swichStates();
  }

  small(): void {
    this.bodyElement.classList.add('mini-navbar');
    this.swichStates();
  }

  private swichStates(): void {
    if (this.sideMenuElement) {
      if (!this.bodyElement.classList.contains('mini-navbar') || this.bodyElement.classList.contains('body-small')) {
        // Hide menu in order to smoothly turn on when maximize menu
        this.sideMenuElement.style.display = 'none';
        // For smoothly turn on menu
        setTimeout(() => {
          this.sideMenuElement.fadeIn(400);
        }, 200);
      } else if (this.bodyElement.classList.contains('fixed-sidebar')) {
        this.sideMenuElement.style.display = 'none';
        setTimeout(() => {
          this.sideMenuElement.fadeIn(400);
        }, 100);
      } else {
        // Remove all inline style from jquery fadeIn function to reset menu state
        this.sideMenuElement.removeAttribute('style');
      }
    }
  }
}
