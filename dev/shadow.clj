(ns shadow
  (:require
   [babashka.process :refer [process]]
   [clojure.java.browse :as browse]
   [shadow.cljs.devtools.api :as shadow]
   [shadow.cljs.devtools.server :as server]))

(defn -main [& _args]
  (server/start!)
  (shadow/watch :climate-change)
  (shadow/watch :contour-by-example)
  (process '[npx run dev])
  (shadow/nrepl-select ::climate-change)
  (browse/browse-url "http://localhost:9101"))
