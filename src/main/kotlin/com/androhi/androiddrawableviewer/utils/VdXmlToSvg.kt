import org.apache.commons.io.FileUtils
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import java.io.StringBufferInputStream
import java.io.StringWriter
import java.nio.file.Files
import java.nio.file.Paths
import javax.xml.parsers.DocumentBuilder
import javax.xml.transform.OutputKeys
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

data class ImageParams(val width: String, val height: String, val content: String, val color: String)

object VdXmlToSvg {

    fun vdXmlToSvg(file: File): File? {

        val vdContent = readStringFromFile(file.path)

        val imageParams: ImageParams
        try {
            imageParams = parseVD(vdContent)
        } catch (e: Exception) {
            return null
        }

        val svgContent = createSvg(imageParams)

        val svgFile = File.createTempFile("batik-default-override-svg", ".svg")

        FileUtils.writeStringToFile(svgFile, svgContent)

        return svgFile
    }

    private fun readStringFromFile(filePath: String): String {
        return String(Files.readAllBytes(Paths.get(filePath)))
    }

    private fun parseVD(content: String): ImageParams {
        val filteredContent = content.substring(content.indexOf("vector") - 1, content.length)
            .replace("\n|\r".toRegex(), "")

        val factory = javax.xml.parsers.DocumentBuilderFactory.newInstance()
        val dBuilder: DocumentBuilder? = factory.newDocumentBuilder()

        val doc: Document? = dBuilder?.parse(StringBufferInputStream(filteredContent))

        doc?.documentElement?.normalize()

        val vector = doc?.firstChild as? Element
        val path = vector?.getElementsByTagName("path")?.item(0) as Element

        return ImageParams(
            vector.getAttribute("android:width").replace("dp", ""),
            vector.getAttribute("android:height").replace("dp", ""),
            path.getAttribute("android:pathData"),
            path.getAttribute("android:fillColor")
        )
    }

    // TODO https://gitlab.com/Hyperion777/VectorDrawable2Svg/blob/master/VectorDrawable2Svg.py
    private fun createSvg(imageParams: ImageParams): String {

        val title = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\" \"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">"

        // root elements
        val factory = javax.xml.parsers.DocumentBuilderFactory.newInstance()
        val doc = factory.newDocumentBuilder().newDocument()
        val rootElement = doc?.createElement("svg") ?: return ""
        doc.appendChild(rootElement) ?: return ""

        // setup basic svg info
        with(rootElement) {
            setAttribute("xmlns", "http://www.w3.org/2000/svg")
            setAttribute("width", imageParams.width)
            setAttribute("height", imageParams.height)
            setAttribute("viewBox", "0 0 " + imageParams.width + " " + imageParams.height)
        }

        val path = doc.createElement("path").apply {

            setAttribute("d", imageParams.content)

            if (imageParams.color.isNotBlank()) {
                setAttribute("fill", imageParams.color)
            }
        }

        rootElement.appendChild(path)

        return title + docToString(doc)
    }

    private fun docToString(doc: Document): String {
        val transformer: Transformer? = TransformerFactory.newInstance().newTransformer()

        transformer?.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")

        val writer = StringWriter()
        transformer?.transform(DOMSource(doc), StreamResult(writer))

        return writer.buffer.toString().replace("\n|\r".toRegex(), "")
    }
}
