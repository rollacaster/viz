(ns tech.thomas-sojka.viz.climate-change.app
  (:require
   ["d3-contour" :as d3-contour]
   ["d3-geo" :as d3-geo]
   [reagent.core :as r]
   [reagent.dom :as dom]))

(defn add-row [data] (conj data (vec (repeat (count (first data)) 1))))
(defn add-column [data] (mapv #(conj % 1) data))

(defonce data
  (r/atom
   [[5 2 1][2 2 1][1 1 1] [1 1 1]]))

(def path (d3-geo/geoPath))

(defn contour-example [{:keys [add-row! add-column! data set-data!]}]
  (let [contours (-> (d3-contour/contours)
                     (.size (clj->js [(count (first data)) (count data)])))]
    [:div
     {:style {:display "flex" :align-items "center"}}
     [:div
      [:div
       [:div {:style {:position "absolute"
                      :display "flex"
                      :flex-wrap "wrap"
                      :width (* (count (first data)) (/ 500 (count data)))
                      :max-width "100%"
                      :height 500}}
        (map-indexed
         (fn [y row]
           (map-indexed
            (fn [x cell]
              [:div
               {:style {:display "flex"
                        :flex-direction "column"
                        :justify-content "center"
                        :align-items "center"
                        :width (str (* 100 (/ 1 (count (first data)))) "%")
                        :height (str (* 100 (/ 1 (count data))) "%")}}
               [:input {:style {:background "transparent"
                                :padding 0
                                :border 0
                                :width "100%"
                                :text-align "center"}
                        :key [x y]
                        :value cell
                        :on-change (fn [e] (set-data! x y ^js (.-target.value e)))}]
               [:input {:type "range"
                        :min -10
                        :max 10
                        :value cell
                        :on-change (fn [e] (set-data! x y ^js (.-target.value e)))
                        :style {:width "50%"}}]])
            row))
         data)]
       [:svg
        {:viewBox (str 0 " " 0 " " (count (first data)) " " (count data))
         :style {:height 500 :max-width "100%"}}
        [:g
         (map-indexed
          (fn
            [i t]
            [:path
             {:key i
              :d (path (.contour contours (clj->js (flatten data)) t))
              :fill (nth ["green" "yellow" "red" "orange" "blue" "pink" "gold"] i)}])
          [1 2 3])]]]
      [:div {:style {:display "flex" :justify-content "center"}}
       [:button {:style {:font-size "3rem" :width "4rem" :height "4rem" :border-radius 20 :border 0}
                 :on-click add-row!}"+"]]]
     [:button {:style {:font-size "3rem" :width "4rem" :height "4rem" :border-radius 20 :border 0}
               :on-click add-column!}"+"]]))

(defn app []
  [:div {:style {:margin "5rem"
                 :position "relative"}}
   [contour-example {:data @data
                     :add-row! #(swap! data add-row)
                     :add-column! #(swap! data add-column)
                     :set-data! (fn [x y value]
                                  (prn x y (int value))
                                  (swap! data assoc-in [y x] (int value)))}]])

(dom/render
 [app]
 (js/document.getElementById "app"))


(defn main [])
