import { Component, EventEmitter, Input, Output } from '@angular/core';
import { NgClass } from '@angular/common';

@Component({
  selector: 'app-tab-list',
  imports: [NgClass],
  templateUrl: './tab-list.component.html',
  styleUrl: './tab-list.component.scss',
})
export class TabListComponent {
  protected _allTabName = 'All';

  @Input() selectedTab: string = this._allTabName;
  @Input() showAllTab = true;
  @Output() selectedTabChange = new EventEmitter<string>();

  protected tabsList: string[] = [`${this._allTabName}`];

  @Input()
  set tabs(tabs: string[]) {
    if (this.showAllTab) {
      this.tabsList = [`${this._allTabName}`, ...tabs];
      return;
    }
    this.tabsList = tabs;
  }

  @Input()
  set allTabName(name: string) {
    this._allTabName = name;
    this.selectedTab = name;
  }

  protected changeTab(tab: string): void {
    this.selectedTab = tab;
    this.selectedTabChange.emit(tab);
  }
}
