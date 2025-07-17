import { Directive, ElementRef, AfterViewInit, OnDestroy, inject } from '@angular/core';
import { AppService } from '../../../app.service';
import { Subscription } from 'rxjs';

@Directive({
  selector: '[appSidebar]',
  standalone: true,
})
export class SidebarDirective implements AfterViewInit, OnDestroy {
  private readonly appService: AppService = inject(AppService);
  private readonly sideMenuElementReference: ElementRef<HTMLElement> = inject(ElementRef);
  private sideMenuElement = this.sideMenuElementReference.nativeElement;
  private bodyElement = document.querySelector<HTMLElement>('body')!;
  private subscription = new Subscription();

  ngAfterViewInit(): void {
    const sidebarSubscription = this.appService.toggleSidebar$.subscribe(() => {
      this.toggle();
    });
    this.subscription.add(sidebarSubscription);
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }

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
        this.sideMenuElement.style.opacity = '0';
        // For smoothly turn on menu
        setTimeout(() => {
          this.sideMenuElement?.animate({ opacity: 1 }, 400).finished.then(() => {
            this.sideMenuElement.removeAttribute('style');
          });
        }, 200);
      } else if (this.bodyElement.classList.contains('fixed-sidebar')) {
        this.sideMenuElement.style.opacity = '0';
        setTimeout(() => {
          this.sideMenuElement?.animate({ opacity: 1 }, 400).finished.then(() => {
            this.sideMenuElement.removeAttribute('style');
          });
        }, 100);
      } else {
        // Remove all inline style from jquery fadeIn function to reset menu state
        this.sideMenuElement.removeAttribute('style');
      }
    }
  }
}
