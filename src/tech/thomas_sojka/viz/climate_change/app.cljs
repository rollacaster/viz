(ns tech.thomas-sojka.viz.climate-change.app
  (:require ["d3-contour" :as d3-contour]
            ["d3-geo" :as d3-geo]
            ["d3-scale" :as d3-scale]
            ["d3-scale-chromatic" :as d3-scale-chromatic]
            ["topojson-client" :as topojson]
            [goog.string :as gstring]
            [goog.string.format]
            [reagent.dom :as dom]
            [reagent.ratom :as r]
            [tech.thomas-sojka.viz.climate-change.data :as data]))

(defonce surface-temperature-data (r/atom nil))
(defonce geojson-world (r/atom nil))
(defonce year (r/atom 0))

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

(def contours (-> (d3-contour/contours) (.size (clj->js [180 90])) (.thresholds (clj->js (range 0 21)))))
(defn contours-at-time [surface-temperature-data time]
  (->> time (aget surface-temperature-data) into-array contours))
(defn temperatur-contour [{:keys [surface-temperature-data geojson-world]}]
  (let [data (aget surface-temperature-data @year)
        contours (-> (d3-contour/contours) (.size (clj->js [180 90])) (.thresholds (clj->js (range -10 11))))
        c (->> data into-array contours)]
    [:div
     [:input {:type "range" :min 0 :max 1704 :value @year :on-change #(reset! year (int ^js (.-target.value %)))}]
     [:span (str (+ 1880 (js/Math.floor (/ (int @year) 12)))
                 "-"
                 (gstring/format "%02d" (inc (mod @year 12))))]
     [:button.bg-gray-200.p-1.rounded.ml-2 {:on-click (fn []
                                                        (doseq [i (range 0 1709)]
                                                          (js/setTimeout
                                                           (fn [] (swap! year inc)) (* i 130))))}
      "Play"]
     [:style {:dangerouslySetInnerHTML {:__html ".contour:hover {fill: red}"}}]
     [:div.flex.items-center
      (map-indexed (fn [i multi-polygon]
                     ^{:key (.-value multi-polygon)}
                     [:div.flex.mr-2.items-center
                      [:div.mr-1 (- (.-value multi-polygon) 10)]
                      [:span.inline-block.w-4.h-4 {:style {:background-color (.interpolateMagma d3-scale-chromatic (/ i (count c)))}}]])
                   c)]
     [:svg {:width 960 :height 500 :viewBox "0 0 960 500"}
      [:g [:path {:opacity 0.5 :d (path geojson-world)}]]
      [:g {:opacity 0.7}
       (->> c
            (map-indexed (fn [i multi-polygon]
                           (set! (.-coordinates multi-polygon)
                                 (.map (.-coordinates multi-polygon)
                                       (fn [polygon]
                                         (.map polygon
                                               (fn [ring]
                                                 (.map ring
                                                       (fn [[lng lat]] #js [(lng-scale lng) (lat-scale lat)])))))))
                           (map
                            (fn [[j polygon]]
                              (map-indexed
                               (fn [k ring-path]
                                 [:path.contour
                                  {:key (str (.-value multi-polygon) " " j " " k)
                                   :value (str (.-value multi-polygon) " " j "" k)
                                   :on-click #(prn (.-value multi-polygon))
                                   :d (str ring-path "Z")
                                   :fill (.interpolateMagma d3-scale-chromatic (/ i (count c)))}])
                               (str/split (path #js {:type "Polygon" :coordinates polygon}) #"Z")))
                            (map-indexed vector (.-coordinates multi-polygon))))))]]]))

(defn app []
  (when-not @surface-temperature-data
      (-> (js/fetch "data/gistemp250_GHCNv4.nc")
          (.then (fn [res] (.arrayBuffer res)))
          (.then (fn [d] (reset! surface-temperature-data (-> d data/read data/partition-tempanamoly))))))
  (when-not @geojson-world
      (-> (js/fetch "data/land-50m.json")
          (.then (fn [res] (.json res)))
          (.then (fn [d] (reset! geojson-world (first (.-features (.feature topojson d ^js (.-objects.land d)))))))))
  (fn []
    (when (and @surface-temperature-data @geojson-world)
      [temperatur-contour {:geojson-world @geojson-world
                           :surface-temperature-data @surface-temperature-data}])))

(defn ^:dev/after-load main []
  (dom/render [app] (js/document.getElementById "app")))
