(ns sfmuni.web
  (:use [clojure.contrib.str-utils :only [re-sub re-gsub]])
  (:use [ring.util.codec :only [url-encode url-decode]])
  (:require [compojure.core :refer [defroutes GET PUT POST DELETE ANY]]
            [compojure.handler :refer [site]]
            [compojure.route :as route]
            [clojure.java.io :as io]
            [ring.middleware.stacktrace :as trace]
            [ring.middleware.session :as session]
            [ring.middleware.session.cookie :as cookie]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.basic-authentication :as basic]
            [cemerick.drawbridge :as drawbridge]
            [environ.core :refer [env]]
            [clojure.data.json :as json]
            [net.cgrand.enlive-html :as html]
            [clj-http.client :as client]
            [clojure.string :as string]
  )
)

(defn- authenticated? [user pass]
  ;; TODO: heroku config:add REPL_USER=[...] REPL_PASSWORD=[...]
  (= [user pass] [(env :repl-user false) (env :repl-password false)]))

(def ^:private drawbridge
  (-> (drawbridge/ring-handler)
      (session/wrap-session)
      (basic/wrap-basic-authentication authenticated?)))

(defn json-response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/json"}
   :body (json/write-str data)})


(defn get-stop-list []
   (json-response [
      {:title "3rd St &amp; Mission St" :lat "37.7863699" :lng "-122.4021099" :stopTag "3136" :stopId "13136"}
    ])
)

(defn get-dummy-prediction-map []
     {:tag :predictions :attrs {:stoptag 3136 :stoptitle "3rd St & Mission St" :routetag "30" :routetitle "30-Stockton" :agencytitle "San Francisco Muni"} :content [
       {:tag :direction :attrs {:title "Outbound to the Marina District"} :content [
         {:tag :prediction :attrs {:triptag "5230847" :block "3003" :vehicle 5570 :dirtag "30_OB3" :affectedbylayover true :isdeparture false :minutes 14 :seconds 871 :epochtime 1361250553707} :content nil} 
         {:tag :prediction :attrs {:triptag "5230848" :block "3011" :vehicle 5495 :dirtag "30_OB3" :affectedbylayover true :isdeparture false :minutes 32 :seconds 1951 :epochtime 1361251633707} :content nil} 
         {:tag :prediction :attrs {:triptag "5230848" :block "3011" :vehicle 5510 :dirtag "30_OB3" :affectedbylayover true :isdeparture false :minutes 41 :seconds 2516 :epochtime 1361252198287} :content nil} 
         {:tag :prediction :attrs {:triptag "5230850" :block "3023" :vehicle 5481 :dirtag "30_OB3" :affectedbylayover true :isdeparture false :minutes 71 :seconds 4291 :epochtime 1361253973707} :content nil} 
         {:tag :prediction :attrs {:triptag "5230851" :block "3009" :vehicle 5549 :dirtag "30_OB3" :affectedbylayover true :isdeparture false :minutes 91 :seconds 5491 :epochtime 1361255173707} :content nil} 
      ]}]} 
)

(defn get-dummy-stops-for-stop-name [stop-name]
  [{:routeTag "30" :tag "3941" :title "Chestnut St &amp; Fillmore St" :lat "37.8009099" :lon "-122.43618" :stopId "13941"}]
)

(defn get-parsed-html [url]
  ;(html/html-resource ((java.net.URL. url))
  ;(-> url client/get :body (partial re-gsub #"\n" "") java.io.StringReader. html/html-resource)
  ;(-> url client/get :body java.io.StringReader. html/html-resource)
  (html/html-resource 
    (java.io.StringReader. 
      (re-gsub #"[ \f\r\t\v]*\n[ \f\r\t\v]*" ""  ; strip out whitespace/newlines between tags
        (:body (client/get url)))))
)

(defn get-stops-for-stop-name [stop-name]
  (let [dom (html/html-resource (io/reader (io/resource "allRoutes.xml")))]
    (reduce (fn [existigStopList routeNode]
      (concat existigStopList 
        (map #(merge {:routeTag (:tag (:attrs routeNode))} %)
          (filter 
            #(= (:title (:attrs %)) stop-name)
            (html/select routeNode [[:stop (html/attr? :title)]]))))
      )
      '()
      (html/select dom [:route])
    )
  )
)

(defn get-url-params-for-stop-maps [stop-maps]
  (string/join "&" (map #(str "stops=" (url-encode (str (:routeTag %) "|" (:tag (:attrs %))))) stop-maps))
)

(defn get-stop-prediction [stop-name]
  (json-response 
    (let [prediction-url (str "http://webservices.nextbus.com/service/publicXMLFeed?command=predictionsForMultiStops&a=sf-muni&"
                                (get-url-params-for-stop-maps (get-stops-for-stop-name stop-name)))]
      ;(println stop-name)
      ;(println prediction-url)
      (-> (get-parsed-html prediction-url) first :content first :content)
      ;(get-dummy-prediction-map)
    )
  )
)

(defroutes app
  (ANY "/repl" {:as req}
       (drawbridge req))
  (GET "/" []
       {:status 200
        :headers {"Content-Type" "text/plain"}
        :body (pr-str ["Hello" :from 'Heroku])})
  (GET "/get-stops" [] (get-stop-list))
  (GET "/get-predictions-for-stop" [name] (get-stop-prediction (url-decode name)))
  (ANY "*" []
       (route/not-found (slurp (io/resource "404.html")))))

(defn wrap-error-page [handler]
  (fn [req]
    (try (handler req)
         (catch Exception e
           {:status 500
            :headers {"Content-Type" "text/html"}
            :body (slurp (io/resource "500.html"))}))))

(defn -main [& [port]]
  (let [port (Integer. (or port (env :port) 5000))
        ;; TODO: heroku config:add SESSION_SECRET=$RANDOM_16_CHARS
        store (cookie/cookie-store {:key (env :session-secret)})]
    (jetty/run-jetty (-> #'app
                         ((if (env :production)
                            wrap-error-page
                            trace/wrap-stacktrace))
                         (site {:session {:store store}}))
                     {:port port :join? false})))

;; For interactive development:
;; (.stop server)
;; (def server (-main))

; "3rd St & Mission St"
; http://localhost:5000/get-predictions-for-stop?name=3rd%20St%20%26%20Mission%20St
