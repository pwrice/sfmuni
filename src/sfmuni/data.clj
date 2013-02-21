(ns sfmuni.data
    (:refer-clojure :exclude [==])
    (use [clojure.core.logic])
    (:require [clj-http.client :as client]
              [clojure.java.io :as io]
              [net.cgrand.enlive-html :as html]
              ))


(defn nodes-exist-in-listo [condition nodes out]
  (fresh [node remaining-nodes]
    (conso node remaining-nodes nodes)
    (conde
        [(condition node) (== node out)]
        [(nodes-exist-in-listo condition remaining-nodes out)])))

(defn has-tagnameo [node tagname]
    (== (partial-map {:tag tagname}) node))


(defn has-contento [node content]
  (featurec node {:content content})
)

(defn get-stop-names [stop-name dom]
    (run* [q]
      (let [stop-nodes (html/select dom [:header :stop])]
        (nodes-exist-in-listo #(has-contento % (list stop-name)) stop-nodes q)
      )
    )
)


; (defn get-stops []
;     (let [dom (html/html-resource (io/reader (io/resource "schedule30.xml")))]
;         (get-stop-names "Divisadero St & Chestnut St" dom)
;     )
; )

(defn get-predictions []
  (let [dom (html/html-resource (io/reader (io/resource "multiPrediction.xml")))]
    (print dom))
)


(defn get-stops-for-stopname [stopName]
  (let [dom (html/html-resource (io/reader (io/resource "allRoutes.xml")))]
    (reduce (fn [existigStopList routeNode]
      (concat existigStopList 
        (map #(merge {:routeTag (:tag (:attrs routeNode))} %)
          (filter 
            #(= (:title (:attrs %)) "3rd St & Mission St") 
            (html/select routeNode [[:stop (html/attr? :title)]]))))
      )
      '()
      (html/select dom [:route])
    )
  )
)
