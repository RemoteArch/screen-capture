export interface ScreenCapturePlugin {
  start(options: { url: string }): Promise<void>;
  stop(): Promise<void>;
}
