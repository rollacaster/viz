(ns tech.thomas-sojka.viz.climate-change.data
  (:require ["lodash" :as lodash]
            ["netcdfjs" :as netcdfjs]))

(defn read [file]
  (new netcdfjs/NetCDFReader file))

(defn partition-tempanamoly [reader]
  (lodash/chunk
   (.map
    ^js (.getDataVariable reader "tempanomaly")
    (fn [v] (if (= v 32767) 0 (* v 0.01))))
   (* 90 180)))
