/*
 * Copyright (c) 2019. JetBrains s.r.o.
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package jetbrains.datalore.plot

import jetbrains.datalore.base.geometry.DoubleVector
import jetbrains.datalore.base.jsObject.JsObjectSupport
import jetbrains.datalore.plot.server.config.PlotConfigServerSide

object PlotHtmlHelper {
//    private const val baseUrl = "https://dl.bintray.com/jetbrains/lets-plot"
//    private const val version = "1.0.0"
//    private const val suffix = "min.js"

    @Suppress("SameParameterValue")
    private fun randomString(len: Long): String {
        val alphabet = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        return List(6) { alphabet.random() }.joinToString("")
    }

    fun scriptUrl(
        version: String,
        suffix: String = "min.js",
        baseUrl: String = "https://dl.bintray.com/jetbrains/lets-plot"
    ) = "$baseUrl/lets-plot-$version.$suffix"

    fun getDynamicConfigureHtml(scriptUrl: String, verbose: Boolean): String {
        val outputId = randomString(6)

        val successMessage = if (verbose) {
            """
            |   var div = document.createElement("div");
            |   div.style.color = 'darkblue';
            |   div.textContent = 'Lets-Plot JS successfully loaded.';
            |   document.getElementById("$outputId").appendChild(div);
            """.trimMargin()
        } else ""

        return """
            |   <div id="$outputId"></div>
            |   <script type="text/javascript">
            |       if(!window.letsPlotCallQueue) {
            |           window.letsPlotCallQueue = [];
            |       }; 
            |       window.letsPlotCall = function(f) {
            |           window.letsPlotCallQueue.push(f);
            |       };
            |       (function() {
            |           var script = document.createElement("script");
            |           script.type = "text/javascript";
            |           script.src = "$scriptUrl";
            |           script.onload = function() {
            |               window.letsPlotCall = function(f) {f();};
            |               window.letsPlotCallQueue.forEach(function(f) {f();});
            |               window.letsPlotCallQueue = [];
            |               
            |               $successMessage
            |           };
            |           script.onerror = function(event) {
            |               window.letsPlotCall = function(f) {};
            |               window.letsPlotCallQueue = [];
            |               var div = document.createElement("div");
            |               div.style.color = 'darkred';
            |               div.textContent = 'Error loading Lets-Plot JS';
            |               document.getElementById("$outputId").appendChild(div);
            |           };
            |           var e = document.getElementById("$outputId");
            |           e.appendChild(script);
            |       })();
            |   </script>
        """.trimMargin()
    }

    fun getDynamicDisplayHtmlForRawSpec(plotSpec: MutableMap<String, Any>, size: DoubleVector? = null): String {
        // server-side transforms: statistics, sampling, etc.
        @Suppress("NAME_SHADOWING")
        val plotSpec = PlotConfigServerSide.processTransform(plotSpec)
        val plotSpecJs = JsObjectSupport.mapToJsObjectInitializer(plotSpec)
        return getDynamicDisplayHtml(plotSpecJs, size)
    }

    private fun getDynamicDisplayHtml(plotSpecAsJsObjectInitializer: String, size: DoubleVector?): String {
        val outputId = randomString(6)
        val dim = if (size == null) "-1, -1" else "${size.x}, ${size.y}"
        return """
            |   <div id="$outputId"></div>
            |   <script type="text/javascript">
            |       (function() {
            |           var plotSpec=$plotSpecAsJsObjectInitializer;
            |           var plotContainer = document.getElementById("$outputId");
            |           window.letsPlotCall(function() {{
            |               LetsPlot.buildPlotFromProcessedSpecs(plotSpec, ${dim}, plotContainer);
            |           }});
            |       })();    
            |   </script>
        """.trimMargin()
    }

    fun getStaticConfigureHtml(scriptUrl: String): String {
        return "<script type=\"text/javascript\" src=\"$scriptUrl\"></script>"
    }

    fun getStaticDisplayHtmlForRawSpec(plotSpec: MutableMap<String, Any>, size: DoubleVector? = null): String {
        // server-side transforms: statistics, sampling, etc.
        @Suppress("NAME_SHADOWING")
        val plotSpec = PlotConfigServerSide.processTransform(plotSpec)
        val plotSpecJs = JsObjectSupport.mapToJsObjectInitializer(plotSpec)
        return getStaticDisplayHtml(plotSpecJs, size)
    }

    private fun getStaticDisplayHtml(
        plotSpecAsJsObjectInitializer: String,
        size: DoubleVector?
    ): String {
        val outputId = randomString(6)
        val dim = if (size == null) "-1, -1" else "${size.x}, ${size.y}"
        return """
            |   <div id="$outputId"></div>
            |   <script type="text/javascript">
            |       var plotSpec=$plotSpecAsJsObjectInitializer;
            |       var plotContainer = document.getElementById("$outputId");
            |       LetsPlot.buildPlotFromProcessedSpecs(plotSpec, ${dim}, plotContainer);
            |   </script>
        """.trimMargin()
    }
}