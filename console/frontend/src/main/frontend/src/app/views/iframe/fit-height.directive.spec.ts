import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FitHeightDirective } from './fit-height.directive';

@Component({
  standalone: true,
  template: `<!-- TODO -->`,
  imports: [FitHeightDirective],
})
class TestComponent {}

describe('FitHeightDirective', () => {
  let fixture: ComponentFixture<TestComponent>;
  beforeEach(() => {
    fixture = TestBed.configureTestingModule({
      imports: [FitHeightDirective, TestComponent],
    }).createComponent(TestComponent);

    fixture.detectChanges(); // initial binding
  });
});
