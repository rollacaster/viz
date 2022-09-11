(ns tech.thomas-sojka.viz.climate-change.app
  (:require
   [tech.thomas-sojka.viz.climate-change.d3-countour-by-example :as d3-countour-by-example]
   [reagent.dom :as dom]))

(dom/render
 [d3-countour-by-example/main]
 (js/document.getElementById "app"))
