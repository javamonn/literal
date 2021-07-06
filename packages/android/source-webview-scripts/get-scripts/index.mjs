import "core-js/index.js";
import "regenerator-runtime/runtime.js";

export default () => {
  return Array.from(document.scripts).map((scriptElem) => ({
    attributes: Array.from(scriptElem.attributes).reduce(
      (agg, attr) => ({ ...agg, [attr.name]: attr.value }),
      {}
    ),
    text: scriptElem.text,
  }));
};
