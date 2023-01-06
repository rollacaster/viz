(ns tech.thomas-sojka.viz.climate-change.app
  (:require ["d3-contour" :as d3-contour]
            ["d3-geo" :as d3-geo]
            ["d3-scale" :as d3-scale]
            ["d3-scale-chromatic" :as d3-scale-chromatic]
            ["topojson-client" :as topojson]
            [goog.string :as gstring]
            [reagent.dom :as dom]
            [reagent.ratom :as r]
            [tech.thomas-sojka.viz.climate-change.data :as data]))

(defonce surface-temperature-data (r/atom nil))
(defonce geojson-world (r/atom nil))
(defonce year (r/atom 1550))

(def projection (d3-geo/geoMercator))
(def path (d3-geo/geoPath projection))

(def lng-scale
  (-> (d3-scale/scaleLinear)
      (.domain #js [0 180])
      (.range #js [-180 180])))

(def lat-scale
  (-> (d3-scale/scaleLinear)
      (.domain #js [0 90])
      (.range #js [-90 90])))

(defn temperatur-contour [{:keys [surface-temperature-data geojson-world]}]
  (let [data (update-vals
              (->> @year
                   (data/temp-anamoly-for-time surface-temperature-data)
                   (group-by (juxt :lon :lat)))
              #(first (map :anomaly %)))
        contour-data (for [lat (range -89 91 2)
                           long (range -179 181 2)]
                       (get data [long lat] 0))
        contours (-> (d3-contour/contours) (.size (clj->js [180 90])))
        c (->> contour-data into-array contours)]
    [:div
     [:input {:type "range" :min 0 :max 1704 :value @year :on-change #(reset! year ^js (.-target.value %))}]
     [:span (str (+ 1880 (js/Math.floor (/ (int @year) 12)))
                 "-"
                 (gstring/format "%02d" (inc (mod @year 12))))]
     [:style {:dangerouslySetInnerHTML {:__html ".contour:hover {fill: red}"}}]
     [:div.flex.items-center
      (map-indexed (fn [i contour]
                     ^{:key (.-value contour)}
                     [:div.flex.mr-2.items-center
                      [:div.mr-1 (.-value contour)]
                      [:span.inline-block.w-4.h-4 {:style {:background-color (.interpolateMagma d3-scale-chromatic (/ i (count c)))}}]])
                   c)]

     [:svg {:width 960 :height 500 :viewBox "0 0 960 500"}
      [:g [:path {:d (path geojson-world)}]]
      [:g {:opacity 0.7}
       (->> c
            (map-indexed (fn [i cont]
                           [:path.contour
                            {:value (.-value cont)
                             :d (path (clj->js
                                       (update (js->clj cont)
                                               "coordinates"
                                               (fn [coordiantes]
                                                 (for [polygon coordiantes]
                                                   (for [ring polygon]
                                                     (for [[lng lat] ring]
                                                       [(lng-scale lng) (lat-scale lat)])))))))
                             :fill (.interpolateMagma d3-scale-chromatic (/ i (count c)))}])))]]]))

(defn app []
  (when-not @surface-temperature-data
      (-> (js/fetch "/data/climate-change/gistemp250_GHCNv4.nc")
          (.then (fn [res] (.arrayBuffer res)))
          (.then (fn [d] (reset! surface-temperature-data (data/read d))))))
  (when-not @geojson-world
      (-> (js/fetch "/data/land-50m.json")
          (.then (fn [res] (.json res)))
          (.then (fn [d] (reset! geojson-world (first (.-features (.feature topojson d ^js (.-objects.land d)))))))))
  (fn []
    (when (and @surface-temperature-data @geojson-world)
      [temperatur-contour {:geojson-world @geojson-world
                           :surface-temperature-data @surface-temperature-data}])))

(defn ^:dev/after-load main []
  (dom/render [app] (js/document.getElementById "app")))
