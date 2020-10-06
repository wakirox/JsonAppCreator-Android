package it.sapienza.appinterpreter.model.view_model

interface ElementType {

    enum class ViewElem {
        button, form, image, list, text, layout
    }

    enum class FormElem {
        checkbox, date, selector, text
    }

    enum class EventType {
        call, alert, screen
    }

}