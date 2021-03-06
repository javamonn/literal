[@react.component]
let make = (~text) =>
  <div
    className={Cn.fromList([
      "border-t",
      "border-dotted",
      "border-lightDisabled",
      "flex",
      "flex-row",
      "items-center",
      "p-6",
      "bg-darkAccent",
    ])}>
    <p className={Cn.fromList(["text-lightSecondary", "mr-6", "text-xs"])}>
      {React.string(text)}
    </p>
    <Svg
      icon=Svg.helpOutline
      className={Cn.fromList([
        "pointer-events-none",
        "w-6",
        "w-6",
        "opacity-75",
      ])}
    />
  </div>;
