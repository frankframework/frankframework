import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TestServiceListenerComponent } from './test-service-listener.component';

describe('TestServiceListenerComponent', () => {
  let component: TestServiceListenerComponent;
  let fixture: ComponentFixture<TestServiceListenerComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ TestServiceListenerComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TestServiceListenerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
