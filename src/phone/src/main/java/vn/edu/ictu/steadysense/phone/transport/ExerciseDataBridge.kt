/*
 * Copyright 2026 SteadySense AI Team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package vn.edu.ictu.steadysense.phone.transport

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import vn.edu.ictu.steadysense.core.ImuWindow

data class TimestampedImuWindow(
    val window: ImuWindow,
    val receivedAtMillis: Long = System.currentTimeMillis(),
)

/**
 * Cầu nối real-time giữa [PhoneMessageService] và màn hình tập luyện.
 *
 * Khi nhận được IMU window từ đồng hồ, PhoneMessageService gọi [emit] để đẩy
 * window vào SharedFlow. ExerciseRepEngine ở màn hình tập luyện sẽ collect
 * để phân tích AI ngay lập tức.
 */
object ExerciseDataBridge {
    private val _windows = MutableSharedFlow<TimestampedImuWindow>(
        replay = 1,
        extraBufferCapacity = 10,
    )
    val windows: SharedFlow<TimestampedImuWindow> = _windows

    /** Gọi từ PhoneMessageService sau khi decode IMU window thành công. */
    fun emit(window: ImuWindow) {
        _windows.tryEmit(TimestampedImuWindow(window, System.currentTimeMillis()))
    }

    private val _massageControlEvents = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 5,
    )
    val massageControlEvents: SharedFlow<String> = _massageControlEvents

    /** Nhận lệnh điều khiển massage từ đồng hồ (ví dụ: TOGGLE_PAUSE). */
    fun emitMassageControl(command: String) {
        _massageControlEvents.tryEmit(command)
    }
}
