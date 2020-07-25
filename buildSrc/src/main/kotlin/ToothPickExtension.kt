import org.gradle.api.model.ObjectFactory
import java.io.File

open class ToothPickExtension(@Suppress("UNUSED_PARAMETER") objects: ObjectFactory) {

    var minecraftVersion: String = "1.16.1"
    lateinit var forkName: String
    lateinit var upstreamName: String
    lateinit var groupId: String

    lateinit var subProjects :Map<String, List<File>>
}
