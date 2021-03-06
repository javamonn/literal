open Containers_Onboarding_GraphQL;

[@react.component]
let make = (~user, ~children) => {
  let (onboardingMutation, _s, _f) =
    ApolloHooks.useMutation(OnboardingMutation.definition);

  let _ =
    React.useEffect0(() => {
      let identityId =
        switch (user) {
        | Providers_Authentication_User.GuestUser({identityId})
        | SignedInUser({identityId}) => Some(identityId)
        | _ => None
        };

      identityId->Belt.Option.forEach(identityId => {
        let createAnnotationInputs =
          Containers_Onboarding_GraphQL.makeOnboardingAnnotations(
            ~identityId,
          );

        let createAgentInput = {
          let email =
            switch (user) {
            | SignedInUser({attributes: {email}}) => Some(email)
            | _ => None
            };

          let hashedEmail =
            email
            ->Belt.Option.map(email =>
                Lib_GraphQL.makeHash(~digest="SHA-1", email)
                |> Js.Promise.then_(hash =>
                     hash->Js.Option.some->Js.Promise.resolve
                   )
              )
            ->Belt.Option.getWithDefault(Js.Promise.resolve(None));

          hashedEmail
          |> Js.Promise.then_(hashedEmail =>
               Js.Promise.resolve({
                 "id": Constants.apiOrigin ++ "/agents/" ++ identityId,
                 "email": email->Belt.Option.map(email => [|Some(email)|]),
                 "email_sha1":
                   hashedEmail->Belt.Option.map(hashedEmail =>
                     [|Some(hashedEmail)|]
                   ),
                 "type": `PERSON,
                 "username": identityId,
                 "homepage": None,
                 "name": None,
                 "nickname": None,
               })
             );
        };

        let _ =
          Js.Promise.all2((
            createAgentInput,
            Js.Promise.all(createAnnotationInputs),
          ))
          |> Js.Promise.then_(((createAgentInput, createAnnotationInputs)) => {
               let variables =
                 OnboardingMutation.makeVariables(
                   ~createAgentInput,
                   ~createAnnotationInput1=createAnnotationInputs[0],
                   ~createAnnotationInput2=createAnnotationInputs[1],
                   ~createAnnotationInput3=createAnnotationInputs[2],
                   ~createAnnotationInput4=createAnnotationInputs[3],
                   ~createAnnotationInput5=createAnnotationInputs[4],
                   ~createAnnotationInput6=createAnnotationInputs[5],
                   (),
                 );
               let result = onboardingMutation(~variables, ());
               let _ =
                 Lib_GraphQL_CreateAnnotationMutation.Apollo.updateCacheMany(
                   ~identityId,
                   ~inputs=createAnnotationInputs->Belt.Array.reverse,
                   ~createAnnotationCollection=true,
                   (),
                 );
               result;
             })
          |> Js.Promise.then_(((mutationResult, _)) => {
               switch (mutationResult) {
               | ApolloHooks.Mutation.Errors(errors) =>
                 Error.(report(ApolloMutationError(errors)))
               | NoData => Error.(report(ApolloEmptyData))
               | Data(_) => ()
               };
               Js.Promise.resolve();
             });
        ();
      });

      None;
    });

  children;
};
