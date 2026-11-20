package io.codepassion.doubletriangle

import io.codepassion.doubletriangle.core.designsystem.R
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Test

class BookingErrorResourceTest {
    @Test fun mapsReservationRejectionsToIosMessages() {
        val cases = mapOf(
            "room_full" to R.string.someone_just_took_the_last_place_for_this_time__availability_has_been_refreshed_,
            "time_not_available" to R.string.this_time_is_no_longer_available__the_room_may_have_been_closed_,
            "starts_in_past" to R.string.this_time_has_already_started__choose_a_later_one_,
            "outside_booking_window" to R.string.this_time_is_outside_the_room_s_booking_window_,
            "invalid_duration" to R.string.this_duration_isn_t_allowed_in_this_room_,
            "client_already_reserved" to R.string.you_already_have_another_reservation_at_this_time_,
            "room_not_reservable" to R.string.this_room_can_t_be_booked_right_now_,
            "not_active_client" to R.string.your_gym_membership_is_no_longer_active_,
            "cancellation_closed" to R.string.it_s_too_late_to_cancel_this_reservation_,
        )
        cases.forEach { (code, expected) -> assertEquals(expected, bookingErrorResource(BookingHttpException(409, "server text", code))) }
    }

    @Test fun mapsOfflineAndUnknownErrorsWithoutShowingRawText() {
        assertEquals(R.string.you_seem_to_be_offline__connect_to_the_internet_and_try_again_, bookingErrorResource(IOException("technical detail")))
        assertEquals(R.string.the_operation_could_not_be_completed__please_try_again_, bookingErrorResource(BookingHttpException(500, "server text")))
    }
}
