const resolvePath = (path) => new URL(path, import.meta.url).pathname;

const config = {
  mode: "production",
  optimization: {
    minimize: false,
  },
  entry: {
    GetAnnotation: resolvePath("./get-annotation/index.mjs"),
    AnnotationRenderer: resolvePath("./annotation-renderer/index.mjs"),
    GetAnnotationBoundingBox: resolvePath(
      "./get-annotation-bounding-box/index.mjs"
    ),
    GetScripts: resolvePath("./get-scripts/index.mjs"),
  },
  output: {
    filename: "SourceWebView[name].js",
    path: resolvePath("./dist"),
    library: "Literal",
  },
  module: {
    rules: [
      {
        test: /\.m?js$/,
        exclude: [
          // \\ for Windows, \/ for Mac OS and Linux
          /node_modules[\\\/]core-js/,
          /node_modules[\\\/]webpack[\\\/]buildin/,
        ],
        use: {
          loader: "babel-loader",
          options: {
            presets: ["@babel/preset-env"],
          },
        },
      },
    ],
  },
};

export default config;
