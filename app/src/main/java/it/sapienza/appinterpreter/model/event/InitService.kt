package it.sapienza.appinterpreter.model.event

/**
 * Serve per riempire le liste con dati paginati
 */
class InitService(
    val url : String, //nell url è obbligatoria la presenza di ${page} che consentere di ripetere la chiamata con un numero di pagina diverso
    var startPage : Int = 0, //la pagina da cui partire
    var pageEnd : Int?, //la pagina a cui arrivare
    var pageCountMapping : String?, //il mapping del numero di pagine presenti
    val method : CallServiceMethod? = CallServiceMethod.get
) : Event {

}