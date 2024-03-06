import { Component } from '@angular/core';
import { SideNavigationDirective } from './side-navigation.directive';
import { ComponentFixture, TestBed } from '@angular/core/testing';

@Component({
  // standalone: true,
  template: `
    <ul>
      <li>
        <a>Category 1</a>
        <ul>
          <li><a>Item 1</a></li>
          <li><a>Item 2</a></li>
          <li><a>Item 3</a></li>
        </ul>
      </li>
      <li>
        <a>Category 2</a>
        <ul>
          <li><a>Item 1</a></li>
          <li><a>Item 2</a></li>
          <li><a>Item 3</a></li>
        </ul>
      </li>
    </ul>
  `,
  imports: [SideNavigationDirective],
})
class TestComponent {}

describe('SideNavigationDirective', () => {
  let fixture: ComponentFixture<TestComponent>;
  beforeEach(() => {
    fixture = TestBed.configureTestingModule({
      imports: [],
      declarations: [TestComponent, SideNavigationDirective],
    }).createComponent(TestComponent);

    fixture.detectChanges(); // initial binding
  });

  // TODO figure out a way to test metismenu
  // https://angular.io/guide/testing-attribute-directives

  // temporatory, remove when making actual tests
  it('parent component should exist', () => {
    expect(fixture).toBeTruthy();
  });
});
