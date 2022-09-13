(ns tech.thomas-sojka.viz.climate-change.app
  (:require ["d3-contour" :as d3-contour]
            ["d3-geo" :as d3-geo]
            ["nice-color-palettes" :as palettes]
            [reagent.dom :as dom]
            [reagent.ratom :as r]
            [tech.thomas-sojka.viz.climate-change.data :as data]))

(def data (r/atom nil))

(->(js/fetch "/data/climate-change/gistemp250_GHCNv4.nc")
   (.then (fn [res] (.arrayBuffer res)))
   (.then (fn [d]
            (reset! data (data/read d)))))

(def path (d3-geo/geoPath))

(def year (r/atom 0))

(defn temperatur-contour []
  (when @data
    (let [width 180
          height 360
          data (update-vals (group-by (juxt :lat :lon) (data/temp-anamoly-for-time @data @year)) #(first (map :anomaly %)))
          contour-data (for [lat (range -90 90)
                             long (range -180 180)]
                         (get data [lat long] 383838))
          contours (-> (d3-contour/contours) (.size (clj->js [width height])))]
      [:div
       {:style {:width 500}}
       [:input {:type "range" :min 0 :max 200 :value @year :on-change #(reset! year ^js (.-target.value %))}] @year
       [:svg
        {:viewBox (str 0 " " 0 " " width " " height)
         :style {:transform "rotate(-90deg) translateX(-100%)"
                 :transform-origin "top left"}}
        [:g
         (->> contour-data
              into-array
              contours
              (map-indexed (fn [i c]
                             [:path
                              {:key i
                               :value (.-value c)
                               :d (path c)
                               :fill
                               (if (= (.-value c) 383838)
                                 "rgba(127,127,127,0.5)"
                                 (get (nth palettes 96)
                                      (mod i 5)))}])))]]])))

(defn main [])

(dom/render
 [temperatur-contour]
 (js/document.getElementById "app"))
