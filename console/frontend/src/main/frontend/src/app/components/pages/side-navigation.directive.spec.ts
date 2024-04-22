import { Component } from '@angular/core';
import { SideNavigationDirective } from './side-navigation.directive';
import { ComponentFixture, TestBed } from '@angular/core/testing';

@Component({
  standalone: true,
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
      imports: [TestComponent, SideNavigationDirective],
    }).createComponent(TestComponent);

    fixture.detectChanges(); // initial binding
  });

  // temporatory test, create more meaningful tests when metismenu has been replaced with own implementation or remove test if new dependency is sufficient enough
  it('parent component should exist & recognise directive class', () => {
    expect(fixture).toBeTruthy();
  });
});
