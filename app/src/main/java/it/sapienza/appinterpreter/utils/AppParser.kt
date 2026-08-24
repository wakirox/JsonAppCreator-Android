package it.sapienza.appinterpreter.utils

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import it.sapienza.appinterpreter.model.Layout
import it.sapienza.appinterpreter.model.ModelApplication
import it.sapienza.appinterpreter.model.event.*
import it.sapienza.appinterpreter.model.view_model.ListView
import it.sapienza.appinterpreter.model.view_model.helper.MView


class AppParser(val obj : String) {

    inline fun<T> measureTimeMillisPair(function: () -> T): T {
        val startTime = System.currentTimeMillis()
        val result: T = function.invoke()
        val endTime = System.currentTimeMillis()

        return result
    }

    fun parseApplication2() : ModelApplication {

        /*
        * per facilitare la scrittura del modello si è deciso di dare la possibilità di riutilizzare determinati elementi
        * all'iterno del modello specificando solamente l'id di un elemento è possibile referenziarlo dalla lista generale dell'app
        * Per evitare problemi con la scrittura non è
        * possibile referenziare un elemento che si trova definito all'interno di un altro elemento*/

        val mapper = jacksonObjectMapper()
        val tree = try {
            mapper.readTree(obj)
        } catch (error: Exception) {
            throw ConfigurationValidationException("$", "invalid JSON: ${error.message}")
        }
        ReferenceValidator().validate(tree)

        val model = try {
            measureTimeMillisPair { mapper.readValue<ModelApplication>(obj) }
        } catch (error: ConfigurationValidationException) {
            throw error
        } catch (error: Exception) {
            throw ConfigurationValidationException("$", "configuration cannot be parsed: ${error.message}")
        }

        val declaredActions = model.actions.toList()
        val declaredViews = model.views.toList()

        declaredActions.forEach { it.event?.let { event -> findView(event, model) } }
        declaredViews.forEach { findGenericView(it, model, addSelf = false) }
        if (!model.mainView.isReference()) findGenericView(model.mainView, model)

//        model.initService?.let {
//            it.thenDo?.eventInstance?.let { ev -> findScreens(ev, model) }
//        }

        checkModelConsistency(model)

        return model

        //}
    }


//    fun parseApplication() : ModelApplication {
//
//        /*
//        * per facilitare la scrittura del modello si è deciso di dare la possibilità di riutilizzare determinati elementi
//        * all'iterno del modello specificando solamente l'id di un elemento è possibile referenziarlo dalla lista generale dell'app
//        * Per evitare problemi con la scrittura non è
//        * possibile referenziare un elemento che si trova definito all'interno di un altro elemento*/
//
//        val mapper = jacksonObjectMapper()
//        val model = mapper.readValue<ModelApplication>(obj)
//
//
//
////        model.initService?.let {
////            it.thenDo?.eventInstance?.let { ev -> analyzeEvent(ev, model) }
////        }
//
//        model.main.takeIf { !it.isEmpty() }?.let {
//            model.main = it.toReference()
//            model.screens.add(it)
//        }
//
//        model.screens.forEach {
//            model.layouts.addAll(it.layouts.filter { l->!l.isEmpty() })
//        }
//
//        model.layouts.iterator().forEach {
//            it.views.forEach { v ->
//                v.action?.event?.let {
//                    it.eventInstance?.let { ev -> analyzeEvent(ev, model) }
//                }
//            }
//        }
//
//        var toAddLayout = mutableListOf<Layout>()
//        model.layouts.forEach {
//            //converto le view in realtà
//            it.views.forEach{
//
//                if(it is ListView){
//                    toAddLayout.add(it.layout)
//                }
//
//                it.action?.takeIf { !it.isEmpty() }?.let {
//                    model.actions.add(it)
//                }
//            }
//
//        }
//
//        model.layouts.addAll(toAddLayout)
//
//        checkModelConsistency(model)
//
//        model.actions.filter { e->e.event?.eventInstance is ShowView}.onEach {
////            var ev = (it.event?.eventInstance as ShowScreen)
////            if(ev.screen.isEmpty() ){
////                ev.screen = model.screens.find { e->e.id == ev.screen.id }!!
////            }
//        }
//
//        //setto le azioni qualora siano solamente riferimenti
//        model.layouts.forEach { it.views.forEach{
//            it.action?.let { ac ->
//                if (ac.isEmpty()) {
//                    it.action = model.actions.find { ai -> ai.id == ac.id }!!
//                }
//            }
//        } }
//
//
////        if(model.main.isEmpty()){
////            model.main = model.screens.find { it.id == model.main.id }!!
////        }
//
//        return model
//    }

    private fun findView(
        ev: Event,
        model: ModelApplication
    ) {
        when(ev){
            is ShowView -> if(!ev.view.isReference()){
                findGenericView(ev.view, model)
            }
            is RESTService -> {
                ev.thenDo?.let { findView(it,model) }
            }
            is AlertMessage -> {
                ev.thenDoOK?.let { findView(it,model) }
                ev.thenDoKO?.let { findView(it,model) }
            }
        }
    }

    private fun findGenericView(
        view: MView,
        model: ModelApplication,
        addSelf: Boolean = true
    ) {
        when(view){
            is ListView -> {
                findGenericView(view.itemView,model)
            }
            is Layout -> {
                view.views.forEach {
                    findGenericView(it, model)
                }
            }
        }

        view.action?.event?.let {
            findView(it, model)
        }
        view.action?.takeUnless { it.isReference() }?.let { action ->
            if (model.actions.none { it.id == action.id }) model.actions.add(action)
        }

        if (addSelf && !view.isReference()) {
            model.views.add(view)
        }

    }


    private fun checkModelConsistency(model: ModelApplication) {
        model.actions.firstOrNull { it.isReference() }?.let {
            throw ConfigurationValidationException("$.actions", "references cannot be registered as action definitions")
        }
        model.views.firstOrNull { it.isReference() }?.let {
            throw ConfigurationValidationException("$.views", "references cannot be registered as view definitions")
        }
    }

}
