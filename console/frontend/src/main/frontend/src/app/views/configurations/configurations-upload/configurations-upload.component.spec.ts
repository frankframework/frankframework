import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ConfigurationsUploadComponent } from './configurations-upload.component';

describe('ConfigurationsUploadComponent', () => {
  let component: ConfigurationsUploadComponent;
  let fixture: ComponentFixture<ConfigurationsUploadComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ConfigurationsUploadComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ConfigurationsUploadComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
