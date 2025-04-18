import { WebPlugin } from '@capacitor/core';
import type { ScreenCapturePlugin } from './definitions';

export class ScreenCaptureWeb extends WebPlugin implements ScreenCapturePlugin {
  async start(options: { url: string }): Promise<void> {
    console.warn('[ScreenCapture] start() is not supported on web.', options);
  }

  async stop(): Promise<void> {
    console.warn('[ScreenCapture] stop() is not supported on web.');
  }


}
