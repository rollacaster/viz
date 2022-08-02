(ns tech.thomas-sojka.viz.climate-change.app
  (:require
   ["d3-contour" :as d3-contour]
   ["d3-geo" :as d3-geo]
   ["nice-color-palettes" :as palettes]
   [reagent.core :as r]
   [reagent.dom :as dom]))

(defn add-row [data] (conj data (vec (repeat (count (first data)) 1))))
(defn add-column [data] (mapv #(conj % 1) data))

(defonce data
  (r/atom
   [[5 2 1][2 2 1][1 1 1] [1 1 1]]))

(def thresholds [1 2 4 6 8])

(defn randomize [data] (mapv (fn [row] (mapv (fn [_] (rand-int 10)) row)) data))

(def path (d3-geo/geoPath))
(def palette-idx (r/atom 19))

(defn contour-example [{:keys [add-row! add-column! data set-cell! update-data!]}]
  (let [contours (-> (d3-contour/contours)
                     (.size (clj->js [(count (first data)) (count data)])))]
    [:div
     [:div.mb-4 {:class "w-3/4"}
      [:div.flex.justify-between.items-stretch.w-full
       [:div.flex.flex-wrap.max-w-full.absolute
        {:style {:height 500}}
        (map-indexed
         (fn [y row]
           (map-indexed
            (fn [x cell]
              [:div.flex.flex-col.justify-center.items-center
               {:style {:width (str (* 100 (/ 1 (count (first data)))) "%")
                        :height (str (* 100 (/ 1 (count data))) "%")}}
               [:input.bg-transparent.text-center.max-w-full.hidden
                {:key [x y]
                 :value cell
                 :on-change (fn [e] (set-cell! x y ^js (.-target.value e)))}]
               [:input.max-w-full.hidden
                {:type "range"
                 :min 0
                 :max 9
                 :value cell
                 :on-change (fn [e] (set-cell! x y ^js (.-target.value e)))}]])
            row))
         data)]
       [:svg.max-w-full.flex-1
        {:viewBox (str 0 " " 0 " " (count (first data)) " " (count data))
         :style {:height 500}}
        [:g
         (doall
           (map-indexed
            (fn
              [i t]
              [:path
               {:key i
                :d (path (.contour contours (clj->js (flatten data)) t))
                :fill (nth (nth palettes @palette-idx) i)}])
            thresholds))]]
       [:button.text-3xl.bg-gray-400.w-10.hover:bg-gray-600.hover:text-white
        {:on-click add-column!}"+"]]
      [:div.flex.justify-center.items-center
       [:button.text-3xl.bg-gray-400.w-full.h-10.hover:bg-gray-600.hover:text-white
        {:on-click add-row!} "+"]]]
     [:button.bg-gray-400.mr-2 {:on-click #(update-data! randomize)} "Randomize weights"]
     [:button.bg-gray-400 {:on-click #(reset! palette-idx (rand-int (count palettes)))} "Randomize colors"]]))

(defn app []
  [:div
   [contour-example {:data @data
                     :add-row! #(swap! data add-row)
                     :add-column! #(swap! data add-column)
                     :update-data! (fn [f] (swap! data f))
                     :set-cell! (fn [x y value] (swap! data assoc-in [y x] (int value)))}]])

(dom/render
 [app]
 (js/document.getElementById "app"))


(defn main [])
