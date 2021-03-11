module.exports = {
  important: true,
  purge: {
    content: ["./src/**/*.js"],
  },
  theme: {
    fontFamily: {
      sans: [
        "Roboto Mono",
        "system-ui",
        "-apple-system",
        "BlinkMacSystemFont",
        "Segoe\\ UI",
        "Roboto",
        "Helvetica\\ Neue",
        "Arial",
        "Noto\\ Sans",
        "sans-serif",
        "Apple Color\\ Emoji",
        "Segoe\\ UI\\ Emoji",
        "Segoe\\ UI\\ Symbol",
        "Noto\\ Color\\ Emoji",
      ],
      serif: [
        "Domine",
        "Georgia",
        "Cambria",
        "Times\\ New\\ Roman",
        "Times",
        "serif",
      ],
    },
    extend: {
      spacing: {
        "14": "3.5rem",
      },
      margin: {
        "1/2": "0.2rem",
      },
      borderWidth: {
        "1/2": "0.5px",
      },
      listStyleType: {
        square: "square",
      },
      backgroundColor: {
        // white
        lightPrimary: "rgba(255, 255, 255, .92)",
        lightSecondary: "rgba(255, 255, 255, .72)",
        lightDisabled: "rgba(255, 255, 255, .50)",

        // accent black
        darkAccent: "#181818",

        // used for fix width backgrounds
        backgroundGray: "rgb(229, 229, 229)",
      },
      borderColor: {
        lightPrimary: "rgba(255, 255, 255, .92)",
        lightSecondary: "rgba(255, 255, 255, .72)",
        lightDisabled: "rgba(255, 255, 255, .50)",
      },
      textColor: {
        lightPrimary: "rgba(255, 255, 255, .92)",
        lightSecondary: "rgba(255, 255, 255, .72)",
        lightDisabled: "rgba(255, 255, 255, .50)",
      },
      zIndex: {
        "-10": "-10",
      },
    },
  },
};
