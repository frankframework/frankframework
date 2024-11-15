import { Component, EventEmitter, Input, Output } from '@angular/core';
import { NgClass, NgForOf } from '@angular/common';

@Component({
  selector: 'app-tab-list',
  standalone: true,
  imports: [NgForOf, NgClass],
  templateUrl: './tab-list.component.html',
  styleUrl: './tab-list.component.scss',
})
export class TabListComponent {
  protected tabsList: string[] = ['All'];
  @Input() protected selectedTab: string = 'All';
  @Output() selectedTabChange: EventEmitter<string> = new EventEmitter();

  @Input()
  set tabs(tabs: string[]) {
    this.tabsList = ['All', ...tabs];
  }

  protected changeTab(tab: string): void {
    this.selectedTab = tab;
    this.selectedTabChange.emit(tab);
  }
}
