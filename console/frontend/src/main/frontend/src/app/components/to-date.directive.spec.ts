import { Component, DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { By } from '@angular/platform-browser';
import { ToDateDirective } from './to-date.directive';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

@Component({
  standalone: true,
  template: `
    <span appToDate [time]="1710111600000"></span>
    <span appToDate [time]="'Mon Mar 11 2024 00:00:00 GMT+0100'"></span>
  `,
  imports: [ToDateDirective],
})
class TestComponent {}

describe('ToDateDirective', () => {
  let fixture: ComponentFixture<TestComponent>;
  let directiveElements: DebugElement[];
  beforeEach(() => {
    fixture = TestBed.configureTestingModule({
      imports: [TestComponent, ToDateDirective],
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()],
    }).createComponent(TestComponent);

    fixture.detectChanges(); // initial binding

    directiveElements = fixture.debugElement.queryAll(By.directive(ToDateDirective));
  });

  it('convert time number to formatted date string', () => {
    const timeString = directiveElements[0].nativeElement.textContent;
    expect(timeString).toBe('2024-03-11 00:00:00');
  });

  it('convert time string to formatted date string', () => {
    const timeString = directiveElements[1].nativeElement.textContent;
    expect(timeString).toBe('2024-03-11 00:00:00');
  });
});
