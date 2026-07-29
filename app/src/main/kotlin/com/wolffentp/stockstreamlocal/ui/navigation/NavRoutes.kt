package com.wolffentp.stockstreamlocal.ui.navigation

object NavRoutes {
    const val LOCK         = "lock"
    const val HOME         = "home"
    const val WATCHLIST    = "watchlist"
    const val ADD_TICKER   = "add_ticker"
    const val TICKER_DETAIL = "ticker_detail/{symbol}"
    const val CSV_IMPORT   = "csv_import"
    const val IMPORT_VALIDATION = "import_validation"
    const val PROVIDER_SETTINGS = "provider_settings"
    const val REFRESH_SETTINGS  = "refresh_settings"
    const val SETTINGS     = "settings"
    const val COLUMN_LAYOUT_EDITOR = "column_layout_editor/{viewId}"
    const val ROTATING_VIEWS_EDITOR = "rotating_views_editor"
    const val FULL_SCREEN_DISPLAY   = "full_screen_display"
    const val DATA_QUALITY_LEGEND   = "data_quality_legend"
    const val ABOUT        = "about"

    fun tickerDetail(symbol: String) = "ticker_detail/$symbol"
    fun columnLayoutEditor(viewId: String) = "column_layout_editor/$viewId"
}
