(ns tech.thomas-sojka.viz.climate-change.app
  (:require
   ["d3-contour" :as d3-contour]
   ["d3-geo" :as d3-geo]
   [reagent.core :as r]
   [reagent.dom :as dom]))

(def data
  (r/atom
   [[5 2 1][2 2 1][1 1 1]]))

(def path (d3-geo/geoPath))

(defn contour-example [{:keys [data set-data!]}]
  (let [contours (-> (d3-contour/contours)
                     (.size (clj->js [(count (first data)) (count data)])))]
    [:<>
     [:div {:style {:position "absolute"
                    :display "flex"
                    :flex-wrap "wrap"
                    :height "100%"}}
      (map-indexed
       (fn [y row]
         (map-indexed
          (fn [x cell]
            [:input {:style {:width (str (* 100 (/ 1 (count (first data)))) "%")
                             :height (str (* 100 (/ 1 (count data))) "%")
                             :background "rgba(255,255,255,0.2)"
                             :padding 0
                             :border 0
                             :font-size "12rem"
                             :text-align "center"}
                     :key [x y]
                     :value cell
                     :on-change (fn [e] (set-data! x y ^js (.-target.value e)))}])
          row))
       data)]
     [:svg
      {:viewBox (str 0 " " 0 " " (count (first data)) " " (count data))}
      [:g
       (map-indexed
        (fn
          [i t]
          [:path
           {:key i
            :d (path (.contour contours (clj->js (flatten data)) t))
            :fill (nth ["green" "yellow" "red" "orange" "blue" "pink" "gold"] i)}])
        [1 2 3])]]]))

(defn app []
  [:div {:style {:margin "5rem"
                 :position "relative"}}
   [contour-example {:data @data
                     :set-data! (fn [x y value]
                                  (prn x y (int value))
                                  (swap! data assoc-in [y x] (int value)))}]])

(dom/render
 [app]
 (js/document.getElementById "app"))


(defn main [])
