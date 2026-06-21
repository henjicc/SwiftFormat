package com.henjicc.swiftformat.engine.api

import com.henjicc.swiftformat.core.model.ConversionRequest

/** 按请求选择第一个声明支持的引擎（见 SPEC 10.4）。引擎注册顺序即优先级。 */
class ConversionEngineSelector(private val engines: List<ConversionEngine>) {
    fun select(request: ConversionRequest): ConversionEngine? =
        engines.firstOrNull { it.supports(request) }
}
