package com.henjicc.swiftformat.engine.tuning

import com.henjicc.swiftformat.core.model.QualityPreset

/**
 * 质量档位（见 SPEC 5.2）→ 各引擎真实参数的数值表，唯一调参入口。
 *
 * 各 `xxxMapper` 对象只负责"按格式族选用哪张表 / 做什么换算"，具体数值全部集中在这里，
 * 后期调参（改善画质或缩小体积）只需要改这一个文件，不用去翻 6 个 mapper 文件。
 *
 * 每张表都按 [QualityPreset] 四档（最佳/高/标准/省空间）覆盖全部取值，语义见 SPEC 5.2：
 * 最佳=尽量接近原文件、高=肉眼差异小、标准=体积与画质平衡（默认档）、省空间=优先减小体积。
 */
internal object QualityPresetTuning {

    /** 图片 JPEG / 有损 WebP 压缩质量（0-100，见 SPEC 5.3）。 */
    val imageCompressQuality: Map<QualityPreset, Int> = mapOf(
        QualityPreset.BEST to 95,
        QualityPreset.HIGH to 85,
        QualityPreset.STANDARD to 75,
        QualityPreset.SMALL_SIZE to 60,
    )

    /** AVIF（libaom-av1）CRF（0-63，越小质量越高）。 */
    val avifCrf: Map<QualityPreset, Int> = mapOf(
        QualityPreset.BEST to 18,
        QualityPreset.HIGH to 24,
        QualityPreset.STANDARD to 30,
        QualityPreset.SMALL_SIZE to 38,
    )

    /** 视频每像素每帧比特数基准（针对常见 H.264/H.265/VP9 软硬编码，经验值，见 SPEC 5.4）。 */
    val videoBitsPerPixelPerFrame: Map<QualityPreset, Double> = mapOf(
        QualityPreset.BEST to 0.12,
        QualityPreset.HIGH to 0.08,
        QualityPreset.STANDARD to 0.05,
        QualityPreset.SMALL_SIZE to 0.03,
    )

    /** AAC/M4A 目标码率（bps，Media3 路径，见 SPEC 5.5）。 */
    val aacBitrateBps: Map<QualityPreset, Int> = mapOf(
        QualityPreset.BEST to 256_000,
        QualityPreset.HIGH to 192_000,
        QualityPreset.STANDARD to 128_000,
        QualityPreset.SMALL_SIZE to 96_000,
    )

    /** MP3 目标码率（bps，FFmpeg 路径，见 SPEC 5.5）。MP3 编解码效率低于 AAC，同档位取值更高。 */
    val mp3BitrateBps: Map<QualityPreset, Int> = mapOf(
        QualityPreset.BEST to 320_000,
        QualityPreset.HIGH to 256_000,
        QualityPreset.STANDARD to 192_000,
        QualityPreset.SMALL_SIZE to 128_000,
    )

    /** Opus 目标码率（bps，视频转 WEBM 时的音轨）。Opus 编码效率高于 MP3，同档位取值更低。 */
    val opusBitrateBps: Map<QualityPreset, Int> = mapOf(
        QualityPreset.BEST to 192_000,
        QualityPreset.HIGH to 160_000,
        QualityPreset.STANDARD to 128_000,
        QualityPreset.SMALL_SIZE to 96_000,
    )

    /**
     * libvpx-vp9（WEBM 视频编码）的 `-cpu-used`（即 `-speed`，0-5，越大越快越粗糙）。
     * VP9 软件编码默认值（0）非常慢，按质量档位给一个"质量越高越精细（慢）、省空间越快"的速度旋钮，
     * 避免所有档位都用最慢速度导致转码耗时过长。
     */
    val vp9EncodeSpeed: Map<QualityPreset, Int> = mapOf(
        QualityPreset.BEST to 1,
        QualityPreset.HIGH to 2,
        QualityPreset.STANDARD to 3,
        QualityPreset.SMALL_SIZE to 5,
    )

    /**
     * 各表理论上覆盖 [QualityPreset] 全部取值，这里的 `getValue` 仅作为枚举新增档位时的防御性兜底，
     * 兜底统一落在 STANDARD（产品默认档，见 SPEC 22），不应在正常路径触发。
     */
    fun <T> Map<QualityPreset, T>.forPresetOrStandard(preset: QualityPreset): T =
        this[preset] ?: getValue(QualityPreset.STANDARD)
}
