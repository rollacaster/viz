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
   {:data [[5 2 1 1 1 1 1 1] [2 2 1 1 1 1 1 1] [1 1 1 1 1 1 1 1] [1 1 1 1 1 1 1 1]]
    :palette-idx 96}))

(def thresholds [1 2 4 6 8])

(defn randomize [data] (mapv (fn [row] (mapv (fn [_] (rand-int 10)) row)) data))

(def path (d3-geo/geoPath))

(defn cell-button [{:keys [hoverable? class]}]
  (let [visible (r/atom (if hoverable? false true))]
    (fn [{:keys [data x y cell set-cell!]}]
      (let [width (count (first data))
            height (count data)]
        [:div.flex.justify-center.items-center.px-4
         {:class [(if @visible "opacity-100" "opacity-0") class]
          :on-mouse-enter #(when hoverable? (reset! visible true))
          :on-mouse-move  #(when hoverable? (reset! visible true))
          :on-mouse-leave #(when hoverable? (reset! visible false))
          :style {:width (str (* 100 (/ 1 width)) "%")
                  :height (str (* 100 (/ 1 height)) "%")}}
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
          "+"]]))))

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

(defn dataset []
  (let [inner-width (r/atom js/window.innerWidth)]
    (.addEventListener js/window "resize" (fn [] (reset! inner-width js/window.innerWidth)) true)
    (fn [{:keys [data container-height class]} children]
      (let [width (count (first data))
            height (count data)]
        [:div.flex.flex-wrap.max-w-full
         (let [container-width (js/Math.min @inner-width (* container-height (/ width height)) 1024)]
           {:style {:height (js/Math.min 500 (* container-width (/ height width)))
                    :width container-width}
            :class class})
         (map-indexed
          (fn [y row]
            (map-indexed
             (fn [x cell]
               ^{:key [x y]}
               (children {:x x :y y :cell cell}))
             row))
          data)]))))

(defn contour-playground []
  (let [inner-width (r/atom js/window.innerWidth)]
    (.addEventListener js/window "resize" (fn [] (reset! inner-width js/window.innerWidth)) true)
    (fn [{:keys [palette random-palette! data set-cell! randomize-weights! container-height]}]
      [:div
       [:div.mb-4
        [:div.flex
         [:div.flex.justify-center.items-center.w-full
          [dataset {:data data  :container-height container-height :class "absolute"}
           (fn [{:keys [x y cell]}]
             [cell-button {:x x :y y :cell cell :data data :set-cell! set-cell!  :hoverable? true}])]
          [contour {:data data :palette palette :container-height container-height}]]]]
       [:button.bg-gray-500.text-white.py-2.px-3.rounded.shadow-lg.transition.hover:scale-105.delay-150.mr-2
        {:on-click randomize-weights!} "Randomize weights"]
       [:button.bg-gray-500.text-white.py-2.px-3.rounded.shadow-lg.transition.hover:scale-105.delay-150
        {:on-click random-palette!}
        "Randomize colors"]])))

(defn headline-size [headline-element]
  (let [bbox (.getBoundingClientRect headline-element)]
    [(.-width bbox) (.-height bbox)]))

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

(defn section [{:keys [title]} children]
  [:<>
   [:h2.text-3xl.mb-3 title]
   [:div.mb-8
    children]])

(defn threshold-layers [{:keys [data palette-idx]}]
  [:div.flex
   (let [width (count (first data))
         height (count data)
         contours (-> (d3-contour/contours) (.size (clj->js [width height])))]
     (map-indexed
      (fn
        [i t]
        [:div {:class "w-1/5"
               :key i
               :style {:left (str (* i (* (/ 1 5) 100)) "%")
                       :transition "all 1s"}}
         [:svg.border
          {:viewBox (str 0 " " 0 " " width " " height)}
          [:path
           {:d (path (.contour contours (clj->js (flatten data)) t))
            :fill (get (nth palettes palette-idx) i)}]]
         [:div (str "Threshold: " t)]])
      thresholds))])

(defn app []
  (let [{:keys [data palette-idx]} @state
        container-height 500
        set-cell! (fn [x y value] (swap! state assoc-in [:data y x] (int value)))]
    [:main.bg-gray-100.h-full
     [:section.max-w-5xl.mx-auto.text-gray-800.p-4
      [headline]
      [:p.mb-12 "Aliquam erat volutpat.  Nunc eleifend leo vitae magna.  In id erat non orci commodo lobortis.  Proin neque massa, cursus ut, gravida ut, lobortis eget, lacus.  Sed diam.  Praesent fermentum tempor tellus.  Nullam tempus.  Mauris ac felis vel velit tristique imperdiet.  Donec at pede.  Etiam vel neque nec dui dignissim bibendum.  Vivamus id enim.  Phasellus neque orci, porta a, aliquet quis, semper a, massa.  Phasellus purus.  Pellentesque tristique imperdiet tortor.  Nam euismod tellus id erat."]
      [section {:title  "Create a dataset"}
       [dataset {:data data  :container-height container-height}
        (fn [{:keys [x y cell]}]
          [cell-button {:x x :y y :cell cell :data data :set-cell! set-cell! :class "border"}])]]
      [section {:title "Create contour generator"}
       [:pre "d3.contours().size([width, height])"]]
      [section {:title "Create a geographic path generator"}
       [:pre "d3.geoPath()"]]
      [section {:title "Define thresholds"}
       [:pre "[1 2 4 6 8]"]]
      [section {:title "Render a path for each threshold"}
       [threshold-layers {:data data :palette-idx palette-idx}]]
      [section {:title "Countour Playground"}
       [:<>
        [:p.mb-8 "Aliquam erat volutpat.  Nunc eleifend leo vitae magna.  In id erat non orci commodo lobortis.  Proin neque massa, cursus ut, gravida ut, lobortis eget, lacus.  Sed diam.  Praesent fermentum tempor tellus.  Nullam tempus.  Mauris ac felis vel velit tristique imperdiet.  Donec at pede.  Etiam vel neque nec dui dignissim bibendum.  Vivamus id enim.  Phasellus neque orci, porta a, aliquet quis, semper a, massa.  Phasellus purus.  Pellentesque tristique imperdiet tortor.  Nam euismod tellus id erat."]
        [contour-playground {:data data
                             :palette (nth palettes palette-idx)
                             :random-palette! #(swap! state assoc :palette-idx (rand-int (count palettes)))
                             :randomize-weights! #(swap! state update :data randomize)
                             :set-cell! set-cell!
                             :container-height container-height}]]]]]))

(defn main []
  )

(comment
  (swap! state update :data add-row)
  (swap! state update :data add-column))

(dom/render
 [app]
 (js/document.getElementById "app"))
