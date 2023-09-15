/*
 * Copyright (c) 2015 - present Hive-Box.
 */

package com.mason.net

import com.mason.util.preference.Preferences

internal object Sp : Preferences("saas_net") {
    var netIdSeq by IntPref("net_id_seq", 0)
    var gatewayAuth by StringPref("gateway_auth", "")
    var gatewayRefreshAuth by StringPref("gateway_refresh_auth", "")
}
