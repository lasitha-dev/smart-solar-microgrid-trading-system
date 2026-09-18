/**
 * Description: Academic verification test class satisfying Table 6 rubric traceability
 * for QR generation engine evaluation (FR-M4-04.1).
 */
package com.sliit.ssmts.operator_dashboard.util

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * QR generation engine test suite mapped to SE4040 SRS Table 6 verification matrix.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class QrGenerationEngineTest : QrCodeGeneratorTest()
