type authenticationError =
  | UserNotFound
  | UserNotAuthorized
  | AuthenticationFailed;

[@react.component]
let default = () => {
  let authentication = Hooks_CurrentUserInfo.use();
  let (isAuthenticating, setIsAuthenticating) = React.useState(() => false);
  let (authenticationError, setAuthenticationError) =
    React.useState(() => None);

  let _ =
    React.useEffect1(
      () => {
        switch (authentication) {
        | Authenticated(currentUser) =>
          Routes.CreatorsIdAnnotationCollectionsId.(
            Next.Router.replaceWithAs(
              staticPath,
              path(
                ~creatorUsername=currentUser.username,
                ~annotationCollectionIdComponent=Lib_GraphQL.AnnotationCollection.recentAnnotationCollectionIdComponent,
              ),
            )
          )
        | _ => ()
        };
        None;
      },
      [|authentication|],
    );

  let ((email, password), setAuthenticationValue) =
    React.useState(_ => ("", ""));

  let handleSubmit = () => {
    setAuthenticationError(_ => None);
    setIsAuthenticating(_ => true);
    let _ =
      Webview.postMessageForResult(
        Webview.WebEvent.make(
          ~type_="AUTH_SIGN_IN",
          ~data=
            Js.Json.object_(
              Js.Dict.fromList([
                ("email", email->Js.Json.string),
                ("password", password->Js.Json.string),
              ]),
            ),
          (),
        ),
      )
      |> Js.Promise.then_(result => {
           let _ =
             switch (
               result->Belt.Option.map(
                 Webview.WebEvent.AuthSignInResult.decode,
               )
             ) {
             | Some(Ok({error: None, tokens: Some(_)})) =>
               Webview.HubEvent.publish(~event="AUTH_SIGN_IN_RESULT", ())
             | Some(Ok({error: Some("SIGN_IN_FAILED_USER_NOT_FOUND")})) =>
               setIsAuthenticating(_ => false);
               setAuthenticationError(_ => Some(UserNotFound));
             | Some(Ok({error: Some("SIGN_IN_FAILED_USER_NOT_AUTHORIZED")})) =>
               setIsAuthenticating(_ => false);
               setAuthenticationError(_ => Some(UserNotAuthorized));
             | Some(Ok({error: Some(_)}))
             | Some(Ok({tokens: None}))
             | Some(Error(_))
             | None =>
               setIsAuthenticating(_ => false);
               setAuthenticationError(_ => Some(AuthenticationFailed));
             };
           Js.Promise.resolve();
         });
    ();
  };

  let handleBack = () => {
    let _ = Next.Router.back();
    ();
  };

  <div
    className={Cn.fromList([
      "w-full",
      "h-full",
      "bg-black",
      "flex",
      "flex-col",
    ])}>
    <Header
      className={Cn.fromList([
        "py-2",
        "px-4",
        "items-center",
        "bg-darkAccent",
        "border-b",
        "border-dotted",
        "border-lightDisabled",
      ])}>
      <MaterialUi.IconButton
        size=`Small
        edge=MaterialUi.IconButton.Edge.start
        onClick={_ => {handleBack()}}
        _TouchRippleProps={
          "classes": {
            "child": Cn.fromList(["bg-white"]),
            "rippleVisible": Cn.fromList(["opacity-75"]),
          },
        }
        classes={MaterialUi.IconButton.Classes.make(
          ~root=Cn.fromList(["p-0"]),
          (),
        )}>
        <Svg
          placeholderViewBox="0 0 24 24"
          className={Cn.fromList([
            "w-5",
            "h-5",
            "pointer-events-none",
            "opacity-75",
          ])}
          icon=Svg.back
        />
      </MaterialUi.IconButton>
      <h1
        className={Cn.fromList([
          "ml-2",
          "font-sans",
          "text-lightPrimary",
          "text-lg",
          "font-bold",
        ])}>
        {React.string("Sign In")}
      </h1>
    </Header>
    <main
      className={Cn.fromList(["flex", "flex-col", "px-4", "py-4", "flex-1"])}>
      <MaterialUi.Collapse
        collapsedHeight={MaterialUi.Collapse.CollapsedHeight.int(0)}
        _in={Js.Option.isSome(authenticationError)}>
        <div
          className={Cn.fromList([
            "flex",
            "flex-row",
            "items-center",
            "border",
            "border-dotted",
            "border-lightSecondary",
            "bg-errorRed",
            "p-3",
            "mb-4",
          ])}>
          <Svg
            placeholderViewBox="0 0 24 24"
            className={Cn.fromList(["w-5", "h-5", "pointer-events-none"])}
            icon=Svg.errorOutline
          />
          <p
            className={Cn.fromList([
              "font-sans",
              "text-lightPrimary",
              "leading-none",
              "text-sm",
              "ml-4",
            ])}>
            {React.string(
               switch (authenticationError) {
               | Some(UserNotFound) => "No user exists for this email. Verify it is correct, or sign up."
               | Some(UserNotAuthorized)
               | Some(AuthenticationFailed)
               | None => "Unable to sign in. Verify that your credentials are correct."
               },
             )}
          </p>
        </div>
      </MaterialUi.Collapse>
      <AuthenticationFields
        onChange={v => setAuthenticationValue(_ => v)}
        onSubmit=handleSubmit
        type_=AuthenticationFields.SignIn
      />
      <div
        className={Cn.fromList([
          "flex",
          "flex-row",
          "items-center",
          "justify-end",
          "mt-6",
        ])}>
        <MaterialUi.Button
          onClick={_ => handleSubmit()}
          fullWidth=false
          classes={MaterialUi.Button.Classes.make(
            ~label=Cn.fromList(["font-sans"]),
            (),
          )}
          style={
            isAuthenticating
              ? ReactDOM.Style.make(
                  ~paddingLeft="42px",
                  ~paddingRight="42px",
                  (),
                )
              : ReactDOM.Style.make()
          }
          variant=`Contained
          size=`Large
          _TouchRippleProps={
            "classes": {
              "child": Cn.fromList(["bg-white"]),
              "rippleVisible": Cn.fromList(["opacity-75"]),
            },
          }>
          {isAuthenticating
             ? <MaterialUi.CircularProgress
                 variant=`Indeterminate
                 size={MaterialUi.CircularProgress.Size.int(26)}
                 classes={MaterialUi.CircularProgress.Classes.make(
                   ~colorPrimary=Cn.fromList(["text-black"]),
                   (),
                 )}
               />
             : React.string("Sign In")}
        </MaterialUi.Button>
      </div>
    </main>
  </div>;
};

let page = "authenticate/sign-in.js";