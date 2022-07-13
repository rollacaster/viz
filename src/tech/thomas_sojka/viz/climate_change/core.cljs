(ns tech.thomas-sojka.viz.climate-change.core
  (:require ["fs" :as fs]
            ["netcdfjs" :as netcdfjs]
            [cljs-bean.core :refer [bean ->clj]]))

(defn main [& _cli-args]
  (prn "hello world"))

(def reader
  (new netcdfjs/NetCDFReader
       (fs/readFileSync "../resources/gistemp250_GHCNv4.nc")))
(comment
  (keys (->clj reader))
  (map (comp :name bean) (:variables (bean (:header (bean reader)))))
  (map (comp bean) (:variables (bean (:header (bean reader)))))
  (mapv (comp #(select-keys % [:name :offset]) bean) (:variables (bean (:header (bean reader)))))
  [{:name "lat", :offset 1100}
   {:name "lon", :offset 1460}
   {:name "time", :offset 2180}
   {:name "time_bnds", :offset 9016}
   {:name "tempanomaly", :offset 22688}]
  (take 10 (.getDataVariable reader "tempanomaly"))
  (take 10 (.getDataVariable reader "time"))
  (take 10 (.getDataVariable reader "time_bnds"))

  (.-dimensions reader)
  (.-recordDimension reader)
  (first (.-variables reader))
  (.getDataVariableAsString reader "lat")
  (.-globalAttributes reader)
  (.getAttribute reader "tempanomaly")
  (bean (:buffer (bean reader)))
  ;; https://help.marine.copernicus.eu/en/articles/5470092-how-to-use-add_offset-and-scale_factor-values
  ;; Real_Value = (Display_Value X scale_factor) + add_offset


  )
