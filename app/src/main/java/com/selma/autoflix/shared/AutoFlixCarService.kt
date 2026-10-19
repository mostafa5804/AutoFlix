package com.selma.autoflix.shared

import com.google.android.apps.auto.sdk.CarActivity
import com.google.android.apps.auto.sdk.CarActivityService

class AutoFlixCarService : CarActivityService() {
    override fun getCarActivity(): Class<out CarActivity> {
        return AutoFlixCarActivity::class.java
    }
}
