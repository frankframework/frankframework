import { WithJavaListenerPipe } from './with-java-listener.pipe';

describe('WithJavaListenerPipe', () => {
  it('create an instance', () => {
    const pipe = new WithJavaListenerPipe();
    expect(pipe).toBeTruthy();
  });
});
