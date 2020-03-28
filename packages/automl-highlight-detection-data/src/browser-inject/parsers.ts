import { DOMAIN, InjectScope, ParserInterface } from "./types";

export const parsers: { [domain: string]: ParserInterface } = {
  [DOMAIN.WIKIPEDIA]: {
    getUrl: () => "https://en.wikipedia.org/wiki/Special:Random",
    parse: (scope: InjectScope): Text[] => {
      // open all closed sections
      document
        .querySelectorAll(
          "#bodyContent .collapsible-heading,.collapsible-block"
        )
        .forEach((elem) => {
          elem.classList.add("open-block");
        });

      // remove interactive elements
      ($("*") as any).off();

      // remove non-text elements
      document.querySelectorAll("table, h2, img, sup, ol").forEach((elem) => {
        elem.remove();
      });

      // disable all links: replace with spans
      document.querySelectorAll("a, img").forEach((el) => {
        const span = document.createElement("span");
        span.style.color = "#0645ad";
        span.innerHTML = el.innerHTML;
        el.parentNode.replaceChild(span, el);
      });

      const textNodes = scope.getTextNodes(
        document.querySelector("#bodyContent")
      );

      return textNodes;
    },
    getBoundaryAncestorSelector: () => "section",
  },
  [DOMAIN.HACKERNEWS]: {
    // hn uses incremental ids, max id taken on 03/27/20
    getUrl: () =>
      `https://news.ycombinator.com/item?id=${Math.floor(
        Math.random() * 22702482
      )}`,
    parse: (scope: InjectScope): Text[] => {
      document.querySelectorAll("a").forEach((el) => {
        el.removeAttribute("href");
      });

      const textNodes = Array.from(document.querySelectorAll(".comment"))
        .map(scope.getTextNodes)
        //@ts-ignore: this should work fine
        .flat();

      return textNodes;
    },
    getBoundaryAncestorSelector: () => ".comment",
  },
};
