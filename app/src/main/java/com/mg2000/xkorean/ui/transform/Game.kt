package com.mg2000.xkorean.ui.transform

data class Game(var id: String, var name: String, var koreanName: String, var localize: String, var seriesXS: String, var oneXEnhanced: String, var oneS: String, var pc: String, var x360: String, var og: String, var message: String, var thumbnail: String?, var thumbnailID: String, var gamePassCloud: String, var gamePassPC: String, var gamePassConsole: String, var gamePassNew: String, var gamePassEnd: String, var gamePassComing: String, var discount: String, var releaseDate: String, var nzReleaseDate: String, var dolbyAtmos: String, var consoleKeyboardMouse: String, var playAnywhere: String, var localCoop: String, var onlineCoop: String, var fps120: String, var fpsBoost: String, var categories: Array<String>, var price: Float, var lowestPrice: Float, var bundle: Array<Edition>, var packages: String, var languageCode: String) {
    fun isAvailable() : Boolean {
        return discount != "판매 중지" || gamePassPC != "" || gamePassConsole != "" || gamePassCloud != "" || (discount.indexOf("출시") >= 0 && price.toInt() == -1 && bundle.isNotEmpty())
    }
}