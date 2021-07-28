package astminer.parse.gumtree.java.srcML

import astminer.common.model.FunctionInfo
import astminer.common.model.TreeFunctionSplitter
import astminer.parse.gumtree.GumTreeNode
import mu.KotlinLogging

class GumTreeJavaSrcmlFunctionSplitter : TreeFunctionSplitter<GumTreeNode> {
    private val functionType = "function"
    private val logger = KotlinLogging.logger("Gumtree with Srcml backend - Java")

    override fun splitIntoFunctions(root: GumTreeNode, filePath: String): Collection<FunctionInfo<GumTreeNode>> {
        return root.preOrder().filter { it.typeLabel == functionType }
            .mapNotNull {
                try {
                    GumTreeJavaSrcmlFunctionInfo(it, filePath)
                } catch (e: IllegalStateException) {
                    logger.warn { "Error occured while parsing $filePath : ${e.message}" }
                    null
                }
            }
    }
}
