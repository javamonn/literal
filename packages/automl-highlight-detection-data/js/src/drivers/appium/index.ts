import { writeFileSync } from "fs";
import { WebDriverLogTypes } from "webdriver";
import { remote as webdriver } from "webdriverio";
import {
  SelectionAnnotation,
  browserInject,
  DOMAIN,
  parsers,
} from "../../browser-inject";
import { APPIUM_PORT, APPIUM_HOSTNAME } from "../../constants";
import { Driver } from "../types";

enum Orientation {
  LANDSCAPE = "LANDSCAPE",
  PORTRAIT = "PORTRAIT",
}

enum AppiumContext {
  NATIVE_APP = "NATIVE_APP",
  CHROMIUM = "CHROMIUM",
}

const RAND_PORTRAIT_ORIENTATION_THRESHOLD = 0.66;
const RAND_SCROLL_THRESHOLD = 0.25;

type Rect = {
  width: number;
  height: number;
  top: number;
  left: number;
};

type AndroidCapabilities = WebDriver.DesiredCapabilities & {
  statBarHeight: number;
  viewportRect: Rect;
  pixelRatio: number;
};

export class AppiumDriver implements Driver {
  context: WebdriverIOAsync.BrowserObject;
  systemBars: {
    navigationBar: { height: number; width: number };
    statusBar: { height: number; width: number };
  };

  initializeContext = async ({
    browser,
    device,
  }: {
    browser: string;
    device: string;
  }) => {
    const opts = {
      port: APPIUM_PORT,
      hostname: APPIUM_HOSTNAME,
      capabilities: {
        platformName: "Android",
        platformVersion: "11",
        deviceName: device,
        browserName: browser,
      },
      logLevel: "warn" as WebDriverLogTypes,
    };

    this.context = await webdriver(opts);

    this.context.setTimeout({ pageLoad: 3 * 60 * 1000 });
  };

  getViewportRect = () => {
    if (!this.context) {
      throw new Error("Driver uninitialized");
    }

    /**
     * size includes status bar but excludes navigation bar
     */
    return this.context.getWindowSize();
  };

  getSystemBars = async () => {
    if (!this.systemBars) {
      this.systemBars = (await this.context.getSystemBars()) as any;
    }
    return this.systemBars;
  };

  getCapabilities = () => {
    return this.context.capabilities as AndroidCapabilities;
  };

  getScreenshot = async ({
    href,
    domain,
    outputPath,
    forceNavigate,
  }: {
    href: string;
    outputPath: string;
    domain: DOMAIN;
    forceNavigate: boolean;
  }): Promise<SelectionAnnotation[]> => {
    if (!this.context) {
      throw new Error("Driver uninitialized");
    }

    let orientation =
      Math.random() > RAND_PORTRAIT_ORIENTATION_THRESHOLD
        ? Orientation.LANDSCAPE
        : Orientation.PORTRAIT;
    const currentOrientation = await this.context.getOrientation();

    if (currentOrientation !== orientation) {
      await this.context.setOrientation(orientation);
      // wait for chrome to reflow
      await new Promise((resolve) => setTimeout(resolve, 500));
      orientation = (await this.context.getOrientation()) as Orientation;
    }

    await this.context.switchContext(AppiumContext.CHROMIUM);
    const currentHref = await this.context.execute(() => window.location.href);
    if (forceNavigate || !parsers[domain].isUrlEqual(currentHref, href)) {
      await this.context.navigateTo(href);

      // scroll some in order to hide the chrome app bar, scroll position will be set by browserInject
      // regardless.
      /**
      if (Math.random() > RAND_SCROLL_THRESHOLD) {
        console.log("scrolling viewport to hide app bar");
        const viewportRect = await this.getViewportRect();
        const midpointX = viewportRect.width / 2;
        const midpointY = viewportRect.height / 2;
        await this.context
          .findElement("id", "android:id/content")
          .then((id) => this.context.$(id))
          .then((el) =>
            el.touchAction([
              { action: "press", x: midpointX, y: midpointY },
              {
                action: "moveTo",
                x: midpointX,
                y: Math.max(midpointY - 50, 0),
              },
              { action: "release" },
              { action: "press", x: midpointX, y: midpointY },
              {
                action: "moveTo",
                x: midpointX,
                y: Math.max(midpointY - 50, 0),
              },
              { action: "release" },
              { action: "press", x: midpointX, y: midpointY },
              {
                action: "moveTo",
                x: midpointX,
                y: Math.max(midpointY - 50, 0),
              },
              { action: "release" },
            ])
          );
      }
      **/

      await this.context.switchContext(AppiumContext.NATIVE_APP);
      await this.context
        .findElement("id", "com.android.chrome:id/infobar_close_button")
        .then((id) => {
          if (id) {
            return this.context.$(id).then((el) => el.click());
          }
        })
        .catch(() => {
          /** noop **/
        });

      await this.context.switchContext(AppiumContext.CHROMIUM);
    }

    const { annotations, size } = await browserInject(
      domain,
      (fn: any, arg: any) => this.context.execute(fn, arg)
    );

    if (annotations.length === 0) {
      console.error("[error] annotations.length === 0, exiting early.");
      console.debug(
        "[debug] ",
        JSON.stringify({ annotations, size, orientation })
      );
      return [];
    }

    await this.context.switchContext(AppiumContext.NATIVE_APP);

    const chromeToolbarHeight = await this.context
      .findElement("id", "com.android.chrome:id/toolbar")
      .then((id) => {
        if (!id) {
          return 0;
        }
        return this.context
          .$(id)
          .then((el) => el.getSize())
          .then((s) => s.height);
      })
      .catch((_err) => 0);

    const statusBarHeight = this.getCapabilities().statBarHeight;
    const devicePixelRatio = this.getCapabilities().pixelRatio;
    const systemBars = (await this.context.getSystemBars()) as any;
    const viewportRect = await this.getViewportRect();

    /**
     * Creating a selection within JS doesn't trigger Chrome's text selection
     * action menu. For realism, we want the normal UI that comes up to
     * appear. Using the highlight coords, tap in the center and wait a
     * short period to have chrome display the UI.
     */

    const {
      boundingBox: { xRelativeMin, xRelativeMax, yRelativeMin, yRelativeMax },
    } = annotations.find(({ label }) => label === "highlight");
    const [xMin, xMax, yMin, yMax] = [
      Math.max(xRelativeMin, 0.0) * size.width * size.scale * devicePixelRatio +
        (orientation === Orientation.LANDSCAPE
          ? systemBars.navigationBar.width
          : 0),
      Math.min(xRelativeMax, 1.0) * size.width * size.scale * devicePixelRatio +
        (orientation === Orientation.LANDSCAPE
          ? systemBars.navigationBar.width
          : 0),
      Math.max(yRelativeMin, 0.0) *
        (size.height * size.scale * devicePixelRatio) +
        statusBarHeight +
        chromeToolbarHeight,
      Math.min(yRelativeMax, 1.0) *
        (size.height * size.scale * devicePixelRatio) +
        statusBarHeight +
        chromeToolbarHeight,
    ];

    const tapX = Math.min(
      xMin + (xMax - xMin) / 2,
      viewportRect.x + viewportRect.width - 1
    );
    const tapY = Math.min(
      yMin + (yMax - yMin) / 2,
      viewportRect.y + viewportRect.height - 1
    );
    await this.context.touchAction({
      action: "tap",
      x: tapX,
      y: tapY,
    });
    await new Promise((resolve) => setTimeout(resolve, 500));

    console.debug(
      "[debug] ",
      JSON.stringify({
        annotations,
        size,
        orientation,
        viewportRect,
        xRelativeMax,
        xRelativeMin,
        yRelativeMin,
        yRelativeMax,
        chromeToolbarHeight,
        statusBarHeight,
        devicePixelRatio,
        tapX,
        tapY,
      })
    );

    /** after clicking, make sure the selection still exists. */
    await this.context.switchContext(AppiumContext.CHROMIUM);
    const selectionExists = await this.context.execute(function() {
      return !window.getSelection().isCollapsed;
    });
    if (!selectionExists) {
      console.error("[error] Selection cleared, exiting early.");
      return [];
    }
    await this.context.switchContext(AppiumContext.NATIVE_APP);

    /**
     * Bounding boxes were calculated relative to DOM viewport, adjust to overall
     * device viewport.
     */
    const reframedBoundingBoxes = annotations.map(
      ({
        boundingBox: { xRelativeMin, xRelativeMax, yRelativeMin, yRelativeMax },
        label,
      }) => {
        const [xMin, xMax, yMin, yMax] = [
          Math.max(xRelativeMin, 0.0) *
            size.width *
            size.scale *
            devicePixelRatio +
            (orientation === Orientation.LANDSCAPE
              ? systemBars.navigationBar.width
              : 0),
          Math.min(xRelativeMax, 1.0) *
            size.width *
            size.scale *
            devicePixelRatio +
            (orientation === Orientation.LANDSCAPE
              ? systemBars.navigationBar.width
              : 0),
          Math.max(yRelativeMin, 0.0) *
            (size.height * size.scale * devicePixelRatio) +
            statusBarHeight +
            chromeToolbarHeight,
          Math.min(yRelativeMax, 1.0) *
            (size.height * size.scale * devicePixelRatio) +
            statusBarHeight +
            chromeToolbarHeight,
        ];

        const [viewportWidth, viewportHeight] =
          orientation === Orientation.PORTRAIT
            ? [
                viewportRect.width,
                viewportRect.height + systemBars.navigationBar.height,
              ]
            : [
                viewportRect.width + systemBars.navigationBar.width,
                viewportRect.height,
              ];

        return {
          label,
          boundingBox: {
            xRelativeMin: Math.max(xMin / viewportWidth, 0),
            xRelativeMax: Math.min(xMax / viewportWidth, 1.0),
            yRelativeMin: Math.max(yMin / viewportHeight, 0),
            yRelativeMax: Math.min(yMax / viewportHeight, 1.0),
          },
        };
      }
    );

    const screenshotData = await this.context.takeScreenshot();

    writeFileSync(outputPath, screenshotData, { encoding: "base64" });

    await this.context.switchContext(AppiumContext.CHROMIUM);

    console.debug(
      JSON.stringify({
        annotations,
        reframedBoundingBoxes,
        size,
        orientation,
        viewportRect,
        chromeToolbarHeight,
        statusBarHeight,
        tapX,
        tapY,
      })
    );
    console.log("getScreenshot complete", outputPath);
    return reframedBoundingBoxes;
  };

  cleanup = async () => {
    if (this.context) {
      return this.context.deleteSession();
    }
  };
}