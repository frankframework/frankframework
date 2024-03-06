import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { QuickSubmitFormDirective } from './quick-submit-form.directive';

@Component({
  standalone: true,
  template: `<!-- TODO -->`,
  imports: [QuickSubmitFormDirective],
})
class TestComponent {}

describe('QuickSubmitFormDirective', () => {
  let fixture: ComponentFixture<TestComponent>;
  beforeEach(() => {
    fixture = TestBed.configureTestingModule({
      imports: [QuickSubmitFormDirective, TestComponent],
    }).createComponent(TestComponent);

    fixture.detectChanges(); // initial binding
  });
});
