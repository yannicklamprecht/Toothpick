import org.gradle.api.model.ObjectFactory
import java.io.File

open class MiniPaperExtension(@Suppress("UNUSED_PARAMETER") objects: ObjectFactory) {

    lateinit var minecraftVersion: String
    lateinit var forkName: String
    lateinit var upstreamName: String
    lateinit var groupId: String

    lateinit var subProjects :Map<String, List<File>>
}
