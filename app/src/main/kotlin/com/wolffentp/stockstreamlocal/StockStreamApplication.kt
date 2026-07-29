package com.wolffentp.stockstreamlocal

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point.
 * No cloud services, no telemetry, no analytics are initialized here.
 * All data lives locally on the device.
 */
@HiltAndroidApp
class StockStreamApplication : Application()
