// Generated by BUCKLESCRIPT, PLEASE EDIT WITH CARE
'use strict';

var Decco = require("decco/src/Decco.js");
var Js_dict = require("bs-platform/lib/js/js_dict.js");
var Js_json = require("bs-platform/lib/js/js_json.js");
var Js_option = require("bs-platform/lib/js/js_option.js");
var Belt_Array = require("bs-platform/lib/js/belt_Array.js");
var Belt_Option = require("bs-platform/lib/js/belt_Option.js");
var State$LiteralModel = require("./State.js");
var Format$LiteralModel = require("./Format.js");
var Language$LiteralModel = require("./Language.js");
var Selector$LiteralModel = require("./Selector.js");
var ResourceType$LiteralModel = require("./ResourceType.js");
var TextDirection$LiteralModel = require("./TextDirection.js");

function textualTarget_encode(v) {
  return Js_dict.fromArray([
              [
                "id",
                Decco.stringToJson(v.id)
              ],
              [
                "format",
                Decco.optionToJson(Decco.stringToJson, v.format)
              ],
              [
                "language",
                Decco.optionToJson(Decco.stringToJson, v.language)
              ],
              [
                "processingLanguage",
                Decco.optionToJson(Decco.stringToJson, v.processingLanguage)
              ],
              [
                "textDirection",
                Decco.optionToJson(Decco.stringToJson, v.textDirection)
              ],
              [
                "accessibility",
                Decco.optionToJson((function (param) {
                        return Decco.arrayToJson(Decco.stringToJson, param);
                      }), v.accessibility)
              ],
              [
                "rights",
                Decco.optionToJson((function (param) {
                        return Decco.arrayToJson(Decco.stringToJson, param);
                      }), v.rights)
              ],
              [
                "value",
                Decco.stringToJson(v.value)
              ],
              [
                "__typename",
                Decco.stringToJson(v.typename)
              ]
            ]);
}

function textualTarget_decode(v) {
  var dict = Js_json.classify(v);
  if (typeof dict === "number") {
    return Decco.error(undefined, "Not an object", v);
  }
  if (dict.TAG !== /* JSONObject */2) {
    return Decco.error(undefined, "Not an object", v);
  }
  var dict$1 = dict._0;
  var id = Decco.stringFromJson(Belt_Option.getWithDefault(Js_dict.get(dict$1, "id"), null));
  if (id.TAG) {
    var e = id._0;
    return {
            TAG: /* Error */1,
            _0: {
              path: ".id" + e.path,
              message: e.message,
              value: e.value
            }
          };
  }
  var format = Decco.optionFromJson(Decco.stringFromJson, Belt_Option.getWithDefault(Js_dict.get(dict$1, "format"), null));
  if (format.TAG) {
    var e$1 = format._0;
    return {
            TAG: /* Error */1,
            _0: {
              path: ".format" + e$1.path,
              message: e$1.message,
              value: e$1.value
            }
          };
  }
  var language = Decco.optionFromJson(Decco.stringFromJson, Belt_Option.getWithDefault(Js_dict.get(dict$1, "language"), null));
  if (language.TAG) {
    var e$2 = language._0;
    return {
            TAG: /* Error */1,
            _0: {
              path: ".language" + e$2.path,
              message: e$2.message,
              value: e$2.value
            }
          };
  }
  var processingLanguage = Decco.optionFromJson(Decco.stringFromJson, Belt_Option.getWithDefault(Js_dict.get(dict$1, "processingLanguage"), null));
  if (processingLanguage.TAG) {
    var e$3 = processingLanguage._0;
    return {
            TAG: /* Error */1,
            _0: {
              path: ".processingLanguage" + e$3.path,
              message: e$3.message,
              value: e$3.value
            }
          };
  }
  var textDirection = Decco.optionFromJson(Decco.stringFromJson, Belt_Option.getWithDefault(Js_dict.get(dict$1, "textDirection"), null));
  if (textDirection.TAG) {
    var e$4 = textDirection._0;
    return {
            TAG: /* Error */1,
            _0: {
              path: ".textDirection" + e$4.path,
              message: e$4.message,
              value: e$4.value
            }
          };
  }
  var accessibility = Decco.optionFromJson((function (param) {
          return Decco.arrayFromJson(Decco.stringFromJson, param);
        }), Belt_Option.getWithDefault(Js_dict.get(dict$1, "accessibility"), null));
  if (accessibility.TAG) {
    var e$5 = accessibility._0;
    return {
            TAG: /* Error */1,
            _0: {
              path: ".accessibility" + e$5.path,
              message: e$5.message,
              value: e$5.value
            }
          };
  }
  var rights = Decco.optionFromJson((function (param) {
          return Decco.arrayFromJson(Decco.stringFromJson, param);
        }), Belt_Option.getWithDefault(Js_dict.get(dict$1, "rights"), null));
  if (rights.TAG) {
    var e$6 = rights._0;
    return {
            TAG: /* Error */1,
            _0: {
              path: ".rights" + e$6.path,
              message: e$6.message,
              value: e$6.value
            }
          };
  }
  var value = Decco.stringFromJson(Belt_Option.getWithDefault(Js_dict.get(dict$1, "value"), null));
  if (value.TAG) {
    var e$7 = value._0;
    return {
            TAG: /* Error */1,
            _0: {
              path: ".value" + e$7.path,
              message: e$7.message,
              value: e$7.value
            }
          };
  }
  var typename = Belt_Option.getWithDefault(Belt_Option.map(Js_dict.get(dict$1, "__typename"), Decco.stringFromJson), {
        TAG: /* Ok */0,
        _0: "TextualTarget"
      });
  if (!typename.TAG) {
    return {
            TAG: /* Ok */0,
            _0: {
              id: id._0,
              format: format._0,
              language: language._0,
              processingLanguage: processingLanguage._0,
              textDirection: textDirection._0,
              accessibility: accessibility._0,
              rights: rights._0,
              value: value._0,
              typename: typename._0
            }
          };
  }
  var e$8 = typename._0;
  return {
          TAG: /* Error */1,
          _0: {
            path: ".__typename" + e$8.path,
            message: e$8.message,
            value: e$8.value
          }
        };
}

function externalTarget_encode(v) {
  return Js_dict.fromArray([
              [
                "id",
                Decco.stringToJson(v.id)
              ],
              [
                "hashId",
                Decco.optionToJson(Decco.stringToJson, v.hashId)
              ],
              [
                "language",
                Decco.optionToJson(Decco.stringToJson, v.language)
              ],
              [
                "processingLanguage",
                Decco.optionToJson(Decco.stringToJson, v.processingLanguage)
              ],
              [
                "textDirection",
                Decco.optionToJson(Decco.stringToJson, v.textDirection)
              ],
              [
                "format",
                Decco.optionToJson(Decco.stringToJson, v.format)
              ],
              [
                "accessibility",
                Decco.optionToJson((function (param) {
                        return Decco.arrayToJson(Decco.stringToJson, param);
                      }), v.accessibility)
              ],
              [
                "rights",
                Decco.optionToJson((function (param) {
                        return Decco.arrayToJson(Decco.stringToJson, param);
                      }), v.rights)
              ],
              [
                "type",
                Decco.optionToJson(Decco.stringToJson, v.type_)
              ],
              [
                "__typename",
                Decco.stringToJson(v.typename)
              ]
            ]);
}

function externalTarget_decode(v) {
  var dict = Js_json.classify(v);
  if (typeof dict === "number") {
    return Decco.error(undefined, "Not an object", v);
  }
  if (dict.TAG !== /* JSONObject */2) {
    return Decco.error(undefined, "Not an object", v);
  }
  var dict$1 = dict._0;
  var id = Decco.stringFromJson(Belt_Option.getWithDefault(Js_dict.get(dict$1, "id"), null));
  if (id.TAG) {
    var e = id._0;
    return {
            TAG: /* Error */1,
            _0: {
              path: ".id" + e.path,
              message: e.message,
              value: e.value
            }
          };
  }
  var hashId = Decco.optionFromJson(Decco.stringFromJson, Belt_Option.getWithDefault(Js_dict.get(dict$1, "hashId"), null));
  if (hashId.TAG) {
    var e$1 = hashId._0;
    return {
            TAG: /* Error */1,
            _0: {
              path: ".hashId" + e$1.path,
              message: e$1.message,
              value: e$1.value
            }
          };
  }
  var language = Decco.optionFromJson(Decco.stringFromJson, Belt_Option.getWithDefault(Js_dict.get(dict$1, "language"), null));
  if (language.TAG) {
    var e$2 = language._0;
    return {
            TAG: /* Error */1,
            _0: {
              path: ".language" + e$2.path,
              message: e$2.message,
              value: e$2.value
            }
          };
  }
  var processingLanguage = Decco.optionFromJson(Decco.stringFromJson, Belt_Option.getWithDefault(Js_dict.get(dict$1, "processingLanguage"), null));
  if (processingLanguage.TAG) {
    var e$3 = processingLanguage._0;
    return {
            TAG: /* Error */1,
            _0: {
              path: ".processingLanguage" + e$3.path,
              message: e$3.message,
              value: e$3.value
            }
          };
  }
  var textDirection = Decco.optionFromJson(Decco.stringFromJson, Belt_Option.getWithDefault(Js_dict.get(dict$1, "textDirection"), null));
  if (textDirection.TAG) {
    var e$4 = textDirection._0;
    return {
            TAG: /* Error */1,
            _0: {
              path: ".textDirection" + e$4.path,
              message: e$4.message,
              value: e$4.value
            }
          };
  }
  var format = Decco.optionFromJson(Decco.stringFromJson, Belt_Option.getWithDefault(Js_dict.get(dict$1, "format"), null));
  if (format.TAG) {
    var e$5 = format._0;
    return {
            TAG: /* Error */1,
            _0: {
              path: ".format" + e$5.path,
              message: e$5.message,
              value: e$5.value
            }
          };
  }
  var accessibility = Decco.optionFromJson((function (param) {
          return Decco.arrayFromJson(Decco.stringFromJson, param);
        }), Belt_Option.getWithDefault(Js_dict.get(dict$1, "accessibility"), null));
  if (accessibility.TAG) {
    var e$6 = accessibility._0;
    return {
            TAG: /* Error */1,
            _0: {
              path: ".accessibility" + e$6.path,
              message: e$6.message,
              value: e$6.value
            }
          };
  }
  var rights = Decco.optionFromJson((function (param) {
          return Decco.arrayFromJson(Decco.stringFromJson, param);
        }), Belt_Option.getWithDefault(Js_dict.get(dict$1, "rights"), null));
  if (rights.TAG) {
    var e$7 = rights._0;
    return {
            TAG: /* Error */1,
            _0: {
              path: ".rights" + e$7.path,
              message: e$7.message,
              value: e$7.value
            }
          };
  }
  var type_ = Decco.optionFromJson(Decco.stringFromJson, Belt_Option.getWithDefault(Js_dict.get(dict$1, "type"), null));
  if (type_.TAG) {
    var e$8 = type_._0;
    return {
            TAG: /* Error */1,
            _0: {
              path: ".type" + e$8.path,
              message: e$8.message,
              value: e$8.value
            }
          };
  }
  var typename = Belt_Option.getWithDefault(Belt_Option.map(Js_dict.get(dict$1, "__typename"), Decco.stringFromJson), {
        TAG: /* Ok */0,
        _0: "ExternalTarget"
      });
  if (!typename.TAG) {
    return {
            TAG: /* Ok */0,
            _0: {
              id: id._0,
              hashId: hashId._0,
              language: language._0,
              processingLanguage: processingLanguage._0,
              textDirection: textDirection._0,
              format: format._0,
              accessibility: accessibility._0,
              rights: rights._0,
              type_: type_._0,
              typename: typename._0
            }
          };
  }
  var e$9 = typename._0;
  return {
          TAG: /* Error */1,
          _0: {
            path: ".__typename" + e$9.path,
            message: e$9.message,
            value: e$9.value
          }
        };
}

function specificTarget_encode(v) {
  var partial_arg = Selector$LiteralModel.codec[0];
  return Js_dict.fromArray([
              [
                "id",
                Decco.stringToJson(v.id)
              ],
              [
                "source",
                t_encode(v.source)
              ],
              [
                "selector",
                Decco.optionToJson((function (param) {
                        return Decco.arrayToJson(partial_arg, param);
                      }), v.selector)
              ],
              [
                "state",
                Decco.optionToJson((function (param) {
                        return Decco.arrayToJson(State$LiteralModel.t_encode, param);
                      }), v.state)
              ],
              [
                "__typename",
                Decco.stringToJson(v.typename)
              ]
            ]);
}

function specificTarget_decode(v) {
  var dict = Js_json.classify(v);
  if (typeof dict === "number") {
    return Decco.error(undefined, "Not an object", v);
  }
  if (dict.TAG !== /* JSONObject */2) {
    return Decco.error(undefined, "Not an object", v);
  }
  var dict$1 = dict._0;
  var id = Decco.stringFromJson(Belt_Option.getWithDefault(Js_dict.get(dict$1, "id"), null));
  if (id.TAG) {
    var e = id._0;
    return {
            TAG: /* Error */1,
            _0: {
              path: ".id" + e.path,
              message: e.message,
              value: e.value
            }
          };
  }
  var source = t_decode(Belt_Option.getWithDefault(Js_dict.get(dict$1, "source"), null));
  if (source.TAG) {
    var e$1 = source._0;
    return {
            TAG: /* Error */1,
            _0: {
              path: ".source" + e$1.path,
              message: e$1.message,
              value: e$1.value
            }
          };
  }
  var partial_arg = Selector$LiteralModel.codec[1];
  var selector = Decco.optionFromJson((function (param) {
          return Decco.arrayFromJson(partial_arg, param);
        }), Belt_Option.getWithDefault(Js_dict.get(dict$1, "selector"), null));
  if (selector.TAG) {
    var e$2 = selector._0;
    return {
            TAG: /* Error */1,
            _0: {
              path: ".selector" + e$2.path,
              message: e$2.message,
              value: e$2.value
            }
          };
  }
  var state = Decco.optionFromJson((function (param) {
          return Decco.arrayFromJson(State$LiteralModel.t_decode, param);
        }), Belt_Option.getWithDefault(Js_dict.get(dict$1, "state"), null));
  if (state.TAG) {
    var e$3 = state._0;
    return {
            TAG: /* Error */1,
            _0: {
              path: ".state" + e$3.path,
              message: e$3.message,
              value: e$3.value
            }
          };
  }
  var typename = Belt_Option.getWithDefault(Belt_Option.map(Js_dict.get(dict$1, "__typename"), Decco.stringFromJson), {
        TAG: /* Ok */0,
        _0: "SpecificTarget"
      });
  if (!typename.TAG) {
    return {
            TAG: /* Ok */0,
            _0: {
              id: id._0,
              source: source._0,
              selector: selector._0,
              state: state._0,
              typename: typename._0
            }
          };
  }
  var e$4 = typename._0;
  return {
          TAG: /* Error */1,
          _0: {
            path: ".__typename" + e$4.path,
            message: e$4.message,
            value: e$4.value
          }
        };
}

function t_encode(v) {
  switch (v.TAG | 0) {
    case /* TextualTarget */0 :
        return [
                "TextualTarget",
                textualTarget_encode(v._0)
              ];
    case /* SpecificTarget */1 :
        return [
                "SpecificTarget",
                specificTarget_encode(v._0)
              ];
    case /* ExternalTarget */2 :
        return [
                "ExternalTarget",
                externalTarget_encode(v._0)
              ];
    case /* NotImplemented_Passthrough */3 :
        return [
                "NotImplemented_Passthrough",
                v._0
              ];
    
  }
}

function t_decode(v) {
  var jsonArr = Js_json.classify(v);
  if (typeof jsonArr === "number") {
    return Decco.error(undefined, "Not a variant", v);
  }
  if (jsonArr.TAG !== /* JSONArray */3) {
    return Decco.error(undefined, "Not a variant", v);
  }
  var jsonArr$1 = jsonArr._0;
  if (jsonArr$1.length === 0) {
    return Decco.error(undefined, "Expected variant, found empty array", v);
  }
  var tagged = jsonArr$1.map(Js_json.classify);
  var match = Belt_Array.getExn(tagged, 0);
  if (typeof match !== "number" && !match.TAG) {
    switch (match._0) {
      case "ExternalTarget" :
          if (tagged.length !== 2) {
            return Decco.error(undefined, "Invalid number of arguments to variant constructor", v);
          }
          var v0 = externalTarget_decode(Belt_Array.getExn(jsonArr$1, 1));
          if (!v0.TAG) {
            return {
                    TAG: /* Ok */0,
                    _0: {
                      TAG: /* ExternalTarget */2,
                      _0: v0._0
                    }
                  };
          }
          var e = v0._0;
          return {
                  TAG: /* Error */1,
                  _0: {
                    path: "[0]" + e.path,
                    message: e.message,
                    value: e.value
                  }
                };
      case "NotImplemented_Passthrough" :
          if (tagged.length !== 2) {
            return Decco.error(undefined, "Invalid number of arguments to variant constructor", v);
          }
          var v0$1 = {
            TAG: /* Ok */0,
            _0: Belt_Array.getExn(jsonArr$1, 1)
          };
          if (!v0$1.TAG) {
            return {
                    TAG: /* Ok */0,
                    _0: {
                      TAG: /* NotImplemented_Passthrough */3,
                      _0: v0$1._0
                    }
                  };
          }
          var e$1 = v0$1._0;
          return {
                  TAG: /* Error */1,
                  _0: {
                    path: "[0]" + e$1.path,
                    message: e$1.message,
                    value: e$1.value
                  }
                };
      case "SpecificTarget" :
          if (tagged.length !== 2) {
            return Decco.error(undefined, "Invalid number of arguments to variant constructor", v);
          }
          var v0$2 = specificTarget_decode(Belt_Array.getExn(jsonArr$1, 1));
          if (!v0$2.TAG) {
            return {
                    TAG: /* Ok */0,
                    _0: {
                      TAG: /* SpecificTarget */1,
                      _0: v0$2._0
                    }
                  };
          }
          var e$2 = v0$2._0;
          return {
                  TAG: /* Error */1,
                  _0: {
                    path: "[0]" + e$2.path,
                    message: e$2.message,
                    value: e$2.value
                  }
                };
      case "TextualTarget" :
          if (tagged.length !== 2) {
            return Decco.error(undefined, "Invalid number of arguments to variant constructor", v);
          }
          var v0$3 = textualTarget_decode(Belt_Array.getExn(jsonArr$1, 1));
          if (!v0$3.TAG) {
            return {
                    TAG: /* Ok */0,
                    _0: {
                      TAG: /* TextualTarget */0,
                      _0: v0$3._0
                    }
                  };
          }
          var e$3 = v0$3._0;
          return {
                  TAG: /* Error */1,
                  _0: {
                    path: "[0]" + e$3.path,
                    message: e$3.message,
                    value: e$3.value
                  }
                };
      default:
        
    }
  }
  return Decco.error(undefined, "Invalid variant constructor", Belt_Array.getExn(jsonArr$1, 0));
}

function t_decode$1(json) {
  var match = Js_json.classify(json);
  if (typeof match === "number") {
    return {
            TAG: /* Error */1,
            _0: {
              path: "",
              message: "Expected JSONObject for target",
              value: json
            }
          };
  }
  if (match.TAG !== /* JSONObject */2) {
    return {
            TAG: /* Error */1,
            _0: {
              path: "",
              message: "Expected JSONObject for target",
              value: json
            }
          };
  }
  var textualTarget = textualTarget_decode(json);
  if (!textualTarget.TAG) {
    return {
            TAG: /* Ok */0,
            _0: {
              TAG: /* TextualTarget */0,
              _0: textualTarget._0
            }
          };
  }
  var specificTarget = specificTarget_decode(json);
  if (!specificTarget.TAG) {
    return {
            TAG: /* Ok */0,
            _0: {
              TAG: /* SpecificTarget */1,
              _0: specificTarget._0
            }
          };
  }
  var externalTarget = externalTarget_decode(json);
  if (externalTarget.TAG) {
    return {
            TAG: /* Ok */0,
            _0: {
              TAG: /* NotImplemented_Passthrough */3,
              _0: json
            }
          };
  } else {
    return {
            TAG: /* Ok */0,
            _0: {
              TAG: /* ExternalTarget */2,
              _0: externalTarget._0
            }
          };
  }
}

function makeTextualTarget(id, value, format, language, processingLanguage, textDirection, accessibility, rights, param) {
  return {
          id: id,
          format: format,
          language: language,
          processingLanguage: processingLanguage,
          textDirection: textDirection,
          accessibility: accessibility,
          rights: rights,
          value: value,
          typename: "TextualTarget"
        };
}

function makeTextualTargetFromGraphQL(textualTarget) {
  return {
          TAG: /* TextualTarget */0,
          _0: makeTextualTarget(textualTarget.textualTargetId, textualTarget.value, Belt_Option.map(textualTarget.format, Format$LiteralModel.toString), Belt_Option.map(textualTarget.language, Language$LiteralModel.toString), Belt_Option.map(textualTarget.processingLanguage, Language$LiteralModel.toString), Belt_Option.map(textualTarget.textDirection, TextDirection$LiteralModel.toString), textualTarget.accessibility, textualTarget.rights, undefined)
        };
}

function makeExternalTarget(id, language, processingLanguage, textDirection, format, accessibility, rights, type_, hashId, param) {
  return {
          id: id,
          hashId: hashId,
          language: language,
          processingLanguage: processingLanguage,
          textDirection: textDirection,
          format: format,
          accessibility: accessibility,
          rights: rights,
          type_: type_,
          typename: "ExternalTarget"
        };
}

function makeExternalTargetFromGraphQL(externalTarget) {
  return {
          TAG: /* ExternalTarget */2,
          _0: makeExternalTarget(externalTarget.externalTargetId, Belt_Option.map(externalTarget.language, Language$LiteralModel.toString), Belt_Option.map(externalTarget.processingLanguage, Language$LiteralModel.toString), Belt_Option.map(externalTarget.textDirection, TextDirection$LiteralModel.toString), Belt_Option.map(externalTarget.format, Format$LiteralModel.toString), externalTarget.accessibility, externalTarget.rights, Belt_Option.map(externalTarget.type_, ResourceType$LiteralModel.toJs), undefined, undefined)
        };
}

function makeSpecificTarget(id, source, selector, state, param) {
  return {
          id: id,
          source: source,
          selector: selector,
          state: state,
          typename: "SpecificTarget"
        };
}

function t_encode$1(inst) {
  switch (inst.TAG | 0) {
    case /* TextualTarget */0 :
        return textualTarget_encode(inst._0);
    case /* SpecificTarget */1 :
        return specificTarget_encode(inst._0);
    case /* ExternalTarget */2 :
        return externalTarget_encode(inst._0);
    case /* NotImplemented_Passthrough */3 :
        return inst._0;
    
  }
}

function makeSpecificTargetFromGraphQL(makeSelector, makeState, specificTarget) {
  var selector = Js_option.some(Belt_Array.keepMap(specificTarget.selector, makeSelector));
  var state = Belt_Option.map(specificTarget.state, (function (a) {
          return Belt_Array.keepMap(a, makeState);
        }));
  var match = specificTarget.source;
  var externalTarget = typeof match === "string" ? undefined : makeExternalTargetFromGraphQL(match.VAL);
  if (externalTarget !== undefined) {
    return {
            TAG: /* SpecificTarget */1,
            _0: makeSpecificTarget(specificTarget.specificTargetId, externalTarget, selector, state, undefined)
          };
  }
  
}

t_encode = t_encode$1; t_decode = t_decode$1
;

var codec = [
  t_encode$1,
  t_decode$1
];

var encode = t_encode$1;

var decode = t_decode$1;

exports.textualTarget_encode = textualTarget_encode;
exports.textualTarget_decode = textualTarget_decode;
exports.externalTarget_encode = externalTarget_encode;
exports.externalTarget_decode = externalTarget_decode;
exports.specificTarget_encode = specificTarget_encode;
exports.specificTarget_decode = specificTarget_decode;
exports.t_decode = t_decode$1;
exports.makeTextualTarget = makeTextualTarget;
exports.makeTextualTargetFromGraphQL = makeTextualTargetFromGraphQL;
exports.makeExternalTarget = makeExternalTarget;
exports.makeExternalTargetFromGraphQL = makeExternalTargetFromGraphQL;
exports.makeSpecificTarget = makeSpecificTarget;
exports.t_encode = t_encode$1;
exports.makeSpecificTargetFromGraphQL = makeSpecificTargetFromGraphQL;
exports.codec = codec;
exports.encode = encode;
exports.decode = decode;
/*  Not a pure module */
