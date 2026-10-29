package it.sapienza.appinterpreter.alerts

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AlertsViewModelTest {
    private val alerts = AlertsViewModel()
    private val ran = mutableListOf<String>()

    @Test
    fun answeringClosesTheAlertThenRunsTheChosenAction() {
        alerts.show("Delete it?", onOk = { ran += "ok" }, onCancel = { ran += "cancel" })
        alerts.answer(ok = false)

        assertNull(alerts.alert)
        assertEquals(listOf("cancel"), ran)
    }

    @Test
    fun anActionThatShowsAnotherAlertKeepsTheNewOne() {
        alerts.show("First", onOk = { alerts.show("Second") })
        alerts.answer(ok = true)

        assertEquals("Second", alerts.alert?.text)
    }

    @Test
    fun aRecreatedActivityKeepsAMessageButClosesAnAlertWithActions() {
        alerts.show("Just so you know")
        alerts.activityRecreated()
        assertNotNull(alerts.alert)

        alerts.show("Close the app?", onOk = { ran += "ok" }, onCancel = {})
        alerts.activityRecreated()
        assertNull(alerts.alert)
        assertEquals(emptyList<String>(), ran)
    }
}
