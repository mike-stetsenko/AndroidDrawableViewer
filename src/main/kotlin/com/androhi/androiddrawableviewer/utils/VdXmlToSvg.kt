import org.apache.commons.io.FileUtils
import org.jdom2.input.SAXBuilder
import org.jdom2.output.XMLOutputter
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths


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

        val builder = SAXBuilder()

        val doc = builder.build(ByteArrayInputStream(filteredContent.toByteArray(Charset.forName("UTF-8"))))

        val vector = doc.rootElement
        val path = vector.getChild("path")

        return ImageParams(
            vector.attributes.first { it.name.toString() == "width" }.value.replace("dp", ""),
            vector.attributes.first { it.name.toString() == "height" }.value.replace("dp", ""),
            path.attributes.first { it.name.toString() == "pathData" }.value,
            path.attributes.first { it.name.toString() == "fillColor" }.value
        )
    }

    // TODO https://gitlab.com/Hyperion777/VectorDrawable2Svg/blob/master/VectorDrawable2Svg.py
    private fun createSvg(imageParams: ImageParams): String {

        val title = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\" \"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">"

        val svg = org.jdom2.Element("svg", "http://www.w3.org/2000/svg")
        val root = org.jdom2.Document(svg)
        svg.setAttribute("width", imageParams.width)
        svg.setAttribute("height", imageParams.height)
        svg.setAttribute("viewBox", "0 0 " + imageParams.width + " " + imageParams.height)

        val path = org.jdom2.Element("path")
        path.setAttribute("d", imageParams.content)
        if (imageParams.color.isNotBlank()) {
            path.setAttribute("fill", imageParams.color)
        }
        root.rootElement.addContent(path)

        return XMLOutputter().outputString(root).replace("xmlns=\"\" ","")
    }
}
