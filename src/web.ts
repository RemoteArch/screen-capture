import { WebPlugin } from '@capacitor/core';

import type { ScreenCapturePlugin } from './definitions';

export class ScreenCaptureWeb extends WebPlugin implements ScreenCapturePlugin {
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
}
