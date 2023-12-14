import { ComponentFixture, TestBed } from '@angular/core/testing';

import { JmsBrowseQueueComponent } from './jms-browse-queue.component';

describe('JmsBrowseQueueComponent', () => {
  let component: JmsBrowseQueueComponent;
  let fixture: ComponentFixture<JmsBrowseQueueComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ JmsBrowseQueueComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(JmsBrowseQueueComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
