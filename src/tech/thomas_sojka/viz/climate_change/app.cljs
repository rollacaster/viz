(ns tech.thomas-sojka.viz.climate-change.app
  (:require ["d3-contour" :as d3-contour]
            ["d3-geo" :as d3-geo]
            ["d3-scale" :as d3-scale]
            ["d3-scale-chromatic" :as d3-scale-chromatic]
            ["topojson-client" :as topojson]
            [applied-science.js-interop :as j]
            [reagent.dom :as dom]
            [reagent.ratom :as r]
            [tech.thomas-sojka.viz.climate-change.data :as data]))

(def data (r/atom nil))
(def world (r/atom nil))
(def geojson (r/atom nil))
(->(js/fetch "/data/climate-change/gistemp250_GHCNv4.nc")
   (.then (fn [res] (.arrayBuffer res)))
   (.then (fn [d]
            (reset! data (data/read d)))))

(->(js/fetch "/data/land-50m.json")
   (.then (fn [res] (.json res)))
   (.then (fn [d]
            (reset! geojson ^js (.-objects.land d))

            (reset! world (.feature topojson d ^js (.-objects.land d))))))

(def projection (d3-geo/geoMercator))

(def path (d3-geo/geoPath projection))
(def path2 (d3-geo/geoPath ))

(def year (r/atom 1550))

(def lng-scale
  (-> (d3-scale/scaleLinear)
      (.domain #js [0 180])
      (.range #js [-180 180])))

(def lat-scale
  (-> (d3-scale/scaleLinear)
      (.domain #js [0 90])
      (.range #js [-90 90])))

(defn temperatur-contour []
  (when @data
    (let [data (update-vals
                (->> @year
                     (data/temp-anamoly-for-time @data)
                     (group-by (juxt :lon :lat)))
                #(first (map :anomaly %)))
          contour-data (for [lat (range -89 91 2)
                             long (range -179 181 2)]
                         (get data [long lat] 0))
          contours (-> (d3-contour/contours) (.size (clj->js [180 90])))
          c (->> contour-data
                 into-array
                 contours)]
      [:div
       [:input {:type "range" :min 0 :max 1704 :value @year :on-change #(reset! year ^js (.-target.value %))}]
       (+ 1880 (js/Math.floor (/ (int @year) 12)))
       [:style {:dangerouslySetInnerHTML {:__html ".contour:hover {fill: red}"}}]
       [:div.flex.items-center
        (map-indexed (fn [i contour]
                       ^{:key (.-value contour)}
                       [:div.flex.mr-2.items-center
                        [:div.mr-1 (.-value contour)]
                        [:span.inline-block.w-4.h-4 {:style {:background-color (.interpolateMagma d3-scale-chromatic (/ i (count c)))}}]])
                     c)]
       (when @world
         [:div.relative
          [:svg.absolute {:width 960 :height 500 :viewBox "0 0 960 500"}
           [:g
            (map
             (fn [coordinates]
               (let [p #js {:type "Polygon" :coordinates coordinates}]
                 [:path {:key [coordinates]
                         :d (path p)
                         :on-click #(js/console.log
                                     (.bounds path p)
                                     (clj->js (map (fn [p] (.invert projection p)) (.bounds path p))))}]))
             (.-geometry.coordinates (first (.-features @world))))]]
          [:svg.absolute {:width 960 :height 500 :viewBox "0 0 180 90"}
           [:g {:opacity 0.7}
            (->> c
                 (map-indexed (fn [i cont]
                                (j/assoc! cont :coordinates
                                          (clj->js
                                           (for [polygon (js->clj (.-coordinates cont))]
                                             (for [ring polygon]
                                               (for [[lng lat] ring]
                                                 [(lng-scale lng)
                                                  (lat-scale lat)])))))
                                (map
                                 (fn [polygon]
                                   (let [p #js {:type "Polygon" :coordinates polygon}]
                                     [:path.contour
                                      {:key [i polygon]
                                       :value (.-value cont)
                                       :on-click #(js/console.log
                                                   (.-value cont)
                                                   (.bounds path p)
                                                   (clj->js (map (fn [p] (.invert projection p)) (.bounds path p))))
                                       :d ((-> path2
                                               (.projection  (-> (d3-geo/geoMercator)
                                                                 (.translate #js [(* 480 (/ 180 960)) (* 250 (/ 180 960))])
                                                                 (.scale (* (.scale (d3-geo/geoMercator)) (/ 180 960))))))
                                           p)
                                       :fill (.interpolateMagma d3-scale-chromatic (/ i (count c)))}]))
                                 (.-coordinates cont)))))]]])])))

(dom/render
 [temperatur-contour]
 (js/document.getElementById "app"))
