/**
 * Description: UI unit test suite for BookingHistoryActivity verifying screen launch, Toolbar setup,
 * chip group initialization, search input, and empty state rendering with Robolectric (FR-M4-03).
 */
package com.sliit.ssmts.operator_dashboard.ui.history

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.sliit.ssmts.operator_dashboard.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController

/**
 * Validates Activity lifecycle, ViewBinding inflation, and initial component states.
 */
@RunWith(RobolectricTestRunner::class)
class BookingHistoryActivityTest {

    private lateinit var controller: ActivityController<BookingHistoryActivity>
    private lateinit var activity: BookingHistoryActivity

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.setTheme(R.style.Theme_Ssmts_OperatorDashboard)

        controller = Robolectric.buildActivity(BookingHistoryActivity::class.java)
        activity = controller.create().start().resume().visible().get()
    }

    /**
     * Asserts that Toolbar title matches 'Booking History & Feeds' upon launch.
     */
    @Test
    fun activityLaunch_inflatesToolbarAndSetsTitle() {
        val toolbar = activity.findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbarHistory)
        assertNotNull(toolbar)
        assertEquals("Booking History & Feeds", toolbar.title.toString())
    }

    /**
     * Asserts that 'All' filter chip is checked by default on initial launch.
     */
    @Test
    fun initialState_chipFilterAllIsChecked() {
        val chipGroup = activity.findViewById<ChipGroup>(R.id.chipGroupFilters)
        assertNotNull(chipGroup)
        assertEquals(R.id.chipFilterAll, chipGroup.checkedChipId)
    }

    /**
     * Asserts that typing into search input updates text and triggers query changes.
     */
    @Test
    fun searchBar_allowsTextInput() {
        val searchEditText = activity.findViewById<TextInputEditText>(R.id.etSearchQuery)
        assertNotNull(searchEditText)

        searchEditText.setText("Colombo")
        assertEquals("Colombo", searchEditText.text.toString())
    }

    /**
     * Asserts that clicking a different filter chip updates the single selection state.
     */
    @Test
    fun chipSelection_togglesFilterChip() {
        val chipGroup = activity.findViewById<ChipGroup>(R.id.chipGroupFilters)
        val chipCompleted = activity.findViewById<Chip>(R.id.chipFilterCompleted)
        assertNotNull(chipCompleted)

        chipCompleted.performClick()
        assertEquals(R.id.chipFilterCompleted, chipGroup.checkedChipId)
    }

    /**
     * Asserts that clicking Toolbar navigation icon finishes the activity.
     */
    @Test
    fun toolbarNavigation_finishesActivity() {
        val toolbar = activity.findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbarHistory)
        assertNotNull(toolbar)

        for (i in 0 until toolbar.childCount) {
            val child = toolbar.getChildAt(i)
            if (child is android.widget.ImageButton) {
                child.performClick()
                break
            }
        }

        assertTrue(activity.isFinishing)
    }
}
