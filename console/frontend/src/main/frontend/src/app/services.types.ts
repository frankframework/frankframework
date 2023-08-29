export interface HooksService {
  call: (...args: any[]) => void;
  register: (...args: any[]) => void;
}
