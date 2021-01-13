package it.sapienza.appinterpreter.model.view_model

interface ElementType {

    enum class ViewElem {
        button, form, image, list, text, layout, pager
    }

    enum class FormElem {
        checkbox, date, selector, text
    }

    enum class EventType {
        rest, alert, show, opensite, save, getsaved
    }

    enum class DataType {
        rest, save, getsaved
    }

}