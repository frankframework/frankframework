import { Component, EventEmitter, Input, Output, ChangeDetectionStrategy } from '@angular/core';
import { NgClass } from '@angular/common';

@Component({
  selector: 'app-tab-list',
  imports: [NgClass],
  templateUrl: './tab-list.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './tab-list.component.scss',
})
export class TabListComponent {
  @Input() selectedTab: string;
  @Input() showAllTab = true;
  @Output() selectedTabChange = new EventEmitter<string>();

  protected _allTabName = 'All';
  protected tabsList: string[] = [String(this._allTabName)];

  constructor() {
    this.selectedTab = this._allTabName;
  }

  @Input()
  set tabs(tabs: string[]) {
    if (this.showAllTab) {
      this.tabsList = [String(this._allTabName), ...tabs];
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
