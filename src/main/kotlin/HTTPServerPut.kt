package org.bread_experts_group.maven_microserver

import org.bread_experts_group.http.HTTPProtocolSelector
import org.bread_experts_group.http.HTTPRequest
import org.bread_experts_group.http.HTTPResponse
import org.bread_experts_group.logging.ColoredHandler
import org.bread_experts_group.static_microserver.checkAuthorization
import org.bread_experts_group.stream.LongStream
import java.nio.file.Path
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.io.path.copyTo
import kotlin.io.path.createParentDirectories
import kotlin.io.path.outputStream

private val putLogger = ColoredHandler.newLogger("Maven Server PUT")

@OptIn(ExperimentalEncodingApi::class)
fun httpServerPut(
	selector: HTTPProtocolSelector,
	stores: List<Path>,
	request: HTTPRequest,
	putCredentials: Map<String, String>? = null
) {
	if (!putCredentials.isNullOrEmpty()) {
		val failResponse = checkAuthorization(request, putCredentials)
		if (failResponse != null) {
			selector.sendResponse(failResponse)
			return
		}
	}

	if (request.headers["expect"]?.contains("100-continue") == true)
		selector.sendResponse(HTTPResponse(request, 100))

	var writtenFile: Path? = null
	val storePath = '.' + request.path.path
	stores.forEach {
		val requestedPath = it.resolve(storePath).normalize()
		if (!requestedPath.startsWith(it)) {
			selector.sendResponse(HTTPResponse(request, 404))
			return
		}

		requestedPath.createParentDirectories()
		val size = when (val data = request.data) {
			is LongStream -> data.longAvailable()
			else -> data.available().toLong()
		}
		if (writtenFile == null) {
			val fileStream = requestedPath.outputStream()
			val buffer = ByteArray(4096)
			while (true) {
				val read = request.data.read(buffer)
				if (read == -1) break
				fileStream.write(buffer, 0, read)
			}
			fileStream.flush()
			fileStream.close()
			writtenFile = requestedPath
			putLogger.info { "New file [$size] written for \"$storePath\" at \"$requestedPath\"" }
		} else {
			putLogger.fine { "File [$size] for \"$storePath\" copied to \"$requestedPath\" from \"$writtenFile\"" }
			writtenFile.copyTo(requestedPath, true)
		}
	}
	selector.sendResponse(HTTPResponse(request, 204))
}