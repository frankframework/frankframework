import { SidebarDirective } from './sidebar.directive';
import { Component, ViewChild } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';

@Component({
  standalone: true,
  template: `<ul appSidebar>
    <li></li>
    <li></li>
    <li></li>
  </ul>`,
  imports: [SidebarDirective],
})
class TestComponent {
  @ViewChild(SidebarDirective) content!: SidebarDirective;
}

describe('SidebarDirective', () => {
  let fixture: ComponentFixture<TestComponent>;
  beforeEach(() => {
    fixture = TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, SidebarDirective, TestComponent],
    }).createComponent(TestComponent);

    fixture.detectChanges(); // initial binding
  });

  it('should create an instance', () => {
    expect(fixture.componentInstance.content).toBeTruthy();
  });
});
