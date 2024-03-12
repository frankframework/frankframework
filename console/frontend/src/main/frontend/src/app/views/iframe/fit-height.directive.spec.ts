import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FitHeightDirective } from './fit-height.directive';

@Component({
  // standalone: true,
  template: `<!-- TODO -->`,
  imports: [FitHeightDirective],
})
class TestComponent {}

describe('FitHeightDirective', () => {
  let fixture: ComponentFixture<TestComponent>;
  beforeEach(() => {
    fixture = TestBed.configureTestingModule({
      imports: [],
      declarations: [TestComponent, FitHeightDirective],
    }).createComponent(TestComponent);

    fixture.detectChanges(); // initial binding
  });

  // temporatory, remove when making actual tests
  it('parent component should exist', () => {
    expect(fixture).toBeTruthy();
  });

  // TODO test if height gets set properly, somehow
});
