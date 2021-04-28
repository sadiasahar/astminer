package astminer.parse.antlr.java

import astminer.common.model.*
import astminer.parse.antlr.AntlrNode
import astminer.parse.antlr.firstLabelIn
import astminer.parse.antlr.hasLastLabel
import astminer.parse.antlr.lastLabelIn

data class AntlrJavaFunctionInfo(override val root: AntlrNode) : FunctionInfo<AntlrNode> {
    override val nameNode: AntlrNode? = collectNameNode()
    override val parameters: List<MethodInfoParameter> = collectParameters()
    override val returnType: String? = collectReturnType()
    override val enclosingElement: EnclosingElement<AntlrNode>? = collectEnclosingClass()

    companion object {
        private const val METHOD_RETURN_TYPE_NODE = "typeTypeOrVoid"
        private const val METHOD_NAME_NODE = "IDENTIFIER"

        private const val CLASS_DECLARATION_NODE = "classDeclaration"
        private const val CLASS_NAME_NODE = "IDENTIFIER"

        private const val METHOD_PARAMETER_NODE = "formalParameters"
        private const val METHOD_PARAMETER_INNER_NODE = "formalParameterList"
        private val METHOD_SINGLE_PARAMETER_NODES = listOf("formalParameter", "lastFormalParameter")
        private const val PARAMETER_RETURN_TYPE_NODE = "typeType"
        private const val PARAMETER_NAME_NODE = "variableDeclaratorId"
    }

    private fun collectNameNode(): AntlrNode? {
        return root.getChildOfType(METHOD_NAME_NODE)
    }

    private fun collectReturnType(): String? {
        val returnTypeNode = root.getChildOfType(METHOD_RETURN_TYPE_NODE)
        return returnTypeNode?.let { getTokensFromSubtree(it) }
    }

    private fun collectEnclosingClass(): EnclosingElement<AntlrNode>? {
        val enclosingClassNode = findEnclosingClassNode(root) ?: return null
        return EnclosingElement(
            type = EnclosingElementType.Class,
            name = enclosingClassNode.getChildOfType(CLASS_NAME_NODE)?.getToken(),
            root = enclosingClassNode
        )
    }

    private fun findEnclosingClassNode(node: AntlrNode?): AntlrNode? {
        if (node == null || node.hasLastLabel(CLASS_DECLARATION_NODE)) {
            return node
        }
        return findEnclosingClassNode(node.getParent() as AntlrNode)
    }

    private fun collectParameters(): List<MethodInfoParameter> {
        val parametersRoot = root.getChildOfType(METHOD_PARAMETER_NODE)
        val innerParametersRoot = parametersRoot?.getChildOfType(METHOD_PARAMETER_INNER_NODE) ?: return emptyList()

        if (innerParametersRoot.lastLabelIn(METHOD_SINGLE_PARAMETER_NODES)) {
            return listOf(getParameterInfo(innerParametersRoot))
        }

        return innerParametersRoot.getChildren().filter {
            it.firstLabelIn(METHOD_SINGLE_PARAMETER_NODES)
        }.map {singleParameter -> getParameterInfo(singleParameter) }
    }

    private fun getParameterInfo(parameterNode: AntlrNode): MethodInfoParameter {
        val returnTypeNode = parameterNode.getChildOfType(PARAMETER_RETURN_TYPE_NODE)
        val returnTypeToken = returnTypeNode?.let { getTokensFromSubtree(it) }

        val parameterName = parameterNode.getChildOfType(PARAMETER_NAME_NODE)?.getToken()
            ?: throw IllegalStateException("Parameter name wasn't found")

        return MethodInfoParameter(parameterName, returnTypeToken)
    }

    private fun getTokensFromSubtree(node: AntlrNode): String {
        if (node.isLeaf()) {
            return node.getToken()
        }
        return node.getChildren().joinToString(separator = "") { child ->
            getTokensFromSubtree(child)
        }
    }
}

