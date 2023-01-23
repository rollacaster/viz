(ns tech.thomas-sojka.viz.climate-change.app
  (:require ["box-intersect" :as box-intersect]
            ["d3-contour" :as d3-contour]
            ["d3-geo" :as d3-geo]
            ["d3-scale" :as d3-scale]
            ["d3-scale-chromatic" :as d3-scale-chromatic]
            ["flubber" :as flubber]
            ["lodash" :as lodash]
            ["topojson-client" :as topojson]
            [goog.string :as gstring]
            [goog.string.format]
            [reagent.dom :as dom]
            [reagent.ratom :as r]
            [tech.thomas-sojka.viz.climate-change.data :as data]))

(defonce surface-temperature-data (r/atom nil))
(defonce geojson-world (r/atom nil))
(defonce inputs (r/atom {:year 0
                         :value-filter 14}))

(def projection (-> (d3-geo/geoMercator)
                    (.translate #js [480 300])))
(def path (d3-geo/geoPath projection))

(def lng-scale
  (-> (d3-scale/scaleLinear)
      (.domain #js [0 180])
      (.range #js [-180 180])))

(def lat-scale
  (-> (d3-scale/scaleLinear)
      (.domain #js [0 90])
      (.range #js [-90 90])))

(def contours (-> (d3-contour/contours)
                  (.size (clj->js [180 90]))
                  (.thresholds (clj->js (range 0 21)))))
(defn contours-at-time [surface-temperature-data time]
  (->> time (aget surface-temperature-data) into-array contours))

(defn- polygon-bounds [polygon]
  (-> path
      (.bounds #js {:type "Polygon" :coordinates polygon})
      (.flat)))

(defn- render-ring []
  (let [start (.now js/Date)
        time (r/atom 0)
        duration 500]
    (letfn [(on-frame []
              (when (< @time duration)
                (reset! time (- (.now js/Date) start))
                (on-loop)))
            (on-loop [] (js/requestAnimationFrame on-frame))]
      (on-loop)
      (fn [{:keys [value fill label d next-d center]}]
        [:<>
         [:path.contour
          {:value value
           :on-click #(prn value)
           :d ((flubber/interpolate d next-d)
               (/ @time duration))
           :fill fill}]
         (let [[x y] center]
           [:text.fill-red-700.font-bold {:x x :y y} label])]))))

(defn- polygons-with-next [multi-polygon next-contour intersections]
  (->> (range (count (.-coordinates next-contour)))
       (map (fn [next-polygon-idx]
              (if ((group-by second intersections) next-polygon-idx)
                (let [[[polygon-idx]] ((group-by second intersections) next-polygon-idx)]
                  {:type :update
                   :data
                   {:from (get (.-coordinates multi-polygon) polygon-idx)
                    :to (get (.-coordinates next-contour) next-polygon-idx)}})
                {:type :new
                 :data (get (.-coordinates multi-polygon) next-polygon-idx)})))))

(defn- render-animated-contours [{:keys [contour color-scale next-contour]}]
  (->> contour
       (keep-indexed (fn [i multi-polygon]
                       (when (and
                              (or (= (:value-filter @inputs) (.-value multi-polygon))
                                  (nil? (:value-filter @inputs)))
                              (not= (.-value multi-polygon) 0))
                         (let [intersections
                               (if next-contour
                                 (box-intersect
                                  (into-array (map polygon-bounds (.-coordinates multi-polygon)))
                                  (into-array (map polygon-bounds (.-coordinates (get next-contour i)))))
                                 #js [])]
                           (->> intersections
                                (polygons-with-next multi-polygon (get next-contour i))
                                (map-indexed vector)
                                (map
                                 (fn [[j {:keys [type data]}]]
                                   (case type
                                     :update (let [{:keys [from to]} data]
                                               ^{:key j}
                                               [render-ring {:d (path #js {:type "Polygon" :coordinates from})
                                                             :next-d (path #js {:type "Polygon" :coordinates to})
                                                             :center (.centroid path #js {:type "Polygon" :coordinates to})
                                                             :fill (color-scale (/ i (count contour)))
                                                             :label j}])
                                     :new
                                     (when data
                                       ^{:key j}
                                       [render-ring {:d (path #js {:type "Polygon" :coordinates data})
                                                     :next-d (path #js {:type "Polygon" :coordinates data})
                                                     :center (.centroid path #js {:type "Polygon" :coordinates data})
                                                     :fill (color-scale (/ i (count contour)))
                                                     :label j}])))))))))))

(defn- render-contours [{:keys [contour color-scale]}]
  (->> contour
       (keep-indexed (fn [i multi-polygon]
                       (when (and
                              (or (= (:value-filter @inputs) (.-value multi-polygon))
                                  (nil? (:value-filter @inputs)))
                              (not= (.-value multi-polygon) 0))
                         (->> (.-coordinates multi-polygon)
                              (map-indexed vector)
                              (map
                               (fn [[j polygon]]
                                 ^{:key j}
                                 [render-ring {:d (path #js {:type "Polygon" :coordinates polygon})
                                               :next-d (path #js {:type "Polygon" :coordinates polygon})
                                               :center (.centroid path #js {:type "Polygon" :coordinates polygon})
                                               :fill (color-scale (/ i (count contour)))
                                               :label j}]))))))))

(defn- fix-coords [next-contour]
  (doseq [multi-polygon next-contour]
       (set! (.-coordinates multi-polygon)
             (.map (.-coordinates multi-polygon)
                   (fn [polygon]
                     (.map polygon
                           (fn [ring]
                             (.map ring (fn [[lng lat]] #js [(lng-scale lng) (lat-scale lat)])))))))))

(defn temperatur-contour [{:keys [surface-temperature-data geojson-world]}]
  (let [{:keys [value-filter year]} @inputs
        c (contours-at-time surface-temperature-data year)
        next-contour (contours-at-time surface-temperature-data (inc year))]
    [:div
     [:input {:type "range" :min 0 :max 1704 :value year :on-change #(swap! inputs assoc :year (int ^js (.-target.value %)))}]
     [:span (str (+ 1880 (js/Math.floor (/ (int year) 12)))
                 "-"
                 (gstring/format "%02d" (inc (mod year 12))))]
     [:button.bg-gray-200.p-1.rounded.ml-2 {:on-click (fn []
                                                        (doseq [i (range 0 1709)]
                                                          (js/setTimeout
                                                           (fn [] (swap! inputs update :year inc)) (* i 1000))))}
      "Play"]
     [:style {:dangerouslySetInnerHTML {:__html ".contour:hover {fill: red}"}}]
     [:div.flex.items-center.mb-4
      (map-indexed (fn [i multi-polygon]
                     ^{:key (.-value multi-polygon)}
                     [:div.flex.mr-1.items-center
                      [:div.mr-1 (- (.-value multi-polygon) 10)]
                      [:span.inline-block.w-3.h-3.rounded {:style {:background-color (.interpolateTurbo d3-scale-chromatic (/ i (count c)))}}]])
                   c)]
     [:div.flex.mb-4
      (map (fn [multi-polygon]
             ^{:key (.-value multi-polygon)}
             [:div.flex.mr-1.items-center
              [:button.p-1.rounded
               {:class (if (= value-filter (.-value multi-polygon))
                         "bg-blue-400 text-white" "bg-gray-100")
                :on-click #(swap! inputs assoc :value-filter
                                  (when-not (= value-filter (.-value multi-polygon))
                                    (.-value multi-polygon)))}
               (.-value multi-polygon)]])
           c)]
     (fix-coords c)
     (fix-coords next-contour)
     (let [[width height] (map #(/ % 1.3) [960 500])]
       [:<>
        [:svg {:width width :height height :viewBox (str "0 0 "960 " " 500)}
         [:g [:path {:opacity 0.5 :d (path geojson-world)}]]
         [:g {:opacity 0.7}
          (doall
           (render-animated-contours
            {:contour c
             :next-contour next-contour
             :color-scale #(.interpolateTurbo d3-scale-chromatic %)}))]]
        [:svg {:width width :height height :viewBox (str "0 0 "960 " " 500)}
         [:g [:path {:opacity 0.5 :d (path geojson-world)}]]
         [:g {:opacity 0.7}
          (doall
           (render-contours
            {:contour c
             :color-scale #(.interpolateTurbo d3-scale-chromatic %)}))]]
        [:svg {:width width :height height :viewBox (str "0 0 "960 " " 500)}
         [:g [:path {:opacity 0.5 :d (path geojson-world)}]]
         [:g {:opacity 0.7}
          (doall
           (render-contours
            {:contour next-contour
             :color-scale #(.interpolateTurbo d3-scale-chromatic %)}))]]
        (inc year)])]))

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
