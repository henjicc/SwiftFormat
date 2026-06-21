package com.henjicc.swiftformat.core.file

/** 输出文件命名规则（见 SPEC 12.4）：替换扩展名、重名自动加序号。纯函数，便于单元测试。 */
object OutputNaming {

    /** 用新输出格式的扩展名替换原文件名的扩展名（保留文件主名）。 */
    fun withExtension(originalDisplayName: String, outputFormat: String): String {
        val dot = originalDisplayName.lastIndexOf('.')
        val base = if (dot > 0) originalDisplayName.substring(0, dot) else originalDisplayName
        return "$base.${outputFormat.lowercase()}"
    }

    /**
     * 若 [desiredName] 已存在于 [existingNames]，自动追加序号直到不冲突：
     * `video.mp4` → `video (1).mp4` → `video (2).mp4` ...
     */
    fun resolveCollision(desiredName: String, existingNames: Set<String>): String {
        if (desiredName !in existingNames) return desiredName
        val dot = desiredName.lastIndexOf('.')
        val base = if (dot > 0) desiredName.substring(0, dot) else desiredName
        val extension = if (dot > 0) desiredName.substring(dot) else ""
        var index = 1
        while (true) {
            val candidate = "$base ($index)$extension"
            if (candidate !in existingNames) return candidate
            index++
        }
    }
}
