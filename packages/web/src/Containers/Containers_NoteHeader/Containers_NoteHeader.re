open Styles;

external castToListHighlights:
  Js.Json.t => QueryRenderers_Notes_GraphQL.ListHighlights.Query.t =
  "%identity";

[@react.component]
let make = (~highlightFragment as highlight, ~currentUser) => {
  let (deleteHighlightMutation, _s, _f) =
    ApolloHooks.useMutation(
      Containers_NoteHeader_GraphQL.DeleteHighlightMutation.definition,
    );

  let handleCreate = () => {
    let _ = Next.Router.push("/notes/new");
    ();
  };

  let handleDelete = () => {
    let variables =
      Containers_NoteHeader_GraphQL.DeleteHighlightMutation.makeVariables(
        ~input={"id": highlight##id},
        (),
      );

    let _ =
      deleteHighlightMutation(~variables, ())
      |> Js.Promise.then_(_ => {
           let cacheQuery =
             QueryRenderers_Notes_GraphQL.ListHighlights.Query.make(
               ~owner=currentUser->AwsAmplify.Auth.CurrentUserInfo.username,
               (),
             );
           let _ =
             switch (
               QueryRenderers_Notes_GraphQL.ListHighlights.readCache(
                 ~query=cacheQuery,
                 ~client=Provider.client,
                 (),
               )
             ) {
             | None => ()
             | Some(cachedQuery) =>
               let updatedListHighlights =
                 QueryRenderers_Notes_GraphQL.ListHighlights.Raw.(
                   cachedQuery
                   ->listHighlights
                   ->Belt.Option.flatMap(items)
                   ->Belt.Option.map(items => {
                       let updatedItems =
                         items
                         ->Belt.Array.keep(
                             fun
                             | Some(h) => h.id !== highlight##id
                             | None => false,
                           )
                         ->Js.Option.some;
                       {
                         ...cachedQuery,
                         listHighlights:
                           Some({
                             ...
                               cachedQuery->listHighlights->Belt.Option.getExn,
                             items: updatedItems,
                           }),
                       };
                     })
                 );
               let _ =
                 switch (updatedListHighlights) {
                 | Some(updatedListHighlights) =>
                   QueryRenderers_Notes_GraphQL.ListHighlights.writeCache(
                     ~client=Provider.client,
                     ~data=updatedListHighlights,
                     ~query=cacheQuery,
                     (),
                   )
                 | None => ()
                 };
               ();
             };
           Js.Promise.resolve();
         });
    ();
  };

  <Header
    className={cn([
      "absolute",
      "left-0",
      "right-0",
      "top-0",
      "bg-black",
      "z-10",
    ])}>
    <div
      style={style(~borderColor="rgba(255, 255, 255, 0.5)", ())}
      className={cn([
        "justify-between",
        "items-center",
        "border-b",
        "py-2",
        "mx-6",
        "flex",
        "flex-1",
      ])}>
      <h1
        className={cn([
          "text-white",
          "font-sans",
          "font-semibold",
          "italic",
          "leading-none",
          "text-xl",
        ])}>
        {React.string("#recent")}
      </h1>
      <div className={cn(["flex", "flex-row"])}>
        <MaterialUi.IconButton
          size=`Small
          edge=`End
          onClick={_ => handleDelete()}
          _TouchRippleProps={
            "classes": {
              "child": cn(["bg-white"]),
              "rippleVisible": cn(["opacity-50"]),
            },
          }
          classes=[Root(cn(["p-0", "ml-1"]))]>
          <Svg
            placeholderViewBox="0 0 24 24"
            className={cn(["pointer-events-none", "opacity-75"])}
            style={ReactDOMRe.Style.make(
              ~width="1.75rem",
              ~height="1.75rem",
              (),
            )}
            icon=Svg.delete
          />
        </MaterialUi.IconButton>
        <MaterialUi.IconButton
          size=`Small
          edge=`End
          onClick={_ => handleCreate()}
          _TouchRippleProps={
            "classes": {
              "child": cn(["bg-white"]),
              "rippleVisible": cn(["opacity-50"]),
            },
          }
          classes=[Root(cn(["p-0", "ml-4"]))]>
          <Svg
            placeholderViewBox="0 0 24 24"
            className={cn(["pointer-events-none", "opacity-75"])}
            style={ReactDOMRe.Style.make(
              ~width="1.75rem",
              ~height="1.75rem",
              (),
            )}
            icon=Svg.add
          />
        </MaterialUi.IconButton>
      </div>
    </div>
  </Header>;
};
