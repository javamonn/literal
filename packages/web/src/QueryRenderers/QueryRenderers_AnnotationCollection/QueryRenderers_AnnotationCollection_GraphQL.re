module GetAnnotationCollection = {
  module Query = [%graphql
    {|
      query getAnnotationCollection($creatorUsername: String!, $id: String!) {
        getAnnotationCollection(creatorUsername: $creatorUsername, id: $id) {
          ...Containers_AnnotationCollectionHeader_GraphQL.GetAnnotationCollectionFragment.AnnotationCollectionHeader_AnnotationCollection @bsField(name: "annotationCollectionHeader")
          first {
            items(limit: 100) {
              items {
                annotation {
                  created
                  id
                  ...Containers_AnnotationEditor_Annotation_GraphQL.GetAnnotationFragment.EditorNotesAnnotationFragment @bsField(name: "editorAnnotationFragment")
                  ...Containers_AnnotationCollectionHeader_GraphQL.GetAnnotationFragment.AnnotationCollectionHeader_Annotation @bsField(name: "annotationCollectionHeader")
                }
              }
            }
          }
        }
      }
    |}
  ];

  /**
   * Apollo cache representation of the above query. Note that this will need to be manually
   * updated as the query changes.
   */
  type cache = {
    .
    "__typename": string,
    "getAnnotationCollection":
      option({
        .
        "__typename": string,
        "label": string,
        "first":
          option({
            .
            "__typename": string,
            "items":
              option({
                .
                "__typename": string,
                "items":
                  option(
                    array({
                      .
                      "__typename": string,
                      "annotation": {
                        .
                        "__typename": string,
                        "id": string,
                        "body":
                          option(array({
                            .
                            "__typename": string,
                            "created": option(Js.Json.t),
                            "id": option(string),
                            "value": string,
                            "purpose": option(array(string)),
                            "format": option(string),
                            "language": option(string),
                            "processingLanguage": option(string),
                            "accessibility": string,
                            "rights": option(array(string)),
                          })),
                        "target":
                          option(array({
                            .
                            "_typename": string,
                            "value": string,
                            "textualTargetId": option(string),
                            "externalTargetId": option(string),
                            "purpose": option(array(string)),
                            "format": option(string),
                            "language": option(string),
                            "processingLanguage": option(string),
                            "accessibility": string,
                            "rights": option(array(string)),
                          }))
                      },
                    }),
                  ),
              }),
          }),
      }),
  };

  external unsafeToCache: Js.Json.t => cache = "%identity";

  module CacheReadQuery = ApolloClient.ReadQuery(Query);
  module CacheWriteQuery = ApolloClient.WriteQuery(Query);

  // setter to update items
  let setCacheItems = (cacheQuery, newItems): cache => {
    let newItemsQuery = {
      "getAnnotationCollection":
        Some({"first": Some({"items": Some({"items": Some(newItems)})})}),
    };
    Ramda.mergeDeepLeft(newItemsQuery, cacheQuery);
  };

  /**
   * FIXME: Return type is not actually Query.t, as Apollo cache
   * does not persist fragment alias fields -- everything is
   * inlined on the root type.
   */
  let readCache = (~query, ~client, ()) => {
    let readQueryOptions =
      CacheReadQuery.{
        query: ApolloClient.gql(. query##query),
        variables: Js.Nullable.fromOption(Some(query##variables)),
      };
    switch (CacheReadQuery.readQuery(client, readQueryOptions)) {
    | exception _ => None
    | cachedResponse =>
      cachedResponse->Js.Nullable.toOption->Belt.Option.map(unsafeToCache)
    };
  };

  /**
   * Data in cache format, i.e. everything inlined on GraphQL types. Not a good
   * way to enforce type safety.
   */
  external unsafeFromCache: cache => Query.t = "%identity";
  let writeCache = (~query, ~client, ~data: cache, ()) =>
    CacheWriteQuery.make(
      ~client,
      ~variables=query##variables,
      ~data=unsafeFromCache(data),
      (),
    );
};
