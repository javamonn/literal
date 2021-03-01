open Containers_NewAnnotationEditor_GraphQL;

[@react.component]
let make = (~currentUser) => {
  let scrollContainerRef = React.useRef(Js.Nullable.null);
  let textInputRef = React.useRef(Js.Nullable.null);
  let (textValue, setTextValue) = React.useState(() => "");
  let (tagsValue, setTagsValue) =
    React.useState(() =>
      [|
        Containers_AnnotationEditor_Tag.{
          text: Lib_GraphQL.AnnotationCollection.recentAnnotationCollectionLabel,
          id:
            Some(
              Lib_GraphQL.AnnotationCollection.(
                makeIdFromComponent(
                  ~creatorUsername=
                    currentUser->AwsAmplify.Auth.CurrentUserInfo.username,
                  ~annotationCollectionIdComponent=recentAnnotationCollectionIdComponent,
                  (),
                )
              ),
            ),
          href: None,
        },
      |]
    );
  let previousTagsValue = React.useRef(tagsValue);
  let (pendingTagValue, setPendingTagValue) = React.useState(_ => "");

  let (createAnnotationMutation, _s, _f) =
    ApolloHooks.useMutation(CreateAnnotationMutation.definition);

  let handleSave = () => {
    let idPromise =
      Lib_GraphQL.Annotation.makeId(
        ~creatorUsername=
          AwsAmplify.Auth.CurrentUserInfo.(currentUser->username),
        ~textualTargetValue=textValue,
      );

    let bodyPromise =
      tagsValue
      ->Belt.Array.map(tag => {
          let id =
            switch (tag.id) {
            | Some(id) => Js.Promise.resolve(id)
            | None =>
              Lib_GraphQL.AnnotationCollection.makeId(
                ~creatorUsername=
                  AwsAmplify.Auth.CurrentUserInfo.(currentUser->username),
                ~label=tag.text,
              )
            };
          id
          |> Js.Promise.then_(id =>
               Lib_GraphQL_AnnotationBodyInput.(
                 makeBody(
                   ~textualBody=
                     makeTextualBody(
                       ~id,
                       ~value=tag.text,
                       ~purpose=[|`TAGGING|],
                       ~format=`TEXT_PLAIN,
                       ~textDirection=`LTR,
                       ~language=`EN_US,
                       ~processingLanguage=`EN_US,
                       (),
                     ),
                   (),
                 )
               )
               ->Js.Promise.resolve
             );
        })
      ->Js.Promise.all;

    let _ =
      Js.Promise.all2((idPromise, bodyPromise))
      |> Js.Promise.then_(((id, body)) => {
           let input =
             Lib_GraphQL_CreateAnnotationMutation.Input.make(
               ~context=[|Lib_GraphQL.Annotation.defaultContext|],
               ~id,
               ~motivation=[|`HIGHLIGHTING|],
               ~creatorUsername=
                 AwsAmplify.Auth.CurrentUserInfo.(currentUser->username),
               ~target=[|
                 Lib_GraphQL_AnnotationTargetInput.(
                   make(
                     ~textualTarget=
                       makeTextualTarget(
                         ~format=`TEXT_PLAIN,
                         ~language=`EN_US,
                         ~processingLanguage=`EN_US,
                         ~textDirection=`LTR,
                         ~value=textValue,
                         ~id=Uuid.makeV4(),
                         (),
                       ),
                     (),
                   )
                 ),
               |],
               ~body=?Js.Array2.length(body) > 0 ? Some(body) : None,
               (),
             );
           let variables = CreateAnnotationMutation.makeVariables(~input, ());

           let _ =
             Lib_GraphQL_CreateAnnotationMutation.Apollo.updateCache(
               ~currentUser,
               ~input,
               (),
             );
           let _ = createAnnotationMutation(~variables, ());
           let _ =
             Routes.CreatorsIdAnnotationCollectionsId.(
               Next.Router.pushWithAs(
                 staticPath,
                 path(
                   ~annotationCollectionIdComponent=Lib_GraphQL.AnnotationCollection.recentAnnotationCollectionIdComponent,
                   ~creatorUsername=currentUser.username,
                 ),
               )
             );
           Js.Promise.resolve();
         });
    ();
  };

  let handleTextChange = value => setTextValue(_ => value);
  let handleTagsChange = value => setTagsValue(_ => value);
  let handlePendingTagChange = value => setPendingTagValue(_ => value);
  let handlePendingTagCommit = value => {
    let _ =
      setTagsValue(tags =>
        Js.Array2.concat(
          [|
            Containers_AnnotationEditor_Tag.{
              text: value,
              id: None,
              href: None,
            },
          |],
          tags,
        )
      );
    let _ = setPendingTagValue(_ => "");
    let _ =
      Lib_GraphQL.AnnotationCollection.makeId(
        ~creatorUsername=currentUser->AwsAmplify.Auth.CurrentUserInfo.username,
        ~label=value,
      )
      |> Js.Promise.then_(id => {
           let _ =
             setTagsValue(tags => {
               tags
               ->Belt.Array.getIndexBy(tag => tag.text === value)
               ->Belt.Option.map(idx => {
                   let copy = Js.Array2.copy(tags);
                   let _ =
                     Js.Array2.spliceInPlace(
                       tags,
                       ~pos=idx,
                       ~remove=1,
                       ~add=[|
                         Containers_AnnotationEditor_Tag.{
                           text: value,
                           id: Some(id),
                           href: None,
                         },
                       |],
                     );
                   copy;
                 })
               ->Belt.Option.getWithDefault(tags)
             });
           Js.Promise.resolve();
         });
    ();
  };

  let _ =
    React.useEffect1(
      () => {
        // scroll the newly added tag into view
        let _ =
          switch (
            Js.Nullable.toOption(scrollContainerRef.current),
            Js.Nullable.toOption(textInputRef.current),
          ) {
          | (Some(scrollContainerElem), Some(textInputElem))
              when
                Js.Array2.length(tagsValue)
                > Js.Array2.length(previousTagsValue.current) =>
            let rect =
              Webapi.Dom.Element.getBoundingClientRect(textInputElem);
            let targetTop =
              Webapi.Dom.DomRect.top(rect)
              +. Webapi.Dom.DomRect.height(rect)
              +. Webapi.Dom.(Window.scrollY(window));

            let _ =
              Webapi.Dom.Element.scrollToWithOptions(
                {"top": targetTop, "left": 0., "behavior": "smooth"},
                scrollContainerElem,
              );
            ();
          | _ => ()
          };

        previousTagsValue.current = tagsValue;
        None;
      },
      [|tagsValue|],
    );

  <div
    ref={scrollContainerRef->ReactDOMRe.Ref.domRef}
    className={Cn.fromList([
      "w-full",
      "h-full",
      "bg-black",
      "flex",
      "flex-col",
      "overflow-y-auto",
    ])}>
    <div className={Cn.fromList(["px-6", "py-16"])}>
      <TextInput.Annotation
        onTextChange=handleTextChange
        onTagsChange=handleTagsChange
        textValue
        tagsValue
        placeholder="Lorem Ipsum..."
        autoFocus=true
        textInputRef
      />
    </div>
    <div
      className={Cn.fromList([
        "absolute",
        "bottom-0",
        "left-0",
        "right-0",
        "flex",
        "flex-col",
        "items-end",
      ])}>
      {Js.String.length(textValue) > 0
         ? <FloatingActionButton
             onClick={_ev => handleSave()}
             className={Cn.fromList(["m-6", "z-10"])}>
             <Svg
               placeholderViewBox="0 0 24 24"
               className={Cn.fromList([
                 "w-10",
                 "h-10",
                 "pointer-events-none",
               ])}
               icon=Svg.done_
             />
           </FloatingActionButton>
         : React.null}
      <TextInput_Tags
        className={Cn.fromList(["px-2", "bg-black", "z-10"])}
        onValueChange=handlePendingTagChange
        onValueCommit=handlePendingTagCommit
        value=pendingTagValue
      />
    </div>
  </div>;
};
