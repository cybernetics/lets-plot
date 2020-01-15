/*
 * Copyright (c) 2020. JetBrains s.r.o.
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package jetbrains.datalore.plot.base.render.svg

object SvgIndex {
    private var NEXT = 0

    fun reset() {
        NEXT = 0
    }

    fun next() = NEXT++
}