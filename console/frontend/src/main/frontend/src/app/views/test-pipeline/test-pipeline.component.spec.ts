import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TestPipelineComponent } from './test-pipeline.component';

describe('TestPipelineComponent', () => {
  let component: TestPipelineComponent;
  let fixture: ComponentFixture<TestPipelineComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ TestPipelineComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TestPipelineComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
