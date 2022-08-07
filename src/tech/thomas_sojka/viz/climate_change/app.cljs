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
   {:data [[5 2 1 1 1 1] [2 2 1 1 1 1] [1 1 1 1 1 1]]
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

(defn contour [{:keys [data container-height palette style]}]
  (let [width (count (first data))
        height (count data)
        contours (-> (d3-contour/contours) (.size (clj->js [width height])))]
    [:svg.max-w-full.flex-1
     {:viewBox (str 0 " " 0 " " (count (first data)) " " (count data))
      :style (merge {:height container-height} style)}
     [:g
      (doall
       (map-indexed
        (fn
          [i t]
          [:path
           {:key i
            :d (path (.contour contours (clj->js (flatten data)) t))
            :fill (nth palette i)}])
        thresholds))]]))

(defn contour-playground []
  (let [inner-width (r/atom js/window.innerWidth)]
    (.addEventListener js/window "resize" (fn [] (reset! inner-width js/window.innerWidth)) true)
    (fn [{:keys [palette random-palette! add-row! add-column! data set-cell! randomize-weights!]}]
      (let [width (count (first data))
            height (count data)
            container-height 500]
        [:div
         [:div.mb-4
          [:div.flex
           [:div.flex.justify-center.items-center.w-full
            [:div.flex.flex-wrap.max-w-full.absolute
             (let [container-width (js/Math.min @inner-width (* container-height (/ width height)) 1024)]
               {:style {:height (js/Math.min 500 (* container-width (/ height width)))
                        :width container-width}})
             (map-indexed
              (fn [y row]
                (map-indexed
                 (fn [x cell]
                   [cell-button {:data data :cell cell :set-cell! set-cell! :x x :y y}])
                 row))
              data)]
            [contour {:data data :palette palette}]]
           [:button.text-3xl.bg-gray-500.w-10.hover:bg-gray-600.text-white.z-10.rounded-t-lg
            {:on-click add-column!}"+"]]
          [:div.flex.justify-center.items-center.z-10
           [:button.text-3xl.bg-gray-500.w-full.h-10.hover:bg-gray-600.text-white.rounded-l-lg.rounded-br.-lg
            {:on-click add-row!} "+"]]]
         [:button.bg-gray-500.text-white.py-2.px-3.rounded.shadow-lg.transition.hover:scale-105.delay-150.mr-2
          {:on-click randomize-weights!} "Randomize weights"]
         [:button.bg-gray-500.text-white.py-2.px-3.rounded.shadow-lg.transition.hover:scale-105.delay-150
          {:on-click random-palette!}
          "Randomize colors"]]))))

(defn headline-size [headline-element]
  [(.-width (.getBoundingClientRect headline-element))
   (.-height (.getBoundingClientRect headline-element))])


(defn headline []
  (let [headline-data (r/atom {:element nil})]
    (.addEventListener js/window "resize"
                       (fn []
                         (when (and @headline-data
                                    (not= (:size @headline-data) (headline-size (:element @headline-data))))
                           (swap! headline-data assoc :size (headline-size (:element @headline-data)))))
                       true)
    (fn []
      [:div.relative.mt-12.mb-3
       [:h1.inline-block.text-6xl.font-bold.tracking-wide.text-gray-900
        {:ref (fn [element]
                (when (and element (not= (:element @headline-data) element))
                  (reset! headline-data {:element element :size (headline-size element)})))}
        "d3-contour by example"]
       (when (:element @headline-data)
         (let [[width height] (:size @headline-data)
               resolution 150]
           [:div.absolute.top-0
            {:style {:width width :height height}}
            [contour {:style {:mix-blend-mode "lighten"}
                      :data
                      (->> (rand-int 10)
                           (for [_ (range (* resolution (/ width (+ width height))))])
                           vec
                           (for [_ (range (* resolution (/ height (+ width height))))])
                           vec)
                      :palette (nth palettes 96)}]]))])))

(defn app []
  (let [{:keys [data palette-idx]} @state]
    [:main.bg-gray-100.h-full
     [:section.max-w-5xl.mx-auto.text-gray-800.p-4
      [headline]
      [:p.mb-12 "Aliquam erat volutpat.  Nunc eleifend leo vitae magna.  In id erat non orci commodo lobortis.  Proin neque massa, cursus ut, gravida ut, lobortis eget, lacus.  Sed diam.  Praesent fermentum tempor tellus.  Nullam tempus.  Mauris ac felis vel velit tristique imperdiet.  Donec at pede.  Etiam vel neque nec dui dignissim bibendum.  Vivamus id enim.  Phasellus neque orci, porta a, aliquet quis, semper a, massa.  Phasellus purus.  Pellentesque tristique imperdiet tortor.  Nam euismod tellus id erat."]
      [:h2.text-3xl.mb-3 "Countour Playground"]
      [:p.mb-8 "Aliquam erat volutpat.  Nunc eleifend leo vitae magna.  In id erat non orci commodo lobortis.  Proin neque massa, cursus ut, gravida ut, lobortis eget, lacus.  Sed diam.  Praesent fermentum tempor tellus.  Nullam tempus.  Mauris ac felis vel velit tristique imperdiet.  Donec at pede.  Etiam vel neque nec dui dignissim bibendum.  Vivamus id enim.  Phasellus neque orci, porta a, aliquet quis, semper a, massa.  Phasellus purus.  Pellentesque tristique imperdiet tortor.  Nam euismod tellus id erat."]
      [contour-playground {:data data
                           :palette (nth palettes palette-idx)
                           :random-palette! #(swap! state assoc :palette-idx (rand-int (count palettes)))
                           :add-row! #(swap! state update :data add-row)
                           :add-column! #(swap! state update :data add-column)
                           :randomize-weights! #(swap! state update :data randomize)
                           :set-cell! (fn [x y value] (swap! state assoc-in [:data y x] (int value)))}]]]))

(dom/render
 [app]
 (js/document.getElementById "app"))


(defn main [])
