import { registerPlugin } from '@capacitor/core';

import type { ScreenCapturePlugin } from './definitions';

const ScreenCapture = registerPlugin<ScreenCapturePlugin>('ScreenCapture', {
  web: () => import('./web').then((m) => new m.ScreenCaptureWeb()),
});

export * from './definitions';
export { ScreenCapture };
