%raw
"require('isomorphic-fetch')";

let authenticatedClientAuthOptions = {
  Webview.isWebview()
    ? AwsAppSync.Client.authWithCognitoUserPools(~jwtToken=() => {
        Timer.thunkP(~label="AUTH_GET_TOKENS", () =>
          Webview.(
            postMessageForResult(WebEvent.make(~type_="AUTH_GET_TOKENS"))
          )
        )
        |> Js.Promise.then_(result => {
             switch (result) {
             | Some(data) =>
               switch (Webview.WebEvent.authGetTokensResult_decode(data)) {
               | Belt.Result.Ok(tokens) =>
                 tokens.idToken
                 ->AwsAmplify.Auth.JwtToken.unsafeOfString
                 ->Js.Promise.resolve
               | Belt.Result.Error(_) =>
                 let _ = Next.Router.replace("/authenticate");
                 Js.Promise.reject(Error.AuthenticationRequired);
               }
             | None =>
               let _ = Next.Router.replace("/authenticate");
               Js.Promise.reject(Error.AuthenticationRequired);
             }
           })
      })
    : AwsAppSync.Client.authWithCognitoUserPools(~jwtToken=() =>
        AwsAmplify.(
          Auth.(inst->currentSession)
          |> Js.Promise.then_(s =>
               s
               ->Auth.CognitoUserSession.getIdToken
               ->Auth.CognitoIdToken.getJwtToken
               ->Js.Promise.resolve
             )
        )
      );
};

let unauthenticatedClientAuthOptions =
  AwsAppSync.Client.authWithIAM(~credentials=() =>
    AwsAmplify.Auth.(inst->currentCredentials)
  );

/**
 * FIXME: https://github.com/awslabs/aws-mobile-appsync-sdk-js/issues/450#issuecomment-522264166
 */

let appSyncLinkOptions =
  AwsAppSync.Client.{
    url:
      AwsAmplify.(
        Constants.awsAmplifyConfig->Config.appSyncGraphqlEndpointGet
      ),
    region: AwsAmplify.(Constants.awsAmplifyConfig->Config.appSyncRegionGet),
    auth: authenticatedClientAuthOptions,
    disableOffline: true,
    complexObjectsCredentials: () =>
      AwsAmplify.Auth.(inst->currentCredentials),
    mandatorySignIn: false,
  };
let appSyncLink = AwsAppSync.Client.createAppSyncLink(appSyncLinkOptions);

let client =
  AwsAppSync.Client.(
    makeWithOptions(appSyncLinkOptions, {link: appSyncLink})
  );

[@react.component]
let make = (~render) =>
  <ReasonApollo.Provider client>
    <AwsAppSync.Rehydrated
      render={({rehydrated}) =>
        <ApolloHooks.Provider client>
          {render(~rehydrated)}
        </ApolloHooks.Provider>
      }
    />
  </ReasonApollo.Provider>;
