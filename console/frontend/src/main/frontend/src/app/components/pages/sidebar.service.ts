import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class SidebarService {
  toggle(): void {
    $('body').toggleClass('mini-navbar');
    this.swichStates();
  }

  small(): void {
    $('body').addClass('mini-navbar');
    this.swichStates();
  }

  private swichStates(): void {
    if (!$('body').hasClass('mini-navbar') || $('body').hasClass('body-small')) {
      // Hide menu in order to smoothly turn on when maximize menu
      $('#side-menu').hide();
      // For smoothly turn on menu
      setTimeout(function () {
        $('#side-menu').fadeIn(400);
      }, 200);
    } else if ($('body').hasClass('fixed-sidebar')) {
      $('#side-menu').hide();
      setTimeout(function () {
        $('#side-menu').fadeIn(400);
      }, 100);
    } else {
      // Remove all inline style from jquery fadeIn function to reset menu state
      $('#side-menu').removeAttr('style');
    }
  }
}
