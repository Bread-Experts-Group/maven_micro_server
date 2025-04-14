package bread_experts_group

import bread_experts_group.http.HTTPRequest
import bread_experts_group.http.HTTPResponse
import java.io.File
import java.net.Socket
import java.util.logging.Logger
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.min

val unauthorizedHeadersPut = mapOf(
	"WWW-Authenticate" to "Basic realm=\"Access to PUT operations\", charset=\"UTF-8\"",
)

private val putLogger = Logger.getLogger("Maven Server PUT")

@OptIn(ExperimentalEncodingApi::class)
fun httpServerPut(
	stores: List<File>,
	storePath: String,
	request: HTTPRequest,
	sock: Socket,
	putCredentials: Map<String, String>? = null
) {
	if (!putCredentials.isNullOrEmpty()) {
		val authorization = request.headers["Authorization"]
		if (authorization == null) {
			putLogger.warning("No user provided, unauthorized for GET")
			HTTPResponse(401, request.version, unauthorizedHeadersPut, "")
				.write(sock.outputStream)
			return
		}
		val pair = Base64.decode(authorization.substringAfter("Basic "))
			.decodeToString()
			.split(':')
		val password = putCredentials[pair[0]]
		if (password == null || password != pair[1]) {
			putLogger.warning { "\"${pair[0]}\" unauthorized for GET, not a user or wrong password" }
			HTTPResponse(403, request.version, unauthorizedHeadersPut, "")
				.write(sock.outputStream)
			return
		}
		putLogger.info { "\"${pair[0]}\" authorized." }
	}

	val size = request.headers["Content-Length"]?.toLongOrNull()
	if (size == null || size < 1) {
		putLogger.warning("No size specified for PUT.")
		HTTPResponse(400, request.version, emptyMap(), "")
			.write(sock.outputStream)
	} else {
		var writtenFile: File? = null
		stores.forEach {
			val requestedPath = it.resolve(storePath).absoluteFile.normalize()
			requestedPath.deleteRecursively()
			requestedPath.parentFile.mkdirs()
			if (writtenFile == null) {
				putLogger.info { "New file [$size] written for \"$storePath\" at \"${requestedPath.canonicalPath}\"" }
				var remainder = size
				while (remainder > 0) {
					val block = min(remainder.toInt(), 65536)
					requestedPath.appendBytes(sock.inputStream.readNBytes(block))
					remainder -= block
				}
				writtenFile = requestedPath
			} else {
				putLogger.fine {
					"File [$size] for \"$storePath\" copied to " +
							"\"${requestedPath.canonicalPath}\" from \"${writtenFile.canonicalPath}\""
				}
				writtenFile.copyTo(requestedPath)
			}
		}
		HTTPResponse(204, request.version, emptyMap(), "")
			.write(sock.outputStream)
	}
}