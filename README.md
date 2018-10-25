# Aplikacja internetowa w Clojure(Script)
*Niniejszy dokument prezentuje kolejne kroki w tworzeniu aplikacji internetowej z wykorzystaniem języka Clojure i ClojureScript. Aplikacja składa się z: frontendu napisanego za pomocą biblioteki Rum (wrapper ReactJS dla ClojureScript) i bibliotek pomocniczych, oraz backendu w formie REST Api.*

Do pełnego zrozumienia treści przedstawionej w artykule konieczna jest podstawowa znajomość zasad działania ReactJS oraz REST Api.

### Kody źródłowe

https://github.com/rilek/cljs-demo-app

### Wymagania
Do wykonania projektu konieczne jest zainstalowanie oprogramowania:
 - Leiningen ([Link]("https://leiningen.org/"))
 - PostgreSQL ([Link]("https://www.postgresql.org/"))

Projekt powinien dobrze działać na dystrybucjach Linuxa oraz macOS, nie ma gwarancji działania na Windowsie.

Wykorzystano również przykładową bazę danych DVDRental ([Link](http://www.postgresqltutorial.com/postgresql-sample-database/))

<!-- ### Parę słów o Clojure
Clojure to funkcyjny język programowania stworzy przez Richa Hickeya. Jest to dialekt Lispa z którym ma wiele wspólneg -->

### REPL
Po zainstalowaniu Leningen możliwe jest uruchomienie środowiska interaktywnego w terminalu za pomocą komendy: `lein repl`. Linie kodu w kolejnym rozdziale zaczynające się od `user=>` można uruchomić w środowisku i sprawidzić efekt. Linie z `  #_=> ` na ich początku są ciągiem dalszym instrukcji powyżej. Wspomnianych prefixów się nie wpisuje do linii poleceń.

### Parę słów o składni Clojure
Clojure to funkcyjny język programowania stworzy przez Richa Hickeya. Jest to dialekt Lispa z którym dzieli między innymi składnię. Oznacza to masę nawiasów, które przy pierwszej styczności są zupełnie nieczytelne, żeby po chwili przyzwyczajenia być jedną z najprzyjemniejszych i najpraktyczniejszych wśród języków programowania. Zasadniczą zależy w porównaniu do innych składni jest jej czytelność i jednoznaczność. Ze względu na swoją specyfikę (wszystko jest listą), korzysta się tutaj z tzw. "odwróconej notacji polskiej", czyli wywołanie funkcji jest listą, gdzie jej pierwszym elementem jest sama funkcja, a kolejnymi argumenty, np:
```javascript
// Javascript
2 + 3 * 4 + 5
19
```

```clojure
;; Clojure
(+ 2 (* 3 4) 5)
19
```

W Clojure istnieją stałe, oraz atomy - nie ma zmiennych działajych w sposób znany z innych języków. Atomy są to typy referencyjne, które, po przyjęciu pewnych (poniekąd błędnych) uproszczeń, można traktować jako zmienne. Ich zasadnicze cechy mają znaczenia w przypadku programów wielowątkowych, które nie wchodzą w zakres niniejszego poradnika. Stworzenie atomu odbywa się poprzez wywałanie funkcji `atom`.
#### Przypisanie wartości do nazw
Przypisanie wartości może obywać się albo globalnie
```clojure
user=> (def x 1)
#'user/x
user=> x
1
```
albo tworząc zakres lokalny - o czym za chwilę.

#### Atomy
Atomy tworzy się poprzez przypisanie do nazwy wartości zwracanej przez funkcję atom. Jej argumentem jest początkowa wartość atomu. Zmiana wartości odbywa się poprzez przypisanie (funkcja `reset!`), bądź zmianę (funkcja `swap!`). Dostęp do jego wartości uzyskać można dodając znak `@` przed nazwą.

```clojure
;; definiowanie
user=> (def x (atom nil))
#'user/x
user=> x
#object[clojure.lang.Atom 0x14c21098 {:status :ready, :val nil}]

;; przypisanie wartości
user=> (reset! x 10)
10
user=> x
#object[clojure.lang.Atom 0x14c21098 {:status :ready, :val 10}]
user=> @x
10

;; zmiana
user=> (swap! x + 10)
20
```
Po powyższym przykładzie wywnioskować można, że funkcja `reset!` przyjmuje jako argumenty atom oraz wartość do przypisania, natomiast `swap!` atom, funkcję zmieniającą, oraz kolejne parametry funkcji zmieniającej. Ostatni przykład jest równoważny z poniższym:
```clojure
user=> (reset! x (+ @x 10))
20
```

#### Zakres lokalny
```clojure
user=> (let [z 1]
#_=>     z)
1
user=> z
CompilerException java.lang.RuntimeException: Unable to resolve symbol: z in this context, compiling:(/tmp/form-init1150485929264120195.clj:1:1282)
user=> x
```
Zakres lokalny pozwala na lokalne zdefiniowane stałych. Zwraca on  wartość ostatniej operacji. Zakres tworzony jest poprzez słowo kluczone `let`. Następnie, w nawiasach kwadratowych, definiowane są stałe, a po nich występują instrukcje.

#### Funkcje
Funkcje podobnie jak zakresy zwracają jedynie wartość ostatniej operacji. Clojure pozwala na przysłanianie oraz dynamiczną liczbe parametrów. Można je definiować w formie nazwanej jak i anonimowej. Drugi przypadek posiada również nienazwane argumenty - są one reprezentowane poprzez znak `%` (drugi argument to `%2` itd.). Sama jej definicja to `#`, po którym występują nawiasy, w których umieszcozne są instrukcje. Definicja globalna polega na wywołaniu `defn`, po którym występuje nazwa, argumenty w nawiasach kwadratowych, a następnie instrukcje.

```clojure
;; funkcja nazwana
(defn funkcja [arg1 arg2]
  (+ arg1 arg2))

;; funkcja anonimowa
(def funkcja2 #(+ %1 %2))
```

#### Struktury danych
Wykorzystywane w projekcie struktury to głównie prymitywy, wektory oraz tablice asocjacyjne (hash-mapy).
```clojure
;; prymitiwy
1 "asd" 1.23 10e10

;; wektory
[] (vec) (vector 10 1 01)

;; hash-map
{:a 1 "b" 2}
```
Dostęp do konkretnych elementów wektora można uzyskać między innymi za pomocą funkcji `nth`. Argumentami są kolejno: struktura oraz nty element. Numeracja zaczyna się od zera.
```clojure
user=> (def x [1 2 3])
#'user/x
user=> (nth x 2)
3
```
Hash mapy zawierają pary `klucz-wartość`. Pierwszym może być albo string, liczba bądź klucz - specjalna wartość poprzedzona znakiem dwukropka `:`. Jeśli klucz nie jest instancją `key`, dostęp do wartości wymaga użycia funkcji `get`. W przeciwnym razie można wykorzystać klucz jako funkcję.
```clojure
user=> (def x {:a 1 "b" 2})
#'user/x
user=> (:a x)
1
user=> (get x "b")
2
```

### REST Api
#### Przygotowanie projektu
Do stworzenia szkieletu api wykorzystamy Leiningena. Taka generyczna aplikacja korzysta z `ring` jak serwera, oraz biblioteki `compojure` do routingu.
```sh
> lein new compojure api && cd api
```
Wygenerowany projekt ma następującą strukturę:
```
.
├── project.clj
├── README.md
├── resources
│   └── public
├── src
│   └── api
│       └── handler.clj
└── test
    └── api
        └── handler_test.clj
```
 - `project.clj` - jest plikiem konfiguracyjnym projektu. Są zawarte w nim informacje, zależności, pluginy i konfiguracje buildów.
 - `src/api/handler.clj` - kod źródłowy naszej aplikacji.
Pozostałe foldery/pliki nie mają znaczenia w realizacji projektu.

#### Uruchomienie
Serwer domyślnie działa na porcie 3000. Uruchomić go można komendą:
```clojure
lein ring server
```
Po pobraniu zależności i uruchomieniu, w domyślnej przeglądarce powinna się otworzyć karta z napisem `Hello World`. Teraz możemy przystąpić do edycji!

#### project.clj
Wygenerowany plik ma postać:
```clojure
(defproject api "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [compojure "1.6.1"]
                 [ring/ring-defaults "0.3.2"]]
  :plugins [[lein-ring "0.12.4"]]
  :ring {:handler api.handler/app}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring/ring-mock "0.3.2"]]}})

```
Fragment, który będzie nas interesował jest pod kluczem `:dependencies`. Należy dodać kolejne zależności:
```clojure
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [compojure "1.6.1"]
                 [ring/ring-json "0.4.0"]
                 [ring/ring-defaults "0.3.2"]
                 [ring-cors "0.1.12"]
                 [clojure.jdbc/clojure.jdbc-c3p0 "0.3.3"]
                 [org.postgresql/postgresql "42.1.4"]
                 [com.h2database/h2 "1.3.168"]
                 [cheshire "5.8.1"]
                 [oksql "1.2.1"]
                 [org.clojure/java.jdbc "0.7.8"]]
```
