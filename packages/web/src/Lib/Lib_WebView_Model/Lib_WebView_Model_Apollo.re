let unsafeAsCache = [%raw
  {|
  function asCache(annotation) {

    const stateAsCache = (state) => {
      if (state.__typename === "TimeState") {
        return {
          ...state,
          type_: state.type
        }
      }

      return state
    }

    const selectorAsCache = (selector) => {
      if (selector.__typename === "RangeSelector") {
        return {
          ...selector,
          startSelector: selectorAsCache(selector.startSelector),
          endSelector: selectorAsCache(selector.endSelector),
          type_: selector.type
        }
      } else if (selector.__typename === "XPathSelector") {
        return {
          ...selector,
          refinedBy: selector.refinedBy
            ? selector.refinedBy.map(selectorAsCache)
            : null,
          type_: selector.type
        }
      } else if (selector.__typename === "TextPositionSelector") {
        return {
          ...selector,
          end_: selector.end,
          type_: selector.type
        }
      }

      return selector
    }

    const targetAsCache = (target) => {
      if (target.__typename === "SpecificTarget") {
        return {
          ...target,
          specificTargetId: target.id,
          selector:
            target.selector
              ? target.selector.map(selectorAsCache)
              : null,
          state:
            target.state
              ? target.state.map(stateAsCache)
              : null,
          source: targetAsCache(target.source)
        }
      } else if (target.__typename === "ExternalTarget") {
        return {
          ...target,
          externalTargetId: target.id,
          type_: target.type
        }
      } else if (target.__typename === "TextualTarget") {
        return {
          ...target,
          textualTargetId: target.id
        }
      }
      return target
    }

    return {
      ...annotation,
      target:
        annotation.target
          ? annotation.target.map(targetAsCache)
          : null
    }
  }
|}
];

let writeToCache = (~annotation, ~currentUser) => {
  let cacheAnnotation =
    annotation->Lib_WebView_Model_Annotation.encode->unsafeAsCache;

  annotation.body
  ->Belt.Option.getWithDefault([||])
  ->Belt.Array.keepMap(body =>
      switch (body) {
      | Lib_WebView_Model_Body.TextualBody(textualBody)
          when
            textualBody.purpose
            ->Belt.Option.map(a =>
                a->Belt.Array.some(purpose => purpose == "TAGGING")
              )
            ->Belt.Option.getWithDefault(false) =>
        Some(textualBody.id)
      | _ => None
      }
    )
  ->Belt.Array.forEach(annotationCollectionId => {
      Lib_GraphQL_AnnotationCollection.Apollo.setAnnotationInCollection(
        ~annotation=cacheAnnotation,
        ~currentUser,
        ~annotationCollectionId,
      )
    });
};

let addManyToCache = (~annotations, ~currentUser) => {
  let textualBodyAnnotationTuples =
    annotations
    ->Belt.Array.map(annotation => {
        let cacheAnnotation =
          annotation->Lib_WebView_Model_Annotation.encode->unsafeAsCache;

        annotation.body
        ->Belt.Option.getWithDefault([||])
        ->Belt.Array.keepMap(body =>
            switch (body) {
            | TextualBody(body)
                when
                  body.purpose
                  ->Belt.Option.map(a =>
                      a->Belt.Array.some(purpose => purpose == "TAGGING")
                    )
                  ->Belt.Option.getWithDefault(false) =>
              Some((body, cacheAnnotation))
            | _ => None
            }
          );
      })
    ->Belt.Array.concatMany;

  let textualBodyById =
    textualBodyAnnotationTuples->Belt.Array.reduce(
      Js.Dict.empty(),
      (agg, (textualBody, _)) => {
        if (Js.Dict.get(agg, textualBody.id)->Js.Option.isNone) {
          let _ = Js.Dict.set(agg, textualBody.id, textualBody);
          ();
        };
        agg;
      },
    );

  textualBodyAnnotationTuples
  ->Belt.Array.reduce(
      Js.Dict.empty(),
      (agg, (textualBody, cacheAnnotation)) => {
        let annotations =
          agg
          ->Js.Dict.get(textualBody.id)
          ->Belt.Option.map(a => Belt.Array.concat(a, [|cacheAnnotation|]))
          ->Belt.Option.getWithDefault([|cacheAnnotation|]);
        let _ = Js.Dict.set(agg, textualBody.id, annotations);
        agg;
      },
    )
  ->Js.Dict.entries
  ->Belt.Array.forEach(((annotationCollectionId, annotations)) => {
      let onCreateAnnotationCollection = () => None;
      Lib_GraphQL_AnnotationCollection.Apollo.addAnnotationsToCollection(
        ~annotations,
        ~annotationCollectionId,
        ~currentUser,
        ~onCreateAnnotationCollection,
      );
    });
};

let deleteFromCache = (~annotation, ~currentUser) => {
  annotation.Lib_WebView_Model_Annotation.body
  ->Belt.Option.getWithDefault([||])
  ->Belt.Array.keepMap(body =>
      switch (body) {
      | Lib_WebView_Model_Body.TextualBody(textualBody)
          when
            textualBody.purpose
            ->Belt.Option.map(a =>
                a->Belt.Array.some(purpose => purpose == "TAGGING")
              )
            ->Belt.Option.getWithDefault(false) =>
        Some(textualBody.id)
      | _ => None
      }
    )
  ->Belt.Array.forEach(annotationCollectionId =>
      annotation.id
      ->Belt.Option.forEach(annotationId =>
          Lib_GraphQL_AnnotationCollection.Apollo.removeAnnotationFromCollection(
            ~annotationId,
            ~currentUser,
            ~annotationCollectionId,
          )
        )
    );
};
