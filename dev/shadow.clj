(ns shadow
  (:require
   [babashka.process :refer [process]]
   [clojure.java.browse :as browse]
   [shadow.cljs.devtools.api :as shadow]
   [shadow.cljs.devtools.server :as server]))

(defn -main [& _args]
  (server/start!)
  (shadow/watch :script)
  (shadow/watch :app)
  (process '[node target/script.js])
  (process '[npx run dev])
  (shadow/nrepl-select :script)
  (browse/browse-url "http://localhost:9101"))
