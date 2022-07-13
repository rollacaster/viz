(ns shadow
  (:require [babashka.process :refer [process]]
            [shadow.cljs.devtools.api :as shadow]
            [shadow.cljs.devtools.server :as server]))

(defn -main [& _args]
  (server/start!)
  (shadow/watch :script)
  (process '[node target/script.js])
  (shadow/nrepl-select :script))
