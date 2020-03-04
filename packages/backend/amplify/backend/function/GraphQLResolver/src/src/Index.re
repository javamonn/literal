let handler = event => {
  switch (event->Lib_Lambda.event_decode) {
  | Belt.Result.Ok({
      typeName: "Query", 
      fieldName: "createHighlightFromScreenshot"
    } as ctx) => QueryResolver.CreateHighlightFromScreenshot.resolver(ctx)
  | Belt.Result.Error(e) =>
    Js.log("Unable to decode event.");
    Js.Exn.raiseError(e.message);
  | _ =>
    Js.log("Resolver not found.");
    Js.Exn.raiseError("Resolver not found.");
  };
};