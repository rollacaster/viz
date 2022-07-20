(ns tech.thomas-sojka.viz.climate-change.core
  (:require ["fs" :as fs]
            ["netcdfjs" :as netcdfjs]
            [cljs-bean.core :refer [bean ->clj]]))

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

(def reader
  (new netcdfjs/NetCDFReader
       (fs/readFileSync "./resources/gistemp250_GHCNv4.nc")))

(defn temp-anamoly-for-time [point-in-time]
  (for [[i anamoly] (->> (.getDataVariable reader "tempanomaly")
                         (take (* 90 180 point-in-time))
                         (map-indexed vector))
        :let [lat (mod i 90)
              lon (Math/floor (/ i 180))]
        :when (not= anamoly (fill-value reader :tempanomaly))]
    {:anomaly (* anamoly (scale-factor reader :tempanomaly))
     :lat lat
     :lon lon}))

(defn main [& _cli-args]
  (prn reader))

(comment
  (fs/writeFileSync "resources/data.edn" (prn-str (vec (temp-anamoly-for-time 1))))
  (variables reader))
