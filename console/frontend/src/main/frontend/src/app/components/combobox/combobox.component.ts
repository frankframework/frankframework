import { Component, CUSTOM_ELEMENTS_SCHEMA, EventEmitter, Input, OnInit, Output, ViewChild } from '@angular/core';
import { NgClass, NgForOf, NgIf } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-combobox',
  standalone: true,
  imports: [NgClass, FormsModule, NgIf, NgForOf],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  templateUrl: './combobox.component.html',
  styleUrl: './combobox.component.scss',
})
export class ComboboxComponent implements OnInit {
  @Input({ required: true }) options!: string[];
  @Input() required: boolean = true;
  @Input() name: string = '';
  @Input() selectedOption?: string;
  @Output() selectedOptionChange: EventEmitter<string> = new EventEmitter<string>();

  @ViewChild('comboBoxOptions') comboBoxOptions!: HTMLElement;

  protected input: string = '';
  protected filteredOptions: string[] = [];
  protected selectedIndex: number = -1;
  protected listShown: boolean = false;
  protected showError: boolean = false;

  ngOnInit(): void {
    this.resetListItems();
  }

  private resetListItems(): void {
    this.filteredOptions = this.options;
  }

  protected showListDisplay(): void {
    if (this.listShown) return;
    this.listShown = true;
    this.filterListItems();
    this.highlightItemMatchingInput();
  }

  private filterListItems(): void {
    this.filteredOptions = this.options.filter((item) => item.toLowerCase().startsWith(this.input.toLowerCase()));
  }

  hideListDisplay(): void {
    if (!this.listShown) return;
    this.listShown = false;
    this.validateInput();
  }

  private validateInput(): void {
    this.showError = !!(this.input || this.required) && !this.options.includes(this.input);
    if (this.showError) this.resetListItems();
  }

  protected onUpdateInput(): void {
    this.filterListItems();
    this.highlightItemMatchingInput();
  }

  private highlightItemMatchingInput(): void {
    if (!this.input) return;
    const matchingItem = this.filteredOptions.indexOf(this.input);
    if (matchingItem >= 0) {
      this.selectItemInListDisplay(matchingItem);
    } else {
      this.selectedIndex = -1;
    }
  }

  private selectItemInListDisplay(index: number): void {
    this.selectedIndex = index;
    document.querySelectorAll('.list-item')[this.selectedIndex]?.scrollIntoView();
  }

  protected clickItem(index: number): void {
    this.selectItem(index);
    this.hideListDisplay();
  }

  private selectItem(index: number): void {
    this.input = this.filteredOptions[index];
    this.setSelectedOption(this.input);
  }

  private setSelectedOption(option: string): void {
    this.selectedOption = option;
    this.selectedOptionChange.emit(this.selectedOption);
  }

  protected onKeyPress(event: KeyboardEvent): void {
    this.showListDisplay();
    switch (event.key) {
      case 'Escape': {
        this.clearSelectedItem();
        this.hideListDisplay();
        break;
      }
      case 'Enter': {
        this.selectItem(this.selectedIndex);
        this.hideListDisplay();
        break;
      }

      case 'ArrowDown': {
        this.selectNext();
        break;
      }
      case 'ArrowUp': {
        this.selectPrevious();
        break;
      }
    }
  }

  protected clearSelectedItem(): void {
    this.selectedIndex = -1;
    this.input = '';
    this.setSelectedOption('');
    this.hideListDisplay();
  }

  private selectNext(): void {
    if (this.filteredOptions.length <= 0) {
      return;
    }
    this.selectItemInListDisplay((this.selectedIndex + 1) % this.filteredOptions.length);
  }

  private selectPrevious(): void {
    if (this.filteredOptions.length <= 0) {
      return;
    }
    if (this.selectedIndex === -1) this.selectedIndex = 0;
    if (this.selectedIndex <= 0) {
      this.selectedIndex = this.filteredOptions.length + this.selectedIndex;
    }
    this.selectItemInListDisplay((this.selectedIndex - 1) % this.filteredOptions.length);
  }
}
