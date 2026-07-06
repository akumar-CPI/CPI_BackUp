import com.sap.gateway.ip.core.customdev.util.Message
import groovy.xml.XmlUtil
import groovy.util.XmlParser
import groovy.util.Node

def Message processData(Message message) {
    // Parse the XML from the message body InputStream
    def xml = new XmlParser().parse(message.getBody(java.io.InputStream))

    // Create new root node with the same name as input root (e.g., 'root')
    def newRoot = new Node(null, xml.name())

    // For each <item>, split fdf_location_external_reference and create replicated <item> nodes
    xml.item.each { itemNode ->
        def unitCodes = itemNode.fdf_location_external_reference?.text() ?: ''
        def unitCodesValue = unitCodes.split('\\|')

        unitCodesValue.each { unitCode ->
            def unitCodeValue = unitCode.trim()

            // Create a new <item> under the new root
            def newItem = new Node(newRoot, 'item')

            // Clone and append all children except the original fdf_location_external_reference (preserve order)
            itemNode.children().findAll { child ->
                child instanceof Node && child.name() != 'fdf_location_external_reference'
            }.each { child ->
                newItem.append(child.clone())
            }

            // Insert the new location node at index 1 (second position), safely
            def childrenList = newItem.value() // mutable list of children of <item>
            def insertIndex = Math.min(1, childrenList.size())
            childrenList.add(insertIndex, new Node(null, 'fdf_location_external_reference', unitCodeValue))
        }
    }

    // Serialize and set back to message body
    message.setBody(XmlUtil.serialize(newRoot))
    return message
}