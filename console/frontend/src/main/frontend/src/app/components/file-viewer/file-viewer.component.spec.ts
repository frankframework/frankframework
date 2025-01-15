import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { FileViewerComponent } from './file-viewer.component';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('FileViewerComponent', () => {
  let component: FileViewerComponent;
  let fixture: ComponentFixture<FileViewerComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FileViewerComponent],
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(FileViewerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
