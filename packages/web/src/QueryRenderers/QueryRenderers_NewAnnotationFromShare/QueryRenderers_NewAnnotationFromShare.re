open Styles;
open QueryRenderers_NewAnnotationFromShare_GraphQL;

let pollInterval = 500;
let pollTimeout = 15 * 1000;
let noDataAlert = "Unable to parse image. Make sure the text is clearly highlighted and try again, or enter the text manually.";

module Data = {
  [@react.component]
  let make = (~annotation, ~identityId) => {
    <div
      className={cn([
        "w-full",
        "h-full",
        "bg-black",
        "flex",
        "flex-col",
        "relative",
        "overflow-y-auto",
      ])}>
      <Containers_NewAnnotationFromShareHeader
        identityId
        annotationFragment={annotation##headerAnnotationFragment}
      />
      <Containers_NewAnnotationFromShareEditor
        identityId
        annotationFragment={annotation##editorAnnotationFragment}
      />
    </div>;
  };
};

module Loading = {
  [@react.component]
  let make = () => {
    <>
      <Containers_NewAnnotationFromShareHeader />
      <TextInput_Loading className={cn(["px-6", "pb-4", "pt-16"])} />
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
        <FloatingActionButton
          onClick={_ev => ()}
          className={Cn.fromList(["m-6", "z-10"])}
          disabled=true>
          <MaterialUi.CircularProgress
            size={MaterialUi.CircularProgress.Size.int(26)}
            classes={MaterialUi.CircularProgress.Classes.make(
              ~colorPrimary=cn(["text-black"]),
              (),
            )}
          />
        </FloatingActionButton>
      </div>
    </>;
  };
};

module FromAnnotationId = {
  [@react.component]
  let make = (~identityId, ~annotationId) => {
    let (isLoaded, setIsLoaded) = React.useState(_ => false);
    let (query, _fullQuery) =
      ApolloHooks.useQuery(
        ~variables=
          GetAnnotationQuery.makeVariables(
            ~id=annotationId,
            ~creatorUsername=identityId,
            (),
          ),
        ~pollInterval=isLoaded ? 0 : pollInterval,
        GetAnnotationQuery.definition,
      );

    let _ =
      React.useEffect0(() => {
        let timeoutId =
          Js.Global.setTimeout(() => setIsLoaded(_ => true), pollTimeout);
        Some(() => {Js.Global.clearTimeout(timeoutId)});
      });

    let _ =
      React.useEffect1(
        () => {
          let _ =
            switch (query) {
            | Data(data) =>
              switch (data##getAnnotation) {
              | Some(_) => setIsLoaded(_ => true)
              | _ => ()
              }
            | _ => ()
            };
          None;
        },
        [|query|],
      );
    switch (query, isLoaded) {
    | (Loading, _)
    | (_, false) => <Loading />
    | (Data(data), true) =>
      switch (data##getAnnotation) {
      | Some(annotation) => <Data annotation identityId />
      | None =>
        <Redirect
          path={Routes.CreatorsIdAnnotationsNew.path(~identityId)}
          staticPath=Routes.CreatorsIdAnnotationsNew.staticPath
          search=Alert.(query_encode({alert: noDataAlert}))>
          <Loading />
        </Redirect>
      }
    | (NoData, true)
    | (Error(_), true) =>
      <Redirect
        path={Routes.CreatorsIdAnnotationsNew.path(~identityId)}
        staticPath=Routes.CreatorsIdAnnotationsNew.staticPath
        search=Alert.(query_encode({alert: noDataAlert}))>
        <Loading />
      </Redirect>
    };
  };
};

module FromFileUrl = {
  type phase =
    | PhaseLoading
    | PhaseData(CreateAnnotationFromExternalTargetMutation.t);

  [@react.component]
  let make = (~identityId, ~fileUrl) => {
    let (createAnnotationFromExternalTargetMutation, _s, _f) =
      ApolloHooks.useMutation(
        CreateAnnotationFromExternalTargetMutation.definition,
      );
    let (phase, setPhase) = React.useState(() => PhaseLoading);

    let handleError = () => {
      let search =
        "?"
        ++ Alert.{alert: noDataAlert}
           ->Alert.query_encode
           ->Externals_URLSearchParams.makeWithJson
           ->Externals_URLSearchParams.toString;

      Next.Router.replaceWithAs(
        Routes.CreatorsIdAnnotationsNew.staticPath,
        Routes.CreatorsIdAnnotationsNew.path(~identityId) ++ search,
      );
    };

    let _ =
      React.useEffect0(() => {
        let fileId = Uuid.makeV4();
        let _ =
          Fetch.fetch(fileUrl)
          |> Js.Promise.then_(Fetch.Response.blob)
          |> Js.Promise.then_(blob =>
               AwsAmplify.Storage.(
                 inst->putBlob(
                   "screenshots/" ++ fileId,
                   blob,
                   Some({
                     contentType: blob->Webapi.Blob.type_,
                     level: "private",
                   }),
                 )
               )
             )
          |> Js.Promise.then_(result => {
               let s3Url = {
                 let bucketName =
                   AwsAmplify.Config.(
                     Constants.awsAmplifyConfig->userFilesS3BucketGet
                   );

                 "s3://"
                 ++ bucketName
                 ++ "/private/"
                 ++ identityId
                 ++ "/"
                 ++ result.AwsAmplify.Storage.key;
               };

               Lib_GraphQL.makeHash(s3Url)
               |> Js.Promise.then_(hashId => {
                    let variables =
                      CreateAnnotationFromExternalTargetMutation.makeVariables(
                        ~input=
                          Lib_GraphQL_CreateAnnotationFromExternalTargetMutation.Input.make(
                            ~creatorUsername=identityId,
                            ~annotationId=
                              Lib_GraphQL.Annotation.makeIdFromComponent(
                                ~identityId,
                                ~annotationIdComponent=fileId,
                              )
                              ->Js.Option.some,
                            ~externalTarget=
                              Lib_GraphQL_AnnotationTargetInput.makeExternalTargetInput(
                                ~id=s3Url,
                                ~format=`TEXT_PLAIN,
                                ~language=`EN_US,
                                ~processingLanguage=`EN_US,
                                ~type_=`IMAGE,
                                ~textDirection=`LTR,
                                ~hashId,
                                (),
                              ),
                          ),
                        (),
                      );

                    createAnnotationFromExternalTargetMutation(
                      ~variables,
                      (),
                    );
                  });
             })
          |> Js.Promise.then_(((result, _)) => {
               let _ =
                 setPhase(currentPhase =>
                   switch (result) {
                   | ApolloHooks.Mutation.Errors(errors) =>
                     handleError();
                     currentPhase;
                   | NoData =>
                     handleError();
                     currentPhase;
                   | Data(result) => PhaseData(result)
                   }
                 );
               Js.Promise.resolve();
             })
          |> Js.Promise.catch(exn => {
               let _ = Error.(report(PromiseError(exn)));
               let _ = handleError();
               Js.Promise.resolve();
             });
        None;
      });
    switch (phase) {
    | PhaseLoading => <Loading />
    | PhaseData(data) =>
      switch (data##createAnnotationFromExternalTarget) {
      | Some(data) => <Data annotation=data identityId />
      | None =>
        <Redirect
          path={Routes.CreatorsIdAnnotationsNew.path(~identityId)}
          staticPath=Routes.CreatorsIdAnnotationsNew.staticPath
          search=Alert.{alert: noDataAlert}>
          <Loading />
        </Redirect>
      }
    };
  };
};

[@react.component]
let make = (~fileUrl=?, ~annotationId=?, ~identityId) => {
  switch (annotationId, fileUrl) {
  | (Some(annotationId), _) => <FromAnnotationId identityId annotationId />
  | (_, Some(fileUrl)) => <FromFileUrl identityId fileUrl />
  };
};
