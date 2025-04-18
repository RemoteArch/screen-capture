export interface ScreenCapturePlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
}
