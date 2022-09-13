(ns tech.thomas-sojka.viz.climate-change.data
  (:require ["netcdfjs" :as netcdfjs]
            [cljs-bean.core :refer [bean ->clj]]))

(defn read [file]
  (new netcdfjs/NetCDFReader file))

(defn index-by [f coll]
  (-> (group-by f coll)
      (update-vals #(dissoc (first %) f))
      (update-keys keyword)))

(defn variable-attributes [reader key]
  (let [variableAttributes (index-by :name (->clj ^js (.-variables reader)))]
    (index-by :name (:attributes (key variableAttributes)))))

(defn fill-value [reader key]
  (let [variable-attributes (variable-attributes reader key)]
    (-> variable-attributes :_FillValue :value)))

(defn scale-factor [reader key]
  (let [variable-attributes (variable-attributes reader key)]
    (-> variable-attributes :scale_factor :value)))

(defn variables [reader]
  (->> (:variables (bean (:header (bean reader))))
       ->clj
       (index-by :name)))

(defn temp-anamoly-for-time [reader point-in-time]
  (for [[i anamoly] (->> ^js (.getDataVariable reader "tempanomaly")
                         (drop (* 90 180 point-in-time))
                         (take (* 90 180))
                         (map-indexed vector))
        :let [lat (mod i 90)
              lon (Math/floor (/ i 90))]
        :when (not= anamoly (fill-value reader :tempanomaly))]
    {:anomaly (* anamoly (scale-factor reader :tempanomaly))
     :lat (nth ^js (.getDataVariable reader "lat") lat)
     :lon (nth ^js (.getDataVariable reader "lon") lon)}))

(comment)
