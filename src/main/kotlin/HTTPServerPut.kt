package org.bread_experts_group

import org.bread_experts_group.http.HTTPRequest
import org.bread_experts_group.http.HTTPResponse
import org.bread_experts_group.socket.failquick.FailQuickInputStream
import org.bread_experts_group.socket.failquick.FailQuickOutputStream
import java.io.File
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
	nIn: FailQuickInputStream,
	nOut: FailQuickOutputStream,
	putCredentials: Map<String, String>? = null
) {
	if (!putCredentials.isNullOrEmpty()) {
		val authorization = request.headers["Authorization"]
		if (authorization == null) {
			putLogger.warning("No user provided, unauthorized for GET")
			HTTPResponse(401, request.version, unauthorizedHeadersPut, "")
				.write(nOut)
			return
		}
		val pair = Base64.decode(authorization.substringAfter("Basic "))
			.decodeToString()
			.split(':')
		val password = putCredentials[pair[0]]
		if (password == null || password != pair[1]) {
			putLogger.warning { "\"${pair[0]}\" unauthorized for GET, not a user or wrong password" }
			HTTPResponse(403, request.version, unauthorizedHeadersPut, "")
				.write(nOut)
			return
		}
		putLogger.info { "\"${pair[0]}\" authorized." }
	}

	val size = request.headers["Content-Length"]?.toLongOrNull()
	if (size == null || size < 1) {
		putLogger.warning("No size specified for PUT.")
		HTTPResponse(400, request.version, emptyMap(), "")
			.write(nOut)
	} else {
		var writtenFile: File? = null
		HTTPResponse(100, request.version, emptyMap(), "")
			.write(nOut)
		stores.forEach {
			val requestedPath = it.resolve(storePath).absoluteFile.normalize()
			requestedPath.deleteRecursively()
			requestedPath.parentFile.mkdirs()
			if (writtenFile == null) {
				putLogger.info { "New file [$size] written for \"$storePath\" at \"${requestedPath.canonicalPath}\"" }
				var remainder = size
				while (remainder > 0) {
					val block = min(remainder, 1048576)
					requestedPath.appendBytes(nIn.readNBytes(block.toInt()))
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
			.write(nOut)
	}
}