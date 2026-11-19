package boardgame.catan

import boardgame.api.codegen.TsScanner
import java.io.File

/**
 * Entry point for `:back:generateTs`: scans the compiled Catan classes for `@GenerateTs` models and
 * writes the TypeScript types used by the front. Front-only helpers stay in ../types.ts.
 */
object TsExport {
    @JvmStatic
    fun main(args: Array<String>) {
        val output = File(args.firstOrNull() ?: error("usage: TsExport <output> <classesDir>..."))
        val classDirs = args.drop(1).map(::File).filter { it.isDirectory }
        val count = TsScanner.generate(output, classDirs)
        println("Generated $count types into ${output.absolutePath}")
    }
}
