(ns tech.thomas-sojka.viz.climate-change.app
  (:require
   ["d3-contour" :as d3-contour]
   ["d3-geo" :as d3-geo]
   ["nice-color-palettes" :as palettes]
   [reagent.core :as r]
   [reagent.dom :as dom]))

(defn add-row [data] (conj data (vec (repeat (count (first data)) 1))))
(defn add-column [data] (mapv #(conj % 1) data))

(defonce state
  (r/atom
   {:data [[5 2 1][2 2 1][1 1 1] [1 1 1]]
    :palette-idx 19}))

(def thresholds [1 2 4 6 8])

(defn randomize [data] (mapv (fn [row] (mapv (fn [_] (rand-int 10)) row)) data))

(def path (d3-geo/geoPath))

(defn cell-button []
  (let [visible (r/atom false)]
    (fn [{:keys [data x y cell set-cell!]}]

      [:div.flex.justify-center.items-center.px-4
       {:class (if @visible "opacity-100" "opacity-0")
        :on-mouse-enter #(reset! visible true)
        :on-mouse-move #(reset! visible true)
        :on-mouse-leave #(reset! visible false)
        :style {:width (str (* 100 (/ 1 (count (first data)))) "%")
                :height (str (* 100 (/ 1 (count data))) "%")}}
       [:button.bg-gray-100.w-4.h-4.leading-none.rounded-full.font-bold
        {:type "button"
         :value cell
         :on-click (fn [] (set-cell! x y (js/Math.max 0 (dec cell))))}
        "-"]
       [:input.bg-transparent.text-center.flex-1
        {:key [x y]
         :style {:max-width 20}
         :value cell
         :on-change (fn [e] (set-cell! x y ^js (.-target.value e)))}]
       [:button.bg-gray-100.w-4.h-4.leading-none.rounded-full.font-bold
        {:type "button"
         :value cell
         :on-click (fn []
                     (set-cell! x y (js/Math.min 9 (inc cell))))}
        "+"]])))

(defn contour-playground [{:keys [palette-idx set-palette-idx! add-row! add-column! data set-cell! update-data!]}]
  (let [width (count (first data))
        height (count data)
        contours (-> (d3-contour/contours)
                     (.size (clj->js [width height])))
        contour-height 500]
    [:div
     [:div.mb-4 {:class "w-3/4"}
      [:div.flex
       [:div.flex.justify-center.items-center.w-full
        [:div.flex.flex-wrap.max-w-full.absolute
         {:style {:height (js/Math.min 500 (* (* js/window.innerWidth 0.75) (/ height width)))
                  :width (js/Math.min (* js/window.innerWidth 0.75) (* contour-height (/ width height)))}
          :class "w-3/4"}
         (map-indexed
          (fn [y row]
            (map-indexed
             (fn [x cell]
               [cell-button {:data data :cell cell :set-cell! set-cell! :x x :y y}])
             row))
          data)]
        [:svg.max-w-full.flex-1
         {:viewBox (str 0 " " 0 " " (count (first data)) " " (count data))
          :style {:height contour-height}}
         [:g
          (doall
           (map-indexed
            (fn
              [i t]
              [:path
               {:key i
                :d (path (.contour contours (clj->js (flatten data)) t))
                :fill (nth (nth palettes palette-idx) i)}])
            thresholds))]]]
       [:button.text-3xl.bg-gray-400.w-10.hover:bg-gray-600.hover:text-white.z-10
        {:on-click add-column!}"+"]]
      [:div.flex.justify-center.items-center.z-10
       [:button.text-3xl.bg-gray-400.w-full.h-10.hover:bg-gray-600.hover:text-white
        {:on-click add-row!} "+"]]]
     [:button.bg-gray-400.mr-2 {:on-click #(update-data! randomize)} "Randomize weights"]
     [:button.bg-gray-400 {:on-click #(set-palette-idx! (rand-int (count palettes)))} "Randomize colors"]]))

(defn app []
  [:div
   (let [{:keys [data palette-idx]} @state]
     [contour-playground {:data data
                          :palette-idx palette-idx
                          :set-palette-idx! #(swap! state assoc :palette-idx %)
                          :add-row! #(swap! state update :data add-row)
                          :add-column! #(swap! state update :data add-column)
                          :update-data! (fn [f] (swap! state update :data f))
                          :set-cell! (fn [x y value] (swap! state assoc-in [:data y x] (int value)))}])])

(dom/render
 [app]
 (js/document.getElementById "app"))


(defn main [])
