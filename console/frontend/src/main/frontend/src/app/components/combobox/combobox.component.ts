import {
  Component,
  ElementRef,
  EventEmitter,
  Input,
  OnChanges,
  OnInit,
  Output,
  SimpleChanges,
  ViewChild,
} from '@angular/core';
import { NgClass, NgForOf, NgIf } from '@angular/common';
import { FormsModule } from '@angular/forms';

export type Option = {
  label: string;
  description?: string;
};

@Component({
  selector: 'app-combobox',
  standalone: true,
  imports: [NgClass, FormsModule, NgIf, NgForOf],
  templateUrl: './combobox.component.html',
  styleUrl: './combobox.component.scss',
})
export class ComboboxComponent implements OnInit, OnChanges {
  @Input({ required: true }) options!: Option[];
  @Input() required: boolean = true;
  @Input() name: string = '';
  @Input() id: string = '';
  @Input() disabled: boolean = false;
  @Input() selectedOption?: string;
  @Output() selectedOptionChange: EventEmitter<string> = new EventEmitter<string>();

  @ViewChild('comboboxOptions') comboboxOptionsRef!: ElementRef;
  @ViewChild('comboboxDropdownIcon') comboboxDropdownIcon!: ElementRef;

  protected input: string = '';
  protected filteredOptions: Option[] = [];
  protected selectedIndex: number = -1;
  protected listShown: boolean = false;
  protected showError: boolean = false;

  ngOnInit(): void {
    this.resetListItems();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['selectedOption']) {
      this.input = changes['selectedOption'].currentValue ?? '';
    }
  }

  private resetListItems(): void {
    this.filteredOptions = this.options;
  }

  protected showListDisplay(): void {
    if (this.listShown) return;
    this.listShown = true;
    this.comboboxDropdownIcon.nativeElement.classList.add('combobox__dropdown-icon--active');
    this.filterListItems();
    this.highlightItemMatchingInput();
  }

  private filterListItems(): void {
    this.filteredOptions = this.options.filter(({ label }) => label.toLowerCase().startsWith(this.input.toLowerCase()));
  }

  hideListDisplay(): void {
    if (!this.listShown) return;
    this.comboboxDropdownIcon.nativeElement.classList.remove('combobox__dropdown-icon--active');
    this.listShown = false;
    this.setSelectedOption(this.input);
    this.validateInput();
  }

  private validateInput(): void {
    this.showError = !!(this.input || this.required) && !this.options.some(({ label }) => label === this.input);
    if (this.showError) this.resetListItems();
  }

  protected onUpdateInput(): void {
    this.filterListItems();
    this.highlightItemMatchingInput();
  }

  private highlightItemMatchingInput(): void {
    if (!this.listShown) return;
    const matchingItem = this.filteredOptions.findIndex(({ label }) => label === this.input);
    if (matchingItem >= 0) {
      this.selectItemInListDisplay(matchingItem);
    } else {
      this.selectedIndex = -1;
    }
  }

  private selectItemInListDisplay(index: number): void {
    this.selectedIndex = index;
    this.comboboxOptionsRef?.nativeElement
      .querySelectorAll('.combobox__list-item')
      [this.selectedIndex]?.scrollIntoView({ behavior: 'smooth', block: 'center' });
  }

  protected clickItem(index: number): void {
    this.selectItem(index);
    this.hideListDisplay();
  }

  private selectItem(index: number): void {
    if (index < 0 || index >= this.filteredOptions.length) return;
    this.input = this.filteredOptions[index].label;
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
