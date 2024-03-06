import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TimeSinceDirective } from './time-since.directive';
import { Component } from '@angular/core';

@Component({
  // standalone: true,
  template: `<!-- TODO -->`,
  imports: [TimeSinceDirective],
})
class TestComponent {}

describe('TimeSinceDirective', () => {
  let fixture: ComponentFixture<TestComponent>;
  beforeEach(() => {
    fixture = TestBed.configureTestingModule({
      imports: [],
      declarations: [TestComponent, TimeSinceDirective],
    }).createComponent(TestComponent);

    fixture.detectChanges(); // initial binding
  });

  // temporatory, remove when making actual tests
  it('parent component should exist', () => {
    expect(fixture).toBeTruthy();
  });
});
