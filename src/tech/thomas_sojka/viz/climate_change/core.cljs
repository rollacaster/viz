(ns tech.thomas-sojka.viz.climate-change.core
  (:require ["fs" :as fs]
            ["netcdfjs" :as netcdfjs]
            [tech.thomas-sojka.viz.climate-change.data :as data]))

(def reader
  (new netcdfjs/NetCDFReader
       (fs/readFileSync "./resources/gistemp250_GHCNv4.nc")))

(comment
  (fs/writeFileSync "resources/public/data/climate-change/1880.edn" (prn-str (vec (data/temp-anamoly-for-time reader 1))))
  (data/variables reader))
