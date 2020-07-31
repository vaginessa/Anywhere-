package com.absinthe.anywhere_.constants

import io.michaelrocks.paranoid.Obfuscate

@Obfuscate
object AnywhereType {

    object Card {
        const val URL_SCHEME = 0
        const val ACTIVITY = 1
        const val MINI_PROGRAM = 2
        const val QR_CODE = 3
        const val IMAGE = 4
        const val SHELL = 5
        const val SWITCH_SHELL = 6
        const val FILE = 7
        const val BROADCAST = 8
        const val WORKFLOW = 9
    }

    object Property {
        const val NONE = 0
        const val SHORTCUTS = 1
        const val EXPORTED = 1
    }

    object WhereMode {
        const val ANYWHERE = "Anywhere-"
        const val SOMEWHERE = "Somewhere-"
        const val NOWHERE = "Nowhere-"
    }

    object Category {
        const val DEFAULT_CATEGORY = "Default"
    }

    object Page {
        const val CARD_PAGE = 0
        const val WEB_PAGE = 1
    }

    object Prefix {
        const val IMAGE_PREFIX = "[Image]"
        const val QRCODE_PREFIX = "[QR_Code]"
        const val DYNAMIC_PARAMS_PREFIX = "[DYNAMIC_PARAMS "
        const val DYNAMIC_PARAMS_PREFIX_FORMAT = "[DYNAMIC_PARAMS %s]"
        const val SHELL_PREFIX = "[ANYWHERE_SHELL]"
    }
}