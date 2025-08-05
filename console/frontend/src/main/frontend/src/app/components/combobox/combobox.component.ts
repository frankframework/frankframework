import {
  Component,
  ElementRef,
  EventEmitter,
  HostListener,
  Input,
  OnChanges,
  OnInit,
  Output,
  SimpleChanges,
  ViewChild,
} from '@angular/core';
import { NgClass } from '@angular/common';
import { FormsModule } from '@angular/forms';

export type Option = {
  label: string;
  description?: string;
};

@Component({
  selector: 'app-combobox',
  imports: [NgClass, FormsModule],
  templateUrl: './combobox.component.html',
  styleUrl: './combobox.component.scss',
})
export class ComboboxComponent implements OnInit, OnChanges {
  @Input({ required: true }) options!: Option[];
  @Input() required = true;
  @Input() name = '';
  @Input() id = '';
  @Input() disabled = false;
  @Input() selectedOption?: string;
  @Output() selectedOptionChange: EventEmitter<string> = new EventEmitter<string>();

  @ViewChild('comboboxOptions') comboboxOptionsRef!: ElementRef;
  @ViewChild('comboboxDropdownIcon') comboboxDropdownIcon!: ElementRef;

  protected input = '';
  protected filteredOptions: Option[] = [];
  protected selectedIndex = -1;
  protected listShown = false;
  protected showError = false;

  @HostListener('keydown.enter', ['$event'])
  protected onEnter(event: KeyboardEvent): void {
    event.preventDefault();
    this.selectItem(this.selectedIndex);
    this.hideListDisplay();
  }

  @HostListener('keydown.escape')
  protected onEscape(): void {
    this.clearSelectedItem();
    this.hideListDisplay();
  }

  @HostListener('keydown.arrowUp')
  protected onArrowUp(): void {
    this.selectPrevious();
  }

  @HostListener('keydown.arrowDown')
  protected onArrowDown(): void {
    this.selectNext();
  }

  ngOnInit(): void {
    this.resetListItems();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['selectedOption']) {
      this.input = changes['selectedOption'].currentValue ?? '';
    }
  }

  protected showListDisplay(): void {
    if (this.listShown) return;
    this.listShown = true;
    this.comboboxDropdownIcon.nativeElement.classList.add('combobox__dropdown-icon--active');
    this.filterListItems();
    this.highlightItemMatchingInput();
  }

  protected onUpdateInput(): void {
    this.filterListItems();
    this.highlightItemMatchingInput();
  }

  protected clickItem(index: number): void {
    this.selectItem(index);
    this.hideListDisplay();
  }

  protected clearSelectedItem(): void {
    this.selectedIndex = -1;
    this.input = '';
    this.setSelectedOption('');
    this.hideListDisplay();
  }

  protected hideListDisplay(): void {
    if (!this.listShown) return;
    this.comboboxDropdownIcon.nativeElement.classList.remove('combobox__dropdown-icon--active');
    this.listShown = false;
    this.setSelectedOption(this.input);
    this.validateInput();
  }

  private resetListItems(): void {
    this.filteredOptions = this.options;
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

  private filterListItems(): void {
    this.filteredOptions = this.options.filter(({ label }) => label.toLowerCase().startsWith(this.input.toLowerCase()));
  }

  private validateInput(): void {
    this.showError = !!(this.input || this.required) && !this.options.some(({ label }) => label === this.input);
    if (this.showError) this.resetListItems();
  }

  private highlightItemMatchingInput(): void {
    if (!this.listShown) return;
    const matchingItem = this.filteredOptions.findIndex(({ label }) => label === this.input);
    if (matchingItem === -1) {
      this.selectedIndex = -1;
    } else {
      this.selectItemInListDisplay(matchingItem);
    }
  }

  private selectItemInListDisplay(index: number): void {
    this.selectedIndex = index;
    this.comboboxOptionsRef?.nativeElement
      .querySelectorAll('.combobox__list-item')
      [this.selectedIndex]?.scrollIntoView({ behavior: 'smooth', block: 'center' });
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
