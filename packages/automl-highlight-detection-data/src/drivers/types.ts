import { SelectionAnnotation, DOMAIN } from "../browser-inject";

export interface Driver {
  initializeContext(arg: {
    browser: "firefox" | "chrome";
    device: "string";
  }): Promise<void>;
  getScreenshot(arg: {
    href: string;
    outputPath: string;
    domain: DOMAIN;
  }): Promise<SelectionAnnotation[]>;
  cleanup(): Promise<void>;
}
