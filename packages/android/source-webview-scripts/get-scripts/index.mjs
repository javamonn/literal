import "core-js/index.js";
import "regenerator-runtime/runtime.js";

export default () => {
  return Array.from(document.scripts).map((scriptElem) => ({
    src: scriptElem.src,
    text: scriptElem.text,
    type: scriptElem.type
  }));
};
