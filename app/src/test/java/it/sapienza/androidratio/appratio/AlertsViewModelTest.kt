package it.sapienza.androidratio.appratio

import it.sapienza.appinterpreter.alerts.AlertsViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

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
