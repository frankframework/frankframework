import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Component, DebugElement } from '@angular/core';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TimeSinceDirective } from './time-since.directive';
import { By } from '@angular/platform-browser';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

@Component({
  template: `
    <span appTimeSince [time]="fiveMinAgo"></span>
    <span appTimeSince [time]="oneHourAgo"></span>
    <span appTimeSince [time]="oneDayAgo"></span>
  `,
  imports: [TimeSinceDirective],
})
class TestComponent {
  protected fiveMinAgo = Date.now() - 3e5;
  protected oneHourAgo = Date.now() - 36e5; // 3600000
  protected oneDayAgo = Date.now() - 864e5; // 86400000
}

describe('TimeSinceDirective', () => {
  let fixture: ComponentFixture<TestComponent>;
  let directiveElements: DebugElement[];
  beforeEach(() => {
    fixture = TestBed.configureTestingModule({
      imports: [TestComponent, TimeSinceDirective],
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()],
    }).createComponent(TestComponent);

    fixture.detectChanges(); // initial binding

    directiveElements = fixture.debugElement.queryAll(By.directive(TimeSinceDirective));
  });

  it('get time since 5 minutes ago', () => {
    const timeString = directiveElements[0].nativeElement.textContent;
    expect(timeString).toBe('5m');
  });

  it('get time since 5 minutes ago', () => {
    const timeString = directiveElements[1].nativeElement.textContent;
    expect(timeString).toBe('1h');
  });

  it('get time since 5 minutes ago', () => {
    const timeString = directiveElements[2].nativeElement.textContent;
    expect(timeString).toBe('1d');
  });
});
