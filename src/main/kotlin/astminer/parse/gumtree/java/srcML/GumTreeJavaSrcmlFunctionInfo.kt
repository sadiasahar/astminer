package astminer.parse.gumtree.java.srcML

import astminer.common.model.EnclosingElement
import astminer.common.model.EnclosingElementType
import astminer.common.model.FunctionInfo
import astminer.common.model.FunctionInfoParameter
import astminer.parse.findEnclosingElementBy
import astminer.parse.gumtree.GumTreeNode
import mu.KotlinLogging

private val logger = KotlinLogging.logger("GumTree_srcML-Java-function-info")

class GumTreeJavaSrcmlFunctionInfo(override val root: GumTreeNode, override val filePath: String) :
    FunctionInfo<GumTreeNode> {
    override val nameNode: GumTreeNode? = root.getChildOfType(NAME)

    override val returnType: String = root.extractType()

    override val parameters: List<FunctionInfoParameter>? = run {
        root.preOrder().filter { it.typeLabel == PARAMETER }
            .map {
                try {
                    assembleParameter(it)
                } catch (e: IllegalStateException) {
                    logger.warn { e.message }
                    return@run null
                }
            }
    }

    override val annotations: List<String>? = run {
        root.children.filter { it.typeLabel == ANNOTATION }.map {
            val token = it.getChildOfType(NAME)?.originalToken
            if (token == null) {
                logger.warn { "Annotation in function $name in file $filePath don't have a name" }
                return@run null
            }
            return@map token
        }
    }

    override val modifiers: List<String>? = run {
        val type = checkNotNull(root.getChildOfType(TYPE)) { "Function $name in file $filePath doesn't have a type" }
        type.children.filter { it.typeLabel == MODIFIER }.map {
            val token = it.originalToken
            if (token == null) {
                logger.warn { "Modifier in function $name in file $filePath doesn't have a name" }
                return@run null
            }
            return@map token
        }
    }

    override val body: GumTreeNode? = root.getChildOfType(FUNCTION_BODY)

    override val isConstructor: Boolean = false

    override val enclosingElement: EnclosingElement<GumTreeNode>? =
        root.findEnclosingElementBy { it.typeLabel in possibleEnclosingElements }?.assembleEnclosing()

    private fun assembleParameter(node: GumTreeNode): FunctionInfoParameter {
        val parameter = checkNotNull(node.getChildOfType(VAR_DECLARATION)) { "No variable found" }
        val name = checkNotNull(parameter.getChildOfType(NAME)?.originalToken) { "Parameter name was not found" }
        val type = parameter.extractType()
        return FunctionInfoParameter(name, type)
    }

    private fun GumTreeNode.assembleEnclosing(): EnclosingElement<GumTreeNode>? = extractWithLogger(logger) {
        val enclosingType = when (this.typeLabel) {
            CLASS_DECLARATION -> EnclosingElementType.Class
            ENUM_DECLARATION -> EnclosingElementType.Enum
            else -> error("Can't find any enclosing type association")
        }
        EnclosingElement(
            type = enclosingType,
            name = this.getChildOfType(NAME)?.originalToken ?: return@extractWithLogger null,
            root = this
        )
    }

    private fun GumTreeNode.extractType(): String {
        val typeNode = checkNotNull(this.getChildOfType(TYPE)?.getChildOfType(NAME)) { "No type found" }
        return typeNode.preOrder().joinToString(separator = "") { node ->
            if (node.typeLabel == ARRAY_BRACKETS) {
                "[]"
            } else {
                checkNotNull(node.originalToken) { "No type found" }
            }
        }
    }

    companion object {
        const val FUNCTION_BODY = "block"
        const val MODIFIER = "specifier"
        const val TYPE = "type"
        const val NAME = "name"
        const val ARRAY_BRACKETS = "index"
        const val ANNOTATION = "annotation"
        const val PARAMETER = "parameter"
        const val VAR_DECLARATION = "decl"
        const val CLASS_DECLARATION = "class"
        const val ENUM_DECLARATION = "enum"
        val possibleEnclosingElements = listOf(
            CLASS_DECLARATION,
            ENUM_DECLARATION
        )
    }
}
